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

/**
 * Phân trang phía MÁY CHỦ cho danh sách quản trị (AdminPagingAdvice):
 * GET url?page&size&q&sort=field,desc&f.field=value → { content, totalElements, totalPages }.
 * Cùng giao diện với usePaged để dùng lại ListTools/Pager.
 */
import api from '../api'
export function useServerPaged(url, { size = 20, sorts = {}, sort: initial = '', filters = () => ({}) } = {}) {
  const q = ref(''), sort = ref(initial || Object.keys(sorts)[0] || ''), page = ref(0)
  const rows = ref([]), total = ref(0), totalPages = ref(1), loading = ref(false), error = ref('')
  let seqN = 0, t
  async function reload() {
    const my = ++seqN; loading.value = true; error.value = ''
    try {
      const params = { page: page.value, size, q: q.value.trim() || undefined, sort: sorts[sort.value]?.sort, ...filters() }
      for (const k of Object.keys(params)) if (params[k] === '' || params[k] == null) delete params[k]
      const { data } = await api.get(url, { params })
      if (my !== seqN) return
      rows.value = data.content; total.value = data.totalElements; totalPages.value = data.totalPages
      if (page.value > 0 && page.value >= data.totalPages) { page.value = 0 }
    } catch (e) { if (my === seqN) error.value = e?.response?.data?.message || 'Không tải được dữ liệu' } finally { if (my === seqN) loading.value = false }
  }
  watch(q, () => { clearTimeout(t); t = setTimeout(() => { page.value = 0; reload() }, 300) })
  watch(sort, () => { page.value = 0; reload() })
  watch(page, reload)
  watch(() => JSON.stringify(filters()), () => { page.value = 0; reload() })
  return { q, sort, page, pages: totalPages, items: rows, total, sorts, reload, loading, error }
}
