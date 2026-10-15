<template>
  <div class="container page narrow">
    <Breadcrumb :items="[{ label: 'Tài khoản', to: '/account' }, { label: 'Thông báo' }]" />
    <div class="row between hd"><h1>Thông báo</h1><button class="btn btn-sm btn-outline" :disabled="!unread" @click="readAll">Đánh dấu đã đọc tất cả</button></div>
    <div class="chips"><button v-for="(l, k) in KINDS" :key="k" :class="['chip', { on: kind === k }]" @click="kind = k">{{ l }} <small>{{ count(k) }}</small></button></div>
    <div v-if="loading"><div v-for="i in 4" :key="i" class="sk" style="height:64px;margin-bottom:8px" /></div>
    <p v-else-if="!shown.length" class="empty muted">Không có thông báo{{ kind ? ' trong mục này' : '' }}.</p>
    <ul v-else class="nl" v-stagger>
      <li v-for="n in shown" :key="n.id" :class="{ unread: !n.read }">
        <span class="ic">{{ ICON[n.kind] || '🔔' }}</span>
        <div><b>{{ n.title }}</b><p>{{ n.body }}</p><small class="muted">{{ ago(n.createdAt) }}</small></div>
        <router-link v-if="n.link" :to="n.link" class="btn btn-sm btn-outline">Xem</router-link>
      </li>
    </ul>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { ago } from '../format'
import Breadcrumb from '../components/Breadcrumb.vue'
const KINDS = { '': 'Tất cả', ORDER: 'Đơn hàng', AUCTION: 'Đấu giá', SYSTEM: 'Khuyến mãi & hệ thống' }
const ICON = { ORDER: '📦', AUCTION: '🔨', SYSTEM: '📣' }
const list = ref([]), unread = ref(0), kind = ref(''), loading = ref(true)
const shown = computed(() => list.value.filter(n => !kind.value || n.kind === kind.value || (kind.value === 'SYSTEM' && !KINDS[n.kind])))
const count = k => k ? list.value.filter(n => n.kind === k).length : list.value.length
const load = async () => { const { data } = await api.get('/account/notifications'); list.value = data.items; unread.value = data.unread; loading.value = false }
const readAll = async () => { await api.post('/account/notifications/read'); list.value.forEach(n => (n.read = true)); unread.value = 0 }
onMounted(load)
</script>
<style scoped>
.narrow { max-width: 820px; } .hd { gap: 10px; flex-wrap: wrap; }
.chips { display: flex; gap: 6px; flex-wrap: wrap; margin: 14px 0; } .chip { border: 1px solid var(--border); background: var(--surface); padding: 6px 12px; font: 600 .8rem inherit; font-family: inherit; cursor: pointer; color: var(--text); } .chip.on { background: #111; color: #fff; border-color: #111; }
.nl { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.nl li { display: grid; grid-template-columns: 40px 1fr auto; gap: 12px; align-items: center; background: var(--surface); border: 1px solid var(--border); padding: 12px 14px; }
.nl li.unread { border-left: 4px solid var(--primary); } .nl p { margin: 2px 0; font-size: .88rem; } .ic { font-size: 1.4rem; text-align: center; }
.empty { padding: 40px 0; text-align: center; }
@media (max-width: 520px) { .nl li { grid-template-columns: 32px 1fr; } .nl li .btn { grid-column: 2; justify-self: start; } }
</style>
