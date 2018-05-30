package com.jobs.cache.version;

import com.jobs.cache.Version;

public class TimestampVersion implements Version {

    @Override
    public String getVersion() {
        return String.valueOf(System.currentTimeMillis());
    }

    public long getTimestamp(String cacheKey) {
        String[] timestamp = cacheKey.split("_");
        return Long.valueOf(timestamp[1]);
    }

}
