package com.jobs.cache;

import com.jobs.cache.configuration.JobsCacheProperties;
import com.jobs.cache.services.AnnotatedClassShowCacheService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JobsCacheTest {

    @Autowired
    private AnnotatedClassShowCacheService cacheService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private JobsCacheProperties cacheProperties;

    @Test
    public void testGetShowInfoForRecByAnnotation() throws Exception {
        Object val = cacheService.getShowInfoForRec("20180510");
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        if (cache != null) {
            if (cache.get("show:show_detail_20180510") != null) {
                Assert.assertEquals(val.toString(), cache.get("show:show_detail_20180510").get().toString());
            }
        } else {
            Assert.assertNull(cache.get("show:show_detail_20180510"));
        }
    }

    @Test
    public void testGetRecommandShows() throws Exception {
        Object val = cacheService.getRecommandShows("102", "张三");
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        if (cache != null) {
            if (cache.get("show:recommend_shows") != null) {
                Assert.assertEquals(val.toString(), cache.get("show:recommend_shows").get().toString());
            }
        } else {
            Assert.assertNull(cache.get("show:recommend_shows"));
        }
    }

    @Test
    public void testUpdateShowForRecByAnnotation() throws Exception {
        Object id = cacheService.updateShowForRecommend("20180510", "王五");
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        Thread.sleep(10000);
        Assert.assertNull(cache.get("show:show_detail_20180510"));
    }

    @Test
    public void TestCacheableGetShowInfoForRecByAnnotation() throws Exception {
        Object val = cacheService.getCacheableShowInfoForRecByAnnotation("2018051416510000000000000000");
        System.out.println(val);
    }

    @Test
    public void TestCacheableGetRecommandShows() throws Exception {
        Object val = cacheService.getCacheableRecommandShows("20180000000", "中国上海");
        System.out.println(val);
    }

    @Test
    public void TestCacheableUpdateShowForRecByAnnotation() throws Exception {
        Object val = cacheService.getCacheableUpdateShowForRecommend("Wh201805100000000", "wuhan湖北武汉");
        System.out.println(val);
    }

    @Test
    public void TestCachePutGetShowInfoForRecByAnnotation() throws Exception {
        Object val = cacheService.getsCachePutGetShowInfoForRecByAnnotation("Wust2018051416510000000000000000");
        System.out.print(val);
    }
}
