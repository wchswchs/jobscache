package com.jobs.cache.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("juqi.cache")
public class JobsCacheProperties {

    private String cacheName = "all";
    private int maxEvictThreadNum = 500;
    private int batchEvictThreadPoolSize = Integer.MAX_VALUE;
    private long defaultExpiredTime = 300;

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public void setDefaultExpiredTime(long defaultExpiredTime) {
        this.defaultExpiredTime = defaultExpiredTime;
    }

    public void setMaxEvictThreadNum(int maxEvictThreadNum) {
        maxEvictThreadNum = maxEvictThreadNum;
    }

    public void setBatchEvictThreadPoolSize(int batchEvictThreadPoolSize) {
        this.batchEvictThreadPoolSize = batchEvictThreadPoolSize;
    }

    public String getCacheName() {
        return cacheName;
    }

    public long getDefaultExpiredTime() {
        return defaultExpiredTime;
    }

    public int getMaxEvictThreadNum() {
        return maxEvictThreadNum;
    }

    public int getBatchEvictThreadPoolSize() {
        return batchEvictThreadPoolSize;
    }

}
