<template>
  <div>
    <h1>Bản dịch (vi → en)</h1>
    <p class="muted small">Khoá là chuỗi tiếng Việt gốc. Sửa bản tiếng Anh rồi lưu — để trống để quay về bản dịch mặc định. Áp dụng ngay cho khách chọn EN.</p>
    <div class="card" style="margin-top:14px">
      <ListTools :p="pg" placeholder="Tìm chuỗi…"><select v-model="ov" class="input" aria-label="Lọc"><option value="">Tất cả</option><option value="true">Đã ghi đè</option></select></ListTools>
      <div v-if="pg.loading.value && !pg.items.value.length" class="sk" style="height:160px" />
      <div v-else-if="!pg.items.value.length" class="empty">Không có chuỗi phù hợp.</div>
      <div v-else class="table-wrap"><table class="table"><thead><tr><th style="width:42%">Tiếng Việt</th><th>English</th><th /></tr></thead>
        <tbody><tr v-for="r in pg.items.value" :key="r.key"><td>{{ r.key }}</td>
          <td><input v-model="draft[r.key]" class="input" :placeholder="r.en" :aria-label="'Bản dịch: ' + r.key" /></td>
          <td style="white-space:nowrap"><span v-if="r.overridden" class="badge badge-SHIPPING">Đã sửa</span>
            <button class="btn btn-sm" :disabled="busy || draft[r.key] === undefined || draft[r.key] === r.en" @click="save(r)">Lưu</button>
            <button v-if="r.overridden" class="btn btn-sm btn-outline" :disabled="busy" @click="save(r, '')">Mặc định</button></td></tr></tbody></table></div>
      <Pager :p="pg" />
    </div>
  </div>
</template>
<script setup>
import { ref, watch } from 'vue'
import ListTools from '../../components/ListTools.vue'
import Pager from '../../components/Pager.vue'
import { useServerPaged } from '../../composables/usePaged'
import api, { errMsg } from '../../api'
import { toast } from '../../stores/prefs'
const ov = ref(''), draft = ref({}), busy = ref(false)
const pg = useServerPaged('/admin/i18n', { size: 25, filters: () => ({ 'f.overridden': ov.value }), sorts: { key: { label: 'A–Z', sort: 'key' } } })
watch(pg.items, l => { for (const r of l) draft.value[r.key] = r.en })
pg.reload()
async function save(r, v) {
  busy.value = true
  try { await api.put('/admin/i18n/en', { [r.key]: v ?? draft.value[r.key] }); toast('Đã lưu bản dịch'); await pg.reload() } catch (e) { toast(errMsg(e), 'error') } finally { busy.value = false }
}
</script>
