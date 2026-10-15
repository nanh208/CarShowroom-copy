// Tìm kiếm + sắp xếp + phân trang phía client cho danh sách quản trị.
import { ref, computed, watch } from 'vue'
const norm = s => String(s ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd')
export function usePaged(source, { size = 20, fields = () => [], sorts = {}, sort: initial = '' } = {}) {
  const q = ref(''), sort = ref(initial || Object.keys(sorts)[0] || ''), page = ref(0)
  const filtered = computed(() => {
    const n = norm(q.value.trim())
    let l = n ? source.value.filter(x => norm(fields(x).join(' ')).includes(n)) : [...source.value]
    if (sorts[sort.value]?.fn) l.sort(sorts[sort.value].fn)
    return l
  })
  const pages = computed(() => Math.max(1, Math.ceil(filtered.value.length / size)))
  const items = computed(() => filtered.value.slice(page.value * size, (page.value + 1) * size))
  watch([q, sort, () => source.value.length], () => { if (page.value >= pages.value) page.value = 0 })
  watch(q, () => (page.value = 0))
  return { q, sort, page, pages, items, total: computed(() => filtered.value.length), sorts }
}
