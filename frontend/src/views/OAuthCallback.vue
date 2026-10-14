<template><AuthShell><div class="spinner" /><p class="muted" style="text-align:center">Đang đăng nhập...</p></AuthShell></template>
<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../stores/auth'
import AuthShell from '../components/AuthShell.vue'
const route = useRoute(), router = useRouter(), auth = useAuth()
onMounted(async () => {
  if (!route.query.token) return router.replace('/login?error=' + encodeURIComponent('Đăng nhập mạng xã hội thất bại'))
  await auth.loginWithToken(route.query.token)
  router.replace(auth.loggedIn ? '/' : '/login')
})
</script>
