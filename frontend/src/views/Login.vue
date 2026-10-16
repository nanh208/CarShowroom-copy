<template>
  <AuthShell>
    <h2>{{ $t('Đăng nhập') }}</h2>
    <p class="muted small" style="margin:4px 0 20px">{{ $t('Chào mừng trở lại với ARES Toys') }}</p>
    <div v-if="$route.query.expired" class="alert alert-info">{{ $t('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.') }}</div>
    <div v-if="err" class="alert alert-error">{{ err }}</div>
    <template v-if="mode === 'phone'">
      <PhoneLogin @done="router.push(route.query.redirect || '/')" />
      <button type="button" class="btn btn-outline btn-block back" @click="mode = 'email'">{{ $t('Đăng nhập bằng email') }}</button>
    </template>
    <form v-else @submit.prevent="submit">
      <div class="field"><label>Email</label><input v-model="email" class="input" type="email" required autocomplete="email" /></div>
      <div class="field"><label>{{ $t('Mật khẩu') }}</label><input v-model="password" class="input" type="password" required autocomplete="current-password" /></div>
      <div v-if="needTotp" class="field totp"><label for="totp">{{ $t('Mã xác thực 2 bước') }}</label><input id="totp" ref="totpEl" v-model.trim="totp" class="input" inputmode="numeric" autocomplete="one-time-code" maxlength="6" :placeholder="$t('6 số trong ứng dụng xác thực')" required /></div>
      <p class="small" style="text-align:right;margin:-4px 0 12px"><router-link to="/forgot-password">{{ $t('Quên mật khẩu?') }}</router-link></p>
      <button class="btn btn-block" :disabled="busy">{{ busy ? 'Đang đăng nhập...' : 'Đăng nhập' }}</button>
    </form>
    <SocialLogin v-if="mode === 'email'" @phone="mode = 'phone'" />
    <p class="small muted" style="margin-top:18px;text-align:center">{{ $t('Chưa có tài khoản?') }} <router-link :to="{ path: '/register', query: $route.query }">{{ $t('Đăng ký ngay') }}</router-link></p>
  </AuthShell>
</template>
<script setup>
import { ref, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { errMsg } from '../api'
import AuthShell from '../components/AuthShell.vue'
import SocialLogin from '../components/SocialLogin.vue'
import PhoneLogin from '../components/PhoneLogin.vue'
const mode = ref(useRoute().query.phone ? 'phone' : 'email')
const auth = useAuth(), route = useRoute(), router = useRouter()
const email = ref(''), password = ref(''), err = ref(route.query.error || ''), busy = ref(false), needTotp = ref(false), totp = ref(''), totpEl = ref()
async function submit() {
  err.value = ''; busy.value = true
  try {
    const r = await auth.login(email.value, password.value, needTotp.value ? totp.value : null)
    if (r === 'TOTP') { needTotp.value = true; await nextTick(); totpEl.value?.focus(); return }
    router.push(route.query.redirect || (auth.isStaff ? '/admin' : '/')) }
  catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
</script>
<style scoped>
.back { margin-top: 12px }
</style>
