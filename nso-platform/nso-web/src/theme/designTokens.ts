// ECharts 无法直接可靠读取 CSS 变量，图表统一复用这一内部令牌映射。
export const APP_COLORS = {
    primary: '#5D87FF',
    primaryDark: '#456DD9',
    primaryLight: '#8EABFF',
    primarySoft: '#EEF3FF',
    success: '#2AAF8A',
    warning: '#B18435',
    danger: '#B95750',
    dangerDark: '#873C36',
    neutral: '#A8B1BC',
    text: '#29343D',
    textSecondary: '#66727D',
    border: '#E7EAF0',
    divider: '#EFF1F5',
    chartTrack: '#EEF2F7',
    chartShadow: 'rgba(54, 75, 116, .14)'
} as const;

export const CHART_SERIES_COLORS = [
    APP_COLORS.primary,
    APP_COLORS.success,
    APP_COLORS.warning,
    APP_COLORS.danger,
    APP_COLORS.neutral
] as const;
