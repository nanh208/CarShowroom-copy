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
  ['Mua hàng', 'Hàng ở ARES Toys có chính hãng không?', 'Tất cả sản phẩm Hot Wheels, Mini GT, Tomica, Maisto, Bburago, LEGO, Tamiya, Traxxas… đều nhập chính hãng, có tem và hoá đơn.'],
  ['Mua hàng', 'Chọn tỷ lệ mô hình thế nào?', '1:64 nhỏ gọn (~7cm) hợp sưu tầm số lượng; 1:24 (~19cm) cân bằng chi tiết và giá; 1:18 (~25cm) cho tủ trưng bày cao cấp.'],
  ['Mua hàng', 'Độ tuổi phù hợp?', 'Mỗi sản phẩm ghi độ tuổi khuyến nghị (3+, 6+, 8+, 14+). Xe RC tốc độ cao và pin LiPo dành cho 14+ có người lớn giám sát.'],
  ['Thanh toán', 'Có những hình thức thanh toán nào?', 'COD, chuyển khoản VietQR, ví MoMo, VNPay và trả góp 0% qua thẻ tín dụng 3–12 tháng cho đơn từ 3 triệu.'],
  ['Thanh toán', 'Điểm thưởng dùng thế nào?', 'Mỗi 10.000đ trên đơn hoàn tất tích 1 điểm. 1 điểm = 1.000đ, dùng tối đa 50% giá trị đơn ở bước thanh toán.'],
  ['Vận chuyển', 'Phí ship bao nhiêu?', 'HCM/Hà Nội 20.000đ, tỉnh lân cận 30.000đ, tỉnh khác 40.000đ. Miễn phí cho đơn từ 500.000đ hoặc dùng mã FREESHIP.'],
  ['Vận chuyển', 'Bao lâu nhận được hàng?', 'Nội thành 1–2 ngày, các tỉnh 2–4 ngày. Theo dõi hành trình trong trang chi tiết đơn hàng.'],
  ['Đổi trả', 'Chính sách đổi trả?', 'Trong 7 ngày kể từ khi nhận hàng, gửi yêu cầu kèm ảnh ở trang đơn hàng. Lỗi nhà sản xuất được đổi mới, ARES chịu phí vận chuyển.'],
  ['Bảo hành', 'Tra cứu bảo hành ở đâu?', 'Vào Dịch vụ › Tra cứu bảo hành, nhập mã đơn và số điện thoại. Xe RC bảo hành 3–6 tháng, tay khiển 6–12 tháng.'],
  ['Đấu giá', 'Đấu giá hàng hiếm hoạt động ra sao?', 'Đăng nhập, chọn phiên đang diễn ra và đặt giá ≥ giá hiện tại + bước giá. Đặt giá trong 2 phút cuối sẽ gia hạn phiên. Người thắng nhận đơn thanh toán tự động.'],
  ['Kỹ thuật', 'Bảo quản pin LiPo thế nào?', 'Dùng sạc cân bằng, không sạc qua đêm không giám sát, cất ở 3.8V/cell trong túi chống cháy, ngưng dùng khi pin phồng.']]
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
