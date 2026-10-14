<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Yêu thích' }]" />
    <h1>Danh sách yêu thích</h1>
    <p>{{ prefs.wish.length ? `${prefs.wish.length} mẫu xe bạn đã lưu. Danh sách được lưu trên trình duyệt này.` : 'Bấm biểu tượng trái tim trên ảnh xe để lưu lại mẫu bạn thích.' }}</p>
    <div v-if="!prefs.wish.length" class="empty card">Chưa có mẫu xe yêu thích. <router-link to="/products">Khám phá mẫu xe →</router-link></div>
    <div v-else class="grid grid-3"><ProductCard v-for="p in prefs.wish" :key="p.id" :p="p" /></div>
  </div>
</template>
<script setup>
import Breadcrumb from '../components/Breadcrumb.vue'
import ProductCard from '../components/ProductCard.vue'
import { usePrefs } from '../stores/prefs'
const prefs = usePrefs()
</script>
<style scoped>.grid-3 { grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); }</style>
