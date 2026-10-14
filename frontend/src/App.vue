<template>
  <RoadProgress />
  <template v-if="$route.meta.bare || $route.path.startsWith('/admin')">
    <router-view />
  </template>
  <template v-else>
    <AppHeader />
    <main>
      <router-view v-slot="{ Component, route }">
        <transition name="page" mode="out-in"><component :is="Component" :key="route.path" /></transition>
      </router-view>
    </main>
    <AppFooter />
    <ChatWidget />
    <CompareBar />
    <TabBar />
    <MobileNav />
  </template>
  <Toasts />
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'
import ChatWidget from './components/ChatWidget.vue'
import CompareBar from './components/CompareBar.vue'
import Toasts from './components/Toasts.vue'
import RoadProgress from './components/RoadProgress.vue'
import TabBar from './components/TabBar.vue'
import MobileNav from './components/MobileNav.vue'
import { loadCatalog } from './format'
loadCatalog()
</script>
