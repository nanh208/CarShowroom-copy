// Đổi URL ảnh tĩnh sang bản WebP đã tạo sẵn (scripts/optimize-images.mjs). Ảnh khác (uploads, blob, data:) giữ nguyên.
import list from './webp-manifest.json'
const S = new Set(list)
export const img = s => (typeof s === 'string' && S.has(s.split('?')[0]) ? s.replace(/\.(jpe?g|png)(?=\?|$)/i, '.webp') : s)
export const hasWebp = s => S.has(s)
