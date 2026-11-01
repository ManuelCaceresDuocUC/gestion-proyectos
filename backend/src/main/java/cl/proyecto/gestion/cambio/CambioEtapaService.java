package cl.proyecto.gestion.cambio;

import cl.proyecto.gestion.common.ApiException;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-04: las 10 etapas, su estado y el avance. No expone el historial (C-05)
 * ni valida transiciones de la solicitud (C-08).
 */
@Service
public class CambioEtapaService {

    private final CambioEtapaRepository etapas;
    private final CambioHistorialRepository historial;
    private final SolicitudCambioRepository solicitudes;
    private final ProyectoService proyectos;

    public CambioEtapaService(CambioEtapaRepository etapas, CambioHistorialRepository historial,
                              SolicitudCambioRepository solicitudes, ProyectoService proyectos) {
        this.etapas = etapas;
        this.historial = historial;
        this.solicitudes = solicitudes;
        this.proyectos = proyectos;
    }

    @Transactional(readOnly = true)
    public List<CambioEtapa> listar(Long solicitudId) {
        solicitud(solicitudId);
        return etapas.findBySolicitudIdOrderByNumeroAsc(solicitudId);
    }

    /** Etapas de varias solicitudes, para no consultar una por una al listar. */
    @Transactional(readOnly = true)
    public Map<Long, List<CambioEtapa>> porSolicitudes(Collection<Long> solicitudIds) {
        if (solicitudIds.isEmpty()) {
            return Map.of();
        }
        return etapas.findBySolicitudIdIn(solicitudIds).stream()
            .collect(Collectors.groupingBy(e -> e.solicitudId));
    }

    /** Crea las 10 en PENDIENTE. Si ya existen, las devuelve sin duplicar. */
    @Transactional
    public List<CambioEtapa> generar(Long solicitudId) {
        solicitud(solicitudId);
        List<CambioEtapa> existentes = etapas.findBySolicitudIdOrderByNumeroAsc(solicitudId);
        if (!existentes.isEmpty()) {
            return existentes;
        }
        List<CambioEtapa> creadas = new ArrayList<>();
        for (EtapasCambio def : EtapasCambio.values()) {
            CambioEtapa e = new CambioEtapa();
            e.solicitudId = solicitudId;
            e.numero = (short) def.numero;
            e.nombre = def.nombre;
            e.estado = EstadoEtapa.PENDIENTE;
            creadas.add(etapas.save(e));
        }
        return creadas;
    }

    @Transactional
    public CambioEtapa cambiarEstado(Long solicitudId, short numero, EstadoEtapa estado) {
        if (estado == null) {
            throw ApiException.invalido("VALIDACION", "El estado de la etapa es obligatorio.");
        }
        SolicitudCambio s = solicitud(solicitudId);
        proyectos.obtenerEditable(s.proyectoId);
        CambioEtapa e = etapas.findBySolicitudIdAndNumero(solicitudId, numero)
            .orElseThrow(() -> ApiException.noEncontrado(
                "No existe la etapa " + numero + " de la solicitud " + solicitudId + "."));
        if (e.estado == estado) {
            return e;
        }
        EstadoEtapa anterior = e.estado;
        e.estado = estado;
        etapas.save(e);
        registrar(solicitudId, numero, anterior, estado);
        return e;
    }

    static int etapasCompletas(List<CambioEtapa> lista) {
        return (int) lista.stream().filter(e -> e.estado == EstadoEtapa.COMPLETADO).count();
    }

    static int etapasBloqueadas(List<CambioEtapa> lista) {
        return (int) lista.stream().filter(e -> e.estado == EstadoEtapa.BLOQUEADO).count();
    }

    /** Porcentaje 0–100: (completas / 10) × 100. NO_APLICA no suma. */
    static int avancePorcentaje(List<CambioEtapa> lista) {
        return etapasCompletas(lista) * 10;
    }

    private SolicitudCambio solicitud(Long id) {
        return solicitudes.findById(id)
            .orElseThrow(() -> ApiException.noEncontrado("No existe la solicitud de cambio " + id + "."));
    }

    private void registrar(Long solicitudId, short numero, EstadoEtapa anterior, EstadoEtapa nuevo) {
        CambioHistorial h = new CambioHistorial();
        h.solicitudId = solicitudId;
        h.campo = "etapa." + numero + ".estado";
        h.valorAnterior = anterior.name();
        h.valorNuevo = nuevo.name();
        historial.save(h);
    }
}
