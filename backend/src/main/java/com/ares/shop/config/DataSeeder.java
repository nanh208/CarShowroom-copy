package com.ares.shop.config;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/** Dữ liệu mẫu: tài khoản admin/khách, thương hiệu, mô hình, phụ kiện, mã ưu đãi. Chỉ chạy khi DB trống. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepo users; private final BrandRepo brands; private final ProductRepo products;
    private final PromotionRepo promos; private final PasswordEncoder enc;

    public DataSeeder(UserRepo users, BrandRepo brands, ProductRepo products, PromotionRepo promos, PasswordEncoder enc) {
        this.users = users; this.brands = brands; this.products = products; this.promos = promos; this.enc = enc;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            user("admin@ares.vn", "admin123", "Quản trị viên", Enums.Role.ADMIN);
            user("khach@ares.vn", "123456", "Nguyễn Văn Khách", Enums.Role.USER);
        }
        if (brands.count() > 0) return;
        Map<String, Brand> b = new LinkedHashMap<>();
        for (String[] r : new String[][]{
                {"Ferrari", "Ý"}, {"Lamborghini", "Ý"}, {"Porsche", "Đức"}, {"Mercedes-Benz", "Đức"}, {"BMW", "Đức"},
                {"Toyota", "Nhật Bản"}, {"Nissan", "Nhật Bản"}, {"Pagani", "Ý"}, {"Bugatti", "Pháp"}, {"Ford", "Mỹ"},
                {"VinFast", "Việt Nam"}, {"ARES Parts", "Việt Nam"}}) {
            Brand x = new Brand(); x.name = r[0]; x.country = r[1];
            x.description = "Mô hình xe " + r[0] + " chính hãng, tỉ lệ chuẩn, chi tiết sắc nét.";
            b.put(r[0], brands.save(x));
        }
        P(b, "Ferrari SF90 Stradale", "Ferrari", "SUPERCAR", "1:18", 3_290_000L, 3_590_000L, 12, 87, "Bburago Signature", "#c1121f", true, true);
        P(b, "Ferrari 488 Pista", "Ferrari", "SUPERCAR", "1:24", 890_000L, null, 25, 140, "Bburago", "#facc15", false, false);
        P(b, "Lamborghini Aventador SVJ", "Lamborghini", "SUPERCAR", "1:18", 3_850_000L, null, 6, 64, "AUTOart", "#65a30d", true, true);
        P(b, "Lamborghini Urus", "Lamborghini", "SUV", "1:24", 1_150_000L, 1_290_000L, 18, 51, "Maisto", "#f97316", false, false);
        P(b, "Porsche 911 GT3 RS (992)", "Porsche", "RACING", "1:18", 4_200_000L, null, 4, 39, "Minichamps", "#f8fafc", true, true);
        P(b, "Porsche 356 Speedster 1955", "Porsche", "CLASSIC", "1:43", 650_000L, null, 30, 22, "Schuco", "#94a3b8", false, false);
        P(b, "Mercedes-AMG G63", "Mercedes-Benz", "SUV", "1:18", 2_750_000L, null, 9, 77, "Norev", "#111827", true, false);
        P(b, "Mercedes-Benz 300SL Gullwing", "Mercedes-Benz", "CLASSIC", "1:18", 2_490_000L, 2_790_000L, 5, 33, "Norev", "#e5e7eb", false, true);
        P(b, "BMW M4 GT3", "BMW", "RACING", "1:24", 980_000L, null, 20, 58, "Welly", "#1d4ed8", true, false);
        P(b, "Toyota Supra MK4 (A80)", "Toyota", "SEDAN", "1:24", 750_000L, null, 35, 210, "Jada Toys", "#ea580c", false, true);
        P(b, "Nissan Skyline GT-R R34", "Nissan", "SEDAN", "1:18", 2_190_000L, null, 0, 160, "AUTOart", "#1e3a8a", false, true);
        P(b, "Pagani Huayra Roadster", "Pagani", "SUPERCAR", "1:18", 5_990_000L, null, 3, 18, "LCD Models", "#3f3f46", true, true);
        P(b, "Bugatti Chiron Sport", "Bugatti", "SUPERCAR", "1:18", 4_590_000L, 4_990_000L, 7, 45, "Bburago Plus", "#0c4a6e", true, false);
        P(b, "Ford F-150 Raptor", "Ford", "TRUCK", "1:24", 820_000L, null, 15, 49, "Maisto", "#475569", false, false);
        P(b, "Ford Mustang Shelby GT500 1967", "Ford", "CLASSIC", "1:18", 1_890_000L, null, 10, 96, "Greenlight", "#b91c1c", false, false);
        P(b, "VinFast VF 8", "VinFast", "SUV", "1:18", 1_590_000L, null, 22, 120, "VinFast Official", "#0369a1", true, true);
        P(b, "VinFast Lux A2.0", "VinFast", "SEDAN", "1:43", 590_000L, null, 40, 75, "VinFast Official", "#7f1d1d", false, false);
        P(b, "Toyota GR86 Hot Wheels Premium", "Toyota", "RACING", "1:64", 180_000L, null, 80, 330, "Hot Wheels", "#dc2626", true, false);
        A(b, "Tủ kính trưng bày mica 1:18 có đèn LED", 450_000L, 50, 140);
        A(b, "Đế xoay trưng bày 360° (chạy pin)", 290_000L, 60, 88);
        A(b, "Bộ dụng cụ vệ sinh mô hình (cọ, khăn, bình xịt)", 190_000L, 100, 205);
        A(b, "Diorama gara mini 1:64", 350_000L, 25, 41);

        promo("CHAOMUNG", "Chào thành viên mới", "Giảm 10% cho đơn hàng đầu tiên, tối đa 300.000đ", 10, 300_000, 0, "#0284c7");
        promo("SUPERCAR15", "Tuần lễ Siêu xe", "Giảm 15% cho đơn từ 3.000.000đ, tối đa 1.000.000đ", 15, 1_000_000, 3_000_000, "#c1121f");
        promo("FREESHIP", "Ưu đãi cuối tuần", "Giảm 5% mọi đơn hàng", 5, 0, 0, "#16a34a");
    }

    private void user(String email, String pw, String name, Enums.Role role) {
        User u = new User(); u.email = email; u.password = enc.encode(pw); u.fullName = name; u.role = role; u.phone = "0900000000";
        users.save(u);
    }

    private void P(Map<String, Brand> b, String name, String brand, String type, String scale, long price, Long old, int stock, int sold,
                   String maker, String color, boolean isNew, boolean featured) {
        Product p = new Product();
        p.name = name; p.brand = b.get(brand); p.type = Enums.ProductType.valueOf(type); p.scale = scale; p.price = price;
        p.oldPrice = old; p.stock = stock; p.sold = sold; p.material = "Hợp kim đúc (Diecast) + nhựa ABS"; p.baseColor = color;
        p.isNew = isNew; p.featured = featured;
        p.description = "Mô hình " + name + " tỉ lệ " + scale + " do " + maker + " sản xuất. Thân vỏ hợp kim đúc, sơn tĩnh điện nhiều lớp, "
                + "cửa và nắp capo mở được, nội thất chi tiết, bánh xe xoay và đánh lái được. Phù hợp sưu tầm, trưng bày hoặc làm quà tặng.";
        p.specs.put("Tỉ lệ", scale);
        p.specs.put("Hãng sản xuất mô hình", maker);
        p.specs.put("Chất liệu", "Hợp kim kẽm + nhựa ABS");
        p.specs.put("Kích thước (D x R x C)", switch (scale) {
            case "1:18" -> "~25 x 11 x 7 cm"; case "1:24" -> "~19 x 8 x 5 cm"; case "1:43" -> "~10 x 4.5 x 3 cm"; default -> "~7 x 3 x 2 cm"; });
        p.specs.put("Chi tiết mở được", scale.equals("1:64") ? "Không" : "Cửa, capo, cốp");
        p.specs.put("Phụ kiện kèm theo", "Hộp trưng bày, giấy chứng nhận");
        p.specs.put("Bảo hành", "12 tháng lỗi sơn & khớp nối");
        products.save(p);
    }

    private void A(Map<String, Brand> b, String name, long price, int stock, int sold) {
        Product p = new Product();
        p.name = name; p.brand = b.get("ARES Parts"); p.type = Enums.ProductType.ACCESSORY; p.scale = "Phụ kiện"; p.price = price;
        p.stock = stock; p.sold = sold; p.customizable = false; p.material = "Mica / nhựa"; p.baseColor = "#64748b";
        p.description = name + " — phụ kiện chính hãng ARES giúp bảo quản và trưng bày bộ sưu tập của bạn.";
        p.specs.put("Loại", "Phụ kiện trưng bày"); p.specs.put("Bảo hành", "6 tháng");
        products.save(p);
    }

    private void promo(String code, String title, String desc, int pct, long max, long min, String color) {
        Promotion p = new Promotion(); p.code = code; p.title = title; p.description = desc; p.discountPercent = pct;
        p.maxDiscount = max; p.minOrder = min; p.startDate = LocalDate.now().minusDays(1); p.endDate = LocalDate.now().plusMonths(3);
        p.bannerColor = color; promos.save(p);
    }
}
