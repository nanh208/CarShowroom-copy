// Tạo bản WebP (≤1600px, q78) cạnh mỗi ảnh JPG/PNG trong public/images và ghi src/webp-manifest.json.
// Chạy tự động trước build (prebuild); chỉ xử lý ảnh mới/đổi.
import sharp from 'sharp'
import { readdir, stat, writeFile } from 'node:fs/promises'
import { join, relative, sep } from 'node:path'
const ROOT = new URL('../public/', import.meta.url).pathname, DIR = join(ROOT, 'images')
async function* walk(d) { for (const e of await readdir(d, { withFileTypes: true })) { const p = join(d, e.name); if (e.isDirectory()) yield* walk(p); else yield p } }
const list = []; let made = 0, before = 0, after = 0
for await (const f of walk(DIR)) {
  if (!/\.(jpe?g|png)$/i.test(f)) continue
  const out = f.replace(/\.(jpe?g|png)$/i, '.webp'), s = await stat(f)
  let o = null; try { o = await stat(out) } catch {}
  if (!o || o.mtimeMs < s.mtimeMs) { await sharp(f).rotate().resize({ width: 1600, withoutEnlargement: true }).webp({ quality: 78, effort: 5 }).toFile(out); o = await stat(out); made++ }
  if (o.size >= s.size) continue // WebP không nhỏ hơn thì giữ ảnh gốc
  before += s.size; after += o.size
  list.push('/' + relative(ROOT, f).split(sep).join('/'))
}
list.sort()
await writeFile(new URL('../src/webp-manifest.json', import.meta.url), JSON.stringify(list) + '\n')
console.log(`webp: ${list.length} images (${made} new) ${(before / 1048576).toFixed(1)}MB → ${(after / 1048576).toFixed(1)}MB`)
