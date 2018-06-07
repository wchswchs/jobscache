# JobsCache
高性能Cache框架（支持关联Cache更新）

该框架创新地解决了使用Redis模糊删除key的性能问题，更高效地支持了关联Cache的更新

例如：

现存在两个key

key1是show_detail_1，缓存id为1的演出详情<br/>
key2是hot_shows，缓存热门演出列表，其中id为1的演出也是热门演出

此时，如果show_detail_1更新，hot_shows也同时更新

## 特性

1. 兼容Spring Cache @Cacheable，@CachePut，@CacheEvict注解
2. 支持cache与cache关联更新
3. 支持db与cache关联更新
4. 支持缓存自动过期
5. key支持正则表达式
6. 支持Spring Expression
7. 支持二级关联缓存更新
8. 当前仅支持Redis(未来会扩展Guava,EHCache,Caffeine等)

## Getting Started

### UnitTest ###
修改cache.properties

```java
spring.cache.type=redis

spring.redis.host=127.0.0.1
spring.redis.password=123456
spring.redis.port=6379

spring.redis.pool.max-active=500
spring.redis.pool.max-idle=500

jobs.cache.cacheName=test
```

## 用法

### Maven依赖 ###

```java
<dependency>
  <groupId>com.jobs.cache</groupId>
  <artifactId>jobscache-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```

### cache配置项 ###            
* `cacheName`: 全局cache名，对应redis一级目录
* `maxEvictThreadNum`: 删除缓存最大线程数，即最多开启多少个线程批量删除关联key，默认500
* `batchEvictThreadPoolSize`: 删除缓存线程池大小，默认Integer.MAX_VALUE
* `defaultExpiredTime`: 默认过期时间，默认5mins

### 使用示例 ###

```java
    @JobsCacheable(domain = "'show'", key = "'show_detail_'+#id")
    public Object getShowInfoForRec(Long id) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(String.valueOf(id));
        methodShow.setName("李四");

        return methodShow;
    }

    @JobsCacheable(domain = "'show_'+#id", key = "'show_detail_'+#id")
    public Object getShowDetailForRec(Long id) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(String.valueOf(id));
        methodShow.setName("李四");

        return methodShow;
    }

    @Override
    @JobsCacheEvict(domain = "'show_*'")
    public Object updateShowForPattern(String id, String name) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(id);
        methodShow.setName(name);

        return id;
    }

    @Override
    @JobsCacheEvict(domain = "'show'")
    public Object updateShowForRecommend(String id, String name) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(id);
        methodShow.setName(name);

        return id;
    }

    @Override
    @JobsCacheable(domain = "'show'", key = "'recommend_shows'", expireTime = 5000L)
    public Object getRecommandShows(String id, String name) {
        List<ShowInfo> list = new ArrayList<ShowInfo>();
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(id);
        methodShow.setName(name);
        list.add(methodShow);
        return list;
    }

    @Override
    @JobsCacheable(key = "'order_detail_'+#id")
    public Object getCacheableShowInfoForRecByAnnotation(String id) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(id);
        methodShow.setName("李四");
        return methodShow;
    }

    @Override
    @JobsCacheable(key = "'recommend_orders'")
    public Object getCacheableRecommandShows(String id, String name) {
        List<ShowInfo> list = new ArrayList<ShowInfo>();//getCacheableRecommandShows
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(id);
        methodShow.setName(name);
        list.add(methodShow);
        return list;
    }

    @Override
    @JobsCacheEvict(key = "'order_detail_'+#id")
    public Object getCacheableUpdateShowForRecommend(String id, String name) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(id);
        methodShow.setName(name);
        return id;
    }

    @Override
    @JobsCachePut(key = "'seller_detail_'+#var1")
    public Object getsCachePutGetShowInfoForRecByAnnotation(String var1) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(var1);
        methodShow.setName("赵六");
        return var1;
    }

    @JobsCacheable(domain = "'seller'", key = "'seller_detail_'+#id")
    public Object getSellerInfo(Long id) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(String.valueOf(id));
        methodShow.setName("李四");

        return methodShow;
    }

    @Override
    @JobsCachePut(domain = "'seller'", key = "'seller_detail_'+#var1")
    public Object getsCachePutGetShowInfoForDomainByAnnotation(String var1) {
        ShowInfo methodShow = new ShowInfo();
        methodShow.setId(var1);
        methodShow.setName("赵六");
        return methodShow;
    }

    @Override
    @JobsCacheEvict(domain = "'show'", key = "'show_detail_'+#id")
    public Object deleteShow(String id) {
        return null;
    }
```
