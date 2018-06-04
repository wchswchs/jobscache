package com.jobs.cache;

public interface CacheService<T> {

    T getShowDetailForRec(Long arg1);

    T getShowInfoForRec(Long arg1);

    T getSellerInfo(Long arg1);

    T updateShowForPattern(String id, String name);

    T updateShowForRecommend(String id, String name);

    T deleteShow(String id);

    T getRecommandShows(String id, String name);

    T getCacheableShowInfoForRecByAnnotation(String id);

    T getCacheableRecommandShows(String id, String name);

    T getCacheableUpdateShowForRecommend(String id, String name);

    T getsCachePutGetShowInfoForRecByAnnotation(String var1);

    T getsCachePutGetShowInfoForDomainByAnnotation(String var1);

}