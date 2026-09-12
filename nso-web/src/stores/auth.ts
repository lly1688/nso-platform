import { defineStore } from 'pinia';
import { api } from '@/api';
import type { UserProfile } from '@/types';

// 登录会话、个人资料与权限判断状态。
export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('nso_session') || 'null') as any,
        token: localStorage.getItem('nso_access_token') || '',
        refreshToken: localStorage.getItem('nso_refresh_token') || '',
        profile: JSON.parse(localStorage.getItem('nso_profile') || 'null') as UserProfile | null,
        avatarObjectUrl: ''
    }),
    actions: {
        // 会话写入同时更新内存状态和浏览器存储。
        saveSession(session: any) {
            // 登录和刷新后同时更新 Pinia 状态与本地存储，保证刷新页面仍可恢复会话。
            this.user = session;
            this.token = session.accessToken || '';
            this.refreshToken = session.refreshToken || '';
            if (this.token) {
                localStorage.setItem('nso_access_token', this.token);
            }
            if (this.refreshToken) {
                localStorage.setItem('nso_refresh_token', this.refreshToken);
            }
            localStorage.setItem('nso_session', JSON.stringify(session));
        },
        clearSession() {
            this.user = null;
            this.token = '';
            this.refreshToken = '';
            this.profile = null;
            if (this.avatarObjectUrl) {
                URL.revokeObjectURL(this.avatarObjectUrl);
            }
            this.avatarObjectUrl = '';
            localStorage.removeItem('nso_access_token');
            localStorage.removeItem('nso_refresh_token');
            localStorage.removeItem('nso_session');
            localStorage.removeItem('nso_profile');
        },
        // 登录成功后加载完整个人资料。
        async login(username: string, password: string) {
            const session = await api.login({ username, password });
            this.saveSession(session);
            await this.loadProfile();
            return session;
        },
        async refresh() {
            if (!this.refreshToken) {
                throw new Error('登录已过期');
            }
            const session = await api.refresh(this.refreshToken);
            this.saveSession(session);
            return session;
        },
        async logout() {
            if (this.refreshToken) {
                try {
                    await api.logout(this.refreshToken);
                }
                catch {
                    // 本地会话撤销继续执行，不阻断退出流程。
                }
            }
            this.clearSession();
        },
        // 资料更新后同步显示名称、角色和头像缓存。
        saveProfile(profile: UserProfile) {
            this.profile = profile;
            localStorage.setItem('nso_profile', JSON.stringify(profile));
            if (this.user) {
                this.user = { ...this.user, nickname: profile.nickname, roles: profile.roles };
                localStorage.setItem('nso_session', JSON.stringify(this.user));
            }
        },
        async loadProfile() {
            const profile = await api.profile();
            this.saveProfile(profile);
            if (this.avatarObjectUrl) {
                URL.revokeObjectURL(this.avatarObjectUrl);
                this.avatarObjectUrl = '';
            }
            if (profile.avatarUrl) {
                try {
                    this.avatarObjectUrl = await api.profileAvatar();
                }
                catch {
                    // 历史头像失效时继续使用当前登录资料，不阻断资料加载。
                    this.avatarObjectUrl = '';
                }
            }
            return profile;
        },
        // 同时兼容业务权限和系统权限前缀。
        can(permission: string) {
            const permissions = this.user?.permissions || [];
            return permissions.includes(permission) || (!permission.startsWith('sys:') && permissions.includes(`nso:${permission}`));
        }
    }
});
