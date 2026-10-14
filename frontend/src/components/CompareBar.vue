<template>
  <Transition name="up">
    <div v-if="prefs.compare.length && $route.path !== '/compare'" class="cbar">
      <div class="container inner">
        <b>So sánh xe</b>
        <div class="slots">
          <div v-for="i in 3" :key="i" class="slot">
            <template v-if="prefs.compare[i - 1]"><img loading="lazy" decoding="async" :src="prefs.compare[i - 1].images?.[0]" alt="" /><span>{{ prefs.compare[i - 1].name }}</span>
              <button @click="prefs.toggleCompare(prefs.compare[i - 1])">×</button></template>
            <span v-else class="muted">Chọn thêm xe</span>
          </div>
        </div>
        <button class="btn-text" @click="prefs.clearCompare()">Xoá hết</button>
        <router-link to="/compare" class="btn btn-sm" :class="{ disabled: prefs.compare.length < 2 }">So sánh ngay ({{ prefs.compare.length }})</router-link>
      </div>
    </div>
  </Transition>
</template>
<script setup>import { usePrefs } from '../stores/prefs'; const prefs = usePrefs()</script>
<style scoped>
.cbar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 60; background: #fff;  border-top: 1px solid var(--border); padding: 16px 0; }
.inner { display: flex; align-items: center; gap: 24px; }
.slots { display: flex; gap: 12px; flex: 1; }
.slot { flex: 1; display: flex; align-items: center; gap: 12px; height: 56px; padding: 0 12px; border: 1px dashed rgba(0,0,0,.15); border-radius: 12px; font-size: .85rem; min-width: 0; }
.slot img { width: 64px; height: 40px; object-fit: cover; border-radius: 6px; } .slot span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.slot button { background: none; border: none; color: var(--muted); font-size: 1.2rem; cursor: pointer; } .slot button:hover { color: var(--text); }
.btn-text { background: none; border: none; color: var(--muted); cursor: pointer; font-family: inherit; } .btn-text:hover { color: var(--text); }
.disabled { opacity: .5; pointer-events: none; }
.up-enter-active, .up-leave-active { transition: transform .2s ease-out; } .up-enter-from, .up-leave-to { transform: translateY(100%); }
@media (max-width: 800px) { .slots { display: none; } }
</style>
