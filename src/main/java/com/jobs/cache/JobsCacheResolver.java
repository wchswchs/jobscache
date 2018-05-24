package com.jobs.cache;

import com.jobs.cache.configuration.JobsCacheProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class JobsCacheResolver implements CacheResolver, InitializingBean {

    private CacheManager cacheManager;
    private JobsCacheProperties cacheProperties;


    public JobsCacheResolver() {
    }

    public JobsCacheResolver(CacheManager cacheManager, JobsCacheProperties cacheProperties) {
        this.cacheManager = cacheManager;
        this.cacheProperties = cacheProperties;
    }


    /**
     * Set the {@link CacheManager} that this instance should use.
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Return the {@link CacheManager} that this instance use.
     */
    public CacheManager getCacheManager() {
        return this.cacheManager;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.cacheManager, "CacheManager must not be null");
    }


    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Collection<String> cacheNames = getCacheNames(context);
        if (cacheNames == null) {
            return Collections.emptyList();
        } else {
            Collection<Cache> result = new ArrayList<Cache>();
            for (String cacheName : cacheNames) {
                if (((JobsCacheOperation) context.getOperation()).getExpireTime() != 0) {
                    ((RedisCacheManager) this.cacheManager).setDefaultExpiration(((JobsCacheOperation) context.getOperation()).getExpireTime());
                } else {
                    ((RedisCacheManager) this.cacheManager).setDefaultExpiration(cacheProperties.getDefaultExpiredTime());
                }
                Cache cache = this.cacheManager.getCache(cacheName);
                if (cache == null) {
                    throw new IllegalArgumentException("Cannot find cache named '" + cacheName + "' for " + context.getOperation());
                }
                result.add(cache);
            }
            return result;
        }
    }

    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        return context.getOperation().getCacheNames();
    }

}
