# cache_man
Cache框架

支持关联Cache更新

例如：

现存在两个key

key1是show_detail_1，缓存id为1的演出详情
key2是hot_shows，缓存热门演出列表，其中id为1的演出也是热门演出

此时，如果show_detail_1更新，hot_shows也同时更新

特性：

1. 兼容Spring Cache @Cacheable，@CachePut，@CacheEvict注解
2. 支持关联缓存更新
3. 支持缓存自动过期
