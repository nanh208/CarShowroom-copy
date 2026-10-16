<template>
  <div class="container page">
    <div v-if="loading" class="sk" style="height:420px" />
    <div v-else-if="err" class="empty">{{ err }} · <router-link to="/blog">{{ $t('Quay lại tin tức') }}</router-link></div>
    <article v-else-if="p" class="art">
      <Breadcrumb :items="[{ label: $t('Tin tức & Review'), to: '/blog' }, { label: title }]" />
      <p class="kind small">{{ $t(KIND[p.kind]) }} · {{ d(p.publishedAt) }} · {{ $t('{0} phút đọc', p.readMinutes) }} · {{ p.views }} {{ $t('lượt xem') }}</p>
      <h1>{{ title }}</h1>
      <p v-if="excerpt" class="lead muted">{{ excerpt }}</p>
      <div v-if="p.videoUrl" class="video">
        <video v-if="/\.(mp4|webm)$/.test(p.videoUrl)" :src="p.videoUrl" controls preload="metadata" />
        <iframe v-else :src="p.videoUrl" :title="title" loading="lazy" allow="accelerometer; encrypted-media; gyroscope; picture-in-picture" allowfullscreen referrerpolicy="strict-origin-when-cross-origin" />
      </div>
      <img v-else-if="p.coverImage" :src="p.coverImage" :alt="title" class="cover" decoding="async" />
      <!-- HTML đã được server làm sạch bằng jsoup Safelist -->
      <div class="rich" v-html="html" />
      <div v-if="p.tags?.length" class="tags"><router-link v-for="t in p.tags" :key="t" :to="{ path: '/blog', query: { tag: t } }" class="tag">#{{ t }}</router-link></div>
      <section v-if="p.products?.length"><h2>{{ $t('Sản phẩm trong bài') }}</h2><div class="grid grid-4"><ProductCard v-for="x in p.products" :key="x.id" :p="x" /></div></section>
      <section v-if="p.related?.length"><h2>{{ $t('Bài viết liên quan') }}</h2>
        <div class="grid grid-3"><router-link v-for="r in p.related" :key="r.id" :to="`/blog/${r.slug}`" class="card rel"><b>{{ en && r.titleEn ? r.titleEn : r.title }}</b><span class="small muted">{{ d(r.publishedAt) }}</span></router-link></div>
      </section>
    </article>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import Breadcrumb from '../components/Breadcrumb.vue'
import ProductCard from '../components/ProductCard.vue'
import api, { errMsg } from '../api'
import { d } from '../format'
import { locale } from '../i18n'
const KIND = { NEWS: 'Tin tức', BLOG: 'Blog', VIDEO: 'Video review' }
const route = useRoute(), p = ref(null), loading = ref(true), err = ref(''), en = computed(() => locale.value === 'en')
const title = computed(() => en.value && p.value?.titleEn ? p.value.titleEn : p.value?.title)
const excerpt = computed(() => en.value && p.value?.excerptEn ? p.value.excerptEn : p.value?.excerpt)
const html = computed(() => en.value && p.value?.contentHtmlEn ? p.value.contentHtmlEn : p.value?.contentHtml)
watch(() => route.params.slug, async s => { if (!s) return; loading.value = true; err.value = ''
  try { p.value = (await api.get('/public/posts/' + s)).data } catch (e) { err.value = errMsg(e) } finally { loading.value = false } }, { immediate: true })
</script>
<style scoped>
.art { max-width: 820px; margin: 0 auto }
.kind { color: var(--accent, #f15a24); text-transform: uppercase; letter-spacing: .04em; margin: 8px 0 }
h1 { font-size: clamp(1.5rem, 4vw, 2.2rem); line-height: 1.25; margin: 0 0 10px }
.lead { font-size: 1.1rem }
.video { position: relative; aspect-ratio: 16 / 9; margin: 16px 0; background: #000; border-radius: 8px; overflow: hidden }
.video iframe, .video video { position: absolute; inset: 0; width: 100%; height: 100%; border: 0 }
.cover { width: 100%; border-radius: 8px; margin: 16px 0 }
.rich { line-height: 1.75; font-size: 1.02rem; overflow-wrap: anywhere }
.rich :deep(img) { max-width: 100%; height: auto; border-radius: 6px }
.rich :deep(iframe) { width: 100%; aspect-ratio: 16 / 9; height: auto; border: 0 }
.rich :deep(table) { display: block; overflow-x: auto }
.tags { display: flex; gap: 6px; flex-wrap: wrap; margin: 20px 0 }
.tag { border: 1px solid var(--border); border-radius: 999px; padding: 3px 10px; font-size: 13px; color: inherit; text-decoration: none }
section { margin-top: 32px }
.rel { display: flex; flex-direction: column; gap: 6px; color: inherit; text-decoration: none }
</style>
