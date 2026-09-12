// 日期文本解析与显示格式。
type DateValue = string | null | undefined;

interface DateParts {
    year: string;
    month: string;
    day: string;
    hour?: string;
    minute?: string;
}

function parseDateParts(value: DateValue): DateParts | undefined {
    if (!value) {
        return undefined;
    }

    const matched = /^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2}))?/.exec(value.trim());
    if (!matched) {
        return undefined;
    }

    const [, year, month, day, hour, minute] = matched;
    return { year, month, day, hour, minute };
}

function fallback(value: DateValue, emptyText: string) {
    return value?.trim() || emptyText;
}

// 面向页面展示的中文日期格式化方法。
export function formatChineseDate(value: DateValue, emptyText = '-') {
    const parts = parseDateParts(value);
    return parts ? `${parts.year}年${parts.month}月${parts.day}日` : fallback(value, emptyText);
}

export function formatChineseDateTime(value: DateValue, emptyText = '-') {
    const parts = parseDateParts(value);
    if (!parts) {
        return fallback(value, emptyText);
    }

    const date = `${parts.year}年${parts.month}月${parts.day}日`;
    return parts.hour && parts.minute ? `${date} ${parts.hour}:${parts.minute}` : date;
}
