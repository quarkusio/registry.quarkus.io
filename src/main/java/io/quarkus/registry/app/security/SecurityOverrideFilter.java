package io.quarkus.registry.app.security;

import java.io.IOException;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

/**
 * This is a very basic token authentication that check for a TOKEN that is set
 * in the Header, and if it match the configured token, set the use as the Admin user
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
@Provider
@PreMatching
public class SecurityOverrideFilter implements ContainerRequestFilter {

    @Inject
    @ConfigProperty(name = "TOKEN")
    Instance<Optional<String>> appToken;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional<String> serverToken = appToken.get();
        String token = requestContext.getHeaders().getFirst("TOKEN");
        if (serverToken.isEmpty() || Objects.equals(serverToken.orElse(null), token)) {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> "Admin";
                }

                @Override
                public boolean isUserInRole(String r) {
                    return "admin".equals(r);
                }

                @Override
                public boolean isSecure() {
                    return false;
                }

                @Override
                public String getAuthenticationScheme() {
                    return "basic";
                }
            });
        } else {
            if (!"GET".equalsIgnoreCase(requestContext.getMethod()) &&
                    !"HEAD".equalsIgnoreCase(requestContext.getMethod())) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
            }
        }
    }
}
