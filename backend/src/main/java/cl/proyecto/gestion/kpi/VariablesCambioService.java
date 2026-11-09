package cl.proyecto.gestion.kpi;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.CambioEtapaRepository;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.cambio.SolicitudCambioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee las solicitudes y etapas guardadas de un proyecto y arma las variables KC.
 * No evalúa fórmulas ni escribe histórico: eso queda para el paso de KPI.
 */
@Service
public class VariablesCambioService {

    private final SolicitudCambioRepository solicitudes;
    private final CambioEtapaRepository etapas;

    public VariablesCambioService(SolicitudCambioRepository solicitudes, CambioEtapaRepository etapas) {
        this.solicitudes = solicitudes;
        this.etapas = etapas;
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> deProyecto(Long proyectoId) {
        return deProyecto(proyectoId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> deProyecto(Long proyectoId, LocalDate hoy) {
        if (proyectoId == null) {
            throw new IllegalArgumentException("El proyecto es obligatorio.");
        }
        List<SolicitudCambio> lista = solicitudes.findByProyectoId(proyectoId);
        List<Long> ids = lista.stream().map(s -> s.id).toList();
        Map<Long, List<CambioEtapa>> porSolicitud = ids.isEmpty()
                ? Map.of()
                : etapas.findBySolicitudIdIn(ids).stream()
                        .collect(Collectors.groupingBy(e -> e.solicitudId));
        return VariablesCambio.calcular(lista, porSolicitud, hoy);
    }
}
