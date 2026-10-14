<template>
  <div class="toasts" role="status" aria-live="polite">
    <TransitionGroup name="t">
      <div v-for="t in toasts" :key="t.id" :class="['toast', t.kind]">
        <span class="ic">{{ t.kind === 'error' ? '!' : '✓' }}</span>{{ t.text }}
      </div>
    </TransitionGroup>
  </div>
</template>
<script setup>import { toasts } from '../stores/prefs'</script>
<style scoped>
.toasts { position: fixed; top: 88px; right: 24px; z-index: 200; display: flex; flex-direction: column; gap: 8px; pointer-events: none; }
.toast { display: flex; align-items: center; gap: 12px; padding: 14px 20px; border-radius: 0; border: 1px solid var(--border); box-shadow: 0 16px 40px -12px rgba(0,0,0,.8); font-weight: 500; min-width: 260px; }
.ic { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: .8rem; font-weight: 800; background: rgba(34,197,94,.2); color: var(--success); }
.error .ic { background: rgba(239,68,68,.2); color: var(--danger); }
.toast { position: relative; overflow: hidden; background: var(--surface); color: var(--text); border-left: 4px solid var(--success); }
.toast.error { border-left-color: var(--danger); }
.toast::after { content: ''; position: absolute; left: 0; bottom: 0; height: 2px; width: 100%; background: currentColor; opacity: .15; transform-origin: left; animation: t-life 2.6s linear forwards; }
.t-enter-active { transition: opacity .2s ease-out, transform .22s cubic-bezier(.2,.9,.3,1.15); } .t-leave-active { transition: opacity .16s ease-out, transform .16s ease-out; }
.t-enter-from { opacity: 0; transform: translateX(110%); } .t-leave-to { opacity: 0; transform: translateX(40px); }
.t-move { transition: transform .2s ease-out; }
@keyframes t-life { to { transform: scaleX(0); } }
@media (max-width: 899px) { .t-enter-from { transform: translateY(120%); } .t-leave-to { transform: translateY(30px); } }
@media (prefers-reduced-motion: reduce) { .t-enter-active, .t-leave-active, .t-move { transition: opacity .1s; } .t-enter-from, .t-leave-to { transform: none; } .toast::after { animation: none; } }
</style>
