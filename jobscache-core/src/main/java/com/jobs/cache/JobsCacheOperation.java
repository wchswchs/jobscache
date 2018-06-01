package com.jobs.cache;

import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.util.Assert;

public class JobsCacheOperation extends CacheOperation {

    protected final long expireTime;
    protected final String domain;
    protected final String domainKey;
    protected final String key;

    @Override
    public String getKey() {
        return this.key;
    }

    public long getExpireTime() {
        return expireTime;
    }

    /**
     * @param b
     * @since 4.3
     */
    protected JobsCacheOperation(Builder b) {
        super(b);
        this.expireTime = b.expireTime;
        this.domainKey = b.domainKey;
        this.domain = b.domain;
        this.key = b.key;
    }

    public static class Builder extends CacheOperation.Builder {

        private long expireTime;
        protected String domainKey;
        protected String domain;
        protected String key = "";

        public String getDomain() {
            return domain;
        }

        public String getDomainKey() {
            return domainKey;
        }

        @Override
        public void setKey(String key) {
            Assert.notNull(key, "Key must not be null");
            this.key = key;
        }

        public void setDomainKey(String domainKey) {
            this.domainKey = domainKey;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public void setExpireTime(long expireTime) {
            this.expireTime = expireTime;
        }

        @Override
        public JobsCacheOperation build() {
            return new JobsCacheOperation(this);
        }

    }

}
