package cl.proyecto.gestion.cambio;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de etapas de una solicitud (C-04). Contrato en docs/api/cambios.md. */
@RestController
@RequestMapping("/api/cambios/{solicitudId}/etapas")
public class CambioEtapaController {

    private final CambioEtapaService servicio;

    public CambioEtapaController(CambioEtapaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public EtapasCambioResponse listar(@PathVariable Long solicitudId) {
        return EtapasCambioResponse.de(servicio.listar(solicitudId));
    }

    @PatchMapping("/{numero}")
    public CambioEtapaResponse cambiar(@PathVariable Long solicitudId,
                                       @PathVariable short numero,
                                       @Valid @RequestBody CambioEtapaRequest request) {
        return CambioEtapaResponse.de(servicio.cambiarEstado(solicitudId, numero, request.estado()));
    }
}
