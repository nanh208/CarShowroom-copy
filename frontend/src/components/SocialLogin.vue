<template>
  <div>
    <div class="divider"><span>{{ $t('hoặc tiếp tục với') }}</span></div>
    <div class="row">
      <button class="btn btn-social grow" :disabled="prov && !prov.google" :title="prov && !prov.google ? $t('Chưa được cấu hình') : null" @click="go('google')"><svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true"><path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z"/><path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z"/><path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z"/><path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z"/></svg> Google</button>
      <button class="btn btn-fb grow" :disabled="prov && !prov.facebook" :title="prov && !prov.facebook ? $t('Chưa được cấu hình') : null" @click="go('facebook')">ⓕ Facebook</button>
    </div>
    <button class="btn btn-outline btn-block phone-btn" type="button" @click="$emit('phone')">📱 {{ $t('Đăng nhập bằng số điện thoại') }}</button>
    <p v-if="prov && !prov.google && !prov.facebook" class="small muted note">{{ $t('Đăng nhập Google/Facebook chưa được bật trên máy chủ này.') }}</p>
    <div v-if="msg" class="alert alert-info small" style="margin-top:12px">{{ msg }}</div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../api'
defineEmits(['phone'])
const msg = ref(''), prov = ref(null)
onMounted(() => api.get('/auth/providers').then(r => (prov.value = r.data)).catch(() => {}))
async function go(provider) {
  msg.value = ''
  try { location.href = (await api.get(`/auth/oauth/${provider}/url`)).data.url } catch (e) { msg.value = errMsg(e) }
}
</script>
<style scoped>
.divider { display: flex; align-items: center; gap: 12px; color: var(--muted); font-size: .85rem; margin: 20px 0 14px; }
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: var(--border); }
.phone-btn { margin-top: 10px }
.note { margin: 8px 0 0; text-align: center }
button:disabled { opacity: .5; cursor: not-allowed }
</style>
