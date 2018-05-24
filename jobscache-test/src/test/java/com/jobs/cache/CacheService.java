package com.jobs.cache;

public interface CacheService<T> {

    T getShowInfoForRec(String arg1);

    T updateShowForRecommend(String id, String name);

    T getRecommandShows(String id, String name);

    T getCacheableShowInfoForRecByAnnotation(String id);

    T getCacheableRecommandShows(String id ,String name);

    T getCacheableUpdateShowForRecommend(String id ,String name);

    T getsCachePutGetShowInfoForRecByAnnotation(String var1);


}