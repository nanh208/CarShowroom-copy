# ARES Toys — Cửa hàng xe đồ chơi

Website bán **xe đồ chơi**: xe mô hình die-cast, xe RC, xe trưng bày, bộ kit và phụ kiện RC (pin, tay khiển, bánh xe), kèm **đấu giá hàng hiếm**.
Có trang cho khách hàng và trang quản trị ADMIN/STAFF. Giao diện màu đồng sáng (light bronze), hiệu ứng chuyển động theo chủ đề xe.

| Thư mục | Công nghệ |
|---|---|
| `backend/` | Java 21, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, H2 (mặc định) / MySQL |
| `frontend/` | Vue 3, Vite, Vue Router, Pinia, axios |

## Chạy nhanh

Yêu cầu: JDK 21, Maven 3.9+, Node 18+.

```bash
# 1. Backend (http://localhost:8080)
cd backend
mvn spring-boot:run

# 2. Frontend (http://localhost:5173) – Vite proxy /api và /uploads -> 8080
cd frontend
npm install
npm run dev
```

> **Nâng cấp từ bản showroom ô tô: BẮT BUỘC xoá `backend/data/`** (Windows: `backend\data`) trước khi chạy, để seed lại dữ liệu đồ chơi.

Dữ liệu mẫu (tạo tự động khi DB trống):
- 8 danh mục: Xe Model, Xe RC, Xe Trưng Bày, Bộ kit; Pin, Khiển RC, Bánh xe RC, Linh kiện.
- 17 thương hiệu, 42 sản phẩm (11 có biến thể), flash sale, banner, 5 phiên đấu giá, đánh giá, hỏi đáp và đơn hàng mẫu.
- Ảnh sản phẩm lấy từ Wikimedia Commons theo giấy phép tự do. Nguồn ghi ở `frontend/public/images/toys/CREDITS.md`.

### Tài khoản mặc định

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | `admin@ares.vn` | `admin123` |
| Nhân viên (STAFF) | `staff@ares.vn` | `staff123` |
| Khách hàng (có điểm và đơn mẫu) | `khach@ares.vn` | `123456` |
| Khách hàng 2 | `tran.binh@ares.vn` | `123456` |

Voucher mẫu:
- `CHAOBAN`: giảm 10%, tối đa 100k.
- `GIAM50K`: giảm 50k cho đơn từ 500k.
- `FREESHIP`: miễn phí vận chuyển.
- `RCFAN`: ưu đãi cho xe RC.

## Tính năng

**Khách hàng**
- **Tài khoản:** đăng ký, đăng nhập (email; Google/Facebook nếu cấu hình), hồ sơ, sổ địa chỉ.
- **Tìm kiếm và duyệt sản phẩm:**
  - Tìm kiếm có gợi ý.
  - Lọc theo danh mục, hãng, giá, tỷ lệ, độ tuổi, đánh giá và còn hàng. Có sắp xếp và phân trang.
  - Chi tiết sản phẩm: gallery ảnh, biến thể có giá và tồn kho riêng.
- **Tương tác:**
  - Đánh giá kèm ảnh (đánh dấu "đã mua") và hỏi đáp.
  - Yêu thích, so sánh, đã xem gần đây.
- **Giỏ hàng và ưu đãi:** lưu để mua sau, voucher, flash sale, miễn phí ship từ 500k, phí ship theo tỉnh/thành.
- **Thanh toán:** COD, chuyển khoản VietQR, MoMo, VNPay, trả góp 3/6/12 tháng (đơn từ 3 triệu).
- **Sau mua hàng:**
  - Timeline trạng thái đơn.
  - Huỷ đơn, yêu cầu đổi trả/hoàn tiền trong 7 ngày (kèm ảnh).
  - Xem và in hoá đơn.
  - Điểm thưởng và hạng thành viên, thông báo.
- **Hỗ trợ:** live chat, phiếu hỗ trợ, FAQ, hotline, tra cứu bảo hành, đấu giá hàng hiếm.

**Quản trị** (`/admin`)
- **Dashboard:** doanh thu, top sản phẩm, cảnh báo tồn kho thấp, việc cần xử lý.
- **Hàng hoá:**
  - Sản phẩm có biến thể và upload ảnh.
  - Danh mục và thương hiệu.
  - Kho: nhập/xuất, lịch sử xuất nhập.
- **Đơn hàng:**
  - Quy trình PENDING→CONFIRMED→SHIPPING→COMPLETED, huỷ đơn.
  - Đánh dấu đã thu tiền, in hoá đơn, xử lý đổi trả/hoàn tiền.
