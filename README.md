# ARES Auto — Showroom ô tô đa thương hiệu

Website **showroom ô tô đa thương hiệu** (xe thật, giá VND) kèm **đấu giá xe hiếm**, cho khách hàng và trang quản trị admin.
Giao diện giữ nguyên phong cách gốc của repo (auth.css) (nền tối, thẻ kính mờ, màu xanh `#0284c7`, font Plus Jakarta Sans, ảnh nền Pagani, logo).

| Thư mục | Công nghệ |
|---|---|
| `backend/` | Java 21, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, H2 (mặc định) / MySQL |
| `frontend/` | Vue 3, Vite, Vue Router, Pinia, axios |

## Chạy nhanh

Yêu cầu: JDK 21, Maven 3.9+, Node 18+.

```bash
# 1. Backend (http://localhost:8080)
cd backend
mvn spring-boot:run           # hoặc: mvn package && java -jar target/model-car-shop-1.0.0.jar

# 2. Frontend (http://localhost:5173) – Vite proxy /api -> 8080
cd frontend
npm install
npm run dev                   # build production: npm run build
```

Dữ liệu mẫu (14 thương hiệu, 20 mẫu xe, 4 phụ kiện, 3 mã ưu đãi, 5 phiên đấu giá) được tạo tự động khi DB trống.
Database H2 lưu ở `backend/data/` (xoá thư mục này để reset). H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/modelshop`, user `sa`).

### Tài khoản mặc định

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | `admin@ares.vn` | `admin123` |
| Khách hàng | `khach@ares.vn` | `123456` |
| Khách hàng 2 (thử đấu giá) | `tran.binh@ares.vn` | `123456` |

Mã ưu đãi mẫu: `CHAOMUNG` (-1%, tối đa 20 triệu), `THANG10` (-2% xe từ 1 tỷ, tối đa 50 triệu), `PHUKIEN10` (-10% phụ kiện).

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
