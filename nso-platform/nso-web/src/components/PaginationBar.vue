<script setup lang="ts">
// 分页组件输入与事件。
import { computed, unref, type Ref } from 'vue';

const props = withDefaults(defineProps<{
    pageNo: number | Ref<number>;
    pageSize: number | Ref<number>;
    total: number | Ref<number>;
    loading?: boolean | Ref<boolean>;
}>(), {
    loading: false
});

const emit = defineEmits<{
    'update:page-no': [value: number];
    'update:page-size': [value: number];
}>();

function changePage(value: number) {
    emit('update:page-no', value);
}

function changePageSize(value: number) {
    emit('update:page-size', value);
}

const pageNo = computed(() => unref(props.pageNo));
const pageSize = computed(() => unref(props.pageSize));
const total = computed(() => unref(props.total));
const loading = computed(() => unref(props.loading));
</script>

<template>
    <!-- 分页操作区 -->
    <div class="pagination-bar" :class="{ 'is-loading': loading }">
        <span class="pagination-bar__total">共 {{ total }} 条</span>
        <el-pagination
            background
            size="small"
            :current-page="pageNo"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            :disabled="loading"
            layout="sizes, prev, pager, next, jumper"
            @current-change="changePage"
            @size-change="changePageSize"
        />
    </div>
</template>
