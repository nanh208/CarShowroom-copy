import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { existsSync, readFileSync } from 'node:fs'

const MANIFEST = new URL('./src/webp-manifest.json', import.meta.url)
const webpSet = () => new Set(existsSync(MANIFEST) ? JSON.parse(readFileSync(MANIFEST, 'utf8')) : [])
const ATTR = /\s+([^\s=>/]+)(?:=("[^"]*"|'[^']*'))?/g
// Mọi <img> trong template: :src → $img(...) (dùng WebP nếu có), src tĩnh → .webp, thêm loading="lazy" (trừ ảnh có fetchpriority) và decoding="async".
function imgOptimize() {
  return {
    name: 'ares-img-optimize', enforce: 'pre',
    transform(code, id) {
      if (!id.endsWith('.vue') || !code.includes('<img')) return
      const set = webpSet()
      const out = code.replace(/<img\b((?:\s+[^\s=>/]+(?:=(?:"[^"]*"|'[^']*'))?)*)\s*(\/?)>/g, (all, attrs, slash) => {
        const names = [...attrs.matchAll(ATTR)].map(m => m[1])
        let a = attrs.replace(/(\s):src="([^"]*)"/, (m, sp, e) => e.includes('$img(') ? m : `${sp}:src="$img(${e})"`)
        a = a.replace(/(\s)src="([^"]+)"/, (m, sp, v) => set.has(v) ? `${sp}src="${v.replace(/\.(jpe?g|png)$/i, '.webp')}"` : m)
        if (!names.includes('loading') && !names.includes('fetchpriority')) a += ' loading="lazy"'
        if (!names.includes('decoding')) a += ' decoding="async"'
        return `<img${a}${slash ? ' /' : ''}>`
      })
      return out === code ? undefined : { code: out, map: null }
    }
  }
}
// Cache-Control cho vite preview (giống deploy/nginx.conf): asset có hash → 1 năm immutable; HTML → no-cache.
function cacheHeaders() {
  const set = (req, res, next) => {
    const u = req.url.split('?')[0]
    if (u.startsWith('/assets/')) res.setHeader('Cache-Control', 'public, max-age=31536000, immutable')
    else if (/^\/(images|models|icons)\//.test(u)) res.setHeader('Cache-Control', 'public, max-age=604800, stale-while-revalidate=86400')
    else if (u === '/sw.js' || u === '/' || u.endsWith('.html') || !u.includes('.')) res.setHeader('Cache-Control', 'no-cache')
    next()
  }
  return { name: 'ares-cache-headers', configurePreviewServer(s) { s.middlewares.use(set) } }
}

export default defineConfig({
  plugins: [imgOptimize(), vue({ template: { compilerOptions: { isCustomElement: tag => tag === 'model-viewer' } } }), cacheHeaders()],
  build: {
    target: 'es2020', cssCodeSplit: true, chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (/[\\/](vue|@vue|vue-router|pinia)[\\/]/.test(id)) return 'vendor-vue'
          if (/[\\/](axios)[\\/]/.test(id)) return 'vendor-http'
          if (/[\\/]@stomp[\\/]/.test(id)) return 'vendor-stomp'
          if (/[\\/](qrcode|pngjs|dijkstrajs|encode-utf8)[\\/]/.test(id)) return 'vendor-qrcode'
        }
      }
    }
  },
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080', '/uploads': 'http://localhost:8080', '/sitemap.xml': 'http://localhost:8080', '/robots.txt': 'http://localhost:8080', '/ws': { target: 'ws://localhost:8080', ws: true } } }
})
