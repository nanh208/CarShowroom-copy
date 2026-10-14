<template>
  <div class="toasts">
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
.toast { display: flex; align-items: center; gap: 12px; padding: 14px 20px; border-radius: 14px; background: #fff;  border: 1px solid var(--border); box-shadow: 0 16px 40px -12px rgba(0,0,0,.8); font-weight: 500; min-width: 260px; }
.ic { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: .8rem; font-weight: 800; background: rgba(34,197,94,.2); color: var(--success); }
.error .ic { background: rgba(239,68,68,.2); color: var(--danger); }
.t-enter-active, .t-leave-active { transition: opacity .18s ease-out, transform .18s ease-out; } .t-enter-from, .t-leave-to { opacity: 0; transform: translateX(12px); }
</style>
