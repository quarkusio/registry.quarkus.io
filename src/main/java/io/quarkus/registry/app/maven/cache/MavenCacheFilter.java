package io.quarkus.registry.app.maven.cache;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.EntityTag;
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
    
    @ConfigProperty(name = "maven.cache.header.cache-control", defaultValue = "no-cache") // Check for etag everytime
    String headerCacheControl;
    
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if(isMavenGetRequest(containerRequestContext)){
            String path = containerRequestContext.getUriInfo().getPath();
            MavenResponse mavenResponse = mavenCache.getCache().getIfPresent(path);
            if(mavenResponse!=null){
                
                // Check if we can response with Not Modified
                boolean notModified = notModified(containerRequestContext, mavenResponse);
                    
                if(notModified){
                    containerRequestContext.abortWith(toNotModifiedResponse(mavenResponse));
                }else {
                    log.warn("Serving [" + path + "] from cache");
                    containerRequestContext.abortWith(toResponse(mavenResponse));
                }    
            }
            // Else fall through to the actual endpoint
        }
    }
    
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        if(isMavenGetRequest(containerRequestContext) && !isCachedResponse(containerResponseContext) && containerResponseContext.getStatus()==200){ 
            String path = containerRequestContext.getUriInfo().getPath();
            log.warn("Caching [" + path + "]");
            mavenCache.getCache().put(path, toMavenResponse(containerResponseContext));
        }
    }
    
    private boolean notModified(ContainerRequestContext containerRequestContext, MavenResponse cachedResponse){
        // Here we first check the request header for If-None-Match (etag) or If-Modified-Since (last-modified)
        
        // First Check Etag
        String givenEtag = containerRequestContext.getHeaderString(IF_NONE_MATCH);
        String cachedEtag = cachedResponse.getHeaders().get(ETAG);
        if(givenEtag!=null && cachedEtag!=null && givenEtag.equals(cachedEtag)){
            return true;
        }
        
        // Secondly Check Last Modified    
        String givenLastModified = containerRequestContext.getHeaderString(IF_MODIFIED_SINCE);
        String cachedLastModified = cachedResponse.getHeaders().get(LAST_MODIFIED);
        if(givenLastModified!=null && cachedLastModified!=null && givenLastModified.equals(cachedLastModified)){
            return true;
        }
        
        return false;
    }
    
    private MavenResponse toMavenResponse(ContainerResponseContext containerResponseContext){
        MavenResponse mavenResponse = new MavenResponse(containerResponseContext.getStatus(),containerResponseContext.getEntity());
        
        // We add some cache related headers here for when using via a CDN etc.
        // see https://www.keycdn.com/blog/http-cache-headers#no-cache-and-no-store

        // Cache control
        containerResponseContext.getHeaders().add(CACHE_CONTROL, headerCacheControl);
        
        // Etag (to be compared with If-None-Match)
        // We try and set this based on the actual content. This means that if the content did not change (even if we recreate the cache)
        // the etag will be the same.
        EntityTag etag = containerResponseContext.getEntityTag();
        if(etag==null){
            // etag - We create a hash from the Response if this is a String, so even if the cache clear, if the content is the same, the etag will also be
            if(containerResponseContext.getEntityClass().equals(String.class)){
                String content = (String)containerResponseContext.getEntity();
                String uuid = UUID.nameUUIDFromBytes(content.getBytes()).toString();
                containerResponseContext.getHeaders().put(ETAG, List.of(uuid));
            // etag - Else we just use a UUID, this will change everytime our cache invalidate
            }else{
                String randomEtag = UUID.randomUUID().toString();
                containerResponseContext.getHeaders().put(ETAG, List.of(randomEtag));
            }
        }
        
        // Last-Modified (to be compared with If-Modified-Since)
        // For now we set the date when we cached this, but a better way would be to get a date from when this was persisted (changed)
        LocalDateTime cachedAt = LocalDateTime.now(ZoneOffset.UTC);
        containerResponseContext.getHeaders().add(LAST_MODIFIED, cachedAt.format(lastModifiedDateFormatter));
        
        // Now we should have all the headers we want to cache
        Set<String> headerKeys = containerResponseContext.getStringHeaders().keySet();
        for(String key : headerKeys){
            mavenResponse.addHeader(key, containerResponseContext.getHeaderString(key));
        }
        
        return mavenResponse;
    }

    private Response toNotModifiedResponse(MavenResponse mavenResponse){
        Response.ResponseBuilder responseBuilder = Response.status(NOT_MODIFIED_STATUS);
        responseBuilder = responseBuilder.header(VIA, VIA_CACHE);
        for(Map.Entry<String, String> header : mavenResponse.getHeaders().entrySet()){
            responseBuilder.header(header.getKey(), header.getValue());
        }
        return responseBuilder.build();
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
    private static final String ETAG = "etag";
    private static final String CACHE_CONTROL = "cache-control";
    private static final String LAST_MODIFIED = "last-modified";
    private static final String IF_NONE_MATCH = "if-none-match";
    private static final String IF_MODIFIED_SINCE = "if-modified-since";
    
    private static final int NOT_MODIFIED_STATUS = 304;
    
    private static final DateTimeFormatter lastModifiedDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss' GMT'");
}
