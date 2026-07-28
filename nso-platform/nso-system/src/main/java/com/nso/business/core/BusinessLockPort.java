package com.nso.business.core;

import java.util.function.Supplier;

/**
 * Core business code depends on this port rather than on Redisson directly.
 * Implementations must use a bounded wait and lease to avoid an indefinitely
 * held lock blocking a business state transition.
 */
public interface BusinessLockPort {

    <T> T withLock(String key, Supplier<T> action);
}
