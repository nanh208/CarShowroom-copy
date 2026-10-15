// Kết nối STOMP/WebSocket dùng chung (live chat thời gian thực). Tự kết nối lại; nếu không kết nối được thì các màn hình vẫn polling.
import { Client } from '@stomp/stompjs'
import { ref } from 'vue'
export const rtConnected = ref(false)
let client = null
const subs = new Map() // key -> { dest, cb, handle }
const wsUrl = () => {
  const base = import.meta.env.VITE_API_URL
  if (base) return base.replace(/^http/, 'ws') + '/ws'
  return (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws'
}
function ensure() {
  if (client) return client
  client = new Client({
    brokerURL: wsUrl(), reconnectDelay: 4000, heartbeatIncoming: 20000, heartbeatOutgoing: 20000,
    beforeConnect: () => { const t = localStorage.getItem('token'); client.connectHeaders = t ? { Authorization: 'Bearer ' + t } : {} },
    onConnect: () => { rtConnected.value = true; for (const s of subs.values()) s.handle = client.subscribe(s.dest, f => { try { s.cb(JSON.parse(f.body)) } catch { /* bỏ qua */ } }) },
    onWebSocketClose: () => { rtConnected.value = false },
    onStompError: () => { rtConnected.value = false },
    debug: () => {}
  })
  client.activate()
  return client
}
let seqK = 0
export function subscribe(dest, cb) {
  const c = ensure(), key = ++seqK, s = { dest, cb, handle: null }
  subs.set(key, s)
  if (c.connected) s.handle = c.subscribe(dest, f => { try { cb(JSON.parse(f.body)) } catch { /* bỏ qua */ } })
  return () => { try { s.handle?.unsubscribe() } catch { /* bỏ qua */ } subs.delete(key) }
}
export function publish(dest, body) { const c = ensure(); if (c.connected) c.publish({ destination: dest, body: JSON.stringify(body) }) }
/** Kết nối lại với token mới (sau đăng nhập/đăng xuất). */
export function reconnect() { if (client) { client.deactivate().then(() => client.activate()) } }
