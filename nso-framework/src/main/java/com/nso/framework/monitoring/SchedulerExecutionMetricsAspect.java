package com.nso.framework.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// 定时任务执行监控切面。
@Aspect
@Component
public class SchedulerExecutionMetricsAspect {

    // 指标注册表
    private final MeterRegistry registry;

    public SchedulerExecutionMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    // 记录定时任务的执行耗时和结果。
    @Around("execution(public * com.nso.scheduler.task..*.execute(..))")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        String task = joinPoint.getTarget().getClass().getSimpleName();
        Timer.Sample sample = Timer.start(registry);
        try {
            Object result = joinPoint.proceed();
            record(task, "success", sample);
            return result;
        } catch (Throwable throwable) {
            record(task, "failure", sample);
            Counter.builder("nso.scheduler.task.failures")
                    .description("Failed scheduler task executions")
                    .tag("task", task)
                    .register(registry)
                    .increment();
            throw throwable;
        }
    }

    private void record(String task, String outcome, Timer.Sample sample) {
        sample.stop(Timer.builder("nso.scheduler.task.duration")
                .description("Scheduler task execution duration")
                .tag("task", task)
                .tag("outcome", outcome)
                .register(registry));
    }
}