- **Khách hàng:** khoá tài khoản, cộng/trừ điểm, phân quyền USER/STAFF/ADMIN.
- **Marketing:** voucher, flash sale, banner, đấu giá.
- **Chăm sóc khách hàng:** duyệt đánh giá, hỏi đáp, phiếu hỗ trợ, live chat.
- **Báo cáo CSV:** đơn hàng, sản phẩm, khách hàng, kho. Chỉ ADMIN được xuất; STAFF dùng được các mục còn lại.

**Phần giả lập (mock)**
- MoMo, VNPay và trả góp đi qua trang cổng thanh toán giả `/pay/:code`.
- Mã VietQR là thật về định dạng (img.vietqr.io), nhưng việc xác nhận chuyển khoản là giả lập.
- Đăng nhập Google/Facebook cần cấu hình key thật qua biến môi trường.
- "Lưu để mua sau" chỉ lưu trên trình duyệt (localStorage).
- Không gửi email hay SMS thật.

### Dùng MySQL

```bash
cd backend
DB_URL="jdbc:mysql://localhost:3306/modelshop?createDatabaseIfNotExist=true" DB_USER=root DB_PASS=secret \
  mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `JWT_SECRET` | chuỗi dev | Khoá ký JWT (≥ 32 ký tự) — **đổi khi triển khai** |
| `FRONTEND_URL` | `http://localhost:5173` | CORS + nơi chuyển hướng sau OAuth |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | trống | Đăng nhập Google |
| `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | trống | Đăng nhập Facebook |
| `OAUTH_REDIRECT_BASE` | `http://localhost:8080` | Gốc URL callback OAuth |
| `VITE_API_URL` (frontend) | trống | Đặt khi API ở domain khác (VD `https://api.shop.vn`) |

### Cấu hình đăng nhập Google / Facebook

Nút Google/Facebook đã nối sẵn. Khi chưa cấu hình client id, API trả 501 và giao diện báo "chưa được cấu hình".
1. **Google**: Google Cloud Console → APIs & Services → Credentials → OAuth client ID (Web). Authorized redirect URI:
   `http://localhost:8080/api/auth/oauth/google/callback`
2. **Facebook**: developers.facebook.com → tạo app → Facebook Login → Valid OAuth Redirect URIs:
   `http://localhost:8080/api/auth/oauth/facebook/callback`
