package cl.proyecto.gestion.tablero;

import cl.proyecto.gestion.cambio.CambioEtapa;
import cl.proyecto.gestion.cambio.CambioEtapaRepository;
import cl.proyecto.gestion.cambio.SolicitudCambio;
import cl.proyecto.gestion.cambio.SolicitudCambioRepository;
import cl.proyecto.gestion.kpi.CategoriaKpi;
import cl.proyecto.gestion.kpi.KpiEstadoResponse;
import cl.proyecto.gestion.kpi.KpiService;
import cl.proyecto.gestion.proyecto.ProyectoService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-12: arma el tablero de cambio leyendo solicitudes, etapas e histórico KPI.
 * No llama al recálculo ni inserta definiciones.
 */
@Service
public class TableroCambioService {

    private final ProyectoService proyectos;
    private final SolicitudCambioRepository solicitudes;
    private final CambioEtapaRepository etapas;
    private final KpiService kpi;

    public TableroCambioService(
            ProyectoService proyectos,
            SolicitudCambioRepository solicitudes,
            CambioEtapaRepository etapas,
            KpiService kpi) {
        this.proyectos = proyectos;
        this.solicitudes = solicitudes;
        this.etapas = etapas;
        this.kpi = kpi;
    }

    @Transactional(readOnly = true)
    public TableroCambioResponse deProyecto(Long proyectoId) {
        return deProyecto(proyectoId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public TableroCambioResponse deProyecto(Long proyectoId, LocalDate hoy) {
        proyectos.obtener(proyectoId);
        List<SolicitudCambio> lista = solicitudes.findByProyectoId(proyectoId);
        List<Long> ids = lista.stream().map(s -> s.id).toList();
        Map<Long, List<CambioEtapa>> porSolicitud = ids.isEmpty()
                ? Map.of()
                : etapas.findBySolicitudIdIn(ids).stream()
                        .collect(Collectors.groupingBy(e -> e.solicitudId));
        List<KpiEstadoResponse> kpiCambio = kpi.listar(proyectoId).stream()
                .filter(fila -> CategoriaKpi.CAMBIO.name().equals(fila.categoria()))
                .toList();
        return TableroCambio.resumir(proyectoId, lista, porSolicitud, hoy, kpiCambio);
    }
}
