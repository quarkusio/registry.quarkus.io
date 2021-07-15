package io.quarkus.registry.app.maven.cache;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Serve the response from cache if available
 */
@Provider
public class MavenCacheFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(MavenCacheFilter.class);
    
    @Inject MavenCache mavenCache;
    
    @ConfigProperty(name = "maven.cache.path", defaultValue = "/maven")
    String pathToCache;
    
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if(isMavenGetRequest(containerRequestContext)){
            String path = containerRequestContext.getUriInfo().getPath();
            MavenResponse mavenResponse = mavenCache.getCache().getIfPresent(path);
            if(mavenResponse!=null){
                log.debug("Serving [" + path + "] from cache");
                containerRequestContext.abortWith(toResponse(mavenResponse));
            }
        }
    }
    
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        if(isMavenGetRequest(containerRequestContext) && !isCachedResponse(containerResponseContext) && containerResponseContext.getStatus()==200){
            String path = containerRequestContext.getUriInfo().getPath();
            log.debug("Caching [" + path + "]");
            mavenCache.getCache().put(path, toMavenResponse(containerResponseContext));
        }
    }
    
    private MavenResponse toMavenResponse(ContainerResponseContext containerResponseContext){
        MavenResponse mavenResponse = new MavenResponse(containerResponseContext.getStatus(),containerResponseContext.getEntity());
        Set<String> headerKeys = containerResponseContext.getHeaders().keySet();
        for(String key : headerKeys){
            mavenResponse.addHeader(key, containerResponseContext.getHeaderString(key));
        }
        return mavenResponse;
    }

    private Response toResponse(MavenResponse mavenResponse){
        Response.ResponseBuilder responseBuilder = Response.status(mavenResponse.getStatus()).entity(mavenResponse.getResponse());
        responseBuilder = responseBuilder.header(VIA, VIA_CACHE);
        for(Map.Entry<String, String> header : mavenResponse.getHeaders().entrySet()){
            responseBuilder.header(header.getKey(), header.getValue());
        }
        return responseBuilder.build();
    }
 
    private boolean isMavenGetRequest(ContainerRequestContext containerRequestContext){
        String path = containerRequestContext.getUriInfo().getPath();
        return path.startsWith(pathToCache) && containerRequestContext.getMethod().equalsIgnoreCase(GET);
    }
    
    private boolean isCachedResponse(ContainerResponseContext containerResponseContext){
        return containerResponseContext.getHeaderString(VIA) != null && containerResponseContext.getHeaderString(VIA).equals(VIA_CACHE);
    }
    
    private static final String GET = "GET";
    private static final String VIA = "Via";
    private static final String VIA_CACHE = "register.quarkus.io maven-cache";
    
}