3. Đặt `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `FACEBOOK_CLIENT_ID`, `FACEBOOK_CLIENT_SECRET` rồi khởi động lại backend.

Luồng: frontend gọi `GET /api/auth/oauth/{provider}/url` → chuyển tới Google/Facebook → callback ở backend đổi `code` lấy email, tạo/tìm tài khoản, phát JWT → chuyển về `FRONTEND_URL/oauth-callback?token=...`.

## Chức năng

**Khách hàng**
1. Logo shop, header (menu, tìm kiếm nhanh, giỏ, hotline) & footer. Trang chủ: banner, ưu đãi, thương hiệu, xe mới/bán chạy, phiên đấu giá đang diễn ra.
2. Tài khoản: đăng ký/đăng nhập email + Google/Facebook, quên mật khẩu; hồ sơ; **sổ địa chỉ nhà**; lịch sử giao dịch; lịch sử đặt lịch; lịch sử đấu giá.
3. Danh mục xe: tìm kiếm, lọc theo hãng / dòng xe (Sedan, SUV, bán tải, thể thao, siêu xe, xe điện) / nhiên liệu / giá; hiển thị giá & **số lượng tồn kho**; chi tiết (ảnh, thông số); **cấu hình xe**: màu sơn, mâm, nội thất – cộng giá trực tiếp.
4. Giỏ hàng (xe & phụ kiện), nhập thông tin người mua & địa chỉ nhận xe, mã ưu đãi. Thanh toán: **toàn bộ**, **đặt cọc 10%**, **trả góp** (trả trước 20%, 12–84 tháng, lãi phẳng 0,5%/tháng, đơn từ 300 triệu), hoặc khi nhận xe.
5. Dịch vụ: **tư vấn** (live chat + hotline), **đăng ký lái thử**, **sửa chữa & bảo dưỡng** (đặt lịch mang xe tới xưởng).
6. **Đấu giá xe hiếm**: danh sách phiên (đang / sắp / đã kết thúc), giá khởi điểm, bước giá, đồng hồ đếm ngược, lịch sử đặt giá cập nhật 3 giây/lần, đặt giá khi đăng nhập, chống "bắn tỉa" (đặt giá trong 2 phút cuối gia hạn thêm 2 phút). Hết giờ, hệ thống tự chốt (mỗi 20 giây) và tạo **đơn đặt cọc 10%** cho người thắng.

**Admin** (`/admin`)
- Tổng quan; sản phẩm & thương hiệu; đơn hàng (trạng thái); khách hàng; ưu đãi; lịch hẹn (lái thử / bảo dưỡng); live chat.
- **Đấu giá**: tạo/sửa phiên (giá khởi điểm, bước giá, thời gian, ảnh, thông số), xem lượt đặt (tên thật), chốt ngay, huỷ phiên.


### Tính năng thương mại điện tử (bổ sung)
- **Yêu thích** (trái tim trên ảnh xe, trang `/wishlist`), **so sánh tối đa 3 xe** (thanh so sánh nổi, trang `/compare`, làm nổi bật thông số khác nhau), **đã xem gần đây**.
- **Đánh giá & chấm sao**: điểm trung bình, phân bố sao, khách đăng nhập viết đánh giá (1 lần/sản phẩm); admin **kiểm duyệt** (ẩn/duyệt/xoá) tại `/admin/reviews`.
- **Gợi ý tìm kiếm** khi gõ (tô sáng từ khoá, phím ↑/↓), sắp xếp + phân trang (Trước/Sau).
- **Flash sale** có đồng hồ đếm ngược trên trang chủ; đếm ngược hạn từng mã ưu đãi.
- **Theo dõi đơn hàng** dạng timeline có mốc thời gian từng trạng thái.
- **Chuông thông báo**: đặt hàng, đổi trạng thái đơn, bị trả giá cao hơn, thắng đấu giá.
- **Công cụ tính trả góp** (trả trước, kỳ hạn, lãi suất) và **nút chia sẻ** (Facebook, Zalo, X, sao chép link) trên trang chi tiết; "Có thể bạn quan tâm".
- Đăng ký **nhận tin** (footer), trang **FAQ** `/faq`, breadcrumb, thông báo dạng toast.
- Admin: **biểu đồ doanh thu** 7/14/30 ngày trên Tổng quan.

API mới: `GET/POST /api/products/{id}/reviews`, `GET /api/products/ratings`, `GET /api/account/notifications`, `POST /api/account/notifications/read`, `POST /api/newsletter`, `GET /api/admin/reviews`, `PATCH/DELETE /api/admin/reviews/{id}`, `GET /api/admin/revenue?days=14`.

## Phần giả lập (mock)
- **Thanh toán**: không nối cổng thật. Form thẻ chỉ kiểm tra độ dài; backend ghi nhận số tiền đã trả và sinh mã giao dịch `MOCK-xxxx`. Có thể thay bằng VNPay/MoMo trong `OrderService`.
- **Live chat**: polling HTTP (không WebSocket), có tin chào tự động.
- **Ảnh xe**: ảnh thật từ Wikimedia Commons (giấy phép CC, xem `frontend/public/images/cars/CREDITS.md`). Bản xem trước cấu hình vẽ bằng SVG theo màu đã chọn.
- **Giá xe** là giá tham khảo để demo.
- Google/Facebook: cần client id/secret thật (xem trên).

## API chính

| Method | Endpoint | Quyền |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| GET | `/api/auth/me` | user |
| GET | `/api/products?q&brandId&type&scale&minPrice&maxPrice&inStock&sort&page&size` | public |
| GET | `/api/products/home`, `/api/products/{id}`, `/api/brands`, `/api/options`, `/api/promotions` | public |
| POST | `/api/promotions/check` | public |
| POST | `/api/orders`, `/api/orders/{code}/pay`, `/api/orders/{code}/cancel` | user |
| GET/PUT | `/api/account/profile`, `/api/account/addresses`, `/api/account/orders`, `/api/account/bookings` | user |
| POST | `/api/bookings` (type `TEST_DRIVE` / `MAINTENANCE`) | public (gắn user nếu đăng nhập) |
| GET | `/api/auctions`, `/api/auctions/{id}` | public |
| POST | `/api/auctions/{id}/bids` | user |
| GET | `/api/account/bids` | user |
| GET/POST | `/api/chat/{session}` | public |
| * | `/api/admin/**` (stats, products, brands, orders, customers, promotions, bookings, chat) | admin |
