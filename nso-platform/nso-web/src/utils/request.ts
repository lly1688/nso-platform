import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse, RuleBlockDetail } from '@/types';

// 认证会话与重试请求的内部类型。
type AuthSession = {
    accessToken?: string;
    refreshToken?: string;
};

type RetryConfig = InternalAxiosRequestConfig & {
    _nsoRetried?: boolean;
};

export type NsoRequestError = Error & {
    status?: number;
    detail?: RuleBlockDetail;
};

export const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
    timeout: 15000
});

// 同一时刻只允许一个令牌刷新请求。
let refreshPromise: Promise<string> | undefined;

function clearLocalSession() {
    localStorage.removeItem('nso_access_token');
    localStorage.removeItem('nso_refresh_token');
    localStorage.removeItem('nso_session');
    localStorage.removeItem('nso_profile');
}

function signal(name: 'nso-auth-required' | 'nso-access-denied') {
    window.dispatchEvent(new Event(name));
}

function asRuleBlockDetail(value: unknown): RuleBlockDetail | undefined {
    if (!value || typeof value !== 'object') {
        return undefined;
    }
    const detail = value as RuleBlockDetail;
    return detail.ruleCode || detail.reason || detail.currentValue || detail.expectedValue || detail.action
        ? detail
        : undefined;
}

function asError(message: string, status?: number, detail?: RuleBlockDetail) {
    const error = new Error(message) as NsoRequestError;
    error.status = status;
    error.detail = detail;
    return error;
}

function errorMessage(error: AxiosError<ApiResponse<unknown>>) {
    const body = error.response?.data;
    const detail = asRuleBlockDetail(body?.data);
    const message = detail?.reason || body?.message || (error.response?.status === 401 ? '登录已过期，请重新登录' : error.response?.status === 403 ? '当前账号没有操作权限' : '网络请求失败，请稍后重试');
    return detail?.ruleCode ? `${detail.ruleCode}：${message}` : message;
}

export function getRuleBlockDetail(error: unknown): RuleBlockDetail | undefined {
    if (!error || typeof error !== 'object') {
        return undefined;
    }
    return asRuleBlockDetail((error as NsoRequestError).detail);
}

async function refreshAccessToken() {
    // 多个并发请求共用同一个刷新 Promise，避免重复刷新令牌。
    const refreshToken = localStorage.getItem('nso_refresh_token');
    if (!refreshToken) {
        throw asError('登录已过期，请重新登录', 401);
    }
    if (!refreshPromise) {
        const baseURL = http.defaults.baseURL || '/api/v1';
        refreshPromise = axios.post<ApiResponse<AuthSession>>(`${baseURL}/admin/auth/refresh`, { refreshToken }, { timeout: http.defaults.timeout })
            .then((response) => {
            const session = response.data;
            if (session.code !== 0 || !session.data?.accessToken) {
                throw asError(session.message || '登录刷新失败', 401);
            }
            localStorage.setItem('nso_access_token', session.data.accessToken);
            if (session.data.refreshToken) {
                localStorage.setItem('nso_refresh_token', session.data.refreshToken);
            }
            localStorage.setItem('nso_session', JSON.stringify(session.data));
            window.dispatchEvent(new CustomEvent('nso-session-refreshed', { detail: session.data }));
            return session.data.accessToken;
        })
            .finally(() => {
                refreshPromise = undefined;
            });
    }
    return refreshPromise;
}

// 请求拦截器统一附加访问令牌和写操作幂等标识。
http.interceptors.request.use((config) => {
    const token = localStorage.getItem('nso_access_token');
    const isAuthRequest = config.url?.includes('/admin/auth/');
    if (token && !isAuthRequest) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    if (config.method && ['post', 'put', 'patch', 'delete'].includes(config.method.toLowerCase())) {
        config.headers['X-Request-Id'] = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    }
    return config;
});

// 响应拦截器统一转换业务错误，并在令牌失效后续期重试。
http.interceptors.response.use((response) => {
    const body = response.data as ApiResponse<unknown>;
    if (body && typeof body.code === 'number' && body.code !== 0) {
        const detail = asRuleBlockDetail(body.data);
        const rulePrefix = detail?.ruleCode ? `${detail.ruleCode}：` : '';
        return Promise.reject(asError(rulePrefix + (detail?.reason || body.message || '接口请求失败'), body.code, detail));
    }
    return response;
}, async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetryConfig | undefined;
    const status = error.response?.status;
    const isAuthRequest = original?.url?.includes('/admin/auth/');
    if (status === 401 && original && !original._nsoRetried && !isAuthRequest) {
        original._nsoRetried = true;
        try {
            const accessToken = await refreshAccessToken();
            original.headers.Authorization = `Bearer ${accessToken}`;
            return http.request(original);
        }
        catch {
            // 刷新失败时清理会话，随后由响应拦截器跳转到登录页。
        }
    }
    if (status === 401) {
        clearLocalSession();
        signal('nso-auth-required');
    }
    else if (status === 403) {
        signal('nso-access-denied');
    }
    return Promise.reject(asError(errorMessage(error), status, asRuleBlockDetail(error.response?.data?.data)));
});

export function userFacingError(error: unknown, fallback = '操作未完成，请稍后重试') {
    return error instanceof Error && error.message ? error.message : fallback;
}

// 常用请求方法仅返回统一响应中的数据主体。
export async function getData<T>(url: string, params?: object) {
    const response = await http.get<ApiResponse<T>>(url, { params });
    return response.data.data;
}

export async function postData<T>(url: string, data?: unknown) {
    const response = await http.post<ApiResponse<T>>(url, data);
    return response.data.data;
}

export async function putData<T>(url: string, data?: unknown) {
    const response = await http.put<ApiResponse<T>>(url, data);
    return response.data.data;
}
