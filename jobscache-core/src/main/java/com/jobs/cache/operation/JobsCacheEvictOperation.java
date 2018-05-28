package com.jobs.cache.operation;


import com.jobs.cache.JobsCacheOperation;
import org.springframework.cache.Cache;


@SuppressWarnings("all")
public class JobsCacheEvictOperation extends JobsCacheOperation {

    private final boolean cacheWide;

    private final boolean beforeInvocation;

    private Cache cache;

    /**
     * @since 4.3
     */
    public JobsCacheEvictOperation(JobsCacheEvictOperation.Builder b) {
        super(b);
        this.cacheWide = b.cacheWide;
        this.beforeInvocation = b.beforeInvocation;
    }


    public boolean isCacheWide() {
        return this.cacheWide;
    }

    public boolean isBeforeInvocation() {
        return this.beforeInvocation;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    @Override
    public String getKey() {
        String key = "";

        if (!this.domain.isEmpty()) {
            key += "'" + this.getCacheNames().iterator().next() + "'.concat(':').concat(" + this.domain + ")";
        }
        if (!this.domainKey.isEmpty()) {
            key += ".concat(':').concat(" + this.domainKey + ")";
        }
        if (!this.key.isEmpty()) {
            if (!key.isEmpty()) {
                key += ".concat(':').concat(" + this.key + ")";
            } else {
                key += this.key;
            }
        }

        key += ".concat(':*')";

        return key;
    }

    /**
     * @since 4.3
     */
    public static class Builder extends JobsCacheOperation.Builder {

        private boolean cacheWide = false;

        private boolean beforeInvocation = false;

        private Cache cache;

        public void setCacheWide(boolean cacheWide) {
            this.cacheWide = cacheWide;
        }

        public void setBeforeInvocation(boolean beforeInvocation) {
            this.beforeInvocation = beforeInvocation;
        }

        public void setCache(Cache cache) {
            this.cache = cache;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(",");
            sb.append(this.cacheWide);
            sb.append(",");
            sb.append(this.beforeInvocation);
            sb.append(",");
            sb.append(this.domainKey);
            sb.append(",");
            return sb;
        }

        @Override
        public JobsCacheEvictOperation build() {
            return new JobsCacheEvictOperation(this);
        }
    }
}
