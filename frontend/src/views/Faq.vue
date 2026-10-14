<template>
  <div class="container page narrow">
    <Breadcrumb :items="[{ label: 'Câu hỏi thường gặp' }]" />
    <h1>Câu hỏi thường gặp</h1>
    <p>Chưa tìm thấy câu trả lời? Gọi hotline {{ HOTLINE }} hoặc chat với chuyên viên ở góc phải màn hình.</p>
    <div class="cats"><button v-for="c in cats" :key="c" :class="{ on: cat === c }" @click="cat = c">{{ c }}</button></div>
    <div class="list">
      <details v-for="f in shown" :key="f[1]" class="qa">
        <summary>{{ f[1] }}<span class="plus" /></summary>
        <p>{{ f[2] }}</p>
      </details>
    </div>
  </div>
</template>
<script setup>
import { ref, computed } from 'vue'
import Breadcrumb from '../components/Breadcrumb.vue'
import { HOTLINE } from '../format'
const faqs = [
  ['Mua xe', 'Đặt cọc bao nhiêu để giữ xe?', 'Bạn chỉ cần đặt cọc 10% giá trị xe. Phần còn lại thanh toán khi nhận xe hoặc chuyển sang trả góp ngân hàng.'],
  ['Mua xe', 'Giá niêm yết đã bao gồm thuế phí lăn bánh chưa?', 'Giá niêm yết đã gồm VAT, chưa gồm lệ phí trước bạ, biển số, đăng kiểm và bảo hiểm. Chuyên viên sẽ báo giá lăn bánh chi tiết theo tỉnh/thành.'],
  ['Mua xe', 'Tôi có thể đổi màu sơn, mâm, nội thất không?', 'Có. Ở trang chi tiết xe, chọn màu sơn, mâm và nội thất; giá được cập nhật ngay. Xe cấu hình riêng giao trong 4–12 tuần tuỳ hãng.'],
  ['Trả góp', 'Điều kiện trả góp là gì?', 'Đơn từ 300 triệu, trả trước tối thiểu 20%, kỳ hạn 12–84 tháng, lãi suất phẳng tham khảo 0,5%/tháng. Hồ sơ gồm CCCD, sổ hộ khẩu/xác nhận cư trú và chứng minh thu nhập.'],
  ['Trả góp', 'Bao lâu thì được duyệt vay?', 'Thông thường 1–3 ngày làm việc sau khi đủ hồ sơ. ARES hỗ trợ nộp hồ sơ với các ngân hàng đối tác.'],
  ['Đấu giá', 'Làm sao để tham gia đấu giá xe hiếm?', 'Đăng nhập, vào mục Đấu giá, chọn phiên đang diễn ra và đặt giá tối thiểu bằng giá hiện tại cộng bước giá. Đặt giá trong 2 phút cuối sẽ gia hạn phiên thêm 2 phút.'],
  ['Đấu giá', 'Thắng đấu giá rồi thì sao?', 'Hệ thống tự tạo đơn đặt cọc 10% cho bạn và gửi thông báo. Vui lòng hoàn tất đặt cọc trong 48 giờ để giữ quyền mua.'],
  ['Dịch vụ', 'Có lái thử tại nhà không?', 'Có, trong bán kính 30 km quanh showroom. Đặt lịch ở trang Dịch vụ, mang theo bằng lái B2 trở lên.'],
  ['Dịch vụ', 'Bảo dưỡng định kỳ ở đâu?', 'Tại xưởng dịch vụ ARES, có nhận xe tận nhà và xe thay thế. Đặt lịch ở trang Dịch vụ › Sửa chữa & bảo dưỡng.'],
  ['Giao xe', 'Giao xe mất bao lâu?', 'Xe có sẵn giao trong 3–7 ngày, miễn phí toàn quốc. Bạn có thể theo dõi trạng thái đơn trong Tài khoản › Lịch sử giao dịch.']]
const cats = ['Tất cả', ...new Set(faqs.map(f => f[0]))]
const cat = ref('Tất cả')
const shown = computed(() => faqs.filter(f => cat.value === 'Tất cả' || f[0] === cat.value))
</script>
<style scoped>
.narrow { max-width: 880px; }
.cats { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 32px; }
.cats button { padding: 8px 18px; border-radius: 999px; border: 1px solid var(--border); background: none; color: var(--text-2); cursor: pointer; font: 500 .9rem inherit; font-family: inherit; }
.cats button.on { background: #fff; color: var(--bg-2); border-color: var(--text); }
.qa { border-bottom: 1px solid var(--border); }
.qa summary { list-style: none; cursor: pointer; padding: 24px 0; font-size: 1.1rem; font-weight: 600; display: flex; justify-content: space-between; gap: 16px; }
.qa summary::-webkit-details-marker { display: none; }
.plus { width: 20px; height: 20px; position: relative; flex-shrink: 0; } .plus::before, .plus::after { content: ''; position: absolute; background: var(--accent); left: 9px; top: 2px; width: 2px; height: 16px; transition: .2s; }
.plus::after { transform: rotate(90deg); } .qa[open] .plus::before { transform: rotate(90deg); }
.qa p { color: var(--text-2); padding: 0 0 24px; line-height: 1.7; max-width: 720px; }
</style>
