package com.nso;

import com.nso.framework.security.RequestRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestRateLimitServiceTest {

    @Test
    void rejectsWhenRedisExecutionFails() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(new RequestRateLimitService(redis).tryAcquire("login", "user", 5, Duration.ofMinutes(1)))
                .isFalse();
    }
}
