# ARES Models — Web bán mô hình ô tô

Website bán **mô hình ô tô sưu tầm** (tỉ lệ 1:18 – 1:64) cho khách hàng và trang quản trị cho admin.
Giao diện kế thừa phong cách trang đăng nhập gốc của repo (nền tối, thẻ kính mờ, màu xanh `#0284c7`, font Plus Jakarta Sans, ảnh nền Pagani, logo).

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

Dữ liệu mẫu (12 thương hiệu, 18 mô hình, 4 phụ kiện, 3 mã ưu đãi) được tạo tự động khi DB trống.
Database H2 lưu ở `backend/data/` (xoá thư mục này để reset). H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/modelshop`, user `sa`).

### Tài khoản mặc định

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | `admin@ares.vn` | `admin123` |
| Khách hàng | `khach@ares.vn` | `123456` |

Mã ưu đãi mẫu: `CHAOMUNG` (-10%, tối đa 300k), `SUPERCAR15` (-15% đơn từ 3 triệu), `FREESHIP` (-5%).

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
1. Header (logo, menu, tìm kiếm nhanh, giỏ hàng, hotline) & footer (liên hệ). Trang chủ: banner, thương hiệu, hàng mới, bán chạy, ưu đãi.
2. Tài khoản: đăng ký/đăng nhập email + Google/Facebook; hồ sơ & đổi mật khẩu; sổ địa chỉ (mặc định, thêm/sửa/xoá); lịch sử giao dịch; lịch sử đặt lịch.
3. Danh mục: tìm kiếm, lọc theo thương hiệu/loại xe/tỉ lệ/giá/còn hàng, sắp xếp, phân trang; trang chi tiết (ảnh, thông số, sản phẩm liên quan); **thiết kế mô hình**: chọn màu sơn, mâm, nội thất — xem trước trực tiếp và cộng giá.
4. Giỏ hàng (mô hình & phụ kiện, lưu localStorage), thanh toán: thông tin người mua, chọn địa chỉ từ sổ địa chỉ, mã ưu đãi, phí ship (miễn phí từ 2 triệu), **thanh toán toàn bộ / đặt cọc 30% / trả góp 0% 3–12 tháng / COD**. Trang chi tiết đơn: thanh toán phần còn lại hoặc kỳ trả góp tiếp theo, huỷ đơn.
5. Dịch vụ: live chat (widget góc phải, polling 4s) + hotline; **đăng ký xem & trải nghiệm mẫu tại showroom** (thay cho "lái thử" vì là mô hình); **đặt lịch sửa chữa & bảo dưỡng mô hình** (vệ sinh, sơn lại, thay linh kiện...).

**Admin** (`/admin`, tài khoản ADMIN)
- Tổng quan: doanh thu, đơn, khách, tồn kho thấp, lịch hẹn chờ.
- Sản phẩm: thêm/sửa/ẩn (xoá mềm), ảnh, thông số, tồn kho; quản lý thương hiệu.
- Đơn hàng: lọc theo trạng thái, xem chi tiết, cập nhật trạng thái (huỷ sẽ hoàn kho).
- Khách hàng: thống kê số đơn/chi tiêu, khoá/mở khoá, đổi vai trò.
- Ưu đãi: CRUD mã giảm giá (%, mức tối đa, đơn tối thiểu, thời hạn).
- Lịch hẹn: xem & đổi trạng thái lịch xem mẫu / sửa chữa.
- Live chat: trả lời khách theo từng phiên.

## Phần giả lập (mock)
- **Thanh toán**: không nối cổng thật. Form thẻ chỉ kiểm tra độ dài; backend ghi nhận số tiền đã trả và sinh mã giao dịch `MOCK-xxxx`. Có thể thay bằng VNPay/MoMo trong `OrderService`.
- **Live chat**: polling HTTP (không WebSocket), có tin chào tự động.
- **Ảnh sản phẩm**: dữ liệu mẫu không kèm ảnh; giao diện vẽ mô hình bằng SVG theo màu (cũng là bản xem trước khi thiết kế). Admin có thể thêm URL ảnh thật.
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
| POST | `/api/bookings` | public (gắn user nếu đăng nhập) |
| GET/POST | `/api/chat/{session}` | public |
| * | `/api/admin/**` (stats, products, brands, orders, customers, promotions, bookings, chat) | admin |
