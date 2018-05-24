package com.jobs.cache.services;

import com.jobs.cache.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("all")
@Service
public class AnnotatedClassShowCacheService {

    @Autowired
    private CacheService cacheService;

    public Object getShowInfoForRec(String id) {
        return cacheService.getShowInfoForRec(id);
    }

    public Object updateShowForRecommend(String id, String name) {
        return cacheService.updateShowForRecommend(id, name);
    }

    public Object getRecommandShows(String id, String name) {
        return cacheService.getRecommandShows(id, name);
    }

    public Object getCacheableShowInfoForRecByAnnotation(String id) {
        return cacheService.getCacheableShowInfoForRecByAnnotation(id);
    }

    public Object getCacheableRecommandShows(String id, String name) {
        return cacheService.getCacheableRecommandShows(id, name);
    }

    public Object getCacheableUpdateShowForRecommend(String id, String name) {
        return cacheService.getCacheableUpdateShowForRecommend(id, name);
    }

    public Object getsCachePutGetShowInfoForRecByAnnotation(String var1) {
        return cacheService.getsCachePutGetShowInfoForRecByAnnotation(var1);
    }

}
