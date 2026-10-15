// Đa ngôn ngữ vi/en: từ điển lấy từ backend (/api/public/i18n/{lang}), khoá = chuỗi tiếng Việt gốc.
// Lưu lựa chọn ở localStorage + tuỳ chọn tài khoản (/api/account/prefs/locale) khi đã đăng nhập.
import { ref, reactive } from 'vue'
import api from './api'
export const locale = ref(localStorage.getItem('locale') === 'en' ? 'en' : 'vi')
const messages = reactive({})
export function t(s, ...args) {
  let out = locale.value === 'vi' ? s : (messages[s] || translateString(s))
  if (args.length) out = out.replace(/\{(\d+)\}/g, (m, i) => args[+i] ?? m)
  return out
}

// ===== Dịch toàn trang lúc chạy: khớp nguyên văn hoặc theo mẫu "{0}" cho mọi nút chữ & thuộc tính =====
// Không cần bọc $t ở từng template: MutationObserver dịch nút chữ Vue render ra (và khôi phục khi về tiếng Việt).
let patterns = [], cache = new Map()
const VI = /[À-ỹđĐ]/
const EDGE = /^([\s·•—–\-|:,(]*)([\s\S]*?)([\s·•—–\-|:,)!?.…]*)$/
function rebuild() {
  cache = new Map()
  patterns = Object.keys(messages).filter(k => /\{\d+\}/.test(k)).map(k => {
    const parts = k.split(/(\{\d+\})/), idx = []
    const src = parts.map(p => { const m = /^\{(\d+)\}$/.exec(p); if (m) { idx.push(+m[1]); return '(.+?)' } return p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') }).join('')
    return { re: new RegExp('^' + src + '$'), idx, out: messages[k], lit: k.replace(/\{\d+\}/g, '').length }
  }).sort((a, b) => b.lit - a.lit)
}
function exact(s) { return messages[s] ?? null }
function viaPattern(s) {
  for (const p of patterns) {
    const m = p.re.exec(s); if (!m) continue
    const vals = []; let ok = true
    p.idx.forEach((n, i) => { const v = m[i + 1], tv = messages[v]; if (!tv && VI.test(v) && p.lit < 12) ok = false; vals[n] = tv || v })
    if (!ok) continue // mẫu quá ngắn mà phần biến là tiếng Việt chưa dịch → bỏ (tránh dịch nửa câu)
    return p.out.replace(/\{(\d+)\}/g, (x, n) => vals[+n] ?? x)
  }
  return null
}
export function translateString(s) {
  if (!s || !VI.test(s) || locale.value === 'vi') return s
  if (cache.has(s)) return cache.get(s)
  const norm = s.trim().replace(/\s+/g, ' '), m = EDGE.exec(s), mid = m && m[2] ? m[2].replace(/\s+/g, ' ') : null
  let out = exact(norm)
  if (out == null && mid && mid !== norm) { const c = exact(mid); if (c != null) out = m[1] + c + m[3] }
  if (out == null) out = viaPattern(norm)
  if (out == null && mid && mid !== norm) { const c = viaPattern(mid); if (c != null) out = m[1] + c + m[3] }
  if (out == null && s.includes('\n')) { const lines = s.split('\n').map(l => translateString(l)); out = lines.join('\n') }
  out = out ?? s; if (cache.size > 5000) cache.clear(); cache.set(s, out); return out
}
// Ngày giờ: khi xem bằng EN, các lời gọi toLocale*('vi-VN') trên Date tự đổi sang en-GB (thứ/tháng tiếng Anh, giữ dd/mm).
for (const fn of ['toLocaleDateString', 'toLocaleTimeString', 'toLocaleString']) {
  const orig = Date.prototype[fn]
  Date.prototype[fn] = function (lc, opt) { return orig.call(this, locale.value === 'en' && (lc === 'vi-VN' || lc === 'vi') ? 'en-GB' : lc, opt) }
}
const ATTRS = ['placeholder', 'title', 'aria-label', 'alt']
const SKIP = new Set(['SCRIPT', 'STYLE', 'TEXTAREA', 'CODE', 'PRE', 'NOSCRIPT'])
const txt = new WeakMap() // node -> { orig, out }
function skipEl(el) { for (let e = el; e && e.nodeType === 1; e = e.parentElement) { if (SKIP.has(e.tagName) || e.hasAttribute('data-no-i18n') || e.isContentEditable) return true } return false }
function doText(n) {
  const v = n.nodeValue, rec = txt.get(n)
  if (rec && v === rec.out) { if (locale.value === 'vi') { n.nodeValue = rec.orig; txt.delete(n) } return }
  if (locale.value === 'vi' || !v || !VI.test(v) || skipEl(n.parentElement)) return
  const out = translateString(v); if (out !== v) { txt.set(n, { orig: v, out }); n.nodeValue = out }
}
function doAttrs(el) {
  if (skipEl(el)) return
  const store = el.__i18n || (el.__i18n = {})
  for (const a of ATTRS) {
    const v = el.getAttribute(a); if (v == null) continue
    const rec = store[a]
    if (rec && v === rec.out) { if (locale.value === 'vi') { el.setAttribute(a, rec.orig); delete store[a] } continue }
    if (locale.value === 'vi' || !VI.test(v)) continue
    const out = translateString(v); if (out !== v) { store[a] = { orig: v, out }; el.setAttribute(a, out) }
  }
}
function walk(root) {
  if (root.nodeType === 3) return doText(root)
  if (root.nodeType !== 1) return
  doAttrs(root)
  const tw = document.createTreeWalker(root, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT)
  for (let n = tw.nextNode(); n; n = tw.nextNode()) n.nodeType === 3 ? doText(n) : doAttrs(n)
}
let mo = null
function startObserver() {
  if (mo || typeof MutationObserver === 'undefined') return
  mo = new MutationObserver(list => {
    for (const m of list) {
      if (m.type === 'characterData') doText(m.target)
      else if (m.type === 'attributes') doAttrs(m.target)
      else m.addedNodes.forEach(walk)
    }
  })
  mo.observe(document.body, { subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ATTRS })
}
function retranslateAll() { if (document.body) walk(document.body); document.title = locale.value === 'vi' ? (document.__viTitle || document.title) : translateString(document.__viTitle = document.__viTitle || document.title) }
async function load(lang) {
  for (const k of Object.keys(messages)) delete messages[k]
  if (lang === 'vi') { rebuild(); return }
  try { Object.assign(messages, (await api.get('/public/i18n/' + lang)).data.messages); localStorage.setItem('i18n.' + lang, JSON.stringify(messages)) }
  catch { try { Object.assign(messages, JSON.parse(localStorage.getItem('i18n.' + lang) || '{}')) } catch { /* bỏ qua */ } }
  rebuild()
}
export async function setLocale(lang, { persist = true } = {}) {
  lang = lang === 'en' ? 'en' : 'vi'
  await load(lang); locale.value = lang; localStorage.setItem('locale', lang); document.documentElement.lang = lang
  retranslateAll()
  if (persist && localStorage.getItem('token')) api.put('/account/prefs/locale', JSON.stringify(lang), { headers: { 'Content-Type': 'application/json' } }).catch(() => {})
}
/** Sau đăng nhập: dùng ngôn ngữ đã lưu trong tài khoản. */
export async function syncLocale() {
  try { const { data } = await api.get('/account/prefs'); if (data.locale && data.locale !== locale.value) await setLocale(data.locale, { persist: false }) } catch { /* bỏ qua */ }
}
export default { install(app) {
  app.config.globalProperties.$t = t; document.documentElement.lang = locale.value
  const go = () => { startObserver(); if (locale.value !== 'vi') load(locale.value).then(retranslateAll) }
  document.body ? go() : addEventListener('DOMContentLoaded', go)
} }
