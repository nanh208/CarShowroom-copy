<template>
  <AuthShell>
    <h2>Đăng nhập</h2>
    <p class="muted small" style="margin:4px 0 20px">Chào mừng trở lại với ARES Toys</p>

    <div v-if="$route.query.expired" class="alert alert-info">Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.</div>
    <div v-if="err" class="alert alert-error">{{ err }}</div>

    <form @submit.prevent="submit">
      <div class="field">
        <label>Email</label>
        <div class="input-wrap">
          <svg class="field-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <rect width="20" height="16" x="2" y="4" rx="2" />
            <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
          </svg>
          <input v-model="email" class="input input-with-icon" type="email" required autocomplete="email" placeholder="Nhập địa chỉ email" />
        </div>
      </div>

      <div class="field">
        <label>Mật khẩu</label>
        <div class="input-wrap">
          <svg class="field-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <rect width="18" height="11" x="3" y="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
          <input v-model="password" class="input input-with-icon" type="password" required autocomplete="current-password" placeholder="Nhập mật khẩu" />
        </div>
      </div>

      <div v-if="needTotp" class="field totp">
        <label for="totp">Mã xác thực 2 bước (TOTP)</label>
        <input id="totp" ref="totpEl" v-model.trim="totp" class="input" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="6 số trong ứng dụng xác thực" required />
      </div>

      <p class="small" style="text-align:right;margin:-4px 0 14px">
        <router-link to="/forgot-password" class="forgot-link">Quên mật khẩu?</router-link>
      </p>

      <button class="btn btn-block" :disabled="busy || isLaunching">
        <svg v-if="busy" class="spin-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="12" cy="12" r="10" stroke-opacity="0.25" />
          <path d="M12 2a10 10 0 0 1 10 10" />
        </svg>
        <span>{{ busy ? 'Đang xác thực...' : 'Đăng nhập' }}</span>
      </button>
    </form>

    <SocialLogin />
    <p class="small muted" style="margin-top:20px;text-align:center">
      Chưa có tài khoản? <router-link :to="{ path: '/register', query: $route.query }">Đăng ký thành viên</router-link>
    </p>

    <!-- Luxury Automotive Launch Transition (Zero emojis, sleek vector telemetry) -->
    <Teleport to="body">
      <transition name="launch-fade">
        <div v-if="isLaunching" class="launch-overlay" role="dialog" aria-modal="true" aria-label="Đang chuyển hướng">
          <div class="launch-ambient" />

          <!-- Roadway Laser Track -->
          <div class="launch-road">
            <div class="road-grid" />
            <div class="road-lane-line" />
          </div>

          <!-- Digital Cockpit Telemetry Display -->
          <div class="hud-center">
            <!-- Sleek Status Pill -->
            <div class="auth-pill">
              <svg viewBox="0 0 20 20" width="14" height="14" fill="currentColor">
                <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
              </svg>
              <span>XÁC THỰC THÀNH CÔNG</span>
            </div>

            <!-- Welcome Branding -->
            <div class="welcome-box">
              <span class="welcome-sub">CHÀO MỪNG TRỞ LẠI</span>
              <h2 class="welcome-title">{{ greetingName }}</h2>
            </div>

            <!-- Precision Digital Speedometer HUD -->
            <div class="speedometer-box">
              <div class="speed-readout">
                <span class="speed-num">{{ launchSpeed }}</span>
                <span class="speed-unit">KM / H</span>
              </div>

              <!-- High-Performance Shift Indicator LEDs -->
              <div class="shift-meter" aria-hidden="true">
                <span v-for="n in 8" :key="n" :class="['shift-segment', `s-${n}`, { lit: launchSpeed >= (n * 28) }]" />
              </div>

              <!-- Sleek Power Gauge Bar -->
              <div class="gauge-track" aria-hidden="true">
                <div class="gauge-fill" :style="{ width: Math.min(100, (launchSpeed / 240) * 100) + '%' }" />
              </div>
            </div>

            <!-- Clean Adult Redirect Status (No emojis) -->
            <div class="redirect-status">
              <svg class="spin-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10" stroke-opacity="0.25" />
                <path d="M12 2a10 10 0 0 1 10 10" />
              </svg>
              <span>Đang kết nối hệ thống và chuẩn bị dữ liệu...</span>
            </div>
          </div>

          <!-- Aerodynamic Hypercar Silhouette gliding smoothly across screen -->
          <div class="car-track" aria-hidden="true">
            <div class="laser-beam" />
            <svg class="car-silhouette" viewBox="0 0 280 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="carBodyGrad" x1="10" y1="20" x2="270" y2="52" gradientUnits="userSpaceOnUse">
                  <stop offset="0%" stop-color="#181a20" />
                  <stop offset="50%" stop-color="#f05a1a" />
                  <stop offset="100%" stop-color="#ff7a30" />
                </linearGradient>
              </defs>
              <!-- Chassis Line -->
              <path d="M12 46 C25 46 36 44 48 40 C65 31 92 20 148 20 C182 20 205 26 230 35 L260 41 C272 44 276 48 274 52 L6 52 C5 48 8 46 12 46 Z" fill="url(#carBodyGrad)" />
              <!-- Cockpit Canopy -->
              <path d="M96 28 C116 22 152 22 176 28 L192 36 L86 36 Z" fill="#090a0d" opacity="0.92" />
              <!-- Taillight Neon Accent -->
              <line x1="8" y1="44" x2="22" y2="44" stroke="#ff2020" stroke-width="3" stroke-linecap="round" />
              <!-- Laser Headlight Accent -->
              <polygon points="252,38 272,41 258,43" fill="#38bdf8" />
              <!-- Front Wheel -->
              <circle cx="218" cy="49" r="13" fill="#121316" stroke="#f05a1a" stroke-width="2.5" />
              <circle cx="218" cy="49" r="5" fill="#f1f5f9" />
              <!-- Rear Wheel -->
              <circle cx="56" cy="49" r="13" fill="#121316" stroke="#f05a1a" stroke-width="2.5" />
              <circle cx="56" cy="49" r="5" fill="#f1f5f9" />
            </svg>
            <div class="aero-stream" />
          </div>
        </div>
      </transition>
    </Teleport>
  </AuthShell>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import { errMsg } from '../api'
