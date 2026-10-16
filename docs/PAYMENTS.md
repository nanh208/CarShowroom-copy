# Cổng thanh toán thật: VNPay 2.1.0 & MoMo v2

Code: `backend/.../service/GatewayClient.java` (ký/gọi API) + `web/PaymentGatewayController.java` (luồng, IPN, return, đối soát, hoàn tiền).
Trang quản trị: **Admin › Thanh toán** hiển thị chế độ từng cổng (REAL / MOCK / DISABLED), endpoint, IPN/Return URL — không bao giờ hiển thị khoá bí mật.

## Biến môi trường

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `PUBLIC_BASE_URL` | `http://localhost:4173` | Địa chỉ HTTPS công khai của site (frontend có proxy `/api`). Dùng để tạo `vnp_ReturnUrl`, MoMo `redirectUrl`/`ipnUrl`. |
| `VNPAY_TMN_CODE` / `VNPAY_HASH_SECRET` | *(trống)* | Trống → VNPay chạy cổng **giả lập nội bộ** (nếu `PAYMENT_SANDBOX=true`). |
| `VNPAY_PAY_URL` | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | Production: `https://pay.vnpay.vn/vpcpay.html` |
| `VNPAY_API_URL` | `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` | querydr / refund |
| `MOMO_PARTNER_CODE` / `MOMO_ACCESS_KEY` / `MOMO_SECRET_KEY` | bộ khoá TEST công khai của MoMo (`MOMO` / `F8BBA842ECF85` / `K951B6…`) từ github.com/momo-wallet/payment | Chỉ dùng với môi trường test. |
| `MOMO_ENDPOINT` | `https://test-payment.momo.vn` | Production: `https://payment.momo.vn` |
| `MOMO_ENABLED` | `true` | `false` → MoMo dùng cổng giả lập / tắt |
| `PAYMENT_SANDBOX` | `true` (profile `prod`: `false`) | Bật/tắt cổng giả lập nội bộ |

## VNPay sandbox
VNPay **không công bố khoá test dùng chung**. Đăng ký tại http://sandbox.vnpayment.vn/devreg/ → nhận `vnp_TmnCode` + `vnp_HashSecret` qua email.
Thẻ test sandbox (NCB): `9704198526191432198`, chủ thẻ `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
Khai báo IPN URL với VNPay (trong trang merchant sandbox): `https://<PUBLIC_BASE_URL>/api/public/payments/vnpay/ipn`.

## IPN cần URL công khai
VNPay/MoMo gọi IPN từ Internet, nên `localhost` không nhận được. Dùng tunnel trỏ vào cổng frontend (proxy `/api` → 8080):
```bash
# ngrok
ngrok http 4173            # → https://abcd.ngrok-free.app
# hoặc Cloudflare
cloudflared tunnel --url http://localhost:4173
# chạy backend với URL đó
PUBLIC_BASE_URL=https://abcd.ngrok-free.app VNPAY_TMN_CODE=... VNPAY_HASH_SECRET=... java -jar target/model-car-shop-1.0.0.jar
```
Nếu IPN không tới được (vd. chạy localhost), khi khách quay về Return URL hệ thống sẽ **đối soát chủ động** (VNPay `querydr` / MoMo `query`) và chỉ ghi nhận khi phản hồi hợp lệ, khớp số tiền.
Vite preview cần cho phép host tunnel: `vite preview --host` + `preview.allowedHosts`.

## Bảo đảm an toàn
- Chữ ký: VNPay HMAC-SHA512 trên tham số `vnp_*` sắp xếp a→z (giá trị URL-encode); querydr/refund ký chuỗi nối `|`; MoMo HMAC-SHA256 theo thứ tự trường trong tài liệu. So sánh hằng thời gian.
- IPN VNPay trả `RspCode`: 97 sai chữ ký → 01 không có đơn/sai TmnCode → 04 sai số tiền → 02 đã xác nhận → 00 thành công; 99 lỗi khác.
- IPN MoMo: sai chữ ký/partner/số tiền → 400; hợp lệ → 204.
- Idempotent: mỗi đơn chỉ được đánh dấu PAID một lần; IPN lặp trả 02/204 mà không ghi nhận lại.
- Hết hạn: `vnp_ExpireDate` = tạo + 15 phút (GMT+7); giao dịch chờ quá hạn → EXPIRED; nếu cổng vẫn báo thành công sau đó (khách đã bị trừ tiền) thì vẫn ghi nhận.
- Giao dịch thật không thể hoàn tất qua cổng giả lập (`/sandbox/*` trả 404 cho giao dịch REAL).
- Hoàn tiền (Admin): VNPay `refund` (02 toàn phần / 03 một phần), MoMo `/v2/gateway/api/refund`; không vượt quá số đã thu − đã hoàn.
