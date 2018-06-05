package com.jobs.cache.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchEvictProcessor implements Runnable {

    private final Cursor<byte[]> cursor;
    private final RedisTemplate redisTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(BatchEvictProcessor.class);

    public BatchEvictProcessor(RedisTemplate redisTemplate, Cursor cursor) {
        this.cursor = cursor;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run() {
        List<String> keys = new ArrayList<String>();
        LOG.info("Batch Evict Processing");
        try {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            redisTemplate.delete(keys);
        } catch (Exception ex) {
            LOG.error("Batch Evict Error", ex);
        }
        LOG.info("Batch Evict Ended");
    }

}
