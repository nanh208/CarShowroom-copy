<template><AuthShell>
  <template v-if="needTotp">
    <h2>{{ $t('Mã xác thực 2 bước') }}</h2>
    <div v-if="err" class="alert alert-error">{{ err }}</div>
    <form @submit.prevent="finish"><div class="field"><input v-model.trim="totp" class="input" inputmode="numeric" maxlength="6" autocomplete="one-time-code" required :aria-label="$t('Mã xác thực 2 bước')" /></div><button class="btn btn-block">{{ $t('Xác nhận') }}</button></form>
  </template>
  <template v-else><div class="spinner" /><p class="muted" style="text-align:center">{{ $t('Đang đăng nhập...') }}</p></template>
</AuthShell></template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import api, { errMsg } from '../api'
import AuthShell from '../components/AuthShell.vue'
const route = useRoute(), router = useRouter(), auth = useAuth()
const needTotp = ref(false), totp = ref(''), err = ref(''); let code = route.query.code
const fail = m => router.replace('/login?error=' + encodeURIComponent(m || 'Đăng nhập mạng xã hội thất bại'))
async function finish() {
  try { const { data } = await api.post('/auth/oauth/exchange', { code, totp: needTotp.value ? totp.value : null })
    if (data.totpRequired) { code = data.retryCode; needTotp.value = true; return }
    await auth.loginWithToken(data.token); router.replace(auth.isStaff ? '/admin' : '/') } catch (e) { fail(errMsg(e)) }
}
onMounted(() => { if (!code) return fail(); history.replaceState(null, '', '/oauth-callback'); finish() })
</script>
