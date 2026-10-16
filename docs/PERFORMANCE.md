# Hiệu năng & kiểm thử giao diện (v16)

## Ảnh
- `npm run images` (tự chạy trước `npm run build`): tạo bản **WebP** (≤1600px, q78) cạnh mỗi JPG/PNG trong `public/images` → `src/webp-manifest.json` (hiện 95 ảnh, 11.1MB → 6.3MB).
- Plugin Vite `ares-img-optimize` sửa mọi `<img>` lúc build: `:src` → `$img(...)` (đổi sang `.webp` nếu có bản WebP), `src` tĩnh → `.webp`, tự thêm `loading="lazy"` (trừ ảnh có `fetchpriority`, ví dụ ảnh LCP hero/chi tiết) và `decoding="async"`.
- Ảnh tải lên (`POST /api/uploads`): Spring Boot kiểm tra chữ ký tệp, thu nhỏ cạnh dài ≤ `UPLOAD_MAX_DIMENSION` (mặc định 1920), nén lại JPEG q0.82 (bỏ EXIF); PNG trong suốt giữ PNG. WebP/GIF lưu nguyên (JDK không mã hoá được WebP).

## Code splitting
- Mọi route lazy-load; vendor tách riêng: `vendor-vue` (vue/router/pinia), `vendor-http` (axios), `vendor-stomp`, `vendor-qrcode`; `@google/model-viewer` (~1MB) chỉ tải khi mở tab 3D.

## Cache & nén
- Spring Boot: gzip (`server.compression.*`), **ETag yếu** + 304 cho GET công khai, `Cache-Control`: dữ liệu ít đổi `public, max-age=300, stale-while-revalidate=600`; catalog `no-cache` (luôn revalidate bằng ETag); có `Authorization` / admin / tài khoản → `private, no-store`; `/uploads/**` `max-age=1 năm, immutable`.
- Frontend: `npm run build` nén sẵn `.gz` + `.br`; `vite preview` và `deploy/nginx.conf` đặt `/assets/*` immutable 1 năm, HTML/sw.js `no-cache`.

## Playwright
```
cd frontend
npm run build
npm run test:e2e          # desktop 1440 + mobile Pixel 7; tự chạy vite preview nếu chưa có
npm run test:e2e:report
```
Cần backend ở `http://localhost:8080` (đổi bằng `API_URL`), tài khoản demo (`E2E_ADMIN_EMAIL`/`E2E_ADMIN_PASSWORD`, `E2E_USER_EMAIL`/`E2E_USER_PASSWORD`), hoặc trỏ `E2E_BASE_URL` vào môi trường khác.
Bộ test: trang chủ (SEO meta, JSON-LD, lỗi JS, tràn ngang), sản phẩm → giỏ, đổi ngôn ngữ, tin tức, tìm bằng ảnh, 404, đăng nhập, phân quyền admin, trang quản trị (dự báo, gian lận, bài viết, đơn), WebP/lazy, code splitting, cache header, ETag/gzip.
