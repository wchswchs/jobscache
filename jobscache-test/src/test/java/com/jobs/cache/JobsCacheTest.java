package com.jobs.cache;

import com.jobs.cache.configuration.JobsCacheProperties;
import com.jobs.cache.services.AnnotatedClassShowCacheService;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringBootTest
public class JobsCacheTest {

    @Autowired
    private AnnotatedClassShowCacheService cacheService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private JobsCacheProperties cacheProperties;

    @Test
    public void test01GetShowDetail1ForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 10; i ++) {
        Object val = cacheService.getShowDetailForRec("1");
//        }
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        if (cache != null) {
            if (cache.get("show_1:show_detail_1") != null) {
                Assert.assertEquals(val.toString(), cache.get("show_1:show_detail_1").get().toString());
            }
        } else {
            Assert.assertNull(cache.get("show_1:show_detail_1"));
        }
    }

    @Test
    public void test02GetShowDetail2ForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 100; i ++) {
        Object val = cacheService.getShowDetailForRec("2");
//        }
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        if (cache != null) {
            if (cache.get("show_2:show_detail_2") != null) {
                Assert.assertEquals(val.toString(), cache.get("show_2:show_detail_2").get().toString());
            }
        } else {
            Assert.assertNull(cache.get("show_2:show_detail_2"));
        }
    }

//    @Test
//    public void testGetShowInfoForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 10; i ++) {
//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    int a = ThreadLocalRandom.current().nextInt(1, 10);
//                    System.out.println(a);
//                    Object val = cacheService.getShowInfoForRec("20180510" + a);
//                }
//            });
//            thread.start();
//        }
//        Thread.sleep(15000);
////        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
////        if (cache != null) {
////            if (cache.get("show:show_detail_20180510") != null) {
////                Assert.assertEquals(val.toString(), cache.get("show:show_detail_20180510").get().toString());
////            }
////        } else {
////            Assert.assertNull(cache.get("show:show_detail_20180510"));
////        }
//    }

    @Test
    public void test03GetShowInfoForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 100; i ++) {
        Object val = cacheService.getShowInfoForRec("20180510");
//        }
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
    public void test04GetRecommandShows() throws Exception {
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
    public void test05UpdateShowForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 6; i ++) {
        Object id = cacheService.updateShowForRecommend("20180510", "王五");
//        }
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        Assert.assertNull(cache.get("show:show_detail_20180510"));
        Thread.sleep(20000);
    }

//    @Test
//    public void testUpdateShowForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 10; i ++) {
//            Thread thread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    Object id = cacheService.updateShowForRecommend("20180510", "王五");
//                }
//            });
//            thread.start();
//
//        }
////        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
////        Assert.assertNull(cache.get("show:show_detail_20180510"));
//        Thread.sleep(30000);
//    }

    @Test
    public void test06CacheableGetShowInfoForRecByAnnotation() throws Exception {
        Object val = cacheService.getCacheableShowInfoForRecByAnnotation("2018051416510000000000000000");
        System.out.println(val);
    }

    @Test
    public void test07CacheableGetRecommandShows() throws Exception {
        Object val = cacheService.getCacheableRecommandShows("20180000000", "中国上海");
        System.out.println(val);
    }

    @Test
    public void test08CacheableUpdateShowForRecByAnnotation() throws Exception {
        Object val = cacheService.getCacheableUpdateShowForRecommend("2018051416510000000000000000", "wuhan湖北武汉");
        System.out.println(val);
    }

    @Test
    public void test09CachePutGetSellerInfoForRecByAnnotation() throws Exception {
        Object val = cacheService.getsCachePutGetShowInfoForRecByAnnotation("Wust2018051416510000000000000000");
        System.out.print(val);
    }

    @Test
    public void test10GetSellerDetailByAnnotation() throws Exception {
//        for (int i = 0; i < 100; i ++) {
        Object val = cacheService.getSellerDetail("1");
//        }
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        if (cache != null) {
            if (cache.get("seller:seller_detail_1") != null) {
                Assert.assertEquals(val.toString(), cache.get("seller:seller_detail_1").get().toString());
            }
        } else {
            Assert.assertNull(cache.get("seller:seller_detail_1"));
        }
    }

    @Test
    public void test11CachePutGetSellerInfoForDomainByAnnotation() throws Exception {
        Object val = cacheService.getsCachePutGetShowInfoForDomainByAnnotation("1");
        System.out.print(val);
    }

    @Test
    public void test12DeleteSellerForRecByAnnotation() throws Exception {
//        for (int i = 0; i < 6; i ++) {
        Object id = cacheService.deleteShow("1");
//        }
        Cache cache = cacheManager.getCache(cacheProperties.getCacheName());
        Assert.assertNull(cache.get("seller:show_detail_1"));
    }

}
