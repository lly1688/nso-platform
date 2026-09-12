import { http } from './request';

const PREVIEW_TYPES = new Set(['application/pdf', 'image/png', 'image/jpeg', 'image/webp']);

export function canPreviewFile(contentType?: string, fileName?: string) {
    const type = (contentType || '').split(';', 1)[0].toLowerCase();
    if (PREVIEW_TYPES.has(type)) return true;
    return /\.(pdf|png|jpe?g|webp)$/i.test(fileName || '');
}

export async function openProtectedFile(url: string, options: { preview: boolean; fileName?: string; contentType?: string }) {
    const response = await http.get<Blob>(url, { responseType: 'blob' });
    const responseType = typeof response.headers['content-type'] === 'string' ? response.headers['content-type'] : undefined;
    const contentType = options.contentType || responseType || 'application/octet-stream';
    const blob = new Blob([response.data], { type: contentType });
    const objectUrl = URL.createObjectURL(blob);
    if (options.preview && canPreviewFile(contentType, options.fileName)) {
        const popup = window.open(objectUrl, '_blank', 'noopener');
        if (!popup) URL.revokeObjectURL(objectUrl);
        else window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
        return;
    }
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = options.fileName || 'download';
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1_000);
}
