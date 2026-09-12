<script setup lang="ts">
// 品牌标识展示配置。
import { computed } from 'vue';

type BrandLogoSize = 'sm' | 'md' | 'lg';

const props = withDefaults(defineProps<{
    compact?: boolean;
    inverse?: boolean;
    label?: string;
    tagline?: string;
    size?: BrandLogoSize;
}>(), {
    compact: false,
    inverse: false,
    label: '智慧非标协同',
    tagline: 'INDUSTRIAL OS',
    size: 'md',
});

const ariaLabel = computed(() => props.tagline ? `${props.label}，${props.tagline}` : props.label);
</script>

<template>
    <!-- 品牌标识 -->
    <div
        class="brand-logo"
        :class="[`brand-logo--${size}`, { 'brand-logo--compact': compact, 'brand-logo--inverse': inverse }]"
        role="img"
        :aria-label="ariaLabel"
    >
        <img class="brand-logo__mark" src="/brand/nso-mark.svg" alt="" aria-hidden="true" />
        <span v-if="!compact" class="brand-logo__copy">
            <strong>{{ label }}</strong>
            <small v-if="tagline">{{ tagline }}</small>
        </span>
    </div>
</template>

<style scoped>
/* 组件局部样式。 */
.brand-logo {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 12px;
}

.brand-logo__mark {
    display: block;
    flex: 0 0 auto;
    width: 44px;
    height: 44px;
}

.brand-logo__copy {
    display: grid;
    min-width: 0;
    gap: 3px;
}

.brand-logo__copy strong,
.brand-logo__copy small {
    display: block;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.brand-logo--inverse .brand-logo__copy strong {
    color: #f8f9fc;
}

.brand-logo--inverse .brand-logo__copy small {
    color: #d7dde8;
}

.brand-logo__copy strong {
    color: var(--nso-ink, #29343d);
    font-size: 20px;
    font-weight: 700;
    line-height: 1.1;
}

.brand-logo__copy small {
    color: var(--nso-muted, #66727d);
    font-family: inherit;
    font-size: 12px;
    font-weight: 550;
    letter-spacing: .06em;
    line-height: 1.2;
}

.brand-logo--sm .brand-logo__mark {
    width: 36px;
    height: 36px;
}

.brand-logo--sm .brand-logo__copy strong {
    font-size: 17px;
}

.brand-logo--sm .brand-logo__copy small {
    font-size: 11px;
}

.brand-logo--lg .brand-logo__mark {
    width: 56px;
    height: 56px;
}

.brand-logo--lg .brand-logo__copy strong {
    font-size: 24px;
}

.brand-logo--lg .brand-logo__copy small {
    font-size: 13px;
}

.brand-logo--compact {
    gap: 0;
}
</style>
