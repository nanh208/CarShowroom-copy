<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Bộ sưu tập' }]" />
    <header class="hd"><h1>Bộ sưu tập</h1><p class="muted">Tuyển chọn theo chủ đề bởi đội ngũ ARES — dễ chọn quà, dễ hoàn thiện kệ trưng bày.</p></header>
    <div v-if="loading" class="cg"><div v-for="i in 4" :key="i" class="sk" style="aspect-ratio:16/10" /></div>
    <p v-else-if="err" class="alert alert-error">{{ err }} <button class="btn btn-sm btn-outline" @click="load">Thử lại</button></p>
    <p v-else-if="!list.length" class="muted">Chưa có bộ sưu tập nào.</p>
    <div v-else class="cg" v-stagger>
      <router-link v-for="(c, i) in list" :key="c.id" :to="`/collections/${c.slug}`" :class="['cc', { big: i === 0 }]" :style="{ '--ac': c.accent || 'var(--primary)' }">
        <img :src="c.cover" alt="" loading="lazy" /><div class="ov"><small>{{ c.subtitle }}</small><h2>{{ c.title }}</h2><span>{{ c.count }} sản phẩm →</span></div></router-link>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api, { errMsg } from '../api'
import Breadcrumb from '../components/Breadcrumb.vue'
const list = ref([]), loading = ref(true), err = ref('')
const load = async () => { loading.value = true; err.value = ''; try { list.value = (await api.get('/public/collections')).data } catch (e) { err.value = errMsg(e) } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>
.hd { margin-bottom: 20px; } .hd p { max-width: 560px; }
.cg { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 16px; }
.cc { position: relative; aspect-ratio: 16/10; overflow: hidden; display: block; color: #fff; background: #111; }
.cc.big { grid-column: 1 / -1; aspect-ratio: 21/8; }
.cc img { width: 100%; height: 100%; object-fit: cover; opacity: .75; }
.ov { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: flex-end; padding: clamp(14px, 3vw, 28px); background: linear-gradient(180deg, transparent 35%, rgba(0,0,0,.85)); border-left: 5px solid var(--ac); }
.ov small { text-transform: uppercase; letter-spacing: .14em; font-size: .7rem; color: #ddd; } .ov h2 { color: #fff; margin: 4px 0; font-size: clamp(1.4rem, 4vw, 2.6rem); } .ov span { font-weight: 600; font-size: .85rem; color: var(--ac); filter: brightness(1.4); }
@media (max-width: 599px) { .cc.big { aspect-ratio: 16/10; } }
</style>
