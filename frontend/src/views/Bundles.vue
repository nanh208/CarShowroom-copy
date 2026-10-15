<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Combo ưu đãi' }]" />
    <h1>Combo ưu đãi</h1>
    <p class="muted" style="margin:6px 0 20px">Giảm giá tự áp dụng trong giỏ khi đủ sản phẩm — cộng dồn với voucher.</p>
    <div v-if="loading" class="spinner" />
    <div v-else class="bgrid"><BundleCard v-for="b in list" :key="b.bundle.id" :b="b" /></div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import BundleCard from '../components/BundleCard.vue'
import Breadcrumb from '../components/Breadcrumb.vue'
const list = ref([]), loading = ref(true)
onMounted(async () => { try { list.value = (await api.get('/public/bundles')).data } finally { loading.value = false } })
</script>
<style scoped>.bgrid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr)); gap: 16px; }</style>
