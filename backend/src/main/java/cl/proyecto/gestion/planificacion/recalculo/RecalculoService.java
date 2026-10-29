package cl.proyecto.gestion.planificacion.recalculo;

import cl.proyecto.gestion.common.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto único de recálculo. Todo servicio que modifique duraciones, dependencias, asignaciones,
 * avance o aplique una solicitud aprobada debe terminar llamando a recalcular(proyectoId).
 * Spring inyecta los pasos ordenados por @Order. Issue A-12.
 */
@Service
public class RecalculoService {
    private static final Logger log = LoggerFactory.getLogger(RecalculoService.class);
    private final List<PasoRecalculo> pasos;

    public RecalculoService(List<PasoRecalculo> pasos) {
        this.pasos = pasos;
    }

    @Transactional
    public void recalcular(Long proyectoId) {
        for (PasoRecalculo paso : pasos) {
            log.debug("Recalculo proyecto {}: {}", proyectoId, paso.getClass().getSimpleName());
            try {
                paso.ejecutar(proyectoId);
            } catch (ApiException e) {
                throw e; // ya trae código y mensaje legibles; la transacción se revierte completa
            } catch (RuntimeException e) {
                log.error("Falló el recálculo del proyecto {} en {}", proyectoId, paso.getClass().getSimpleName(), e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RECALCULO_FALLIDO",
                    "No se pudo recalcular el proyecto (paso " + paso.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }
    }
}
