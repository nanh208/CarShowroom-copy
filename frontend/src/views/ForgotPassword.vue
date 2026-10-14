<template>
  <AuthShell>
    <template v-if="step === 1">
      <h2>Quên mật khẩu</h2>
      <p class="small" style="color:#aaa;margin:4px 0 18px">Nhập email đã đăng ký, chúng tôi sẽ gửi mã xác thực.</p>
      <form @submit.prevent="step = 2">
        <div class="field"><label>Email</label><input v-model="email" class="input" type="email" required /></div>
        <button class="btn btn-blue btn-block">Gửi mã xác thực</button>
      </form>
    </template>
    <template v-else>
      <h2>Xác thực</h2>
      <p class="small" style="color:#aaa;margin:4px 0 18px">Nhập mã 6 số đã gửi tới <b>{{ email }}</b>.</p>
      <div class="otp">
        <input v-for="i in 6" :key="i" class="input" maxlength="1" inputmode="numeric" />
      </div>
      <div class="alert alert-info small">Bản demo: tính năng gửi email chưa được kết nối. Vui lòng liên hệ hotline 1900 6868 để đặt lại mật khẩu.</div>
      <button class="btn btn-blue btn-block" @click="$router.push('/login')">Xác nhận</button>
      <p class="small" style="margin-top:12px;text-align:center"><a href="#" @click.prevent="step = 1">← Đổi email</a></p>
    </template>
    <p class="small" style="margin-top:16px;text-align:center"><router-link to="/login">Quay lại đăng nhập</router-link></p>
  </AuthShell>
</template>
<script setup>
import { ref } from 'vue'
import AuthShell from '../components/AuthShell.vue'
const step = ref(1), email = ref('')
</script>
<style scoped>
.otp { display: grid; grid-template-columns: repeat(6, 1fr); gap: 6px; margin-bottom: 14px; }
.otp .input { text-align: center; font-size: 1.2rem; padding: 10px 0; }
</style>
