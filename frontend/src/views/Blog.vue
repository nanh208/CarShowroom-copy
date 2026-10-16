<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: $t('Tin tức & Review') }]" />
    <h1>{{ $t('Tin tức & Review') }}</h1>
    <div class="bar">
      <div class="kinds" role="tablist">
        <button v-for="k in KINDS" :key="k[0]" role="tab" :aria-selected="kind === k[0]" :class="['kt', { on: kind === k[0] }]" @click="setKind(k[0])">{{ $t(k[1]) }}</button>
      </div>
      <input v-model="q" class="input s" type="search" :placeholder="$t('Tìm bài viết…')" :aria-label="$t('Tìm bài viết')" @keyup.enter="load(0)" />
    </div>
    <div v-if="tags.length" class="tags"><button v-for="t in tags" :key="t.tag" :class="['tag', { on: tag === t.tag }]" @click="tag = tag === t.tag ? '' : t.tag; load(0)">#{{ t.tag }} <small>{{ t.count }}</small></button></div>
    <div v-if="loading && !items.length" class="grid grid-3"><div v-for="i in 3" :key="i" class="sk" style="height:300px" /></div>
    <div v-else-if="!items.length" class="empty">{{ $t('Chưa có bài viết.') }}</div>
    <div v-else class="grid grid-3">
      <router-link v-for="p in items" :key="p.id" :to="`/blog/${p.slug}`" class="card post">
        <div class="cv"><img v-if="p.coverImage" :src="p.coverImage" :alt="p.title" loading="lazy" decoding="async" /><div v-else class="ph">{{ p.kind === 'VIDEO' ? '▶' : 'ARES' }}</div><span class="k">{{ $t(KIND[p.kind]) }}</span></div>
        <div class="bd"><h2>{{ en && p.titleEn ? p.titleEn : p.title }}</h2><p class="muted small">{{ en && p.excerptEn ? p.excerptEn : p.excerpt }}</p>
          <p class="meta small muted">{{ d(p.publishedAt) }} · {{ $t('{0} phút đọc', p.readMinutes) }}</p></div>
      </router-link>
    </div>
    <Pager :p="pager" />
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Breadcrumb from '../components/Breadcrumb.vue'
import Pager from '../components/Pager.vue'
import api from '../api'
import { d } from '../format'
import { locale } from '../i18n'
const KINDS = [['', 'Tất cả'], ['NEWS', 'Tin tức'], ['BLOG', 'Blog'], ['VIDEO', 'Video review']], KIND = { NEWS: 'Tin tức', BLOG: 'Blog', VIDEO: 'Video review' }
const route = useRoute(), router = useRouter(), en = computed(() => locale.value === 'en')
const kind = ref(route.query.kind || ''), tag = ref(route.query.tag || ''), q = ref(''), items = ref([]), tags = ref([]), loading = ref(false), total = ref(0), page = ref(0), size = 9
const pager = { page, pages: computed(() => Math.ceil(total.value / size)) }
let internal = false
watch(page, p => { if (!internal) load(p) })
function setKind(k) { kind.value = k; load(0) }
async function load(p) {
  loading.value = true; internal = true; page.value = p; internal = false; router.replace({ query: { ...(kind.value && { kind: kind.value }), ...(tag.value && { tag: tag.value }) } })
  try { const { data } = await api.get('/public/posts', { params: { kind: kind.value, tag: tag.value, q: q.value, page: p, size } }); items.value = data.content; total.value = data.totalElements } finally { loading.value = false }
}
api.get('/public/posts/tags').then(r => (tags.value = r.data)); load(0)
</script>
<style scoped>
.bar { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; justify-content: space-between; margin: 12px 0 }
.s { max-width: 260px }
.kinds { display: flex; flex-wrap: wrap; gap: 6px }
.kt { border: 1px solid var(--border); background: var(--surface, #fff); color: var(--text, #111); padding: 6px 14px; font-weight: 600; font-size: 14px; border-radius: 6px }
.kt.on { background: var(--accent, #f15a24); border-color: var(--accent, #f15a24); color: #fff }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 16px }
.tag { border: 1px solid var(--border); background: none; border-radius: 999px; padding: 3px 10px; font-size: 13px }
.tag.on { background: var(--text, #111); color: var(--bg, #fff) }
.post { display: flex; flex-direction: column; padding: 0; overflow: hidden; color: inherit; text-decoration: none }
.cv { position: relative; aspect-ratio: 16 / 9; background: var(--surface-2, #eee) }
.cv img { width: 100%; height: 100%; object-fit: cover }
.ph { display: grid; place-items: center; height: 100%; font-weight: 800; font-size: 28px; color: var(--muted) }
.k { position: absolute; left: 10px; top: 10px; background: var(--accent, #f15a24); color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 4px; text-transform: uppercase }
.bd { padding: 14px }
.bd h2 { font-size: 1.05rem; margin: 0 0 6px; line-height: 1.35 }
.meta { margin: 8px 0 0 }
</style>
