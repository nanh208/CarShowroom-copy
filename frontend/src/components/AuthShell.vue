<template>
  <div class="auth-bg">
    <router-link to="/" class="brand"><img src="/images/logo.png" alt="" /><span>ARES MODELS</span></router-link>
    <div class="auth-card"><slot /></div>
    <router-link to="/" class="back">← Về trang chủ</router-link>
  </div>
</template>
<style scoped>
.auth-bg { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px 16px;
  background: linear-gradient(rgba(0,0,0,.72), rgba(0,0,0,.78)), url('/images/bg-pagani.jpg') center/cover fixed; color: #fff; }
.brand { display: flex; align-items: center; gap: 10px; color: #fff; font: 700 1.1rem 'Montserrat', sans-serif; letter-spacing: .14em; margin-bottom: 18px; }
.brand img { height: 36px; } .brand:hover { color: #fff; }
.auth-card { width: 100%; max-width: 380px; background: rgba(28,28,30,.92); border: 1px solid rgba(255,255,255,.08); border-radius: 4px; padding: 28px 24px; box-shadow: 0 20px 50px rgba(0,0,0,.5); }
.auth-card :deep(h2) { font-size: 1.25rem; letter-spacing: .06em; margin-bottom: 4px; }
.auth-card :deep(.input) { background: #2a2a2e; border-color: #3a3a40; color: #fff; }
.auth-card :deep(.input:focus) { background: #323238; border-color: var(--blue); box-shadow: 0 0 0 3px rgba(30,111,217,.25); }
.auth-card :deep(label) { color: #b0b0b0; }
.auth-card :deep(.btn) { background: var(--blue); }
.auth-card :deep(.btn:hover) { background: var(--blue-hover); }
.auth-card :deep(a) { color: #7eb6ff; }
.back { margin-top: 16px; color: #aaa; font-size: .9rem; } .back:hover { color: #fff; }
</style>
