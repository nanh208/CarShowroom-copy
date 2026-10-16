<template>
  <form class="pl" @submit.prevent="step === 1 ? send() : verify()">
    <div v-if="err" class="alert alert-error">{{ err }}</div>
    <div class="field"><label for="pl-phone">{{ $t('Số điện thoại') }}</label>
      <input id="pl-phone" v-model.trim="phone" class="input" type="tel" inputmode="tel" autocomplete="tel" placeholder="09xx xxx xxx" :disabled="step > 1" required /></div>
    <template v-if="step >= 2">
      <p class="small muted">{{ $t('Mã 6 số đã gửi tới {0}. Hiệu lực 5 phút.', masked) }}</p>
      <div class="field"><label for="pl-code">{{ $t('Mã OTP') }}</label>
        <input id="pl-code" ref="codeEl" v-model.trim="code" class="input otp" inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="\d{6}" :disabled="step === 3" required /></div>
      <div v-if="step === 3" class="field"><label for="pl-totp">{{ $t('Mã xác thực 2 bước') }}</label><input id="pl-totp" ref="totpEl" v-model.trim="totp" class="input" inputmode="numeric" maxlength="6" required /></div>
    </template>
    <button class="btn btn-block" :disabled="busy">{{ step === 1 ? $t('Gửi mã OTP') : $t('Xác nhận & đăng nhập') }}</button>
    <div v-if="step >= 2" class="row between small acts">
      <button type="button" class="link" @click="reset">{{ $t('Đổi số') }}</button>
      <button type="button" class="link" :disabled="left > 0 || busy" @click="send">{{ left > 0 ? $t('Gửi lại sau {0}s', left) : $t('Gửi lại mã') }}</button>
    </div>
  </form>
</template>
<script setup>
import { ref, nextTick, onUnmounted } from 'vue'
import api, { errMsg } from '../api'
import { useAuth } from '../stores/auth'
const emit = defineEmits(['done'])
const auth = useAuth()
const phone = ref(''), code = ref(''), totp = ref(''), step = ref(1), busy = ref(false), err = ref(''), masked = ref(''), left = ref(0), codeEl = ref(), totpEl = ref()
let retry = null, timer
async function send() {
  err.value = ''; busy.value = true
  try { const { data } = await api.post('/auth/otp/request', { phone: phone.value }); masked.value = data.phone; step.value = 2; left.value = data.resendIn
    clearInterval(timer); timer = setInterval(() => { if (--left.value <= 0) clearInterval(timer) }, 1000); await nextTick(); codeEl.value?.focus() }
  catch (e) { err.value = errMsg(e) } finally { busy.value = false }
}
async function verify() {
  err.value = ''; busy.value = true
  try {
    const { data } = await api.post('/auth/otp/verify', retry ? { retryCode: retry, totp: totp.value } : { phone: phone.value, code: code.value })
    if (data.totpRequired) { retry = data.retryCode; step.value = 3; await nextTick(); totpEl.value?.focus(); return }
    await auth.loginWithToken(data.token); emit('done')
  } catch (e) { err.value = errMsg(e); if (retry) reset() } finally { busy.value = false }
}
function reset() { step.value = 1; code.value = ''; totp.value = ''; retry = null }
onUnmounted(() => clearInterval(timer))
</script>
<style scoped>
.otp { letter-spacing: .4em; font-size: 1.2rem; text-align: center }
.acts { margin-top: 10px }
.link { background: none; border: 0; padding: 0; color: var(--accent, #f15a24); font: inherit }
.link:disabled { color: var(--muted) }
</style>
