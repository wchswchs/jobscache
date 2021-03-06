package com.jobs.cache.services;

import com.jobs.cache.CacheService;
import com.jobs.cache.ShowInfo;
import com.jobs.cache.annotations.JobsCacheEvict;
import com.jobs.cache.annotations.JobsCachePut;
import com.jobs.cache.annotations.JobsCacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
@Service
public class AnnotatedInnerShowCacheService implements CacheService<Object> {

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
    @JobsCacheEvict(domain = "'seller'", key = "'seller_detail_'+#id")
    public Object deleteShow(String id) {
        return null;
    }

}
