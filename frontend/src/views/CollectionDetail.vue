<template>
  <div class="page">
    <div v-if="loading" class="container"><div class="sk" style="height:260px;margin:16px 0" /><Skeleton :count="4" /></div>
    <div v-else-if="err" class="container"><p class="alert alert-error" style="margin-top:24px">{{ err }}</p><router-link to="/collections" class="btn btn-outline">← Tất cả bộ sưu tập</router-link></div>
    <template v-else>
      <section class="ban" :style="{ '--ac': c.accent || 'var(--primary)' }">
        <img :src="c.cover" alt="" /><div class="container in"><Breadcrumb :items="[{ label: 'Bộ sưu tập', to: '/collections' }, { label: c.title }]" />
          <small v-reveal>{{ c.subtitle }}</small><h1 v-reveal:40>{{ c.title }}</h1><p v-reveal:80>{{ c.description }}</p></div></section>
      <div class="container">
        <div class="row between bar"><b>{{ c.products.length }} sản phẩm</b>
          <select v-model="sort" class="input" style="width:auto" aria-label="Sắp xếp"><option value="">Thứ tự gợi ý</option><option value="asc">Giá tăng dần</option><option value="desc">Giá giảm dần</option></select></div>
        <div class="grid grid-4 pg" v-stagger><ProductCard v-for="p in sorted" :key="p.id" :p="p" /></div>
      </div>
    </template>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import api, { errMsg } from '../api'
import Breadcrumb from '../components/Breadcrumb.vue'
import ProductCard from '../components/ProductCard.vue'
import Skeleton from '../components/Skeleton.vue'
const route = useRoute(), c = ref(null), loading = ref(true), err = ref(''), sort = ref('')
const sorted = computed(() => { const l = [...(c.value?.products || [])]; if (sort.value) l.sort((a, b) => sort.value === 'asc' ? a.price - b.price : b.price - a.price); return l })
watch(() => route.params.slug, async s => { if (!s) return; loading.value = true; err.value = ''; try { c.value = (await api.get('/public/collections/' + s)).data; document.title = c.value.title + ' · ARES Toys' } catch (e) { err.value = errMsg(e) } finally { loading.value = false } }, { immediate: true })
</script>
<style scoped>
.ban { position: relative; background: #111; color: #fff; min-height: clamp(240px, 38vw, 380px); display: flex; align-items: flex-end; overflow: hidden; border-bottom: 5px solid var(--ac); }
.ban img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; opacity: .45; }
.ban .in { position: relative; padding-top: 16px; padding-bottom: clamp(20px, 4vw, 40px); } .ban :deep(a), .ban :deep(span) { color: #ddd; }
.ban small { text-transform: uppercase; letter-spacing: .14em; color: var(--ac); filter: brightness(1.4); font-weight: 700; font-size: .75rem; }
.ban h1 { color: #fff; font-size: clamp(2rem, 6vw, 4rem); margin: 6px 0; } .ban p { max-width: 620px; color: #ddd; }
.bar { margin: 20px 0 14px; gap: 10px; flex-wrap: wrap; }
</style>
