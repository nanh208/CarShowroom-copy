<template>
  <div class="container page">
    <Breadcrumb :items="[{ label: 'Danh sách yêu thích được chia sẻ' }]" />
    <div class="row between head"><div><h1>{{ title }}</h1><p class="muted">{{ list.length }} sản phẩm · chia sẻ từ một người bạn ARES Toys</p></div>
      <button v-if="list.length" class="btn" @click="addAll">Thêm tất cả vào giỏ</button></div>
    <div v-if="loading" class="spinner" />
    <div v-else-if="!list.length" class="card empty">Danh sách trống hoặc liên kết đã hết hạn. <router-link to="/products">Xem sản phẩm →</router-link></div>
    <div v-else class="grid grid-4"><ProductCard v-for="p in list" :key="p.id" :p="p" /></div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'
import ProductCard from '../components/ProductCard.vue'
import Breadcrumb from '../components/Breadcrumb.vue'
import { useCart } from '../stores/cart'
import { toast } from '../stores/prefs'
const route = useRoute(), cart = useCart(), list = ref([]), loading = ref(true)
const title = computed(() => route.query.name ? `Yêu thích của ${String(route.query.name).slice(0, 40)}` : 'Danh sách yêu thích')
const addAll = () => { let n = 0; for (const p of list.value) { if (p.stock > 0 && !p.hasVariants) { cart.add({ productId: p.id, name: p.name, image: p.images?.[0], unitPrice: p.price, oldPrice: p.oldPrice, quantity: 1, stock: p.stock }); n++ } }
  toast(n ? `Đã thêm ${n} sản phẩm vào giỏ` : 'Các mẫu cần chọn phiên bản — mở từng sản phẩm để thêm', n ? 'success' : 'info') }
onMounted(async () => { try { list.value = (await api.get('/public/wishlist', { params: { ids: route.query.ids || '' } })).data } finally { loading.value = false } })
</script>
<style scoped>.head { margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }</style>
