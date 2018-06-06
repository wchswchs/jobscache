package com.jobs.cache.strategy;

import com.jobs.cache.KeyProcessor;
import com.jobs.cache.configuration.JobsCacheProperties;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public class VersionControlStrategy {

    private Cache cache;
    private final JobsCacheProperties cacheProperties;
    private String cacheVersionKey;

    public VersionControlStrategy(JobsCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    public String clear(Object key) {
        String domain = cacheVersionKey + ":" + KeyProcessor.extractDomain2(key);
        RedisTemplate redisTemplate = (RedisTemplate) this.cache.getNativeCache();
        return (String) redisTemplate
                .opsForList()
                .leftPop(domain);
    }

    public Long write(Object key) {
        RedisTemplate redisTemplate = (RedisTemplate) this.cache.getNativeCache();
        String domain = cacheVersionKey + ":" + KeyProcessor.extractDomain(key);
        redisTemplate.expire(domain, -1, TimeUnit.SECONDS);
        return redisTemplate.opsForList()
                            .leftPush(domain, String.valueOf(KeyProcessor.trimKey(key)));
    }

    public Object determine(Object key, boolean hasFirstLevel) {
        String domain = "";

        if (hasFirstLevel == false) {
            domain = cacheVersionKey + ":" + KeyProcessor.extractDomain(key);
        } else {
            domain = cacheVersionKey + ":" + KeyProcessor.extractAbsolutePathDomain(key);
        }
        RedisTemplate redisTemplate = (RedisTemplate) this.cache.getNativeCache();
        String lastVersion = (String) redisTemplate
                                        .opsForList()
                                        .index(domain, 0);
        if (lastVersion != null) {
            return lastVersion + ":" + KeyProcessor.extractKey(key);
        }
        return key;
    }

    public synchronized Cache getCache() {
        return cache;
    }

    public synchronized void setCache(Cache cache) {
        this.cache = cache;
    }

    public String getCacheVersionKey() {
        return cacheVersionKey;
    }

    public void setCacheVersionKey() {
        this.cacheVersionKey = cacheProperties.getCacheName() + ":" + "cache_version_domains";
    }

}
