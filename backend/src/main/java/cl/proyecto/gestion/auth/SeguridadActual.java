package cl.proyecto.gestion.auth;

import cl.proyecto.gestion.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Lee el rol del JWT (o de @WithMockUser en tests). No expone email ni token. */
@Component
public class SeguridadActual {

    public boolean esAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority g : a.getAuthorities()) {
            if ("ROLE_ADMIN".equals(g.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public void exigirAdmin(String mensaje) {
        if (!esAdmin()) {
            throw ApiException.prohibido("PROHIBIDO", mensaje);
        }
    }

    /** Id del JWT (`sub`). En tests con {@code @WithMockUser} no hay id y queda nulo. */
    public Long usuarioId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            return null;
        }
        Object p = a.getPrincipal();
        if (p instanceof Long id) {
            return id;
        }
        if (p instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
