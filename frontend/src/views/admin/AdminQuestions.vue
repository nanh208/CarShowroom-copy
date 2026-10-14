<template>
  <div>
    <div class="row between"><h1>Hỏi đáp sản phẩm</h1>
      <div class="row" style="gap:6px"><button v-for="t in tabs" :key="t[0]" :class="['btn btn-sm', tab === t[0] ? '' : 'btn-outline']" @click="tab = t[0]">{{ t[1] }}</button></div></div>
    <div class="list">
      <div v-for="q in shown" :key="q.id" class="card qa" :class="{ hidden: !q.visible }">
        <div class="small muted">{{ q.authorName }} · {{ d(q.createdAt) }} · <router-link :to="`/products/${q.productId}`" target="_blank">{{ q.productName }}</router-link></div>
        <p class="q">❓ {{ q.question }}</p>
        <p v-if="q.answer" class="a">💬 {{ q.answer }} <span class="small muted">— {{ q.answeredBy }}</span></p>
        <div class="row" style="gap:8px;align-items:flex-start">
          <textarea v-model="draft[q.id]" rows="2" class="input" style="flex:1" :placeholder="q.answer ? 'Sửa câu trả lời…' : 'Nhập câu trả lời…'" />
          <div class="col">
            <button class="btn btn-sm" :disabled="!draft[q.id]" @click="patch(q, { answer: draft[q.id] })">Trả lời</button>
            <button class="btn btn-sm btn-outline" @click="patch(q, { visible: !q.visible })">{{ q.visible ? 'Ẩn' : 'Hiện' }}</button>
            <button class="btn btn-sm btn-danger" @click="remove(q)">Xoá</button>
          </div>
        </div>
      </div>
      <div v-if="!shown.length" class="empty card">Không có câu hỏi.</div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../api'
import { d } from '../../format'
const list = ref([]), draft = ref({}), tab = ref('open')
const tabs = [['open', 'Chưa trả lời'], ['done', 'Đã trả lời'], ['all', 'Tất cả']]
const shown = computed(() => list.value.filter(q => tab.value === 'all' || (tab.value === 'open' ? !q.answer : !!q.answer)))
const load = async () => { list.value = (await api.get('/admin/questions')).data }
const patch = async (q, body) => { await api.patch(`/admin/questions/${q.id}`, body); draft.value[q.id] = ''; load() }
const remove = async q => { if (confirm('Xoá câu hỏi?')) { await api.delete(`/admin/questions/${q.id}`); load() } }
onMounted(load)
</script>
<style scoped>
.list { display: flex; flex-direction: column; gap: 14px; } .qa.hidden { opacity: .55; }
.q { font-weight: 600; margin: 8px 0 4px; } .a { color: var(--text-2); margin: 0 0 10px; }
.col { display: flex; flex-direction: column; gap: 6px; }
</style>
