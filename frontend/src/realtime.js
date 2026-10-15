// Kết nối STOMP/WebSocket dùng chung (live chat thời gian thực). Tự kết nối lại; nếu không kết nối được thì các màn hình vẫn polling.
import { Client } from '@stomp/stompjs'
import { ref } from 'vue'
export const rtConnected = ref(false)
let client = null
let unloading = false
// Socket còn mở thật sự? (tránh gửi frame lên socket đang CLOSING/CLOSED -> cảnh báo console)
const isOpen = () => !!client && client.connected && client.webSocket?.readyState === 1
if (typeof window !== 'undefined') window.addEventListener('pagehide', () => { unloading = true; const c = client; client = null; rtConnected.value = false; if (c) { c.reconnectDelay = 0; c.forceDisconnect?.(); c.deactivate().catch(() => {}) } })
let idleTimer = null
const subs = new Map() // key -> { dest, cb, handle }
const wsUrl = () => {
  const base = import.meta.env.VITE_API_URL
  if (base) return base.replace(/^http/, 'ws') + '/ws'
  return (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws'
}
function ensure() {
  clearTimeout(idleTimer)
  if (client || unloading) return client
  const c = new Client({
    brokerURL: wsUrl(), reconnectDelay: 4000, heartbeatIncoming: 20000, heartbeatOutgoing: 20000,
    beforeConnect: () => { const t = localStorage.getItem('token'); c.connectHeaders = t ? { Authorization: 'Bearer ' + t } : {} },
    onConnect: () => { rtConnected.value = true; for (const s of subs.values()) s.handle = c.subscribe(s.dest, f => { try { s.cb(JSON.parse(f.body)) } catch { /* bỏ qua */ } }) },
    onWebSocketClose: () => { rtConnected.value = false },
    onStompError: () => { rtConnected.value = false },
    debug: () => {}
  })
  client = c
  c.activate()
  return c
}
let seqK = 0
export function subscribe(dest, cb) {
  const c = ensure(), key = ++seqK, s = { dest, cb, handle: null }
  subs.set(key, s)
  if (c && isOpen()) s.handle = c.subscribe(dest, f => { try { cb(JSON.parse(f.body)) } catch { /* bỏ qua */ } })
  return () => {
    subs.delete(key)
    if (!unloading && isOpen()) { try { s.handle?.unsubscribe() } catch { /* bỏ qua */ } }
    // Không còn ai nghe -> đóng kết nối sau 3s (tránh socket treo khi đổi trang)
    if (!subs.size) { clearTimeout(idleTimer); idleTimer = setTimeout(() => { if (!subs.size && client) { const c = client; client = null; rtConnected.value = false; c.deactivate().catch(() => {}) } }, 3000) }
  }
}
export function publish(dest, body) { ensure(); if (isOpen()) client.publish({ destination: dest, body: JSON.stringify(body) }) }
/** Kết nối lại với token mới (sau đăng nhập/đăng xuất). */
export function reconnect() { if (client && !unloading) { const c = client; c.deactivate().then(() => { if (client === c) c.activate() }) } }
