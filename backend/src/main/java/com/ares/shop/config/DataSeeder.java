package com.ares.shop.config;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

/** Dữ liệu mẫu showroom ô tô đa thương hiệu: tài khoản, hãng xe, xe, phụ kiện, ưu đãi, phiên đấu giá. Chỉ chạy khi DB trống. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final long TR = 1_000_000L; // 1 triệu
    private final UserRepo users; private final BrandRepo brands; private final ProductRepo products;
    private final ReviewRepo reviews;
    private final PromotionRepo promos; private final AuctionRepo auctions; private final PasswordEncoder enc;

    public DataSeeder(UserRepo users, BrandRepo brands, ProductRepo products, PromotionRepo promos, AuctionRepo auctions, PasswordEncoder enc, ReviewRepo reviews) {
        this.reviews = reviews;
        this.users = users; this.brands = brands; this.products = products; this.promos = promos; this.auctions = auctions; this.enc = enc;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            user("admin@ares.vn", "admin123", "Quản trị viên", Enums.Role.ADMIN);
            user("khach@ares.vn", "123456", "Nguyễn Văn Khách", Enums.Role.USER);
            user("tran.binh@ares.vn", "123456", "Trần Thị Bình", Enums.Role.USER);
        }
        if (brands.count() == 0) seedCars();
        if (auctions.count() == 0) seedAuctions();
        if (reviews.count() == 0) seedReviews();
    }

    private void seedReviews() {
        String[][] r = {
            {"5", "Xe vận hành êm, tiết kiệm nhiên liệu. Nhân viên ARES tư vấn rất kỹ, giao xe đúng hẹn."},
            {"4", "Thiết kế đẹp, nội thất rộng rãi. Thủ tục trả góp nhanh, chỉ mất 2 ngày."},
            {"3", "Xe đẹp, vận hành ổn nhưng phải chờ giao xe hơi lâu so với dự kiến."},
            {"4", "Giá tốt so với thị trường, được tặng kèm phim cách nhiệt. Sẽ giới thiệu bạn bè."}};
        var khach = users.findByEmailIgnoreCase("khach@ares.vn").orElse(null);
        var binh = users.findByEmailIgnoreCase("tran.binh@ares.vn").orElse(null);
        int i = 0;
        for (Product p : products.findAll()) {
            for (User u : new User[]{khach, binh}) {
                if (u == null || (p.id + i) % 3 == 0) { i++; continue; }
                String[] x = r[(int) ((p.id + i++) % r.length)];
                Review v = new Review(); v.productId = p.id; v.productName = p.name; v.user = u; v.authorName = u.fullName;
                v.rating = Integer.parseInt(x[0]); v.comment = x[1]; v.createdAt = java.time.LocalDateTime.now().minusDays(i % 40 + 1);
                reviews.save(v);
            }
        }
    }

    private void seedCars() {
        Map<String, Brand> b = new LinkedHashMap<>();
        for (String[] r : new String[][]{
                {"Toyota", "Nhật Bản"}, {"Honda", "Nhật Bản"}, {"Mazda", "Nhật Bản"}, {"Hyundai", "Hàn Quốc"}, {"Kia", "Hàn Quốc"},
                {"Ford", "Mỹ"}, {"VinFast", "Việt Nam"}, {"Mercedes-Benz", "Đức"}, {"BMW", "Đức"}, {"Porsche", "Đức"},
                {"Lamborghini", "Ý"}, {"Ferrari", "Ý"}, {"Rolls-Royce", "Anh"}, {"ARES Accessories", "Việt Nam"}}) {
            Brand x = new Brand(); x.name = r[0]; x.country = r[1];
            String slug = r[0].toLowerCase().replace("-benz", "").replace("-", "").replace(" ", "");
            x.logoUrl = slug.equals("aresaccessories") ? "/images/logo.png" : slug.equals("vinfast") ? "/images/brands/vinfast.png" : "/images/brands/" + slug + ".svg";
            x.description = "Đại lý ủy quyền " + r[0] + " tại ARES Auto: xe mới chính hãng, bảo hành toàn quốc.";
            b.put(r[0], brands.save(x));
        }
        //   tên, hãng, loại, nhiên liệu, năm, giá (triệu), giá cũ, tồn, đã bán, động cơ, công suất, chỗ, hộp số, màu, ảnh, mới, nổi bật
        car(b, "Toyota Camry 2.5HEV", "Toyota", "SEDAN", "Hybrid", 2025, 1_530, null, 8, 64, "2.5L Hybrid", "227 hp", 5, "e-CVT", "#e5e7eb", "camry", false, true);
        car(b, "Toyota Fortuner Legender 2.8 4x4", "Toyota", "SUV", "Dầu", 2025, 1_350, 1_395L, 6, 58, "2.8L Diesel", "201 hp", 7, "AT 6 cấp", "#f8fafc", "fortuner", false, false);
        car(b, "Honda CR-V e:HEV RS", "Honda", "SUV", "Hybrid", 2025, 1_259, null, 10, 71, "2.0L Hybrid", "204 hp", 7, "e-CVT", "#111827", "crv", true, true);
        car(b, "Mazda CX-60 Premium", "Mazda", "SUV", "Xăng", 2025, 1_249, 1_299L, 5, 22, "2.5L Skyactiv-G", "188 hp", 5, "AT 8 cấp", "#7f1d1d", "cx60", true, false);
        car(b, "Hyundai Santa Fe 2.5 Turbo", "Hyundai", "SUV", "Xăng", 2025, 1_365, null, 7, 40, "2.5L Turbo", "277 hp", 7, "DCT 8 cấp", "#1e3a8a", "santafe", false, false);
        car(b, "Kia Carnival 2.2D Signature", "Kia", "SUV", "Dầu", 2025, 1_589, null, 4, 37, "2.2L Diesel", "199 hp", 7, "AT 8 cấp", "#3f3f46", "carnival", false, false);
        car(b, "Ford Ranger Raptor 2.0 Bi-Turbo", "Ford", "PICKUP", "Dầu", 2025, 1_299, null, 9, 88, "2.0L Bi-Turbo Diesel", "210 hp", 5, "AT 10 cấp", "#475569", "ranger", false, true);
        car(b, "Ford Mustang GT 5.0 V8", "Ford", "SPORT", "Xăng", 2024, 3_499, null, 2, 6, "5.0L V8", "480 hp", 4, "AT 10 cấp", "#b91c1c", "mustang", false, false);
        car(b, "VinFast VF 8 Plus", "VinFast", "ELECTRIC", "Điện", 2025, 1_199, 1_359L, 15, 120, "2 mô-tơ điện AWD", "402 hp", 5, "1 cấp", "#0369a1", "vf8", true, true);
        car(b, "VinFast VF 9 Plus", "VinFast", "ELECTRIC", "Điện", 2025, 1_699, null, 6, 35, "2 mô-tơ điện AWD", "402 hp", 7, "1 cấp", "#0f172a", "vf9", true, false);
        car(b, "Mercedes-Benz C 300 AMG", "Mercedes-Benz", "SEDAN", "Xăng", 2025, 2_099, null, 5, 44, "2.0L Turbo + EQ Boost", "258 hp", 5, "9G-Tronic", "#e5e7eb", "c300", false, true);
        car(b, "Mercedes-AMG G 63", "Mercedes-Benz", "SUV", "Xăng", 2025, 11_750, null, 1, 5, "4.0L V8 Biturbo", "585 hp", 5, "AMG 9 cấp", "#111827", "g63", false, true);
        car(b, "BMW X5 xDrive40i M Sport", "BMW", "SUV", "Xăng", 2025, 4_299, 4_499L, 3, 18, "3.0L I6 Turbo", "381 hp", 7, "Steptronic 8 cấp", "#1d4ed8", "x5", false, false);
        car(b, "BMW M4 Competition", "BMW", "SPORT", "Xăng", 2025, 6_199, null, 2, 4, "3.0L I6 Twin-Turbo", "530 hp", 4, "M Steptronic 8 cấp", "#65a30d", "m4", true, false);
        car(b, "Porsche 911 GT3 RS", "Porsche", "SPORT", "Xăng", 2025, 20_390, null, 1, 2, "4.0L Boxer 6", "525 hp", 2, "PDK 7 cấp", "#f8fafc", "gt3rs", true, true);
        car(b, "Porsche Taycan GTS Sport Turismo", "Porsche", "ELECTRIC", "Điện", 2025, 6_950, null, 2, 3, "2 mô-tơ điện", "598 hp", 4, "2 cấp", "#94a3b8", "taycan", false, false);
        car(b, "Lamborghini Huracán Tecnica", "Lamborghini", "SUPERCAR", "Xăng", 2024, 19_000, null, 1, 3, "5.2L V10", "640 hp", 2, "LDF 7 cấp", "#0ea5e9", "huracan", false, true);
        car(b, "Lamborghini Urus Performante", "Lamborghini", "SUV", "Xăng", 2025, 16_500, null, 2, 4, "4.0L V8 Twin-Turbo", "666 hp", 5, "AT 8 cấp", "#f97316", "urus", true, false);
        car(b, "Ferrari SF90 Spider", "Ferrari", "SUPERCAR", "Hybrid", 2024, 39_500, null, 1, 1, "4.0L V8 + 3 mô-tơ điện", "1.000 hp", 2, "F1 DCT 8 cấp", "#c1121f", "sf90", true, true);
        car(b, "Rolls-Royce Cullinan Series II", "Rolls-Royce", "SUV", "Xăng", 2025, 41_000, null, 0, 1, "6.75L V12 Twin-Turbo", "571 hp", 5, "AT 8 cấp", "#1e293b", "cullinan", true, false);
        acc(b, "Phim cách nhiệt 3M Crystalline (trọn xe)", 18 * TR, 40, 210, "tint");
        acc(b, "Camera 360° + cảm biến va chạm", 15 * TR, 30, 160, "camera");
        acc(b, "Thảm sàn da 6D cao cấp", 4_500_000L, 60, 320, "mats");
        acc(b, "Gói phủ ceramic 9H bảo vệ sơn", 12 * TR, 50, 140, "ceramic");

        promo("CHAOMUNG", "Chào khách hàng mới", "Giảm 1% giá xe cho đơn đầu tiên, tối đa 20 triệu", 1, 20 * TR, 0, "#0284c7");
        promo("THANG10", "Tháng vàng ARES", "Giảm 2% cho xe từ 1 tỷ, tối đa 50 triệu", 2, 50 * TR, 1_000 * TR, "#c1121f");
        promo("PHUKIEN10", "Ưu đãi phụ kiện", "Giảm 10% đơn phụ kiện, tối đa 5 triệu", 10, 5 * TR, 0, "#16a34a");
    }

    private void seedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auction("Ferrari F40 (1990)", "Ferrari", 1990, "18.900 km", "f40", 45_000 * TR, 500 * TR, now.minusHours(2), now.plusDays(2),
                "Một trong 1.315 chiếc F40 từng được sản xuất. Xe nhập từ Nhật Bản, nguyên bản 100%, đầy đủ sổ bảo dưỡng, chứng nhận Ferrari Classiche.",
                Map.of("Động cơ", "2.9L V8 Twin-Turbo", "Công suất", "478 hp", "Hộp số", "Sàn 5 cấp", "Màu", "Rosso Corsa", "Tình trạng", "Nguyên bản, Classiche"));
        auction("Porsche Carrera GT (2005)", "Porsche", 2005, "9.200 km", "carreragt", 38_000 * TR, 300 * TR, now.minusHours(1), now.plusHours(30),
                "Siêu xe V10 huyền thoại, chỉ 1.270 chiếc. Một chủ từ mới, đã thay ly hợp và lốp tại Porsche Center.",
                Map.of("Động cơ", "5.7L V10", "Công suất", "603 hp", "Hộp số", "Sàn 6 cấp", "Màu", "GT Silver", "Tình trạng", "Một chủ"));
        auction("Lamborghini Miura P400 S (1969)", "Lamborghini", 1969, "41.000 km", "miura", 52_000 * TR, 500 * TR, now.plusDays(1), now.plusDays(4),
                "Biểu tượng của dòng siêu xe động cơ đặt giữa. Đã phục chế toàn bộ bởi Polo Storico Lamborghini.",
                Map.of("Động cơ", "3.9L V12", "Công suất", "370 hp", "Hộp số", "Sàn 5 cấp", "Màu", "Verde Metallizzato", "Tình trạng", "Phục chế chính hãng"));
        auction("Toyota 2000GT (1967)", "Toyota", 1967, "63.500 km", "2000gt", 25_000 * TR, 200 * TR, now.minusMinutes(30), now.plusHours(6),
                "Chiếc xe thể thao Nhật Bản hiếm nhất mọi thời đại, chỉ 351 chiếc. Giấy tờ đầy đủ, xe sẵn sàng trưng bày.",
                Map.of("Động cơ", "2.0L I6", "Công suất", "150 hp", "Hộp số", "Sàn 5 cấp", "Màu", "Trắng Pegasus", "Tình trạng", "Đã phục hồi"));
        auction("Mercedes-Benz 300 SL Gullwing (1955)", "Mercedes-Benz", 1955, "72.000 km", "300sl", 30_000 * TR, 250 * TR, now.minusDays(3), now.minusHours(1),
                "Cửa cánh chim kinh điển, phục chế bởi Mercedes-Benz Classic Center. (Phiên đã kết thúc – ví dụ)",
                Map.of("Động cơ", "3.0L I6", "Công suất", "215 hp", "Hộp số", "Sàn 4 cấp", "Màu", "Silver", "Tình trạng", "Phục chế"));
    }

    private void user(String email, String pw, String name, Enums.Role role) {
        User u = new User(); u.email = email; u.password = enc.encode(pw); u.fullName = name; u.role = role; u.phone = "0900000000";
        users.save(u);
    }

    private void car(Map<String, Brand> b, String name, String brand, String type, String fuel, int year, long priceTr, Long oldTr, int stock, int sold,
                     String engine, String power, int seats, String gearbox, String color, String img, boolean isNew, boolean featured) {
        Product p = new Product();
        p.name = name; p.brand = b.get(brand); p.type = Enums.ProductType.valueOf(type); p.fuel = fuel; p.year = year;
        p.price = priceTr * TR; p.oldPrice = oldTr == null ? null : oldTr * TR; p.stock = stock; p.sold = sold;
        p.material = gearbox; p.baseColor = color; p.isNew = isNew; p.featured = featured;
        p.description = name + " đời " + year + " phân phối chính hãng tại ARES Auto. Xe mới 100%, bảo hành chính hãng 3–5 năm, "
                + "hỗ trợ đăng ký – đăng kiểm, giao xe tận nhà. Hỗ trợ trả góp đến 80% giá trị xe, thủ tục nhanh trong 24h.";
        p.specs.put("Động cơ", engine);
        p.specs.put("Công suất", power);
        p.specs.put("Hộp số", gearbox);
        p.specs.put("Nhiên liệu", fuel);
        p.specs.put("Số chỗ ngồi", seats + " chỗ");
        p.specs.put("Năm sản xuất", String.valueOf(year));
        p.specs.put("Bảo hành", type.equals("ELECTRIC") ? "10 năm pin / 5 năm xe" : "3 năm hoặc 100.000 km");
        p.images.add("/images/cars/" + img + ".jpg");
        if (img.equals("sf90")) p.images.add("/images/cars/sf90b.jpg");
        if (img.equals("huracan")) p.images.add("/images/cars/huracan2.jpg");
        products.save(p);
    }

    private void acc(Map<String, Brand> b, String name, long price, int stock, int sold, String img) {
        Product p = new Product();
        p.name = name; p.brand = b.get("ARES Accessories"); p.type = Enums.ProductType.ACCESSORY; p.fuel = "Phụ kiện"; p.price = price;
        p.stock = stock; p.sold = sold; p.customizable = false; p.material = "Lắp đặt tại xưởng"; p.baseColor = "#64748b";
        p.description = name + " — phụ kiện chính hãng, lắp đặt miễn phí tại xưởng dịch vụ ARES, bảo hành 12 tháng.";
        p.specs.put("Loại", "Phụ kiện ô tô"); p.specs.put("Lắp đặt", "Miễn phí tại xưởng"); p.specs.put("Bảo hành", "12 tháng");
        p.images.add("/images/acc/" + img + ".jpg");
        products.save(p);
    }

    private void promo(String code, String title, String desc, int pct, long max, long min, String color) {
        Promotion p = new Promotion(); p.code = code; p.title = title; p.description = desc; p.discountPercent = pct;
        p.maxDiscount = max; p.minOrder = min; p.startDate = LocalDate.now().minusDays(1); p.endDate = LocalDate.now().plusMonths(3);
        p.bannerColor = color; promos.save(p);
    }

    private void auction(String title, String brand, int year, String km, String img, long start, long step, LocalDateTime from, LocalDateTime to,
                         String desc, Map<String, String> specs) {
        Auction a = new Auction();
        a.title = title; a.brandName = brand; a.year = year; a.mileage = km; a.images.add("/images/cars/" + img + ".jpg");
        a.startPrice = start; a.currentPrice = start; a.minIncrement = step; a.startTime = from; a.endTime = to;
        a.description = desc; a.specs.putAll(specs);
        auctions.save(a);
    }
}
