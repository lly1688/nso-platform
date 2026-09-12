<script setup lang="ts">
export interface WorkflowStep {
    key: string;
    label: string;
    state?: 'done' | 'current' | 'blocked' | 'pending' | 'neutral';
    hint?: string;
}

withDefaults(defineProps<{
    steps: WorkflowStep[];
    currentKey?: string;
    ariaLabel?: string;
}>(), {
    ariaLabel: '业务流程'
});

function resolvedState(step: WorkflowStep, currentKey?: string): NonNullable<WorkflowStep['state']> {
    return step.state || (step.key === currentKey ? 'current' : 'pending');
}

function stateLabel(state: NonNullable<WorkflowStep['state']>): string {
    return ({ done: '已完成', current: '当前步骤', blocked: '存在阻断', pending: '待开始', neutral: '流程节点' } as Record<string, string>)[state];
}
</script>

<template>
    <nav class="workflow-strip" :aria-label="ariaLabel" :style="{ '--workflow-count': steps.length }">
        <div
            v-for="(step, index) in steps"
            :key="step.key"
            class="workflow-strip__step"
            :class="`workflow-strip__step--${resolvedState(step, currentKey)}`"
            :aria-current="resolvedState(step, currentKey) === 'current' ? 'step' : undefined"
        >
            <span class="workflow-strip__index">{{ resolvedState(step, currentKey) === 'done' ? '✓' : String(index + 1).padStart(2, '0') }}</span>
            <span class="workflow-strip__copy">
                <strong>{{ step.label }}</strong>
                <small v-if="step.hint">{{ step.hint }}</small>
                <span class="visually-hidden">{{ stateLabel(resolvedState(step, currentKey)) }}</span>
            </span>
            <span v-if="index < steps.length - 1" class="workflow-strip__connector" aria-hidden="true"></span>
        </div>
    </nav>
</template>
