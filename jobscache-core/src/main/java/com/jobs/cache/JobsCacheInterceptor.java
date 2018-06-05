package com.jobs.cache;

import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jobs.cache.configuration.JobsCacheProperties;
import com.jobs.cache.operation.JobsCacheEvictOperation;
import com.jobs.cache.operation.JobsCachePutOperation;
import com.jobs.cache.operation.JobsCacheableOperation;
import com.jobs.cache.strategy.VersionControlStrategy;
import com.jobs.cache.thread.BatchEvictProcessor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.*;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.util.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;


@SuppressWarnings("all")
public class JobsCacheInterceptor extends CacheInterceptor {

    private boolean initialized = false;
    private CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();
    private KeyGenerator keyGenerator = new SimpleKeyGenerator();
    private VersionControlStrategy evictStrategy = new VersionControlStrategy();
    private CacheResolver cacheResolver;
    private BeanFactory beanFactory;
    private JobsCacheProperties cacheProperties;
    private final Map<CacheOperationCacheKey, CacheOperationMetadata> metadataCache = new ConcurrentHashMap<CacheOperationCacheKey, CacheOperationMetadata>(1024);

    private static final Logger LOG = LoggerFactory.getLogger(JobsCacheInterceptor.class);


    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        CacheOperationInvoker aopAllianceInvoker = new CacheOperationInvoker() {
            @Override
            public Object invoke() {
                try {
                    return invocation.proceed();
                } catch (Throwable var2) {
                    throw new ThrowableWrapper(var2);
                }
            }
        };

