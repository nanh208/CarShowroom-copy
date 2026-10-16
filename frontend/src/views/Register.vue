<template>
  <AuthShell>
    <h2>Tạo tài khoản</h2>
    <p class="muted small" style="margin:4px 0 20px">Nhận ngay mã CHAOMUNG giảm 10% cho đơn đầu tiên</p>
    <div v-if="err" class="alert alert-error">{{ err }}</div>
    <form @submit.prevent="submit">
      <div class="field"><label>Họ và tên</label><input v-model="f.fullName" class="input" required /></div>
      <div class="field"><label>Email</label><input v-model="f.email" class="input" type="email" required /></div>
      <div class="field"><label>Số điện thoại</label><input v-model="f.phone" class="input" /></div>
      <div class="field"><label>Mật khẩu (tối thiểu 6 ký tự)</label><input v-model="f.password" class="input" type="password" required minlength="6" /></div>
      <div class="field"><label>Nhập lại mật khẩu</label><input v-model="confirm" class="input" type="password" required /></div>
      <button class="btn btn-block" :disabled="busy">{{ busy ? 'Đang tạo...' : 'Đăng ký' }}</button>
    </form>
    <SocialLogin @phone="$router.push('/login?phone=1')" />
    <p class="small muted" style="margin-top:18px;text-align:center">Đã có tài khoản? <router-link to="/login">Đăng nhập</router-link></p>
  </AuthShell>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { errMsg } from '../api'
import AuthShell from '../components/AuthShell.vue'
import SocialLogin from '../components/SocialLogin.vue'
const auth = useAuth(), route = useRoute(), router = useRouter()
if (route.query.ref) localStorage.setItem('refCode', String(route.query.ref).slice(0, 20))
const f = reactive({ fullName: '', email: '', phone: '', password: '' }), confirm = ref(''), err = ref(''), busy = ref(false)
async function submit() {
  err.value = ''
  if (f.password !== confirm.value) { err.value = 'Mật khẩu nhập lại không khớp'; return }
  busy.value = true
  try { await auth.register(f); router.push(route.query.redirect || '/') } catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
</script>
