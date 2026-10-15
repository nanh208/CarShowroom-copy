// Web Push (PWA): đăng ký PushManager với khoá VAPID từ backend, gửi subscription lên /api/push/subscribe.
import { ref } from 'vue'
import api from './api'
export const pushSupported = typeof window !== 'undefined' && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
export const pushOn = ref(false), pushPerm = ref(pushSupported ? Notification.permission : 'unsupported')
const b64 = s => { const p = '='.repeat((4 - s.length % 4) % 4), r = atob((s + p).replace(/-/g, '+').replace(/_/g, '/')); return Uint8Array.from(r, c => c.charCodeAt(0)) }
async function reg() { return navigator.serviceWorker.getRegistration() || navigator.serviceWorker.register('/sw.js') }
export async function refreshPush() {
  if (!pushSupported) return false
  try { const r = await reg(); const s = r && await r.pushManager.getSubscription(); pushOn.value = !!s; return pushOn.value } catch { return false }
}
export async function enablePush() {
  if (!pushSupported) throw new Error('Trình duyệt không hỗ trợ thông báo đẩy')
  const perm = await Notification.requestPermission(); pushPerm.value = perm
  if (perm !== 'granted') throw new Error('Bạn đã chặn quyền thông báo — bật lại trong cài đặt trình duyệt')
  const r = await reg(); await navigator.serviceWorker.ready
  const { data } = await api.get('/public/push/key')
  let s = await r.pushManager.getSubscription()
  if (!s) s = await r.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: b64(data.publicKey) })
  await api.post('/push/subscribe', s.toJSON()); pushOn.value = true
}
export async function disablePush() {
  const r = await reg(); const s = r && await r.pushManager.getSubscription()
  if (s) { await api.post('/push/unsubscribe', { endpoint: s.endpoint }).catch(() => {}); await s.unsubscribe() }
  pushOn.value = false
}
