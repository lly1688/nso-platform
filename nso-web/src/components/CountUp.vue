<script setup lang="ts">
// 数字递增动画状态。
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

interface Props {
    to: number;
    from?: number;
    direction?: 'up' | 'down';
    delay?: number;
    duration?: number;
    className?: string;
    startWhen?: boolean;
    separator?: string;
    onStart?: () => void;
    onEnd?: () => void;
}

const props = withDefaults(defineProps<Props>(), {
    from: 0,
    direction: 'up',
    delay: 0,
    duration: 0.8,
    className: '',
    startWhen: true,
    separator: ''
});

const element = ref<HTMLSpanElement>();
const currentValue = ref(initialValue());
const isInView = ref(false);
const hasStarted = ref(false);
let observer: IntersectionObserver | undefined;
let animationFrame: number | undefined;
let delayTimer: number | undefined;
const displayValue = computed(() => {
    const value = Math.round(currentValue.value);
    if (!props.separator) {
        return String(value);
    }
    return new Intl.NumberFormat('zh-CN').format(value).replaceAll(',', props.separator);
});

function initialValue() {
    return props.direction === 'down' ? finiteNumber(props.to) : finiteNumber(props.from);
}

function targetValue() {
    return props.direction === 'down' ? finiteNumber(props.from) : finiteNumber(props.to);
}

function finiteNumber(value: number) {
    return Number.isFinite(value) ? value : 0;
}

function reducedMotion() {
    return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function cancelAnimation() {
    if (animationFrame !== undefined) {
        cancelAnimationFrame(animationFrame);
        animationFrame = undefined;
    }
    if (delayTimer !== undefined) {
        window.clearTimeout(delayTimer);
        delayTimer = undefined;
    }
}

function startAnimation() {
    if (hasStarted.value || !isInView.value || !props.startWhen) {
        return;
    }
    hasStarted.value = true;
    props.onStart?.();
    const start = () => {
        const from = initialValue();
        const to = targetValue();
        const duration = Math.max(0, props.duration) * 1000;
        if (reducedMotion() || duration === 0 || from === to) {
            currentValue.value = to;
            props.onEnd?.();
            return;
        }
        const startedAt = performance.now();
        const tick = (timestamp: number) => {
            const progress = Math.min((timestamp - startedAt) / duration, 1);
            const easedProgress = 1 - Math.pow(1 - progress, 3);
            currentValue.value = from + (to - from) * easedProgress;
            if (progress < 1) {
                animationFrame = requestAnimationFrame(tick);
                return;
            }
            animationFrame = undefined;
            currentValue.value = to;
            props.onEnd?.();
        };
        animationFrame = requestAnimationFrame(tick);
    };
    delayTimer = window.setTimeout(start, Math.max(0, props.delay) * 1000);
}

function resetAnimation() {
    cancelAnimation();
    currentValue.value = initialValue();
    hasStarted.value = false;
    startAnimation();
}

onMounted(() => {
    if (!element.value || typeof IntersectionObserver === 'undefined') {
        isInView.value = true;
        startAnimation();
        return;
    }
    observer = new IntersectionObserver(([entry]) => {
        if (entry.isIntersecting) {
            isInView.value = true;
            startAnimation();
        }
    });
    observer.observe(element.value);
});

watch(() => [props.to, props.from, props.direction, props.delay, props.duration, props.startWhen], resetAnimation);
onBeforeUnmount(() => {
    cancelAnimation();
    observer?.disconnect();
});
</script>

<template>
    <!-- 数值展示 -->
    <span ref="element" :class="className">{{ displayValue }}</span>
</template>
