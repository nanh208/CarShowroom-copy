// Gợi ý cài đặt PWA (beforeinstallprompt) + trạng thái mạng
import { ref } from 'vue'
export const canInstall = ref(false), online = ref(navigator.onLine)
let deferred = null
addEventListener('beforeinstallprompt', e => { e.preventDefault(); deferred = e; canInstall.value = true })
addEventListener('appinstalled', () => { canInstall.value = false; deferred = null })
addEventListener('online', () => (online.value = true)); addEventListener('offline', () => (online.value = false))
export async function install() { if (!deferred) return; deferred.prompt(); await deferred.userChoice.catch(() => {}); deferred = null; canInstall.value = false }
