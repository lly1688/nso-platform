package com.nso.business.core;

import java.util.function.Supplier;

/**
 * 业务锁端口接口。
 * 实现需设置有界等待时间和租约，避免阻塞业务状态迁移。
 */
public interface BusinessLockPort {

    /**
     * 在指定业务锁内执行操作。
     *
     * @param key 业务锁标识
     * @param action 待执行操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T withLock(String key, Supplier<T> action);
}
