<template>
  <AuthShell>
    <h2>Đăng nhập</h2>
    <p class="muted small" style="margin:4px 0 20px">Chào mừng trở lại với ARES Models</p>
    <div v-if="$route.query.expired" class="alert alert-info">Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.</div>
    <div v-if="err" class="alert alert-error">{{ err }}</div>
    <form @submit.prevent="submit">
      <div class="field"><label>Email</label><input v-model="email" class="input" type="email" required autocomplete="email" /></div>
      <div class="field"><label>Mật khẩu</label><input v-model="password" class="input" type="password" required autocomplete="current-password" /></div>
      <p class="small" style="text-align:right;margin:-4px 0 12px"><router-link to="/forgot-password">Quên mật khẩu?</router-link></p>
      <button class="btn btn-blue btn-block" :disabled="busy">{{ busy ? 'Đang đăng nhập...' : 'Đăng nhập' }}</button>
    </form>
    <SocialLogin />
    <p class="small muted" style="margin-top:18px;text-align:center">Chưa có tài khoản? <router-link :to="{ path: '/register', query: $route.query }">Đăng ký ngay</router-link></p>
  </AuthShell>
</template>
<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { errMsg } from '../api'
import AuthShell from '../components/AuthShell.vue'
import SocialLogin from '../components/SocialLogin.vue'
const auth = useAuth(), route = useRoute(), router = useRouter()
const email = ref(''), password = ref(''), err = ref(route.query.error || ''), busy = ref(false)
async function submit() {
  err.value = ''; busy.value = true
  try { await auth.login(email.value, password.value); router.push(route.query.redirect || (auth.isAdmin ? '/admin' : '/')) }
  catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
</script>
