<template>
  <div class="auth-bg">
    <router-link to="/" class="brand">
      <img loading="lazy" decoding="async" src="/images/logo.png" alt="ARES Toys" @error="e => e.target.style.display = 'none'" />
      <span>ARES <b>TOYS</b></span>
    </router-link>
    <div class="auth-card">
      <slot />
    </div>
    <router-link to="/" class="back">← Về trang chủ</router-link>
  </div>
</template>

<style scoped>
/* Giữ nguyên phong cách trang đăng nhập chuẩn của ARES Toys */
.auth-bg {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: var(--bg, #f3efe6);
  position: relative;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text, #111111);
  font-size: 1.55rem;
  font-family: var(--head, 'Oswald', 'Be Vietnam Pro', sans-serif);
  font-weight: 700;
  margin-bottom: 22px;
  text-decoration: none;
  letter-spacing: .04em;
  text-transform: uppercase;
}

.brand img {
  height: 48px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.12));
}

.brand b {
  color: var(--accent, #f05a1a);
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: var(--card, #ffffff);
  border: 1px solid var(--border, #ddd5c6);
  border-radius: var(--radius, 4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  padding: 34px 30px;
  color: var(--text, #111111);
}

.back {
  margin-top: 20px;
  color: var(--muted, #6b665d);
  font-size: .88rem;
  text-decoration: none;
  transition: color .2s;
}

.back:hover {
  color: var(--primary, #f05a1a);
}
</style>
