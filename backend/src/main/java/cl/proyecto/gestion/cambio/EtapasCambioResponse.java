package cl.proyecto.gestion.cambio;

import java.util.List;

/** Las 10 etapas más los calculados (no persistidos). */
public record EtapasCambioResponse(
    List<CambioEtapaResponse> etapas,
    int etapasCompletas,
    int avance,
    int etapasBloqueadas) {

    public static EtapasCambioResponse de(List<CambioEtapa> lista) {
        return new EtapasCambioResponse(
            lista.stream().map(CambioEtapaResponse::de).toList(),
            CambioEtapaService.etapasCompletas(lista),
            CambioEtapaService.avancePorcentaje(lista),
            CambioEtapaService.etapasBloqueadas(lista));
    }
}
