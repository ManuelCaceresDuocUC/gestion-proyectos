package cl.proyecto.gestion.cambio;

import cl.proyecto.gestion.auth.SeguridadActual;
import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** C-05: evidencias (descripción + URL) de una solicitud. */
@Service
public class EvidenciaService {

    private final EvidenciaRepository evidencias;
    private final SolicitudCambioRepository solicitudes;
    private final ProyectoService proyectos;
    private final SeguridadActual seguridad;

    public EvidenciaService(EvidenciaRepository evidencias, SolicitudCambioRepository solicitudes,
                            ProyectoService proyectos, SeguridadActual seguridad) {
        this.evidencias = evidencias;
        this.solicitudes = solicitudes;
        this.proyectos = proyectos;
        this.seguridad = seguridad;
    }

    @Transactional(readOnly = true)
    public List<Evidencia> listar(Long solicitudId) {
        solicitud(solicitudId);
        return evidencias.findBySolicitudIdOrderByCreadoEnAsc(solicitudId);
    }

    @Transactional
    public Evidencia crear(Long solicitudId, EvidenciaRequest request) {
        SolicitudCambio s = solicitud(solicitudId);
        proyectos.obtenerEditable(s.proyectoId);
        Evidencia e = new Evidencia();
        e.solicitudId = solicitudId;
        e.descripcion = request.descripcion().trim();
        e.url = urlOpcional(request.url());
        e.creadoPor = seguridad.usuarioId();
        return evidencias.save(e);
    }

    @Transactional
    public void eliminar(Long solicitudId, Long evidenciaId) {
        SolicitudCambio s = solicitud(solicitudId);
        proyectos.obtenerEditable(s.proyectoId);
        Evidencia e = evidencias.findById(evidenciaId)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la evidencia " + evidenciaId + "."));
        if (!solicitudId.equals(e.solicitudId)) {
            throw ApiException.noEncontrado("No existe la evidencia " + evidenciaId + ".");
        }
        evidencias.delete(e);
    }

    private SolicitudCambio solicitud(Long id) {
        return solicitudes.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la solicitud de cambio " + id + "."));
    }

    private static String urlOpcional(String url) {
        if (url == null) {
            return null;
        }
        String t = url.trim();
        return t.isEmpty() ? null : t;
    }
}
