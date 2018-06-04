package com.jobs.cache;

import com.jobs.cache.annotations.JobsCacheEvict;
import com.jobs.cache.annotations.JobsCachePut;
import com.jobs.cache.annotations.JobsCacheable;
import com.jobs.cache.configuration.JobsCacheProperties;
import com.jobs.cache.operation.JobsCacheEvictOperation;
import com.jobs.cache.operation.JobsCachePutOperation;
import com.jobs.cache.operation.JobsCacheableOperation;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class JobsCacheAnnotationParser implements CacheAnnotationParser, Serializable {

    private JobsCacheProperties cacheProperties;

    public JobsCacheAnnotationParser() {
    }

    public JobsCacheAnnotationParser(JobsCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
        JobsCacheAnnotationParser.DefaultCacheConfig defaultConfig = this.getDefaultCacheConfig(type);
        return this.parseCacheAnnotations(defaultConfig, type);
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Method method) {
        JobsCacheAnnotationParser.DefaultCacheConfig defaultConfig = this.getDefaultCacheConfig(method.getDeclaringClass());
        return this.parseCacheAnnotations(defaultConfig, method);
    }

    static class DefaultCacheConfig {
        private final String[] cacheNames;
        private final String keyGenerator;
        private final String cacheManager;
        private final String cacheResolver;

        public DefaultCacheConfig() {
            this((String[]) null, (String) null, (String) null, (String) null);
        }

        public DefaultCacheConfig(String[] cacheNames, String keyGenerator, String cacheManager, String cacheResolver) {
            this.cacheNames = cacheNames;
            this.keyGenerator = keyGenerator;
            this.cacheManager = cacheManager;
            this.cacheResolver = cacheResolver;
        }

        public void applyDefault(CacheOperation.Builder builder) {
            if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
                builder.setCacheNames(this.cacheNames);
            }

            if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) && StringUtils.hasText(this.keyGenerator)) {
                builder.setKeyGenerator(this.keyGenerator);
            }

            if (!StringUtils.hasText(builder.getCacheManager()) && !StringUtils.hasText(builder.getCacheResolver())) {
                if (StringUtils.hasText(this.cacheResolver)) {
                    builder.setCacheResolver(this.cacheResolver);
                } else if (StringUtils.hasText(this.cacheManager)) {
                    builder.setCacheManager(this.cacheManager);
                }
            }

        }
    }

    JobsCacheAnnotationParser.DefaultCacheConfig getDefaultCacheConfig(Class<?> target) {
        CacheConfig annotation = (CacheConfig) AnnotatedElementUtils.getMergedAnnotation(target, CacheConfig.class);
        return annotation != null ? new JobsCacheAnnotationParser.DefaultCacheConfig(annotation.cacheNames(), annotation.keyGenerator(), annotation.cacheManager(), annotation.cacheResolver()) : new JobsCacheAnnotationParser.DefaultCacheConfig();
    }

    public Collection<CacheOperation> parseCacheAnnotations(JobsCacheAnnotationParser.DefaultCacheConfig cachingConfig, AnnotatedElement ae) {
        Collection<CacheOperation> ops = null;
        Collection<JobsCacheable> cacheables = AnnotatedElementUtils.getAllMergedAnnotations(ae, JobsCacheable.class);
        if (!cacheables.isEmpty()) {
            ops = this.lazyInit(ops);
            Iterator var5 = cacheables.iterator();

            while (var5.hasNext()) {
                JobsCacheable cacheable = (JobsCacheable) var5.next();
                ops.add(this.parseCacheableAnnotation(ae, cachingConfig, cacheable));
            }
        }

        Collection<JobsCacheEvict> evicts = AnnotatedElementUtils.getAllMergedAnnotations(ae, JobsCacheEvict.class);
        if (!evicts.isEmpty()) {
            ops = this.lazyInit(ops);
            Iterator var12 = evicts.iterator();

            while (var12.hasNext()) {
                JobsCacheEvict evict = (JobsCacheEvict) var12.next();
                ops.add(this.parseEvictAnnotation(ae, cachingConfig, evict));
            }
        }
        Collection<JobsCachePut> puts = AnnotatedElementUtils.getAllMergedAnnotations(ae, JobsCachePut.class);
        if (!puts.isEmpty()) {
            ops = lazyInit(ops);
            for (JobsCachePut put : puts) {
                ops.add(parsePutAnnotation(ae, cachingConfig, put));
            }
        }
        return ops;
    }

    public <T extends Annotation> Collection<CacheOperation> lazyInit(Collection<CacheOperation> ops) {
        return (Collection) (ops != null ? ops : new ArrayList(1));
    }


    public void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
        if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" + ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. These attributes are mutually exclusive: either set the SpEL expression used tocompute the key at runtime or set the name of the KeyGenerator bean to use.");
        } else if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" + ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. These attributes are mutually exclusive: the cache manager is used to configure adefault cache resolver if none is set. If a cache resolver is set, the cache managerwon't be used.");
        }
    }

    JobsCacheableOperation parseCacheableAnnotation(AnnotatedElement ae, JobsCacheAnnotationParser.DefaultCacheConfig defaultConfig, JobsCacheable cacheable) {
        JobsCacheableOperation.Builder builder = new JobsCacheableOperation.Builder();
        builder.setName(ae.toString());
        builder.setCondition(cacheable.condition());
        builder.setKey(cacheable.key());
        builder.setDomain(cacheable.domain());
//        builder.setDomainKey(cacheable.domainKey());
        builder.setExpireTime(cacheable.expireTime());
        builder.setCacheNames(cacheProperties.getCacheName());
        defaultConfig.applyDefault(builder);
        JobsCacheableOperation op = builder.build();
        this.validateCacheOperation(ae, op);
        return op;
    }

    JobsCacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, JobsCacheAnnotationParser.DefaultCacheConfig defaultConfig, JobsCacheEvict cacheEvict) {
        JobsCacheEvictOperation.Builder builder = new JobsCacheEvictOperation.Builder();
        builder.setName(ae.toString());
        builder.setCondition(cacheEvict.condition());
        builder.setKey(cacheEvict.key());
        builder.setDomain(cacheEvict.domain());
//        builder.setDomainKey(cacheEvict.domainKey());
        builder.setCacheNames(cacheProperties.getCacheName());
        defaultConfig.applyDefault(builder);
        JobsCacheEvictOperation op = builder.build();
        this.validateCacheOperation(ae, op);
        return op;
    }

    CacheOperation parsePutAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, JobsCachePut cachePut) {
        JobsCachePutOperation.Builder builder = new JobsCachePutOperation.Builder();

        builder.setName(ae.toString());
        builder.setCacheNames(cacheProperties.getCacheName());
        builder.setCondition(cachePut.condition());
        builder.setUnless(cachePut.unless());
        builder.setDomain(cachePut.domain());
        builder.setKey(cachePut.key());
        builder.setKeyGenerator(cachePut.keyGenerator());
        builder.setCacheManager(cachePut.cacheManager());
        builder.setCacheResolver(cachePut.cacheResolver());

        defaultConfig.applyDefault(builder);
        JobsCachePutOperation op = builder.build();
        validateCacheOperation(ae, op);
        return op;
    }

}
