<template>
  <AuthShell>
    <template v-if="pending">
      <h2 style="margin-top:0">Hoàn tất đăng ký</h2>
      <p class="muted small">
        Tài khoản {{ providerName }}{{ name ? ' của ' + name : '' }} không chia sẻ email.
        Nhập email để tạo tài khoản ARES Toys (dùng để nhận thông báo đơn hàng).
      </p>
      <form @submit.prevent="complete">
        <div class="field"><label>Email</label><input v-model.trim="email" type="email" required autocomplete="email" placeholder="ban@example.com" /></div>
        <div v-if="err" class="alert alert-error small">{{ err }}</div>
        <button class="btn btn-primary btn-block" :disabled="busy">{{ busy ? 'Đang tạo tài khoản...' : 'Tiếp tục' }}</button>
      </form>
      <p class="small" style="text-align:center;margin-top:12px"><router-link to="/login">← Quay lại đăng nhập</router-link></p>
    </template>
    <template v-else>
      <div class="spinner" /><p class="muted" style="text-align:center">{{ err || 'Đang đăng nhập...' }}</p>
    </template>
  </AuthShell>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import api, { errMsg } from '../api'
import AuthShell from '../components/AuthShell.vue'

const route = useRoute(), router = useRouter(), auth = useAuth()
const pending = ref(''), name = ref(''), provider = ref(''), email = ref(''), err = ref(''), busy = ref(false)
const providerName = computed(() => provider.value === 'facebook' ? 'Facebook' : provider.value === 'google' ? 'Google' : 'mạng xã hội')

const fail = msg => router.replace('/login?error=' + encodeURIComponent(msg || 'Đăng nhập mạng xã hội thất bại'))
const done = data => { auth.setSession(data.token, data.user); router.replace('/') }

onMounted(async () => {
  const q = route.query
  // Xoá ticket/pending khỏi thanh địa chỉ (không để lại trong lịch sử trình duyệt)
  history.replaceState(null, '', location.pathname)
  if (q.pending) { pending.value = String(q.pending); name.value = String(q.name || ''); provider.value = String(q.provider || ''); return }
  if (!q.ticket) return fail()
  try { done((await api.post('/auth/oauth/exchange', { ticket: String(q.ticket) })).data) } catch (e) { fail(errMsg(e)) }
})

async function complete() {
  err.value = ''; busy.value = true
  try { done((await api.post('/auth/oauth/complete', { pending: pending.value, email: email.value })).data) }
  catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
</script>
