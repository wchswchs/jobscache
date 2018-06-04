package com.jobs.cache.operation;

import com.jobs.cache.JobsCacheOperation;
import com.jobs.cache.version.TimestampVersion;

@SuppressWarnings("all")
public class JobsCachePutOperation extends JobsCacheOperation {
    private final String unless;

    protected JobsCachePutOperation(JobsCachePutOperation.Builder b) {
        super(b);
        this.unless = b.unless;
    }

    @Override
    public String getKey() {
        String key = "";

        TimestampVersion version = new TimestampVersion();
        String domainVersion = version.getVersion();

        if (!this.domain.isEmpty()) {
            key += this.domain;
        }
        if (!key.isEmpty()) {
            key += "+'_" + domainVersion + "'";
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

    public static class Builder extends JobsCacheOperation.Builder {

        private String unless;

        public void setUnless(String unless) {
            this.unless = unless;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append("'");
            sb.append(this.unless);
            sb.append("'");
            return sb;
        }

        public JobsCachePutOperation build() {
            return new JobsCachePutOperation(this);
        }
    }
}
