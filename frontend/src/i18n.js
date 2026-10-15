// Đa ngôn ngữ vi/en: từ điển lấy từ backend (/api/public/i18n/{lang}), khoá = chuỗi tiếng Việt gốc.
// Lưu lựa chọn ở localStorage + tuỳ chọn tài khoản (/api/account/prefs/locale) khi đã đăng nhập.
import { ref, reactive } from 'vue'
import api from './api'
export const locale = ref(localStorage.getItem('locale') === 'en' ? 'en' : 'vi')
const messages = reactive({})
export function t(s) { return locale.value === 'vi' ? s : (messages[s] || s) }
async function load(lang) {
  for (const k of Object.keys(messages)) delete messages[k]
  if (lang === 'vi') return
  try { Object.assign(messages, (await api.get('/public/i18n/' + lang)).data.messages); localStorage.setItem('i18n.' + lang, JSON.stringify(messages)) }
  catch { try { Object.assign(messages, JSON.parse(localStorage.getItem('i18n.' + lang) || '{}')) } catch { /* bỏ qua */ } }
}
export async function setLocale(lang, { persist = true } = {}) {
  lang = lang === 'en' ? 'en' : 'vi'
  await load(lang); locale.value = lang; localStorage.setItem('locale', lang); document.documentElement.lang = lang
  if (persist && localStorage.getItem('token')) api.put('/account/prefs/locale', JSON.stringify(lang), { headers: { 'Content-Type': 'application/json' } }).catch(() => {})
}
/** Sau đăng nhập: dùng ngôn ngữ đã lưu trong tài khoản. */
export async function syncLocale() {
  try { const { data } = await api.get('/account/prefs'); if (data.locale && data.locale !== locale.value) await setLocale(data.locale, { persist: false }) } catch { /* bỏ qua */ }
}
export default { install(app) { app.config.globalProperties.$t = t; document.documentElement.lang = locale.value; if (locale.value !== 'vi') load(locale.value) } }
