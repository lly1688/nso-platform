import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { PageParams, PageResult } from '@/types';

// 分页默认值与单页上限。
const DEFAULT_PAGE_NO = 1;
const DEFAULT_PAGE_SIZE = 20;
const MAX_PAGE_SIZE = 100;

function firstValue(value: unknown): string | undefined {
    return Array.isArray(value) ? value[0] : typeof value === 'string' ? value : undefined;
}

function normalizePageNo(value: unknown): number {
    const pageNo = Number(firstValue(value));
    return Number.isInteger(pageNo) && pageNo > 0 ? pageNo : DEFAULT_PAGE_NO;
}

function normalizePageSize(value: unknown, fallback = DEFAULT_PAGE_SIZE): number {
    const pageSize = Number(firstValue(value));
    if (!Number.isInteger(pageSize) || pageSize < 1) {
        return fallback;
    }
    return Math.min(pageSize, MAX_PAGE_SIZE);
}

export interface PaginationOptions {
    /** 次级表格使用的路由查询参数前缀。 */
    queryPrefix?: string;
    /** 未指定地址栏参数时的初始页大小。 */
    defaultPageSize?: number;
}

/**
 * 统一管理列表分页、地址栏同步与重复请求复用。
 */
export function usePagination<T>(options: PaginationOptions = {}) {
    const route = useRoute();
    const router = useRouter();
    const prefix = options.queryPrefix ? `${options.queryPrefix}` : '';
    const pageNoKey = prefix ? `${prefix}PageNo` : 'pageNo';
    const pageSizeKey = prefix ? `${prefix}PageSize` : 'pageSize';
    const defaultPageSize = normalizePageSize(options.defaultPageSize);
    const pageNo = ref(normalizePageNo(route.query[pageNoKey]));
    const pageSize = ref(normalizePageSize(route.query[pageSizeKey], defaultPageSize));
    const total = ref(0);
    const loading = ref(false);
    const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

    // 请求去重与最新响应保护。
    let requestVersion = 0;
    let activeRequest: Promise<void> | undefined;
    let activeRequestKey = '';
    let loader: ((params: Required<PageParams>) => Promise<PageResult<T>>) | undefined;
    let consumer: ((result: PageResult<T>) => void) | undefined;

    const params = (): Required<PageParams> => ({ pageNo: pageNo.value, pageSize: pageSize.value });

    // 将分页状态同步到路由，支持前进、后退和链接分享。
    async function syncRoute() {
        await router.replace({
            query: {
                ...route.query,
                [pageNoKey]: String(pageNo.value),
                [pageSizeKey]: String(pageSize.value)
            }
        });
    }

    async function performReload(): Promise<void> {
        if (!loader || !consumer) {
            return;
        }
        const requestKey = `${pageNo.value}:${pageSize.value}`;
        if (activeRequest && activeRequestKey === requestKey) {
            return activeRequest;
        }

        const version = ++requestVersion;
        loading.value = true;
        activeRequestKey = requestKey;
        const request = (async () => {
            try {
                const result = await loader!(params());
                if (version !== requestVersion) {
                    return;
                }
                total.value = Math.max(0, Number(result.total) || 0);
                const lastPage = Math.max(DEFAULT_PAGE_NO, Math.ceil(total.value / pageSize.value));
                if (total.value > 0 && pageNo.value > lastPage) {
                    pageNo.value = lastPage;
                    await syncRoute();
                    return performReload();
                }
                consumer!(result);
            }
            finally {
                if (version === requestVersion) {
                    loading.value = false;
                }
            }
        })();
        activeRequest = request;
        try {
            await request;
        }
        finally {
            if (activeRequest === request) {
                activeRequest = undefined;
                activeRequestKey = '';
            }
        }
    }

    function configure(
        nextLoader: (params: Required<PageParams>) => Promise<PageResult<T>>,
        nextConsumer: (result: PageResult<T>) => void
    ) {
        loader = nextLoader;
        consumer = nextConsumer;
    }

    async function goTo(nextPageNo: number) {
        const normalized = normalizePageNo(String(nextPageNo));
        if (normalized === pageNo.value) {
            return performReload();
        }
        pageNo.value = normalized;
        await syncRoute();
        return performReload();
    }

    async function changePageSize(nextPageSize: number) {
        const normalized = normalizePageSize(String(nextPageSize), defaultPageSize);
        if (normalized === pageSize.value && pageNo.value === DEFAULT_PAGE_NO) {
            return performReload();
        }
        pageSize.value = normalized;
        pageNo.value = DEFAULT_PAGE_NO;
        await syncRoute();
        return performReload();
    }

    async function reset() {
        if (pageNo.value === DEFAULT_PAGE_NO) {
            return performReload();
        }
        pageNo.value = DEFAULT_PAGE_NO;
        await syncRoute();
        return performReload();
    }

    // 路由参数变化时重新加载对应页数据。
    watch(
        () => [route.query[pageNoKey], route.query[pageSizeKey]],
        ([nextPageNo, nextPageSize]) => {
            const normalizedPageNo = normalizePageNo(nextPageNo);
            const normalizedPageSize = normalizePageSize(nextPageSize, defaultPageSize);
            if (normalizedPageNo !== pageNo.value || normalizedPageSize !== pageSize.value) {
                pageNo.value = normalizedPageNo;
                pageSize.value = normalizedPageSize;
                void performReload();
            }
        }
    );

    return {
        pageNo,
        pageSize,
        total,
        loading,
        pageCount,
        params,
        configure,
        reload: performReload,
        goTo,
        changePageSize,
        reset
    };
}
