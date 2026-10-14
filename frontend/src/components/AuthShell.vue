<template>
  <div class="auth-bg">
    <router-link to="/" class="brand"><img src="/images/logo.png" alt="" @error="e => e.target.style.display = 'none'" /><span>ARES <b>AUTO</b></span></router-link>
    <div class="auth-card"><slot /></div>
    <router-link to="/" class="back">← Về trang chủ</router-link>
  </div>
</template>
<style scoped>
/* Giữ nguyên phong cách trang đăng nhập gốc của repo (nền Pagani, thẻ kính mờ) */
.auth-bg { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px 16px;
  background: linear-gradient(rgba(10,12,16,.65), rgba(10,12,16,.8)), url('/images/bg-pagani.jpg') center/cover fixed; }
.brand { display: flex; align-items: center; gap: 10px; color: #fff; font-size: 1.5rem; margin-bottom: 22px; }
.brand img { height: 48px; filter: drop-shadow(0 4px 12px rgba(0,0,0,.5)); } .brand b { color: var(--accent); }
.auth-card { width: 100%; max-width: 440px; background: var(--card); border: 1px solid var(--border); border-radius: 20px; box-shadow: var(--shadow); backdrop-filter: blur(20px); padding: 34px 30px; }
.back { margin-top: 18px; color: var(--muted); }
</style>
