package com.jobs.cache.configuration;

import com.jobs.cache.JobsAnnotationCacheOperationSource;
import com.jobs.cache.JobsCacheResolver;
import com.jobs.cache.JobsCacheAnnotationParser;
import com.jobs.cache.JobsCacheInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(JobsCacheProperties.class)
public class JobsCacheConfiguration {

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private JobsCacheProperties cacheProperties;

    @Bean(name = CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)
    public BeanFactoryCacheOperationSourceAdvisor cacheAdvisor() {
        BeanFactoryCacheOperationSourceAdvisor advisor =
                new BeanFactoryCacheOperationSourceAdvisor();
        advisor.setCacheOperationSource(cacheOperationSource());
        advisor.setAdvice(cacheInterceptor());
        return advisor;
    }

    @Bean
    public CacheOperationSource cacheOperationSource() {
        return new JobsAnnotationCacheOperationSource(new JobsCacheAnnotationParser(cacheProperties));
    }

    @Bean
    public JobsCacheInterceptor cacheInterceptor() {
        JobsCacheInterceptor interceptor = new JobsCacheInterceptor();
        interceptor.setCacheProperties(cacheProperties);
        interceptor.setCacheOperationSources(cacheOperationSource());
        interceptor.setCacheResolver(new JobsCacheResolver(cacheManager, cacheProperties));
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<Object, Object>();
        redisTemplate.setConnectionFactory(cf);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

}
