package cl.proyecto.gestion.planificacion.dependencia;

import cl.proyecto.gestion.planificacion.motor.TipoDependencia;

public record DependenciaResponse(Long id, Long predecesoraId, Long sucesoraId, TipoDependencia tipo, int desfaseDias) {

    public static DependenciaResponse de(Dependencia d) {
        return new DependenciaResponse(d.id, d.predecesoraId, d.sucesoraId, d.tipo, d.desfaseDias);
    }
}
