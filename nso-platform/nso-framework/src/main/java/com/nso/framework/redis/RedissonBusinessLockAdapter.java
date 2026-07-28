package com.nso.framework.redis;

import com.nso.business.core.BusinessLockPort;
import com.nso.common.exception.BusinessException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Production adapter for concurrency-sensitive state transitions. */
@Component
@Profile("!test")
public class RedissonBusinessLockAdapter implements BusinessLockPort {

    private static final long WAIT_SECONDS = 5;
    private static final long LEASE_SECONDS = 60;
    private final RedissonClient redissonClient;

    public RedissonBusinessLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock("nso:business:" + key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw BusinessException.ruleBlock("BUSINESS_LOCK_BUSY", "操作正在处理中，请稍后重试", key, "available", "刷新后重试");
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw BusinessException.ruleBlock("BUSINESS_LOCK_INTERRUPTED", "等待并发锁时被中断", key, "available", "重试操作");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