import AuthShell from '../components/AuthShell.vue'
import SocialLogin from '../components/SocialLogin.vue'

const auth = useAuth(), route = useRoute(), router = useRouter()
const email = ref(''), password = ref(''), err = ref(route.query.error || ''), busy = ref(false), needTotp = ref(false), totp = ref(''), totpEl = ref()

const isLaunching = ref(false)
const launchSpeed = ref(0)

const greetingName = computed(() => {
  return auth.user?.fullName || auth.user?.email || 'ARES TOYS SHOWROOM'
})

async function submit() {
  err.value = ''; busy.value = true
  try {
    const r = await auth.login(email.value, password.value, needTotp.value ? totp.value : null)
    if (r === 'TOTP') {
      needTotp.value = true
      await nextTick()
      totpEl.value?.focus()
      return
    }

    // Trigger high-tech automotive launch sequence
    isLaunching.value = true

    // Animate Speedometer counter: 0 -> 240 KM/H
    const start = performance.now()
    const duration = 1000
    const targetSpeed = 240

    function step(timestamp) {
      const elapsed = timestamp - start
      const progress = Math.min(1, elapsed / duration)
      // Ease out cubic
      const ease = 1 - Math.pow(1 - progress, 3)
      launchSpeed.value = Math.round(ease * targetSpeed)
      if (progress < 1) {
        requestAnimationFrame(step)
      }
    }
    requestAnimationFrame(step)

    // Complete transition and navigate smoothly
    setTimeout(() => {
      router.push(route.query.redirect || (auth.isStaff ? '/admin' : '/'))
    }, 1150)
  } catch (e) {
    err.value = errMsg(e)
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.field-icon {
  position: absolute;
  left: 12px;
  color: var(--muted, #8a857b);
  pointer-events: none;
  transition: color 0.15s ease;
}

.input-with-icon {
  padding-left: 38px !important;
}

.input-with-icon:focus + .field-icon,
.input-wrap:focus-within .field-icon {
  color: var(--primary, #f05a1a);
}

.forgot-link {
  color: var(--muted, #6b665d);
  text-decoration: none;
  font-size: 0.85rem;
  transition: color 0.15s;
}

.forgot-link:hover {
  color: var(--primary, #f05a1a);
}

.spin-icon {
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* =====================================================================
   LUXURY AUTOMOTIVE TRANSITION OVERLAY (Zero Emojis, Minimalist Precision)
   ===================================================================== */
.launch-overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  background: #090a0d;
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: var(--head, 'Oswald', 'Be Vietnam Pro', sans-serif);
}

.launch-ambient {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 40%, rgba(240, 90, 26, 0.15) 0%, rgba(9, 10, 13, 0.95) 70%);
  pointer-events: none;
}

/* Asphalt laser lines */
.launch-road {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 90px;
  pointer-events: none;
  overflow: hidden;
}

.road-grid {
  position: absolute;
  inset: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background-image: linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 100% 12px;
}

.road-lane-line {
  position: absolute;
  top: 42px;
  left: 0;
  right: 0;
  height: 2px;
  background: repeating-linear-gradient(90deg, #f05a1a 0 32px, transparent 32px 64px);
  opacity: 0.7;
  animation: roadFlow 0.5s linear infinite;
}

@keyframes roadFlow {
  0% { transform: translateX(0); }
  100% { transform: translateX(-64px); }
}

/* Center HUD Display */
.hud-center {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 480px;
  padding: 0 20px;
}

.auth-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.35);
  color: #4ade80;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 18px;
  box-shadow: 0 0 16px rgba(34, 197, 94, 0.2);
}

.welcome-box {
  margin-bottom: 24px;
}

.welcome-sub {
  display: block;
  font-size: 0.8rem;
  letter-spacing: 0.2em;
  color: rgba(255, 255, 255, 0.55);
  text-transform: uppercase;
  margin-bottom: 4px;
}

.welcome-title {
  font-size: 1.85rem;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin: 0;
}

/* Speedometer Box */
.speedometer-box {
  width: 100%;
  max-width: 320px;
  background: rgba(18, 20, 26, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: 0 16px 36px -10px rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
}

.speed-readout {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  margin-bottom: 10px;
}

.speed-num {
  font-size: 3.2rem;
  line-height: 1;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #ffffff;
  letter-spacing: -0.02em;
}

.speed-unit {
  font-size: 0.82rem;
  font-weight: 600;
  color: #f05a1a;
  letter-spacing: 0.14em;
}

/* Shift LEDs */
.shift-meter {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-bottom: 10px;
}

.shift-segment {
  width: 24px;
  height: 5px;
  border-radius: 1px;
  background: rgba(255, 255, 255, 0.12);
  transform: skewX(-20deg);
  transition: background-color 0.12s ease, box-shadow 0.12s ease;
}

.shift-segment.s-1.lit,
.shift-segment.s-2.lit,
.shift-segment.s-3.lit {
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.6);
}

.shift-segment.s-4.lit,
.shift-segment.s-5.lit,
.shift-segment.s-6.lit {
  background: #eab308;
  box-shadow: 0 0 8px rgba(234, 179, 8, 0.6);
}

.shift-segment.s-7.lit,
.shift-segment.s-8.lit {
  background: #ef4444;
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.8);
}

/* Gauge bar */
.gauge-track {
  width: 100%;
  height: 3px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  overflow: hidden;
}

.gauge-fill {
  height: 100%;
  background: linear-gradient(90deg, #f05a1a, #ff7a30, #facc15);
  transition: width 0.1s linear;
}

/* Redirect status */
.redirect-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 0.85rem;
  letter-spacing: 0.02em;
}

/* Gliding Car Silhouette */
.car-track {
  position: absolute;
  bottom: 24px;
  left: 0;
  width: 280px;
  animation: carRush 1.1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  pointer-events: none;
}

@keyframes carRush {
  0% {
    transform: translateX(-300px);
    opacity: 0;
  }
  20% {
    opacity: 1;
  }
  85% {
    opacity: 1;
  }
  100% {
    transform: translateX(calc(100vw + 100px));
    opacity: 0;
  }
}

.car-silhouette {
  width: 240px;
  height: auto;
  filter: drop-shadow(0 6px 16px rgba(240, 90, 26, 0.35));
}

.laser-beam {
  position: absolute;
  left: 230px;
  top: 28px;
  width: 180px;
  height: 16px;
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.5), transparent);
  clip-path: polygon(0 40%, 100% 0, 100% 100%, 0 60%);
  pointer-events: none;
}

.aero-stream {
  position: absolute;
  right: 230px;
  top: 36px;
  width: 90px;
  height: 2px;
  background: linear-gradient(270deg, rgba(240, 90, 26, 0.7), transparent);
  border-radius: 999px;
}

/* Transition fade */
.launch-fade-enter-active,
.launch-fade-leave-active {
  transition: opacity 0.25s ease;
}

.launch-fade-enter-from,
.launch-fade-leave-to {
  opacity: 0;
}
</style>
