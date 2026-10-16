// Nén sẵn dist (gzip + brotli) cho nginx gzip_static/brotli_static — xem deploy/nginx.conf.
import { readdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { gzipSync, brotliCompressSync, constants } from 'node:zlib'
const DIST = new URL('../dist/', import.meta.url).pathname
async function* walk(d) { for (const e of await readdir(d, { withFileTypes: true })) { const p = join(d, e.name); if (e.isDirectory()) yield* walk(p); else yield p } }
let n = 0, raw = 0, br = 0
for await (const f of walk(DIST)) {
  if (!/\.(js|css|html|svg|json|xml|txt|webmanifest|glb)$/.test(f)) continue
  const b = await readFile(f); if (b.length < 1024) continue
  const g = gzipSync(b, { level: 9 }), r = brotliCompressSync(b, { params: { [constants.BROTLI_PARAM_QUALITY]: 11 } })
  await writeFile(f + '.gz', g); await writeFile(f + '.br', r); n++; raw += b.length; br += r.length
}
console.log(`compressed ${n} files ${(raw / 1024).toFixed(0)}KB → br ${(br / 1024).toFixed(0)}KB`)
