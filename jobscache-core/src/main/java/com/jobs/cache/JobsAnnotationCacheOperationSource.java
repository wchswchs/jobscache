package com.jobs.cache;

import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.*;

public class JobsAnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource {

    private final boolean publicMethodsOnly;

    private final Set<CacheAnnotationParser> annotationParsers;

    /**
     * Create a default AnnotationCacheOperationSource, supporting public methods
     * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
     */
    public JobsAnnotationCacheOperationSource() {
        this(true);
    }

    /**
     * Create a default {@code AnnotationCacheOperationSource}, supporting public methods
     * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
     * @param publicMethodsOnly whether to support only annotated public methods
     * typically for use with proxy-based AOP), or protected/private methods as well
     * (typically used with AspectJ class weaving)
     */
    public JobsAnnotationCacheOperationSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        this.annotationParsers = new LinkedHashSet<CacheAnnotationParser>(1);
        this.annotationParsers.add(new JobsCacheAnnotationParser());
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParser the CacheAnnotationParser to use
     */
    public JobsAnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
        this.publicMethodsOnly = true;
        Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
        this.annotationParsers = Collections.singleton(annotationParser);
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParsers the CacheAnnotationParser to use
     */
    public JobsAnnotationCacheOperationSource(CacheAnnotationParser... annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        Set<CacheAnnotationParser> parsers = new LinkedHashSet<CacheAnnotationParser>(annotationParsers.length);
        Collections.addAll(parsers, annotationParsers);
        this.annotationParsers = parsers;
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParsers the CacheAnnotationParser to use
     */
    public JobsAnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        this.annotationParsers = annotationParsers;
    }


    @Override
    protected Collection<CacheOperation> findCacheOperations(final Class<?> clazz) {
        return determineCacheOperations(new com.jobs.cache.JobsAnnotationCacheOperationSource.CacheOperationProvider() {
            @Override
            public Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser) {
                return parser.parseCacheAnnotations(clazz);
            }
        });

    }

    @Override
    protected Collection<CacheOperation> findCacheOperations(final Method method) {
        return determineCacheOperations(new com.jobs.cache.JobsAnnotationCacheOperationSource.CacheOperationProvider() {
            @Override
            public Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser) {
                return parser.parseCacheAnnotations(method);
            }
        });
    }

    /**
     * Determine the cache operation(s) for the given {@link AnnotationCacheOperationSource.CacheOperationProvider}.
     * <p>This implementation delegates to configured
     * {@link CacheAnnotationParser}s for parsing known annotations into
     * Spring's metadata attribute class.
     * <p>Can be overridden to support custom annotations that carry
     * caching metadata.
     * @param provider the cache operation provider to use
     * @return the configured caching operations, or {@code null} if none found
     */
    protected Collection<CacheOperation> determineCacheOperations(com.jobs.cache.JobsAnnotationCacheOperationSource.CacheOperationProvider provider) {
        Collection<CacheOperation> ops = null;
        for (CacheAnnotationParser annotationParser : this.annotationParsers) {
            Collection<CacheOperation> annOps = provider.getCacheOperations(annotationParser);
            if (annOps != null) {
                if (ops == null) {
                    ops = new ArrayList<CacheOperation>();
                }
                ops.addAll(annOps);
            }
        }
        return ops;
    }

    /**
     * By default, only public methods can be made cacheable.
     */
    @Override
    protected boolean allowPublicMethodsOnly() {
        return this.publicMethodsOnly;
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationCacheOperationSource)) {
            return false;
        }
        com.jobs.cache.JobsAnnotationCacheOperationSource otherCos = (com.jobs.cache.JobsAnnotationCacheOperationSource) other;
        return (this.annotationParsers.equals(otherCos.annotationParsers) &&
                this.publicMethodsOnly == otherCos.publicMethodsOnly);
    }

    @Override
    public int hashCode() {
        return this.annotationParsers.hashCode();
    }


    /**
     * Callback interface providing {@link CacheOperation} instance(s) based on
     * a given {@link CacheAnnotationParser}.
     */
    protected interface CacheOperationProvider {

        /**
         * Return the {@link CacheOperation} instance(s) provided by the specified parser.
         * @param parser the parser to use
         * @return the cache operations, or {@code null} if none found
         */
        Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
    }

}

