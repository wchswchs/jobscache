package com.jobs.cache.operation;

import com.jobs.cache.JobsCacheOperation;
import org.springframework.cache.Cache;

@SuppressWarnings("all")
public class JobsCacheableOperation extends JobsCacheOperation {

    private final String unless;

    private final boolean sync;

    private Cache cache;


    /**
     * @since 4.3
     */
    public JobsCacheableOperation(JobsCacheableOperation.Builder b) {
        super(b);
        this.unless = b.unless;
        this.sync = b.sync;
        this.cache = cache;
    }

    @Override
    public String getKey() {
        String key = "";

        if (!this.domain.isEmpty()) {
            key += this.domain;
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

        return key;
    }

    public String getUnless() {
        return this.unless;
    }

    public boolean isSync() {
        return this.sync;
    }

    public Cache getCache() {
        return cache;
    }

    /**
     * @since 4.3
     */
    public static class Builder extends JobsCacheOperation.Builder {

        private String unless;

        private boolean sync;
        private Cache cache;

        public void setCache(Cache cache) {
            this.cache = cache;
        }

        public void setUnless(String unless) {
            this.unless = unless;
        }

        public void setSync(boolean sync) {
            this.sync = sync;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | unless='");
            sb.append(this.unless);
            sb.append("'");
            sb.append(" | sync='");
            sb.append(this.sync);
            sb.append("'");
            return sb;
        }

        @Override
        public JobsCacheableOperation build() {
            return new JobsCacheableOperation(this);
        }
    }

}
