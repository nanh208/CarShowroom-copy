<template>
  <template v-if="$route.meta.bare || $route.path.startsWith('/admin')">
    <router-view />
  </template>
  <template v-else>
    <AppHeader />
    <main><router-view /></main>
    <AppFooter />
    <ChatWidget />
  </template>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import ChatWidget from './components/ChatWidget.vue'
</script>
