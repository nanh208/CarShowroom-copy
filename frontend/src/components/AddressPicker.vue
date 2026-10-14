<template>
  <div class="ap">
    <div class="ap-grid">
      <div class="field">
        <label :for="id + 'p'">Tỉnh / Thành phố *</label>
        <input :id="id + 'p'" v-model="m.province" class="input" :list="id + 'pl'" autocomplete="address-level1" placeholder="Gõ để tìm, VD: Hồ Chí Minh" @change="pickProvince" @input="pickProvince" />
        <datalist :id="id + 'pl'"><option v-for="p in provinces" :key="p" :value="p" /></datalist>
      </div>
      <div class="field">
        <label :for="id + 'd'">Quận / Huyện *</label>
        <input :id="id + 'd'" v-model="m.district" class="input" :list="id + 'dl'" autocomplete="address-level2" :disabled="!prov" :placeholder="prov ? 'Gõ để tìm quận/huyện' : 'Chọn tỉnh trước'" @input="pickDistrict" />
        <datalist :id="id + 'dl'"><option v-for="d in districts" :key="d" :value="d" /></datalist>
      </div>
      <div class="field">
        <label :for="id + 'w'">Phường / Xã *</label>
        <input :id="id + 'w'" v-model="m.ward" class="input" :list="id + 'wl'" autocomplete="address-level3" :disabled="!dist" :placeholder="dist ? 'Gõ để tìm phường/xã' : 'Chọn quận/huyện trước'" @input="emitAll" />
        <datalist :id="id + 'wl'"><option v-for="w in wards" :key="w" :value="w" /></datalist>
      </div>
    </div>
    <div class="field"><label :for="id + 'l'">Số nhà, tên đường *</label><input :id="id + 'l'" v-model="m.line" class="input" autocomplete="address-line1" placeholder="VD: 12 Nguyễn Huệ" @input="emitAll" /></div>
    <small v-if="loadErr" class="muted">Không tải được danh sách địa giới — bạn vẫn có thể gõ tay.</small>
  </div>
</template>
<script setup>
// Gợi ý địa chỉ VN 3 cấp (63 tỉnh/thành · 696 quận/huyện · ~10.000 phường/xã), dữ liệu tĩnh /vn-divisions.json tải khi cần.
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { toShipCity } from '../format'
const props = defineProps({ modelValue: { type: Object, default: () => ({}) }, cities: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const id = 'ap' + Math.random().toString(36).slice(2, 7)
const m = reactive({ province: '', district: '', ward: '', line: '', ...props.modelValue })
const data = ref([]), loadErr = ref(false)
let cache
const load = () => cache || (cache = fetch('/vn-divisions.json').then(r => r.json()))
onMounted(async () => { try { data.value = await load() } catch { loadErr.value = true } })
const provinces = computed(() => data.value.map(p => p[0]))
const prov = computed(() => data.value.find(p => p[0] === m.province))
const districts = computed(() => prov.value ? prov.value[1].map(d => d[0]) : [])
const dist = computed(() => prov.value?.[1].find(d => d[0] === m.district))
const wards = computed(() => dist.value ? dist.value[1] : [])
const cityOf = name => toShipCity(name, props.cities)
const emitAll = () => emit('update:modelValue', { ...m, city: cityOf(prov.value ? m.province : ''), valid: !!(prov.value && dist.value && wards.value.includes(m.ward) && m.line.trim()),
  full: [m.line.trim(), m.ward, m.district].filter(Boolean).join(', ') })
const pickProvince = () => { if (!prov.value) { m.district = ''; m.ward = '' } emitAll() }
const pickDistrict = () => { if (!dist.value) m.ward = ''; emitAll() }
watch(() => props.cities, emitAll)
</script>
<style scoped>
.ap-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0 12px; }
@media (max-width: 720px) { .ap-grid { grid-template-columns: 1fr; } }
.input:disabled { opacity: .6; }
</style>
