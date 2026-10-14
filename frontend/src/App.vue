<template>
  <template v-if="$route.meta.bare || $route.path.startsWith('/admin')">
    <router-view />
  </template>
  <template v-else>
    <AppHeader />
    <main><router-view /></main>
    <AppFooter />
    <ChatWidget />
    <CompareBar />
  </template>
  <Toasts />
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import ChatWidget from './components/ChatWidget.vue'
import CompareBar from './components/CompareBar.vue'
import Toasts from './components/Toasts.vue'
</script>
