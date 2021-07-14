package io.quarkus.registry.app.maven.cache;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Interceptor that fire a event to notify that the cache should be cleared
 */
@Interceptor
@MavenCacheClear
public class MavenCacheClearInterceptor {
    
    @Inject
    MavenCache mavenCache;
    
    @AroundInvoke
    public Object methodEntry(InvocationContext ctx) throws Exception {
        
        Object returnObject = ctx.proceed();
        mavenCache.clear();
        return returnObject;
    }
    
    
}