        try {
            return execute(aopAllianceInvoker, invocation.getThis(), method, invocation.getArguments());
        } catch (CacheOperationInvoker.ThrowableWrapper var5) {
            throw var5.getOriginal();
        }
    }

    @Override
    public Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
        // Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
        // 这里使用的就是CacheOperationSource，
        // 来获取执行方法上所有的缓存操作集合。如果有缓存操作则执行到execute(...)，如果没有就执行invoker.invoke()直接调用执行方法了
        if (this.initialized) {
            Class<?> targetClass = getTargetClass(target);
            Collection<CacheOperation> operations = getCacheOperationSource().getCacheOperations(method, targetClass);
            if (!CollectionUtils.isEmpty(operations)) {
                return execute(invoker, method, new CacheOperationContexts(operations, method, args, target, targetClass));
            }
        }
        return invoker.invoke();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    protected <T> T getBean(String beanName, Class<T> expectedType) {
        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(this.beanFactory, expectedType, beanName);
    }

    @Override
    public void afterSingletonsInstantiated() {
        evictStrategy.setCacheProperties(cacheProperties);
        evictStrategy.setCacheVersionKey();
        if (getCacheResolver() == null) {
            // Lazily initialize cache resolver via default cache manager...
            try {
                setCacheManager(this.beanFactory.getBean(CacheManager.class));
            } catch (NoUniqueBeanDefinitionException ex) {
                throw new IllegalStateException("No CacheResolver specified, and no unique bean of type " +
                        "CacheManager found. Mark one as primary (or give it the name 'cacheManager') or " +
                        "declare a specific CacheManager to use, that serves as the default one.");
            } catch (NoSuchBeanDefinitionException ex) {
                throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. " +
                        "Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
            }
        }
        this.initialized = true;
    }

    public Class<?> getTargetClass(Object target) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
        if (targetClass == null && target != null) {
            targetClass = target.getClass();
        }
        return targetClass;
    }

    //CacheOperationContexts封装
    //CacheOperationContexts对象，这个对象只是为了便于获取每种具体缓存操作集合。
    // 所有的缓存操作CachePutOperation、CacheableOperation、CacheEvictOperation都存放在operations这个集合中，
    // 不便于获取具体的缓存操作，所以封装成了缓存操作上下文CacheOperationContexts这个类
    public class CacheOperationContexts {
        //保存每种类型缓存操作的上下文数据，Map中的key是CacheOperation类型，也就是@Cacheable、@CacheEvict对应的3中CacheOperation实现类型。
        //Map中的value是CacheOperationContext。这种Map的key是可以重复存放的。
        private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts = new LinkedMultiValueMap<Class<? extends CacheOperation>, CacheOperationContext>();

        private final boolean sync;

        public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method, Object[] args, Object target, Class<?> targetClass) {
            //获取每种CacheOperation类型的缓存操作集合，然后保存到Map中去。
            for (CacheOperation operation : operations) {
                this.contexts.add(operation.getClass(), getOperationContext(operation, method, args, target, targetClass));
            }
            this.sync = determineSyncFlag(method);
        }

        public CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {
            CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
            return new CacheOperationContext(metadata, args, target);
        }

        public CacheOperationMetadata getCacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass) {
            CacheOperationCacheKey cacheKey = new CacheOperationCacheKey(operation, method, targetClass);
            CacheOperationMetadata metadata = metadataCache.get(cacheKey);
            if (metadata == null) {
                KeyGenerator operationKeyGenerator;
                if (StringUtils.hasText(operation.getKeyGenerator())) {
                    operationKeyGenerator = getBean(operation.getKeyGenerator(), KeyGenerator.class);
                } else {
                    operationKeyGenerator = getKeyGenerator();
                }
                CacheResolver operationCacheResolver;
                if (StringUtils.hasText(operation.getCacheResolver())) {
                    operationCacheResolver = getBean(operation.getCacheResolver(), CacheResolver.class);
                } else if (StringUtils.hasText(operation.getCacheManager())) {
                    CacheManager cacheManager = getBean(operation.getCacheManager(), CacheManager.class);
                    operationCacheResolver = new SimpleCacheResolver(cacheManager);
                } else {
                    operationCacheResolver = getCacheResolver();
                }
                metadata = new CacheOperationMetadata(operation, method, targetClass, operationKeyGenerator, operationCacheResolver);
                metadataCache.put(cacheKey, metadata);
            }
            return metadata;
        }

        //根据CacheOperation类型，直接从Map中获取对应的缓存操作上下文集合
        //比如： oprationClass为JobsCacheableOperation，那么就是获取的所有@Cacheable注解对应的缓存操作的上下文集合
        public Collection<CacheOperationContext> get(Class<? extends CacheOperation> operationClass) {
            Collection<CacheOperationContext> result = this.contexts.get(operationClass);
            return (result != null ? result : Collections.<CacheOperationContext>emptyList());
        }

        public boolean isSynchronized() {
            return this.sync;
        }

        private boolean determineSyncFlag(Method method) {
            List<CacheOperationContext> cacheOperationContexts = this.contexts.get(JobsCacheableOperation.class);
            if (cacheOperationContexts == null) {  // no @Cacheable operation at all
                return false;
            }
            boolean syncEnabled = false;
            for (CacheOperationContext cacheOperationContext : cacheOperationContexts) {
                if (((JobsCacheableOperation) cacheOperationContext.getOperation()).isSync()) {
                    syncEnabled = true;
                    break;
                }
            }
            if (syncEnabled) {
                if (this.contexts.size() > 1) {
                    throw new IllegalStateException("@Cacheable(sync=true) cannot be combined with other cache operations on '" + method + "'");
                }
                if (cacheOperationContexts.size() > 1) {
                    throw new IllegalStateException("Only one @Cacheable(sync=true) entry is allowed on '" + method + "'");
                }
                CacheOperationContext cacheOperationContext = cacheOperationContexts.iterator().next();
                JobsCacheableOperation operation = (JobsCacheableOperation) cacheOperationContext.getOperation();
                if (cacheOperationContext.getCaches().size() > 1) {
                    throw new IllegalStateException("@Cacheable(sync=true) only allows a single cache on '" + operation + "'");
                }
                if (StringUtils.hasText(operation.getUnless())) {
                    throw new IllegalStateException("@Cacheable(sync=true) does not support unless attribute on '" + operation + "'");
                }
                return true;
            }
            return false;
        }
    }


    private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
        // Special handling of synchronized invocation
        if (contexts.isSynchronized()) {
            CacheOperationContext context = contexts.get(JobsCacheableOperation.class).iterator().next();
            //断缓存条件condition
            if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
                Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
                Cache cache = context.getCaches().iterator().next();
                try {
                    return wrapCacheValue(method, cache.get(key, new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            return unwrapReturnValue(invokeOperation(invoker));
                        }
                    }));
                } catch (Cache.ValueRetrievalException ex) {
                    throw (CacheOperationInvoker.ThrowableWrapper) ex.getCause();
                }
            } else {
                return invokeOperation(invoker);
            }
        }

        // Process any early evictions
        processCacheEvicts(contexts.get(JobsCacheEvictOperation.class), true, CacheOperationExpressionEvaluator.NO_RESULT);

        // Check if we have a cached item matching the conditions
        Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(JobsCacheableOperation.class));

        // Collect puts from any @Cacheable miss, if no cached item is found
        List<CachePutRequest> cachePutRequests = new LinkedList<CachePutRequest>();
        if (cacheHit == null) {
            collectPutRequests(contexts.get(JobsCacheableOperation.class), CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
        }

        Object cacheValue;
        Object returnValue;

        if (cacheHit != null && cachePutRequests.isEmpty() && !hasCachePut(contexts)) {
            // If there are no put requests, just use the cache hit
            cacheValue = cacheHit.get();
            returnValue = wrapCacheValue(method, cacheValue);
        } else {
            // Invoke the method if we don't have a cache hit
            returnValue = invokeOperation(invoker);
            cacheValue = unwrapReturnValue(returnValue);
        }

        // Collect any explicit @CachePuts
        collectPutRequests(contexts.get(JobsCachePutOperation.class), cacheValue, cachePutRequests);
        // Process any collected put requests, either from @CachePut or a @Cacheable miss
        // 处理@CachePut操作，将数据result数据存放到缓存中去。
        for (CachePutRequest cachePutRequest : cachePutRequests) {
            cachePutRequest.apply(cacheValue);
        }

        // Process any late evictions
        // 处理一般的@CacheEvict缓存删除操作情况，也就是beforeIntercepte=false的情况。
        processCacheEvicts(contexts.get(JobsCacheEvictOperation.class), false, cacheValue);

        return returnValue;
    }

    private class CachePutRequest {

        private final CacheOperationContext context;

        private final Object key;

        public CachePutRequest(CacheOperationContext context, Object key) {
            this.context = context;
            this.key = key;
        }

        public void apply(Object result) {
            if (this.context.canPutToCache(result)) {
                for (Cache cache : this.context.getCaches()) {
                    doPut(cache, this.key, result);
                    if (context.getOperation() instanceof JobsCacheableOperation &&
                            !((JobsCacheOperation) context.getOperation()).domain.isEmpty()) {
                        evictStrategy.setCache(cache);
                        evictStrategy.write(key);
                    }
                }
            }
        }

    }

    private boolean isConditionPassing(CacheOperationContext context, Object result) {
        boolean passing = context.isConditionPassing(result);
        if (!passing && logger.isTraceEnabled()) {
            logger.trace("Cache condition failed on method " + context.metadata.method + " for operation " + context.metadata.operation);
        }
        return passing;
    }

    public static class CacheOperationMetadata {

        private final CacheOperation operation;

        private final Method method;

        private final Class<?> targetClass;

        private final KeyGenerator keyGenerator;

        private final CacheResolver cacheResolver;

        public CacheOperationMetadata(CacheOperation operation, Method method, Class<?> targetClass, KeyGenerator keyGenerator, CacheResolver cacheResolver) {
            this.operation = operation;
            this.method = method;
            this.targetClass = targetClass;
            this.keyGenerator = keyGenerator;
            this.cacheResolver = cacheResolver;
        }
    }

    public Object generateKey(CacheOperationContext context, Object result) {
        Object key = context.generateKey(result);
        if (key == null) {
            throw new IllegalArgumentException("Null key returned for cache operation (maybe you are " + "using named params on classes without debug info?) " + context.metadata.operation);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Computed cache key '" + key + "' for operation " + context.metadata.operation);
        }
        if (!((JobsCacheOperation) context.getOperation()).key.isEmpty()
                && !((JobsCacheOperation) context.getOperation()).domain.isEmpty()) {
            evictStrategy.setCache(context.caches.iterator().next());
            if (context.getOperation() instanceof JobsCacheEvictOperation) {
                return evictStrategy.determine(key, true);
            }
            return evictStrategy.determine(key, false);
        }
        return key;
    }

    //CacheOperationContext 封装缓存参数信息， condition、unless处理
    public class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {

        private final CacheOperationMetadata metadata;

        private final Object[] args;

        private final Object target;

        private final Collection<? extends Cache> caches;

        private final Collection<String> cacheNames;

        private final AnnotatedElementKey methodCacheKey;

        public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
            this.metadata = metadata;
            this.args = extractArgs(metadata.method, args);
            this.target = target;
            this.caches = JobsCacheInterceptor.this.getCaches(this, metadata.cacheResolver);
            this.cacheNames = createCacheNames(this.caches);
            this.methodCacheKey = new AnnotatedElementKey(metadata.method, metadata.targetClass);
        }

        @Override
        public CacheOperation getOperation() {
            return this.metadata.operation;
        }

        @Override
        public Object getTarget() {
            return this.target;
        }

        @Override
        public Method getMethod() {
            return this.metadata.method;
        }

        @Override
        public Object[] getArgs() {
            return this.args;
        }

        private Object[] extractArgs(Method method, Object[] args) {
            if (!method.isVarArgs()) {
                return args;
            }
            Object[] varArgs = ObjectUtils.toObjectArray(args[args.length - 1]);
            Object[] combinedArgs = new Object[args.length - 1 + varArgs.length];
            System.arraycopy(args, 0, combinedArgs, 0, args.length - 1);
            System.arraycopy(varArgs, 0, combinedArgs, args.length - 1, varArgs.length);
            return combinedArgs;
        }

        //这个方法用来判断缓存条件condition
        protected boolean isConditionPassing(Object result) {
            //首先判断CacheOperation是否设置了conditions条件
            //如果没有设置条件，则直接通过条件检测
            //如果设置了条件，那么通过evaluator去判断（ExpressionEvaluator evaluator 会通过SpEL表达式去检测）
            if (StringUtils.hasText(this.metadata.operation.getCondition())) {
                EvaluationContext evaluationContext = createEvaluationContext(result);
                return evaluator.condition(this.metadata.operation.getCondition(),
                        this.methodCacheKey, evaluationContext);
            }
            return true;
        }

        protected boolean canPutToCache(Object value) {
            String unless = "";
            if (this.metadata.operation instanceof JobsCacheableOperation) {
                unless = ((JobsCacheableOperation) this.metadata.operation).getUnless();
            } else if (this.metadata.operation instanceof JobsCachePutOperation) {
                unless = ((JobsCachePutOperation) this.metadata.operation).getUnless();
            }
            if (StringUtils.hasText(unless)) {
                EvaluationContext evaluationContext = createEvaluationContext(value);
                return !evaluator.unless(unless, this.methodCacheKey, evaluationContext);
            }
            return true;
        }

        /**
         * Compute the key for the given caching operation.
         *
         * @return the generated key, or {@code null} if none can be generated
         */
        protected Object generateKey(Object result) {
            if (StringUtils.hasText(this.metadata.operation.getKey())) {
                EvaluationContext evaluationContext = createEvaluationContext(result);
                return evaluator.key(this.metadata.operation.getKey(), this.methodCacheKey, evaluationContext);
            }
            return this.metadata.keyGenerator.generate(this.target, this.metadata.method, this.args);
        }

        private EvaluationContext createEvaluationContext(Object result) {
            return evaluator.createEvaluationContext(this.caches, this.metadata.method, this.args,
                    this.target, this.metadata.targetClass, result, beanFactory);
        }

        protected Collection<? extends Cache> getCaches() {
            return this.caches;
        }

        protected Collection<String> getCacheNames() {
            return this.cacheNames;
        }

        private Collection<String> createCacheNames(Collection<? extends Cache> caches) {
            Collection<String> names = new ArrayList<String>();
            for (Cache cache : caches) {
                names.add(cache.getName());
            }
            return names;
        }
    }

    private static final class CacheOperationCacheKey implements Comparable<CacheOperationCacheKey> {

        private final CacheOperation cacheOperation;

        private final AnnotatedElementKey methodCacheKey;

        private CacheOperationCacheKey(CacheOperation cacheOperation, Method method, Class<?> targetClass) {
            this.cacheOperation = cacheOperation;
            this.methodCacheKey = new AnnotatedElementKey(method, targetClass);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheOperationCacheKey)) {
                return false;
            }
            CacheOperationCacheKey otherKey = (CacheOperationCacheKey) other;
            return (this.cacheOperation.equals(otherKey.cacheOperation) &&
                    this.methodCacheKey.equals(otherKey.methodCacheKey));
        }

        @Override
        public int hashCode() {
            return (this.cacheOperation.hashCode() * 31 + this.methodCacheKey.hashCode());
        }

        @Override
        public String toString() {
            return this.cacheOperation + " on " + this.methodCacheKey;
        }

        @Override
        public int compareTo(CacheOperationCacheKey other) {
            int result = this.cacheOperation.getName().compareTo(other.cacheOperation.getName());
            if (result == 0) {
                result = this.methodCacheKey.compareTo(other.methodCacheKey);
            }
            return result;
        }
    }

    static class CacheOperationExpressionEvaluator extends CachedExpressionEvaluator {

        /**
         * Indicate that there is no result variable.
         */
        public static final Object NO_RESULT = new Object();

        /**
         * Indicate that the result variable cannot be used at all.
         */
        public static final Object RESULT_UNAVAILABLE = new Object();

        /**
         * The name of the variable holding the result object.
         */
        public static final String RESULT_VARIABLE = "result";


        private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

        private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

        private final Map<ExpressionKey, Expression> unlessCache = new ConcurrentHashMap<ExpressionKey, Expression>(64);

        private final Map<AnnotatedElementKey, Method> targetMethodCache =
                new ConcurrentHashMap<AnnotatedElementKey, Method>(64);


        /**
         * Create an {@link EvaluationContext} without a return value.
         *
         * @see #createEvaluationContext(Collection, Method, Object[], Object, Class, Object, BeanFactory)
         */
        public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
                                                         Method method, Object[] args, Object target, Class<?> targetClass, BeanFactory beanFactory) {

            return createEvaluationContext(caches, method, args, target, targetClass, NO_RESULT, beanFactory);
        }

        /**
         * Create an {@link EvaluationContext}.
         *
         * @param caches      the current caches
         * @param method      the method
         * @param args        the method arguments
         * @param target      the target object
         * @param targetClass the target class
         * @param result      the return value (can be {@code null}) or
         *                    {@link #NO_RESULT} if there is no return at this time
         * @return the evaluation context
         */
        public EvaluationContext createEvaluationContext(Collection<? extends Cache> caches,
                                                         Method method, Object[] args, Object target, Class<?> targetClass, Object result,
                                                         BeanFactory beanFactory) {

            CacheExpressionRootObject rootObject = new CacheExpressionRootObject(
                    caches, method, args, target, targetClass);
            Method targetMethod = getTargetMethod(targetClass, method);
            CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
                    rootObject, targetMethod, args, getParameterNameDiscoverer());
            if (result == RESULT_UNAVAILABLE) {
                evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
            } else if (result != NO_RESULT) {
                evaluationContext.setVariable(RESULT_VARIABLE, result);
            }
            if (beanFactory != null) {
                evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
            }
            return evaluationContext;
        }

        public Object key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
            return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext);
        }

        public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
            return getExpression(this.conditionCache, methodKey, conditionExpression).getValue(evalContext, boolean.class);
        }

        public boolean unless(String unlessExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
            return getExpression(this.unlessCache, methodKey, unlessExpression).getValue(evalContext, boolean.class);
        }

        /**
         * Clear all caches.
         */
        void clear() {
            this.keyCache.clear();
            this.conditionCache.clear();
            this.unlessCache.clear();
            this.targetMethodCache.clear();
        }

        private Method getTargetMethod(Class<?> targetClass, Method method) {
            AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
            Method targetMethod = this.targetMethodCache.get(methodKey);
            if (targetMethod == null) {
                targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
                if (targetMethod == null) {
                    targetMethod = method;
                }
                this.targetMethodCache.put(methodKey, targetMethod);
            }
            return targetMethod;
        }


    }

    static class CacheEvaluationContext extends MethodBasedEvaluationContext {

        private final Set<String> unavailableVariables = new HashSet<String>(1);

        CacheEvaluationContext(Object rootObject, Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {

            super(rootObject, method, arguments, parameterNameDiscoverer);
        }

        public void addUnavailableVariable(String name) {
            this.unavailableVariables.add(name);
        }

        @Override
        public Object lookupVariable(String name) {
            if (this.unavailableVariables.contains(name)) {
                throw new VariableNotAvailableException(name);
            }
            return super.lookupVariable(name);
        }

    }

    static class CacheExpressionRootObject {

        private final Collection<? extends Cache> caches;

        private final Method method;

        private final Object[] args;

        private final Object target;

        private final Class<?> targetClass;


        public CacheExpressionRootObject(Collection<? extends Cache> caches, Method method, Object[] args, Object target, Class<?> targetClass) {

            Assert.notNull(method, "Method is required");
            Assert.notNull(targetClass, "targetClass is required");
            this.method = method;
            this.target = target;
            this.targetClass = targetClass;
            this.args = args;
            this.caches = caches;
        }


        public Collection<? extends Cache> getCaches() {
            return this.caches;
        }

        public Method getMethod() {
            return this.method;
        }

        public String getMethodName() {
            return this.method.getName();
        }

        public Object[] getArgs() {
            return this.args;
        }

        public Object getTarget() {
            return this.target;
        }

        public Class<?> getTargetClass() {
            return this.targetClass;
        }

    }


    private Object wrapCacheValue(Method method, Object cacheValue) {
        if (method.getReturnType() == JobsCacheInterceptor.javaUtilOptionalClass && (cacheValue == null || cacheValue.getClass() != javaUtilOptionalClass)) {
            return OptionalUnwrapper.wrap(cacheValue);
        }
        return cacheValue;
    }

    private static class OptionalUnwrapper {
        public static Object unwrap(Object optionalObject) {
            Optional<?> optional = (Optional<?>) optionalObject;
            if (!optional.isPresent()) {
                return null;
            }
            Object result = optional.get();
            Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
            return result;
        }

        public static Object wrap(Object value) {
            return Optional.ofNullable(value);
        }
    }

    private static Class<?> javaUtilOptionalClass = null;

    static {
        try {
            javaUtilOptionalClass = ClassUtils.forName("java.util.Optional", JobsCacheInterceptor.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // Java 8 not available - Optional references simply not supported then.
        }
    }

    private Object unwrapReturnValue(Object returnValue) {
        if (returnValue != null && returnValue.getClass() == javaUtilOptionalClass) {
            return OptionalUnwrapper.unwrap(returnValue);
        }
        return returnValue;
    }

    private void processCacheEvicts(Collection<CacheOperationContext> contexts, boolean beforeInvocation, Object result) {
        for (CacheOperationContext context : contexts) {
            JobsCacheEvictOperation operation = (JobsCacheEvictOperation) context.metadata.operation;
            if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    private void performCacheEvict(CacheOperationContext context, JobsCacheEvictOperation operation, Object result) {
        Object key = null;
        for (Cache cache : context.getCaches()) {
            if (operation.isCacheWide()) {
                logInvalidating(context, operation, null);
                doClear(cache);
            } else {
                if (key == null) {
                    key = generateKey(context, result);
                }
                logInvalidating(context, operation, key);
                doEvict(context, cache, key);
            }
        }
    }

    public void doEvict(CacheOperationContext context, Cache cache, Object key) {
        try {
            if (((JobsCacheOperation) context.getOperation()).key.isEmpty()) {
                evictStrategy.setCache(cache);
                String versionDomain = evictStrategy.clear(key);
                if (versionDomain != null) {
                    RedisTemplate redisTemplate = (RedisTemplate) cache.getNativeCache();
                    ScheduledExecutorService delayEvictExecutor = Executors.newSingleThreadScheduledExecutor(
                            new ThreadFactoryBuilder().setNameFormat("evict-main").build());
                    delayEvictExecutor.schedule(new CacheEvictProcessor(redisTemplate,
                                    cacheProperties.getCacheName() + ":" + KeyProcessor.convertPattern(versionDomain))
                            , 100, TimeUnit.MILLISECONDS);
                }
            } else {
                cache.evict(key);
            }
        } catch (RuntimeException ex) {
            getErrorHandler().handleCacheEvictError(ex, cache, key);
        }
    }

    public void logInvalidating(CacheOperationContext context, JobsCacheEvictOperation operation, Object key) {
        if (logger.isTraceEnabled()) {
            logger.trace("Invalidating " + (key != null ? "cache key [" + key + "]" : "entire cache") +
                    " for operation " + operation + " on method " + context.metadata.method);
        }
    }

    private Cache.ValueWrapper findCachedItem(Collection<CacheOperationContext> contexts) {
        Object result = CacheOperationExpressionEvaluator.NO_RESULT;
        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                Cache.ValueWrapper cached = findInCaches(context, key);
                if (cached != null) {
                    return cached;
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("No cache entry for key '" + key + "' in cache(s) " + context.getCacheNames());
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected Collection<? extends Cache> getCaches(
            CacheOperationInvocationContext<CacheOperation> context, CacheResolver cacheResolver) {

        Collection<? extends Cache> caches = cacheResolver.resolveCaches(context);
        if (caches.isEmpty()) {
            throw new IllegalStateException("No cache could be resolved for '" + context.getOperation() + "' using resolver '" + cacheResolver +
                    "'. At least one cache should be provided per cache operation.");
        }
        return caches;
    }

    private Cache.ValueWrapper findInCaches(CacheOperationContext context, Object key) {
        for (Cache cache : context.getCaches()) {
            Cache.ValueWrapper wrapper = doGet(cache, key);
            if (wrapper != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Cache entry for key '" + key + "' found in cache '" + cache.getName() + "'");
                }
                return wrapper;
            }
        }
        return null;
    }

    private void collectPutRequests(Collection<CacheOperationContext> contexts, Object result, Collection<CachePutRequest> putRequests) {

        for (CacheOperationContext context : contexts) {
            if (isConditionPassing(context, result)) {
                Object key = generateKey(context, result);
                putRequests.add(new CachePutRequest(context, key));
            }
        }
    }

    public void setCacheProperties(JobsCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    private boolean hasCachePut(JobsCacheInterceptor.CacheOperationContexts contexts) {
        // Evaluate the conditions *without* the result object because we don't have it yet...
        Collection<CacheOperationContext> cachePutContexts = contexts.get(JobsCachePutOperation.class);
        Collection<CacheOperationContext> excluded = new ArrayList<CacheOperationContext>();
        for (JobsCacheInterceptor.CacheOperationContext context : cachePutContexts) {
            try {
                if (!context.isConditionPassing(CacheOperationExpressionEvaluator.RESULT_UNAVAILABLE)) {
                    excluded.add(context);
                }
            } catch (VariableNotAvailableException ex) {
                // Ignoring failure due to missing result, consider the cache put has to proceed
            }
        }
        // Check if all puts have been excluded by condition
        return (cachePutContexts.size() != excluded.size());
    }

    static class VariableNotAvailableException extends EvaluationException {

        private final String name;

        public VariableNotAvailableException(String name) {
            super("Variable '" + name + "' is not available");
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    class CacheEvictProcessor implements Runnable {

        private final RedisTemplate redisTemplate;
        private final Object key;

        CacheEvictProcessor(RedisTemplate redisTemplate, Object key) {
            this.redisTemplate = redisTemplate;
            this.key = key;
        }

        @Override
        public void run() {
            if (key.toString().contains("*")) {
                RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
                ScanOptions options = ScanOptions.scanOptions().match(key.toString()).build();
                Cursor<byte[]> c = conn.scan(options);
                int keyNum = Iterators.size(c);
                if (keyNum > 0) {
                    int keyCount, threadCount = 0;
                    ThreadPoolExecutor evictExecutor = new ThreadPoolExecutor(50, cacheProperties.getBatchEvictThreadPoolSize(), 5L,
                            TimeUnit.SECONDS, new SynchronousQueue<>(),
                            (new ThreadFactoryBuilder()).setNameFormat("evict-thead-%d").build());
                    if (keyNum >= cacheProperties.getMaxEvictThreadNum()) {
                        threadCount = cacheProperties.getMaxEvictThreadNum();
                        keyCount = (int) Math.ceil(keyNum / cacheProperties.getMaxEvictThreadNum());
                    } else {
                        threadCount = (int) Math.floor(keyNum / cacheProperties.getMaxEvictThreadNum()) + 1;
                        keyCount = keyNum;
                    }
                    if (threadCount > 0 && keyCount > 0) {
                        for (int i = 0; i < threadCount; i++) {
                            options = ScanOptions.scanOptions().match(key.toString()).count(keyCount).build();
                            c = conn.scan(options);
                            LOG.info("Batch Evict Thread Starting");
                            evictExecutor.submit(new BatchEvictProcessor(redisTemplate, c));
                        }
                    }
                }
            }
        }

    }

}
