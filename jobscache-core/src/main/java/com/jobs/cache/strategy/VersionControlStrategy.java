package com.jobs.cache.strategy;

import com.jobs.cache.KeyProcessor;
import com.jobs.cache.configuration.JobsCacheProperties;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public class VersionControlStrategy {

    private Cache cache;
    private JobsCacheProperties cacheProperties;
    private String cacheVersionKey;

    public VersionControlStrategy() {
    }

    public VersionControlStrategy(Cache cache) {
        this.cache = cache;
    }

    public boolean clear(Object key) {
        String domain = cacheVersionKey + ":" + KeyProcessor.extractDomain2(key);
        RedisTemplate redisTemplate = ((RedisTemplate) this.cache.getNativeCache());
        String lastVersion = (String) redisTemplate
                .opsForList()
                .leftPop(domain);
        if (lastVersion != null) {
            return true;
        }
        return false;
    }

    public Long write(Object key) {
        RedisTemplate redisTemplate = ((RedisTemplate) this.cache.getNativeCache());
        String domain = cacheVersionKey + ":" + KeyProcessor.extractDomain(key);
        redisTemplate.expire(domain, -1, TimeUnit.SECONDS);
        return redisTemplate.opsForList()
                            .leftPush(domain, String.valueOf(KeyProcessor.trimKey(key)));
    }

    public Object determine(Object key) {
        String domain = cacheVersionKey + ":" + KeyProcessor.extractDomain(key);
        RedisTemplate redisTemplate = ((RedisTemplate) this.cache.getNativeCache());
        String lastVersion = (String) redisTemplate
                                        .opsForList()
                                        .index(domain, 0);
        if (lastVersion != null) {
            return lastVersion + ":" + KeyProcessor.extractKey(key);
        }
        return "";
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void setCacheProperties(JobsCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    public String getCacheVersionKey() {
        return cacheVersionKey;
    }

    public void setCacheVersionKey() {
        this.cacheVersionKey = cacheProperties.getCacheName() + ":" + "cache_version_domains";
    }

}
