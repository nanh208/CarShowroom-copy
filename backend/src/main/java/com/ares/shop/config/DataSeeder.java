package com.ares.shop.config;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import com.ares.shop.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

/** Dữ liệu mẫu cửa hàng xe đồ chơi ARES Toys. Chỉ chạy khi DB trống. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final long K = 1_000L;
    /** Số ảnh có sẵn cho mỗi sản phẩm trong /images/toys/{slug}-{i}.jpg */
    private static final Map<String, Integer> IMG = parse("{\"hw-r34\": 3, \"hw-911\": 2, \"hw-monster\": 2, \"minigt-huracan\": 2, \"minigt-supra\": 2, \"tomica-lc\": 1, \"tomica-nsx\": 3, \"maisto-mustang\": 2, \"maisto-aventador\": 2, \"bburago-sf90\": 3, \"bburago-chiron\": 2, \"maisto-911\": 3, \"bburago-f40\": 2, \"kyosho-countach\": 3, \"norev-ds\": 2, \"minichamps-f1\": 2, \"lego-911\": 1, \"lego-chiron\": 3, \"lego-mustang\": 2, \"tamiya-mini4wd\": 3, \"tamiya-kit-gtr\": 2, \"traxxas-slash\": 3, \"traxxas-xmaxx\": 1, \"traxxas-trx4\": 3, \"tamiya-grasshopper\": 1, \"tamiya-scorcher\": 1, \"tamiya-beetle\": 2, \"kyosho-miniz\": 3, \"kyosho-inferno\": 1, \"wltoys-144001\": 2, \"wltoys-crawler\": 1, \"rc-drift\": 3, \"lipo-2s\": 2, \"lipo-3s\": 2, \"nimh\": 2, \"charger\": 3, \"flysky\": 2, \"futaba\": 3, \"wheels-sc\": 2, \"wheels-drift\": 2, \"brushless\": 2, \"display-case\": 2}");

    private final UserRepo users; private final BrandRepo brands; private final ProductRepo products; private final ReviewRepo reviews;
    private final PromotionRepo promos; private final AuctionRepo auctions; private final PasswordEncoder enc; private final CategoryRepo categories;
    private final VariantRepo variants; private final FlashSaleRepo flash; private final BannerRepo banners; private final QuestionRepo questions;
    private final OrderService orderService; private final OrderRepo orders; private final TicketRepo tickets; private final AddressRepo addresses;
    @org.springframework.beans.factory.annotation.Autowired private com.ares.shop.repo.PosShiftRepo posShifts;
    @org.springframework.beans.factory.annotation.Autowired private com.ares.shop.repo.StaffCommissionTxRepo commissionTxRepo;

    public DataSeeder(UserRepo users, BrandRepo brands, ProductRepo products, ReviewRepo reviews, PromotionRepo promos, AuctionRepo auctions,
                      PasswordEncoder enc, CategoryRepo categories, VariantRepo variants, FlashSaleRepo flash, BannerRepo banners,
                      QuestionRepo questions, OrderService orderService, OrderRepo orders, TicketRepo tickets, AddressRepo addresses) {
        this.users = users; this.brands = brands; this.products = products; this.reviews = reviews; this.promos = promos; this.auctions = auctions;
        this.enc = enc; this.categories = categories; this.variants = variants; this.flash = flash; this.banners = banners; this.questions = questions;
        this.orderService = orderService; this.orders = orders; this.tickets = tickets; this.addresses = addresses;
    }

    private static Map<String, Integer> parse(String json) {
        Map<String, Integer> m = new HashMap<>();
        for (String kv : json.replaceAll("[{} \"]", "").split(",")) { String[] x = kv.split(":"); m.put(x[0], Integer.parseInt(x[1])); }
        return m;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            user("admin@ares.vn", "admin123", "Quản trị viên", Enums.Role.ADMIN, null);
            user("staff@ares.vn", "staff123", "Lê Nhân Viên", Enums.Role.STAFF, null);
            User k = user("khach@ares.vn", "123456", "Nguyễn Văn Khách", Enums.Role.USER, "0901234567");
            user("tran.binh@ares.vn", "123456", "Trần Thị Bình", Enums.Role.USER, "0912345678");
            Address a = new Address(); a.user = k; a.label = "Nhà riêng"; a.receiverName = k.fullName; a.phone = k.phone;
            a.line = "Chung cư Hoàng Huy Commerce, Võ Nguyên Giáp"; a.ward = "Kênh Dương"; a.district = "Lê Chân"; a.city = "Hải Phòng"; a.isDefault = true; addresses.save(a);
        }
        if (categories.count() == 0) seedCategories();
        if (products.count() == 0) seedProducts();
        ensureApparel();
        ensureTestWomenApparel();
        if (promos.count() == 0) seedPromos();
        if (flash.count() == 0) seedFlash();
        if (banners.count() == 0) seedBanners();
        if (auctions.count() == 0) seedAuctions();
        if (reviews.count() == 0) seedReviews();
        if (orders.count() == 0) seedOrders();
        if (orders.count() < 15) seedHistoricalOrders();
        ensureStaffAndPosSeed();
    }

    private void seedCategories() {
        String[][] c = {{"MODEL", "Xe Model", "TOY", "hw-r34"}, {"RC", "Xe RC", "TOY", "traxxas-slash"}, {"DISPLAY", "Xe Trưng Bày", "TOY", "kyosho-countach"},
                {"KIT", "Lắp ráp & Kit", "TOY", "lego-chiron"}, {"APPAREL", "Thời trang & Phụ kiện", "ACCESSORY", "apparel-polo"}, {"BATTERY", "Pin & Sạc", "ACCESSORY", "lipo-2s"}, {"REMOTE", "Khiển RC", "ACCESSORY", "futaba"},
                {"WHEEL", "Bánh xe RC", "ACCESSORY", "wheels-sc"}, {"PART", "Linh kiện & Phụ kiện", "ACCESSORY", "brushless"}};
        int i = 0;
        for (String[] r : c) { Category x = new Category(); x.code = r[0]; x.name = r[1]; x.grp = r[2]; x.image = r[3].startsWith("apparel") ? "/images/apparel/polo-black.svg" : ("/images/toys/" + r[3] + "-0.jpg"); x.sortOrder = i++; categories.save(x); }
    }

    private Map<String, Brand> brandMap() {
        Map<String, Brand> b = new LinkedHashMap<>();
        for (String[] r : new String[][]{
                {"Hot Wheels", "Mỹ", "Thương hiệu xe đồ chơi die-cast 1:64 nổi tiếng nhất thế giới của Mattel."},
                {"Mini GT", "Hồng Kông", "Mô hình 1:64 chi tiết cao, bánh cao su, gương rời."},
                {"Tomica", "Nhật Bản", "Dòng xe mô hình nhỏ của Takara Tomy từ 1970."},
                {"Maisto", "Mỹ", "Mô hình die-cast tỷ lệ 1:24 – 1:18 giá tốt."},
                {"Bburago", "Ý", "Mô hình xe Ý với bản quyền Ferrari, Lamborghini, Bugatti."},
                {"Kyosho", "Nhật Bản", "Xe RC thi đấu và mô hình die-cast cao cấp."},
                {"Norev", "Pháp", "Mô hình die-cast châu Âu từ 1946."},
                {"Minichamps", "Đức", "Mô hình F1 và xe đua tỷ lệ 1:43, 1:18."},
                {"LEGO Technic", "Đan Mạch", "Bộ lắp ráp kỹ thuật với hộp số, động cơ hoạt động."},
                {"Tamiya", "Nhật Bản", "Kit lắp ráp, Mini 4WD và xe RC huyền thoại."},
                {"Traxxas", "Mỹ", "Xe RC tốc độ cao, bền bỉ, chống nước."},
                {"WLtoys", "Trung Quốc", "Xe RC giá tốt cho người mới chơi."},
                {"Futaba", "Nhật Bản", "Tay điều khiển RC chuyên nghiệp."},
                {"FlySky", "Trung Quốc", "Tay điều khiển RC phổ thông."},
                {"Gens Ace", "Trung Quốc", "Pin LiPo hiệu suất cao cho RC."},
                {"SkyRC", "Trung Quốc", "Bộ sạc cân bằng thông minh."},
                {"ARES Parts", "Việt Nam", "Phụ kiện và linh kiện do ARES Toys tuyển chọn."}}) {
            Brand x = new Brand(); x.name = r[0]; x.country = r[1]; x.description = r[2];
            x.logoUrl = "/images/brands/" + r[0].toLowerCase().replace(' ', '-') + ".svg"; b.put(r[0], brands.save(x));
        }
        return b;
    }

    // slug | tên | hãng | danh mục | tỷ lệ | tuổi | giá (nghìn) | giá cũ | tồn | đã bán | bảo hành (tháng) | chất liệu | cờ (F=nổi bật, N=mới) | mô tả
    private static final String[] P = {
        "ares-polo|Áo Polo Đua Xe ARES Racing Team|ARES Parts|APPAREL|Free|12|450|550|60|38|0|Vải thun cá sấu Poly cao cấp, thoáng khí|F|Áo polo đồng phục đội đua ARES Motorsport với logo in nổi 3D, cúc dập chìm, form tôn dáng thể thao chuẩn racing.",
        "ares-jacket|Áo Khoác Gió Thể Thao ARES Paddock Bomber|ARES Parts|APPAREL|Free|12|890|1150|40|19|0|Vải dù gió 2 lớp chống nước nhẹ, lót lưới|N|Áo khoác gió thời trang phong cách pit crew F1, bo chun cổ tay và gấu áo, khoá kéo kim loại dập nổi Ares Toys.",
        "ares-cap|Nón Lưỡi Trai ARES Speed Snapback|ARES Parts|APPAREL|Free|6|250|320|80|54|0|Kaki cotton 100% dày dặn, form cứng cáp|N|Nón snapback cao cấp thêu logo khiên ARES 3D mặt trước, phía sau có khoá tăng giảm kích thước phù hợp mọi cỡ đầu.",
        "hw-r34|Hot Wheels Premium Nissan Skyline GT-R (R34)|Hot Wheels|MODEL|1:64|3|189|229|40|126|0|Kim loại đúc, bánh cao su|F|Phiên bản Premium Car Culture với bánh cao su Real Riders, sơn xanh Bayside Blue và tem đua chi tiết.",
        "hw-911|Hot Wheels Car Culture Porsche 911 Carrera RS|Hot Wheels|MODEL|1:64|3|199||35|88|0|Kim loại đúc, bánh cao su|N|Dòng Car Culture dành cho nhà sưu tầm: thân kim loại, đế kim loại, bánh Real Riders.",
        "hw-monster|Hot Wheels Monster Trucks Bone Shaker 1:64|Hot Wheels|MODEL|1:64|3|249||28|64|0|Kim loại + nhựa ABS|N|Xe tải quái vật bánh lớn, khung kim loại, chạy đà cực đã — kèm xe nhỏ để \"nghiền\".",
        "minigt-huracan|Mini GT Lamborghini Huracán STO|Mini GT|MODEL|1:64|8|390|450|18|52|0|Kim loại đúc, chi tiết sơn tay|F|Mini GT tỷ lệ 1:64 với gương rời, đèn in chi tiết, bánh cao su và hộp acrylic.",
        "minigt-supra|Mini GT Toyota GR Supra|Mini GT|MODEL|1:64|8|360||22|40|0|Kim loại đúc|N|Toyota GR Supra bản LHD, la-zăng BBS chi tiết, hộp trưng bày acrylic kèm theo.",
        "tomica-lc|Tomica No.5 Toyota Land Cruiser|Tomica|MODEL|1:66|3|120||60|210|0|Kim loại + nhựa|N|Tomica tiêu chuẩn có hệ thống treo nhún, cửa mở được — đồ chơi an toàn cho bé từ 3 tuổi.",
        "tomica-nsx|Tomica Premium Honda NSX Type R|Tomica|MODEL|1:62|6|220|260|30|95|0|Kim loại đúc|F|Dòng Tomica Premium tinh xảo hơn, sơn nhiều lớp, đèn in sắc nét.",
        "maisto-mustang|Maisto Special Edition Ford Mustang GT|Maisto|MODEL|1:24|8|450||20|34|0|Kim loại đúc, nội thất nhựa||Mở cửa, mở capo, bánh lái điều hướng; nội thất chi tiết.",
        "maisto-aventador|Maisto Lamborghini Aventador SVJ|Maisto|MODEL|1:24|8|480|560|15|41|0|Kim loại đúc|F|Cửa cắt kéo mở được, khoang động cơ V12 chi tiết.",
        "bburago-sf90|Bburago Ferrari SF90 Stradale|Bburago|MODEL|1:24|8|520||14|37|0|Kim loại đúc|N|Bản quyền chính hãng Ferrari, sơn Rosso Corsa, mở cửa.",
        "bburago-chiron|Bburago Bugatti Chiron Sport|Bburago|DISPLAY|1:18|14|1390|1590|8|21|0|Kim loại đúc|F|Mô hình 1:18 dài ~25cm, mở cửa và capo, cánh gió sau chi tiết.",
        "maisto-911|Maisto Exclusive Porsche 911 Carrera|Maisto|DISPLAY|1:18|14|1150||10|18|0|Kim loại đúc||Dòng Exclusive với thảm sàn, dây an toàn vải và đế trưng bày.",
        "bburago-f40|Bburago Ferrari F40|Bburago|DISPLAY|1:18|14|1250||9|26|0|Kim loại đúc|F|Biểu tượng thập niên 80, động cơ V8 lộ dưới kính sau.",
        "kyosho-countach|Kyosho Lamborghini Countach LP400|Kyosho|DISPLAY|1:18|14|3900|4500|4|7|0|Kim loại đúc, chi tiết ăng-ten kim loại|F|Mô hình cao cấp Kyosho với hơn 200 chi tiết, sơn bóng nhiều lớp.",
        "norev-ds|Norev Citroën DS 19|Norev|DISPLAY|1:18|14|2400||5|6|0|Kim loại đúc||Huyền thoại Pháp, hệ treo khí nén mô phỏng, nội thất da.",
        "minichamps-f1|Minichamps Xe đua F1 2023|Minichamps|DISPLAY|1:43|14|1600||7|12|0|Resin + kim loại|N|Mô hình F1 tỷ lệ 1:43 với hộp trưng bày mica.",
        "lego-911|LEGO Technic Porsche 911 GT3 RS (42056)|LEGO Technic|KIT|1:8|16|8900|9900|3|9|0|Nhựa ABS LEGO, 2.704 chi tiết|F|Hộp số tuần tự 4 cấp hoạt động, động cơ boxer 6 xi-lanh với piston chuyển động.",
        "lego-chiron|LEGO Technic Bugatti Chiron (42083)|LEGO Technic|KIT|1:8|16|9500||4|11|0|Nhựa ABS LEGO, 3.599 chi tiết|F|Động cơ W16 chuyển động, hộp số 8 cấp, cánh gió nâng hạ bằng chìa khoá.",
        "lego-mustang|LEGO Technic Ford Mustang Shelby GT500 (42138)|LEGO Technic|KIT|1:14|9|1290|1490|25|48|0|Nhựa ABS LEGO, 544 chi tiết|N|Xe kéo thả tự chạy, có app AR đua xe. Phù hợp cho bé 9+.",
        "tamiya-mini4wd|Tamiya Mini 4WD Avante Mk.III|Tamiya|KIT|1:32|8|290||45|130|0|Nhựa ABS, motor 3V|N|Kit lắp ráp Mini 4WD kinh điển, chạy pin AA, đường đua tốc độ.",
        "tamiya-kit-gtr|Tamiya Plastic Kit Nissan Skyline GT-R|Tamiya|KIT|1:24|14|690||16|22|0|Nhựa styrene, cần sơn|N|Kit mô hình nhựa tĩnh 1:24 cho người thích tự lắp và sơn.",
        "traxxas-slash|Traxxas Slash 4X4 VXL Brushless|Traxxas|RC|1:10|14|16900|18500|6|14|6|Khung composite, động cơ brushless|F|Xe RC đường địa hình tốc độ 95+ km/h, chống nước, TQi 2.4GHz.",
        "traxxas-xmaxx|Traxxas X-Maxx 8S Monster Truck|Traxxas|RC|1:5|14|29900||2|3|6|Khung nhôm, động cơ brushless 8S|F|Quái vật 1:5 nặng 8,6kg, tự lật dậy, tốc độ 80 km/h.",
        "traxxas-trx4|Traxxas TRX-4 Land Rover Defender|Traxxas|RC|1:10|14|18500||3|5|6|Khung thép, cầu khoá vi sai|N|Xe leo đá scale với khoá vi sai và hộp số 2 cấp điều khiển từ xa.",
        "tamiya-grasshopper|Tamiya Grasshopper 2WD Buggy (Kit)|Tamiya|RC|1:10|14|3200||8|19|3|Nhựa ABS, motor 540|N|Kit RC huyền thoại tái bản, lý tưởng để học lắp ráp RC.",
        "tamiya-scorcher|Tamiya Sand Scorcher (Tái bản)|Tamiya|RC|1:10|14|7500||3|4|3|Khung nhôm, vỏ ABS||Buggy Beetle cổ điển với giảm xóc dầu và bánh gai.",
        "tamiya-beetle|Tamiya Monster Beetle|Tamiya|RC|1:10|14|6900|7600|4|6|3|Nhựa ABS, motor 540|F|Monster Beetle bánh lớn, gầm cao, dễ chơi.",
        "kyosho-miniz|Kyosho Mini-Z RWD Readyset|Kyosho|RC|1:28|14|4500||9|17|6|Khung composite|N|Xe RC mini chơi trong nhà, tay khiển 2.4GHz kèm theo.",
        "kyosho-inferno|Kyosho Inferno MP10 Nitro Buggy|Kyosho|RC|1:8|14|19900||2|2|6|Khung nhôm 7075|F|Buggy thi đấu 1:8 động cơ nitro, vô địch thế giới nhiều lần.",
        "wltoys-144001|WLtoys 144001 4WD 60km/h|WLtoys|RC|1:14|14|1690|1990|20|85|3|Nhựa PA, motor 550|N|Xe RC giá tốt chạy 60 km/h, dẫn động 4 bánh, kèm pin và sạc.",
        "wltoys-crawler|WLtoys 12428 Crawler 4WD|WLtoys|RC|1:12|8|1290||26|73|3|Nhựa ABS|N|Xe leo dốc 1:12 vững chắc cho bé 8+, pin 1500mAh.",
        "rc-drift|Xe RC Drift 4WD Gyro 1:16|WLtoys|RC|1:16|8|1490||15|44|3|Nhựa ABS, bánh drift cứng||Có con quay hồi chuyển ổn định góc drift, đèn LED.",
        "lipo-2s|Pin LiPo Gens Ace 2S 7.4V 5000mAh 50C|Gens Ace|BATTERY||14|890||30|120|3|LiPo, vỏ cứng|N|Pin vỏ cứng cho xe RC 1:10, jack Deans/XT60.",
        "lipo-3s|Pin LiPo 3S 11.1V 5000mAh 50C|Gens Ace|BATTERY||14|1590||14|38|3|LiPo|N|Dành cho xe 1:8 – 1:10 cần tốc độ cao.",
        "nimh|Pin NiMH Tamiya 7.2V 3000mAh|Tamiya|BATTERY||8|450||25|61|3|NiMH|N|Pin an toàn cho người mới, jack Tamiya.",
        "charger|Bộ sạc cân bằng SkyRC B6 Neo|SkyRC|BATTERY||14|990|1150|18|56|6|Nhôm|N|Sạc LiPo/NiMH/LiFe 1–6S, màn hình màu, nguồn DC.",
        "flysky|Tay khiển FlySky FS-GT3B 3 kênh 2.4GHz|FlySky|REMOTE||8|690||22|70|6|Nhựa ABS|N|Tay khiển súng 3 kênh kèm receiver, phù hợp xe RC phổ thông.",
        "futaba|Tay khiển Futaba 4PM Plus 2.4GHz|Futaba|REMOTE||14|5900||5|9|12|Nhựa + nhôm|F|Tay khiển thi đấu với telemetry, độ trễ thấp.",
        "wheels-sc|Bộ 4 bánh Short Course 1:10|ARES Parts|WHEEL|1:10|8|790||20|41|0|Cao su, mâm nhựa|N|Lắp vừa Traxxas Slash, Tamiya, đầu lục giác 12mm.",
        "wheels-drift|Bộ 4 bánh Drift 1:10 (lốp cứng)|ARES Parts|WHEEL|1:10|8|290||40|102|0|Nhựa POM|N|Lốp drift cứng trượt mượt trên mọi mặt sàn.",
        "brushless|Combo motor Brushless 3650 + ESC 60A|ARES Parts|PART||14|1190||12|27|3|Kim loại|N|Nâng cấp xe 1:10 lên brushless, ESC chống nước.",
        "display-case|Hộp trưng bày mica 1:64 (20 ô)|ARES Parts|PART|1:64|3|250||50|160|0|Mica + gỗ|N|Tủ treo tường chống bụi cho bộ sưu tập Hot Wheels, Mini GT, Tomica."
    };

    private void seedProducts() {
        Map<String, Brand> b = brandMap();
        int n = 0;
        for (String row : P) {
            String[] f = row.split("\\|", -1);
            Product p = new Product();
            p.sku = "AR-" + f[0].toUpperCase(); p.name = f[1]; p.brand = b.get(f[2]); p.type = f[3];
            p.grp = categories.findByCode(f[3]).map(c -> c.grp).orElse("TOY");
            p.scale = f[4].isBlank() ? null : f[4]; p.ageMin = Integer.parseInt(f[5]);
            p.price = Long.parseLong(f[6]) * K; p.oldPrice = f[7].isBlank() ? null : Long.parseLong(f[7]) * K;
            p.stock = Integer.parseInt(f[8]); p.sold = Integer.parseInt(f[9]); p.warrantyMonths = Integer.parseInt(f[10]);
            p.material = f[11]; p.featured = f[12].contains("F"); p.isNew = f[12].contains("N") && n % 3 == 0; p.description = f[13];
                        if (f[0].startsWith("ares-")) {
                if (f[0].equals("ares-polo")) {
                    p.images.add("/images/apparel/polo-black.svg");
                    p.images.add("/images/apparel/polo-white.svg");
                    p.images.add("/images/apparel/polo-orange.svg");
                } else if (f[0].equals("ares-jacket")) {
                    p.images.add("/images/apparel/jacket-black.svg");
                    p.images.add("/images/apparel/jacket-red.svg");
                } else if (f[0].equals("ares-cap")) {
                    p.images.add("/images/apparel/cap-black.svg");
                    p.images.add("/images/apparel/cap-white.svg");
                    p.images.add("/images/apparel/cap-orange.svg");
                }
            } else {
                for (int i = 0; i < IMG.getOrDefault(f[0], 0); i++) p.images.add("/images/toys/" + f[0] + "-" + i + ".jpg");
            }
            p.specs.put("Thương hiệu", f[2]);
            if (p.scale != null) p.specs.put("Tỷ lệ", p.scale);
            p.specs.put("Độ tuổi", p.ageMin + "+");
            p.specs.put("Chất liệu", p.material);
            p.specs.put("Mã SKU", p.sku);
            p.specs.put("Bảo hành", p.warrantyMonths > 0 ? p.warrantyMonths + " tháng" : "Đổi mới 7 ngày nếu lỗi");
            if (f[3].equals("RC")) { p.specs.put("Tần số", "2.4GHz"); p.specs.put("Dẫn động", f[1].contains("2WD") ? "2WD" : "4WD"); }
            p.createdAt = LocalDateTime.now().minusDays(P.length - n++);
            products.save(p);
        }
        // Biến thể màu / phiên bản có giá & tồn kho riêng
        variants(find("ares-polo"), new Object[][]{{"Đen Racing (L)", "#1a1a1a", 450, 20, "/images/apparel/polo-black.svg"}, {"Trắng Apex (L)", "#f8fafc", 450, 15, "/images/apparel/polo-white.svg"}, {"Cam Hyper (L)", "#f05a1a", 480, 15, "/images/apparel/polo-orange.svg"}, {"Đen Racing (XL)", "#1a1a1a", 450, 10, "/images/apparel/polo-black.svg"}});
        variants(find("ares-jacket"), new Object[][]{{"Carbon Black (L)", "#111111", 890, 15, "/images/apparel/jacket-black.svg"}, {"Racing Red (L)", "#dc2626", 920, 12, "/images/apparel/jacket-red.svg"}, {"Carbon Black (XL)", "#111111", 890, 13, "/images/apparel/jacket-black.svg"}});
        variants(find("ares-cap"), new Object[][]{{"Đen viền cam", "#18181b", 250, 30, "/images/apparel/cap-black.svg"}, {"Trắng line đỏ", "#f1f5f9", 250, 25, "/images/apparel/cap-white.svg"}, {"Cam Ares Edition", "#f05a1a", 270, 25, "/images/apparel/cap-orange.svg"}});
        variants(find("hw-r34"), new Object[][]{{"Bayside Blue", "#1e5aa8", 189, 20, "/images/toys/hw-r34-0.jpg"}, {"Trắng Pearl", "#f2f2f2", 189, 12, "/images/toys/hw-r34-1.jpg"}, {"Đen Midnight", "#1b1b1f", 209, 8, "/images/toys/hw-r34-2.jpg"}});
        variants(find("minigt-huracan"), new Object[][]{{"Verde Selvans", "#5aa02c", 390, 8, "/images/toys/minigt-huracan-0.jpg"}, {"Arancio", "#f07a1a", 390, 6, "/images/toys/minigt-huracan-1.jpg"}, {"Bianco", "#f5f5f5", 410, 4, "/images/toys/minigt-huracan-0.jpg"}});
        variants(find("bburago-sf90"), new Object[][]{{"Rosso Corsa", "#c1121f", 520, 8, "/images/toys/bburago-sf90-0.jpg"}, {"Giallo Modena", "#f5c400", 540, 6, "/images/toys/bburago-sf90-1.jpg"}});
        variants(find("maisto-aventador"), new Object[][]{{"Tím Viola", "#6b2c91", 480, 6}, {"Xanh Lá", "#76b82a", 480, 5}, {"Cam", "#f07a1a", 480, 4}});
        variants(find("lego-mustang"), new Object[][]{{"Bản tiêu chuẩn", null, 1290, 18}, {"Kèm bộ đèn LED", null, 1590, 7}});
        variants(find("traxxas-slash"), new Object[][]{{"RTR (không pin)", null, 16900, 3}, {"RTR + Pin 3S + Sạc", null, 19400, 3}});
        variants(find("wltoys-144001"), new Object[][]{{"1 pin", null, 1690, 12}, {"2 pin (gấp đôi thời gian chơi)", null, 1890, 8}});
        variants(find("wltoys-crawler"), new Object[][]{{"Xanh rêu", "#556b2f", 1290, 14, "/images/toys/wltoys-crawler-0.jpg"}, {"Cam", "#f07a1a", 1290, 12, "/images/toys/wltoys-crawler-0.jpg"}});
        variants(find("tamiya-mini4wd"), new Object[][]{{"Avante Mk.III Trắng/Đỏ", "#f5f5f5", 290, 25}, {"Avante Mk.III Xanh", "#1e5aa8", 310, 20}});
        variants(find("flysky"), new Object[][]{{"Tay khiển + 1 receiver", null, 690, 14}, {"Tay khiển + 2 receiver", null, 850, 8}});
        variants(find("display-case"), new Object[][]{{"20 ô — gỗ sồi", "#c49a6c", 250, 30}, {"20 ô — đen", "#1b1b1f", 250, 20}, {"40 ô — gỗ sồi", "#c49a6c", 450, 10}});
    }

    private Product find(String slug) { return products.findAll().stream().filter(p -> ("AR-" + slug.toUpperCase()).equals(p.sku)).findFirst().orElseThrow(); }

    private void variants(Product p, Object[][] rows) {
        int total = 0; long min = Long.MAX_VALUE;
        for (Object[] r : rows) {
            ProductVariant v = new ProductVariant(); v.product = p; v.name = (String) r[0]; v.color = (String) r[1];
            v.price = ((Integer) r[2]) * K; v.stock = (Integer) r[3]; v.oldPrice = p.oldPrice != null && v.price == p.price ? p.oldPrice : null;
            if (r.length > 4 && r[4] != null) {
                v.image = (String) r[4];
                if (!p.images.contains(v.image)) p.images.add(v.image);
            }
            variants.save(v); total += v.stock; min = Math.min(min, v.price);
        }
        p.hasVariants = true; p.stock = total; p.price = min; products.save(p);
    }

    private void seedPromos() {
        promo("CHAOBAN", "Chào bạn mới", "Giảm 10% cho đơn đầu tiên, tối đa 100.000đ", Enums.VoucherKind.PERCENT, 10, 0, 100 * K, 0, "#a87a3c");
        promo("GIAM50K", "Giảm 50K", "Giảm 50.000đ cho đơn từ 500.000đ", Enums.VoucherKind.FIXED, 0, 50 * K, 0, 500 * K, "#c1121f");
        promo("FREESHIP", "Miễn phí vận chuyển", "Freeship mọi đơn từ 199.000đ", Enums.VoucherKind.FREESHIP, 0, 0, 0, 199 * K, "#0f9d58");
        promo("RCFAN", "Hội mê RC", "Giảm 8% đơn từ 3 triệu, tối đa 1 triệu", Enums.VoucherKind.PERCENT, 8, 0, 1000 * K, 3000 * K, "#1e5aa8");
    }

    private void promo(String code, String title, String desc, Enums.VoucherKind kind, int pct, long amount, long max, long min, String color) {
        Promotion p = new Promotion(); p.code = code; p.title = title; p.description = desc; p.kind = kind; p.discountPercent = pct; p.amount = amount;
        p.maxDiscount = max; p.minOrder = min; p.bannerColor = color; p.startDate = LocalDate.now().minusDays(10); p.endDate = LocalDate.now().plusMonths(3);
        p.usageLimit = code.equals("CHAOBAN") ? 500 : 0; promos.save(p);
    }

    private void seedFlash() {
        var now = LocalDateTime.now();
        LocalDateTime end = now.toLocalDate().atTime(23, 59, 59);
        if (end.isBefore(now.plusHours(2))) end = now.plusHours(6);
        flashSale("wltoys-144001", 1490, 20, 7, now.minusHours(1), end);
        flashSale("minigt-supra", 299, 15, 9, now.minusHours(1), end);
        flashSale("charger", 790, 10, 4, now.minusHours(1), end);
        flashSale("lego-911", 7990, 3, 1, now.minusHours(1), end);
        flashSale("kyosho-miniz", 3790, 5, 0, end, end.plusHours(12));
    }

    private void flashSale(String slug, long price, int quota, int sold, LocalDateTime from, LocalDateTime to) {
        Product p = find(slug);
        FlashSale f = new FlashSale(); f.productId = p.id; f.productName = p.name; f.salePrice = price * K; f.quota = quota; f.sold = sold; f.startTime = from; f.endTime = to;
        flash.save(f);
    }

    private void seedBanners() {
        banner("HERO", 0, "Traxxas X-Maxx", "Xe RC · 1:5", "Quái vật 8S tốc độ 80 km/h — trả góp 0%", "/images/toys/traxxas-xmaxx-0.jpg", "/products/" + find("traxxas-xmaxx").id);
        banner("HERO", 1, "Hot Wheels Premium", "Xe Model · 1:64", "Bộ sưu tập Car Culture mới về", "/images/toys/hw-r34-0.jpg", "/products?brandId=" + find("hw-r34").brand.id);
        banner("HERO", 2, "LEGO Technic", "Lắp ráp · 16+", "Bugatti Chiron 3.599 chi tiết", "/images/toys/lego-chiron-1.jpg", "/products/" + find("lego-chiron").id);
        banner("HERO", 3, "Kyosho 1:18", "Xe Trưng Bày", "Countach LP400 — phiên bản giới hạn", "/images/toys/kyosho-countach-0.jpg", "/products/" + find("kyosho-countach").id);
        banner("PROMO", 0, "Siêu sale RC", "Giảm đến 25%", "Mã RCFAN · Freeship từ 199K", "/images/toys/rc-drift-2.jpg", "/promotions");
    }

    private void banner(String pos, int order, String title, String kicker, String sub, String img, String link) {
        Banner x = new Banner(); x.position = pos; x.sortOrder = order; x.title = title; x.kicker = kicker; x.subtitle = sub; x.image = img; x.link = link; banners.save(x);
    }

    private void seedAuctions() {
        var now = LocalDateTime.now();
        auction("Hot Wheels Treasure Hunt '67 Camaro (1968 Redline)", "Hot Wheels", 1968, "Bản gốc Redline", "hw-r34", 8_000 * K, 200 * K, now.minusHours(2), now.plusDays(2),
                "Một trong những mẫu Hot Wheels đầu tiên năm 1968, bánh Redline nguyên bản, sơn Spectraflame còn 90%.");
        auction("Kyosho Countach LP400 — Limited 001/500", "Kyosho", 2020, "#001/500", "kyosho-countach", 9_000 * K, 300 * K, now.minusHours(1), now.plusHours(30),
                "Số seri 001 kèm chứng nhận, hộp gỗ và tem niêm phong nguyên vẹn.");
        auction("LEGO Technic 8880 Supercar (1994) — nguyên hộp", "LEGO Technic", 1994, "Đã lắp, đủ hộp", "lego-911", 12_000 * K, 500 * K, now.plusDays(1), now.plusDays(4),
                "Huyền thoại LEGO Technic 1994 với hệ dẫn động 4 bánh, đủ sách hướng dẫn.");
        auction("Tamiya Sand Scorcher 1979 bản gốc", "Tamiya", 1979, "Bản gốc 58016", "tamiya-scorcher", 15_000 * K, 500 * K, now.minusMinutes(30), now.plusHours(6),
                "Xe RC gốc năm 1979, khung nhôm nguyên bản, đã phục chế động cơ.");
        auction("Bburago Ferrari F40 1:18 — phiên bản vàng 24K", "Bburago", 1988, "Mạ vàng, 1/100", "bburago-f40", 6_000 * K, 200 * K, now.minusDays(3), now.minusHours(1),
                "Phiên bản kỷ niệm mạ vàng, chỉ 100 chiếc trên thế giới.");
    }

    private void auction(String title, String brand, int year, String edition, String img, long start, long step, LocalDateTime from, LocalDateTime to, String desc) {
        Auction a = new Auction(); a.title = title; a.brandName = brand; a.year = year; a.mileage = edition; a.description = desc;
        for (int i = 0; i < IMG.getOrDefault(img, 0); i++) a.images.add("/images/toys/" + img + "-" + i + ".jpg");
        a.specs.put("Thương hiệu", brand); a.specs.put("Năm sản xuất", String.valueOf(year)); a.specs.put("Phiên bản", edition); a.specs.put("Tình trạng", "Đã kiểm định bởi ARES");
        a.startPrice = start; a.minIncrement = step; a.currentPrice = start; a.startTime = from; a.endTime = to; auctions.save(a);
    }

    private void seedReviews() {
        String[][] r = {
            {"5", "Hàng đóng gói cẩn thận, mô hình sắc nét đúng như hình. Con mình mê lắm!"},
            {"4", "Chất lượng tốt so với giá, giao hàng nhanh 2 ngày. Sẽ ủng hộ tiếp."},
            {"5", "Chạy cực bốc, pin trâu. Shop tư vấn nhiệt tình cách bảo dưỡng."},
            {"3", "Sản phẩm ổn nhưng hộp bị móp nhẹ khi vận chuyển."},
            {"4", "Chi tiết đẹp, sơn bóng. Trưng tủ nhìn rất sang."}};
        var khach = users.findByEmailIgnoreCase("khach@ares.vn").orElse(null);
        var binh = users.findByEmailIgnoreCase("tran.binh@ares.vn").orElse(null);
        int i = 0;
        for (Product p : products.findAll()) {
            for (User u : new User[]{khach, binh}) {
                if (u == null || (p.id + i) % 3 == 0) { i++; continue; }
                String[] x = r[(int) ((p.id + i++) % r.length)];
                Review v = new Review(); v.productId = p.id; v.productName = p.name; v.user = u; v.authorName = u.fullName; v.verified = true;
                v.rating = Integer.parseInt(x[0]); v.comment = x[1]; v.createdAt = LocalDateTime.now().minusDays(i % 40 + 1);
                if (i % 4 == 0 && !p.images.isEmpty()) v.images.add(p.images.get(p.images.size() - 1));
                reviews.save(v);
            }
            var list = reviews.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(p.id);
            p.reviewCount = list.size(); p.rating = Math.round(list.stream().mapToInt(x -> x.rating).average().orElse(0) * 10) / 10.0;
            products.save(p);
        }
        Product slash = find("traxxas-slash");
        Question q = new Question(); q.productId = slash.id; q.productName = slash.name; q.user = khach; q.authorName = khach == null ? "Khách" : khach.fullName;
        q.question = "Xe có chạy được dưới mưa không shop?"; q.answer = "Dạ được ạ, Slash VXL có ESC và servo chống nước, sau khi chạy nước nhớ lau khô và tra dầu vòng bi nhé.";
        q.answeredBy = "ARES Toys"; q.answeredAt = LocalDateTime.now().minusDays(2); questions.save(q);
        Question q2 = new Question(); q2.productId = slash.id; q2.productName = slash.name; q2.user = binh; q2.authorName = binh == null ? "Khách" : binh.fullName;
        q2.question = "Bản RTR đã gồm tay khiển chưa ạ?"; questions.save(q2);
    }

    /** Đơn mẫu: 1 đơn đã giao (tích điểm, tra cứu bảo hành, đổi trả), 1 đơn đang giao, 1 phiếu hỗ trợ. */
    private void seedOrders() {
        User k = users.findByEmailIgnoreCase("khach@ares.vn").orElse(null);
        if (k == null) return;
        Product slash = find("traxxas-slash"); Product hw = find("hw-r34");
        var v1 = variants.findByProductIdOrderByIdAsc(slash.id).get(0); var v2 = variants.findByProductIdOrderByIdAsc(hw.id).get(0);
        var o = orderService.place(k, new OrderService.CheckoutReq(k.fullName, k.phone, k.email, "Chung cư Hoàng Huy Commerce, Võ Nguyên Giáp, Lê Chân", "Hải Phòng", null, null, 0,
                Enums.PaymentMethod.COD, null, List.of(new OrderService.ItemReq(slash.id, v1.id, 1), new OrderService.ItemReq(hw.id, v2.id, 2)), null, null, null, null, null, null, null, null));
        var t0 = LocalDateTime.now().minusDays(3);
        o.createdAt = t0; o.timeline.put("PENDING", t0); o.timeline.put("CONFIRMED", t0.plusHours(2)); o.timeline.put("SHIPPING", t0.plusDays(1)); o.timeline.put("COMPLETED", t0.plusDays(2));
        o.status = Enums.OrderStatus.COMPLETED; orderService.complete(o); orders.save(o);
        User k2 = users.findById(k.id).orElseThrow(); orderService.updateTier(k2); users.save(k2);
        Product lipo = find("lipo-2s");
        var o2 = orderService.place(k, new OrderService.CheckoutReq(k.fullName, k.phone, k.email, "Chung cư Hoàng Huy Commerce, Võ Nguyên Giáp, Lê Chân", "Hải Phòng", null, null, 0,
                Enums.PaymentMethod.MOMO, null, List.of(new OrderService.ItemReq(lipo.id, null, 1)), null, null, null, null, null, null, null, null));
        orderService.pay(o2); o2.status = Enums.OrderStatus.SHIPPING; o2.timeline.put("CONFIRMED", LocalDateTime.now().minusHours(5)); o2.timeline.put("SHIPPING", LocalDateTime.now().minusHours(1)); orders.save(o2);
        Ticket t = new Ticket(); t.user = k; t.code = "HT" + String.valueOf(System.currentTimeMillis()).substring(5); t.subject = "Hỏi cách chỉnh trim tay lái Slash"; t.category = "Kỹ thuật"; t.orderCode = o.code;
        TicketMessage m = new TicketMessage(); m.ticket = t; m.author = k.fullName; m.content = "Xe hơi lệch trái khi chạy thẳng, mình chỉnh ở đâu ạ?"; t.messages.add(m);
        TicketMessage m2 = new TicketMessage(); m2.ticket = t; m2.fromStaff = true; m2.author = "CSKH ARES"; m2.content = "Bạn xoay núm ST.TRIM trên tay khiển TQi sang phải 2–3 nấc là xe sẽ chạy thẳng ạ."; t.messages.add(m2);
        t.status = Enums.TicketStatus.ANSWERED; tickets.save(t);
    }

    private User user(String email, String pw, String name, Enums.Role role, String phone) {
        User u = new User(); u.email = email; u.password = enc.encode(pw); u.fullName = name; u.role = role; u.phone = phone;
        return users.save(u);
    }

    private void ensureApparel() {
        // Ẩn toàn bộ sản phẩm quần áo / thời trang theo yêu cầu
        for (var p : products.findAll()) {
            if ("APPAREL".equals(p.type) && p.active) {
                p.active = false;
                products.save(p);
            }
        }
        var catOpt = categories.findByCode("APPAREL");
        if (catOpt.isEmpty()) {
            Category x = new Category(); x.code = "APPAREL"; x.name = "Quần áo & Trang phục"; x.grp = "ACCESSORY";
            x.image = "/images/apparel/polo-black.svg"; x.sortOrder = 4; categories.save(x);
        } else {
            Category x = catOpt.get();
            if (!"Quần áo & Trang phục".equals(x.name)) {
                x.name = "Quần áo & Trang phục";
                categories.save(x);
            }
        }
        Brand brand = brands.findAll().stream().filter(b -> "ARES Parts".equals(b.name)).findFirst().orElseGet(() -> {
            Brand b = new Brand(); b.name = "ARES Parts"; b.country = "Việt Nam"; b.description = "Phụ kiện và linh kiện do ARES Toys tuyển chọn.";
            b.logoUrl = "/images/brands/ares-parts.svg"; return brands.save(b);
        });

        if (products.findAll().stream().noneMatch(p -> "AR-ARES-POLO".equals(p.sku))) {
            seedApparelItem("ares-polo", "Áo Polo Đua Xe ARES Racing Team", brand, 450, 550, 60, 38,
                    "Vải thun cá sấu Poly cao cấp, thoáng khí", true,
                    "Áo polo đồng phục đội đua ARES Motorsport với logo in nổi 3D, cúc dập chìm, form tôn dáng thể thao chuẩn racing. Quần áo thời trang đua xe cao cấp.",
                    List.of("/images/apparel/polo-black.svg", "/images/apparel/polo-white.svg", "/images/apparel/polo-orange.svg"),
                    new Object[][]{
                        {"Đen Racing (L)", "#1a1a1a", 450, 20, "/images/apparel/polo-black.svg"},
                        {"Trắng Apex (L)", "#f8fafc", 450, 15, "/images/apparel/polo-white.svg"},
                        {"Cam Hyper (L)", "#f05a1a", 480, 15, "/images/apparel/polo-orange.svg"},
                        {"Đen Racing (XL)", "#1a1a1a", 450, 10, "/images/apparel/polo-black.svg"}
                    });

            seedApparelItem("ares-jacket", "Áo Khoác Gió Thể Thao ARES Paddock Bomber", brand, 890, 1150, 40, 19,
                    "Vải dù gió 2 lớp chống nước nhẹ, lót lưới", false,
                    "Áo khoác gió thời trang phong cách pit crew F1, bo chun cổ tay và gấu áo, khoá kéo kim loại dập nổi Ares Toys. Quần áo khoác giữ nhiệt cản gió đường đua.",
                    List.of("/images/apparel/jacket-black.svg", "/images/apparel/jacket-red.svg"),
                    new Object[][]{
                        {"Carbon Black (L)", "#111111", 890, 15, "/images/apparel/jacket-black.svg"},
                        {"Racing Red (L)", "#dc2626", 920, 12, "/images/apparel/jacket-red.svg"},
                        {"Carbon Black (XL)", "#111111", 890, 13, "/images/apparel/jacket-black.svg"}
                    });

            seedApparelItem("ares-cap", "Nón Lưỡi Trai ARES Speed Snapback", brand, 250, 320, 80, 54,
                    "Kaki cotton 100% dày dặn, form cứng cáp", false,
                    "Nón snapback cao cấp thêu logo khiên ARES 3D mặt trước, phía sau có khoá tăng giảm kích thước phù hợp mọi cỡ đầu. Mũ nón thời trang racing.",
                    List.of("/images/apparel/cap-black.svg", "/images/apparel/cap-white.svg", "/images/apparel/cap-orange.svg"),
                    new Object[][]{
                        {"Đen viền cam", "#18181b", 250, 30, "/images/apparel/cap-black.svg"},
                        {"Trắng line đỏ", "#f1f5f9", 250, 25, "/images/apparel/cap-white.svg"},
                        {"Cam Ares Edition", "#f05a1a", 270, 25, "/images/apparel/cap-orange.svg"}
                    });
        }

        if (products.findAll().stream().noneMatch(p -> "AR-ARES-TEE".equals(p.sku))) {
            seedApparelItem("ares-tee", "Áo Thun Graphic ARES Pitlane Streetwear", brand, 320, 390, 80, 45,
                    "100% Cotton 250gsm co giãn 2 chiều, đứng form", true,
                    "Áo thun phông oversize graphic streetwear phong cách garage đường đua ARES Pitlane Crew. Quần áo thời trang cá tính, thấm hút mồ hôi.",
                    List.of("/images/apparel/tee-black.svg", "/images/apparel/tee-white.svg"),
                    new Object[][]{
                        {"Đen Phantom (L)", "#18181b", 320, 30, "/images/apparel/tee-black.svg"},
                        {"Trắng Vintage (L)", "#f1f5f9", 320, 25, "/images/apparel/tee-white.svg"},
                        {"Đen Phantom (XL)", "#18181b", 320, 25, "/images/apparel/tee-black.svg"}
                    });
        }

        if (products.findAll().stream().noneMatch(p -> "AR-ARES-HOODIE".equals(p.sku))) {
            seedApparelItem("ares-hoodie", "Áo Hoodie Nỉ ARES Apex Racing Heavyweight", brand, 790, 990, 50, 28,
                    "Nỉ bông chân cua 380gsm dày dặn, mũ 2 lớp", true,
                    "Áo hoodie nỉ cao cấp phong cách đua xe đường phố ARES Apex Racing. Có túi kangaroo phía trước, bo thun co giãn, giữ ấm cực tốt mùa lạnh.",
                    List.of("/images/apparel/hoodie-black.svg", "/images/apparel/hoodie-red.svg"),
                    new Object[][]{
                        {"Đen Asphalt (L)", "#212124", 790, 20, "/images/apparel/hoodie-black.svg"},
                        {"Đỏ Racing (L)", "#dc2626", 820, 15, "/images/apparel/hoodie-red.svg"},
                        {"Đen Asphalt (XL)", "#212124", 790, 15, "/images/apparel/hoodie-black.svg"}
                    });
        }

        for (Product p : products.findAll()) {
            if ("AR-HW-R34".equals(p.sku)) {
                assignVariantImages(p, Map.of(
                    "Bayside Blue", "/images/toys/hw-r34-0.jpg",
                    "Trắng Pearl", "/images/toys/hw-r34-1.jpg",
                    "Đen Midnight", "/images/toys/hw-r34-2.jpg"
                ));
            } else if ("AR-MINIGT-HURACAN".equals(p.sku)) {
                assignVariantImages(p, Map.of(
                    "Verde Selvans", "/images/toys/minigt-huracan-0.jpg",
                    "Arancio", "/images/toys/minigt-huracan-1.jpg",
                    "Bianco", "/images/toys/minigt-huracan-0.jpg"
                ));
            } else if ("AR-BBURAGO-SF90".equals(p.sku)) {
                assignVariantImages(p, Map.of(
                    "Rosso Corsa", "/images/toys/bburago-sf90-0.jpg",
                    "Giallo Modena", "/images/toys/bburago-sf90-1.jpg"
                ));
            } else if ("AR-WLTOYS-CRAWLER".equals(p.sku)) {
                assignVariantImages(p, Map.of(
                    "Xanh rêu", "/images/toys/wltoys-crawler-0.jpg",
                    "Cam", "/images/toys/wltoys-crawler-0.jpg"
                ));
            }
        }
    }

    private void seedApparelItem(String slug, String name, Brand brand, int priceK, int oldPriceK, int stock, int sold,
                                 String material, boolean featured, String desc, List<String> imgs, Object[][] vRows) {
        Product p = new Product();
        p.sku = "AR-" + slug.toUpperCase();
        p.name = name;
        p.brand = brand;
        p.type = "APPAREL";
        p.grp = "ACCESSORY";
        p.scale = "Free";
        p.ageMin = 12;
        p.price = (long) priceK * K;
        p.oldPrice = oldPriceK > 0 ? (long) oldPriceK * K : null;
        p.stock = stock;
        p.sold = sold;
        p.warrantyMonths = 0;
        p.material = material;
        p.featured = featured;
        p.isNew = true;
        p.description = desc;
        p.images.addAll(imgs);
        p.specs.put("Thương hiệu", brand.name);
        p.specs.put("Phân loại", "Thời trang thể thao");
        p.specs.put("Chất liệu", material);
        p.specs.put("Mã SKU", p.sku);
        p.specs.put("Bảo hành", "Đổi mới 7 ngày nếu lỗi");
        p = products.save(p);
        variants(p, vRows);
    }

    private void assignVariantImages(Product p, Map<String, String> imgMap) {
        List<ProductVariant> list = variants.findByProductIdOrderByIdAsc(p.id);
        boolean changed = false;
        for (ProductVariant v : list) {
            if (imgMap.containsKey(v.name) && (v.image == null || !v.image.equals(imgMap.get(v.name)))) {
                v.image = imgMap.get(v.name);
                variants.save(v);
                if (!p.images.contains(v.image)) {
                    p.images.add(v.image);
                    changed = true;
                }
            }
        }
        if (changed) products.save(p);
    }

    public static boolean SEED_TEST_WOMEN_APPAREL = false;

    private void ensureTestWomenApparel() {
        if (!SEED_TEST_WOMEN_APPAREL) return;
        Brand brand = brands.findAll().stream().filter(b -> "ARES Fashion".equals(b.name)).findFirst().orElseGet(() -> {
            Brand b = new Brand(); b.name = "ARES Fashion"; b.country = "Quốc tế"; b.description = "Bộ sưu tập trang phục & thời trang nữ cao cấp.";
            b.logoUrl = "/images/brands/ares-parts.svg"; return brands.save(b);
        });

        // 1. Váy Ngắn Bodycon Cắt Xẻ Sexy
        if (products.findAll().stream().noneMatch(p -> "TEST-FEM-DRESS".equals(p.sku))) {
            seedTestApparelItem("TEST-FEM-DRESS", "Váy Ngắn Bodycon Cắt Xẻ Sexy (Mẫu test)", brand, 390, 490, 55, 68,
                    "Thun gân dệt kim co giãn 4 chiều, ôm sát tôn dáng", true,
                    "Váy ngắn đầm bodycon ôm sát đường cong quyến rũ, thiết kế cắt xẻ eo và xẻ đùi gợi cảm. Chất liệu co giãn 4 chiều mềm mịn. Đồ nữ thời trang quyến rũ.",
                    List.of("/images/test_female/dress-black.jpg", "/images/test_female/dress-red.jpg", "/images/test_female/dress-white.jpg"),
                    new Object[][]{
                        {"Đen Huyền Bí (S)", "#111111", 390, 20, "/images/test_female/dress-black.jpg"},
                        {"Đen Huyền Bí (M)", "#111111", 390, 20, "/images/test_female/dress-black.jpg"},
                        {"Đỏ Rượu Vang (S)", "#991b1b", 410, 15, "/images/test_female/dress-red.jpg"},
                        {"Trắng Ngọc Trai (S)", "#f8fafc", 390, 15, "/images/test_female/dress-white.jpg"}
                    });
        }

        // 2. Áo Trễ Vai Xẻ Ngực Quyến Rũ
        if (products.findAll().stream().noneMatch(p -> "TEST-FEM-TOP".equals(p.sku))) {
            seedTestApparelItem("TEST-FEM-TOP", "Áo Trễ Vai Xẻ Ngực Quyến Rũ (Mẫu test)", brand, 290, 380, 60, 42,
                    "Lụa satin mỏng mát, tay bồng tiểu thư", true,
                    "Áo kiểu nữ trễ vai xẻ ngực sâu gợi cảm, khoe trọn xương quai xanh và vòng một cuốn hút. Chất liệu lụa mỏng nhẹ bay bổng. Áo trễ vai xẻ ngực thời trang nữ.",
                    List.of("/images/test_female/top-white.jpg", "/images/test_female/top-black.jpg", "/images/test_female/top-pink.jpg"),
                    new Object[][]{
                        {"Trắng Tinh Khôi (Freesize)", "#ffffff", 290, 25, "/images/test_female/top-white.jpg"},
                        {"Đen Quyến Rũ (Freesize)", "#18181b", 290, 20, "/images/test_female/top-black.jpg"},
                        {"Hồng Pastel (Freesize)", "#f472b6", 310, 15, "/images/test_female/top-pink.jpg"}
                    });
        }

        // 3. Bộ Đồ Lót Ren Xuyên Thấu Cao Cấp
        if (products.findAll().stream().noneMatch(p -> "TEST-FEM-LINGERIE".equals(p.sku))) {
            seedTestApparelItem("TEST-FEM-LINGERIE", "Bộ Đồ Lót Ren Xuyên Thấu Cao Cấp (Mẫu test)", brand, 350, 450, 55, 89,
                    "Ren hoa cao cấp mềm mại không dặm ngứa, mút đệm mỏng", true,
                    "Bộ nội y đồ lót ren hoa cao cấp mềm mại, cúp ngực nâng đẩy tự nhiên, dây áo tuỳ chỉnh. Thiết kế ren hoa xuyên thấu tôn dáng cực kỳ quyến rũ. Đồ lót nữ sexy cao cấp.",
                    List.of("/images/test_female/lingerie-black.jpg", "/images/test_female/lingerie-red.jpg"),
                    new Object[][]{
                        {"Đen Quyến Rũ (Size 34/75B)", "#111111", 350, 20, "/images/test_female/lingerie-black.jpg"},
                        {"Đen Quyến Rũ (Size 36/80B)", "#111111", 350, 20, "/images/test_female/lingerie-black.jpg"},
                        {"Đỏ Mận Nóng Bỏng (Size 34/75B)", "#991b1b", 370, 15, "/images/test_female/lingerie-red.jpg"}
                    });
        }

        // 4. Đầm Hai Dây Xẻ Ngực Sâu & Xẻ Tà
        if (products.findAll().stream().noneMatch(p -> "TEST-FEM-SLIP".equals(p.sku))) {
            seedTestApparelItem("TEST-FEM-SLIP", "Đầm Hai Dây Xẻ Ngực Sâu & Xẻ Tà (Mẫu test)", brand, 420, 520, 40, 31,
                    "Lụa băng cao cấp mềm mượt, óng ả", true,
                    "Đầm hai dây cổ chữ V khoét ngực sâu gợi cảm, tà váy xẻ cao khoe đôi chân thon dài. Vải lụa óng ả mướt mịn. Đầm váy xẻ ngực xẻ tà quyến rũ.",
                    List.of("/images/test_female/slip-black.jpg", "/images/test_female/slip-red.jpg"),
                    new Object[][]{
                        {"Đen Huyền Bí (S)", "#111111", 420, 20, "/images/test_female/slip-black.jpg"},
                        {"Đỏ Ruby Nổi Bật (S)", "#dc2626", 450, 20, "/images/test_female/slip-red.jpg"}
                    });
        }

        // 5. Chân Váy Ngắn Chữ A Xếp Ly Tennis
        if (products.findAll().stream().noneMatch(p -> "TEST-FEM-SKIRT".equals(p.sku))) {
            seedTestApparelItem("TEST-FEM-SKIRT", "Chân Váy Ngắn Chữ A Xếp Ly (Mẫu test)", brand, 260, 320, 55, 76,
                    "Kaki tuyết mưa đứng form, có quần lót trong", true,
                    "Chân váy ngắn xếp ly tennis dáng chữ A cạp cao tôn dáng, có quần bảo hộ an toàn bên trong. Phong cách trẻ trung nữ tính năng động. Váy ngắn sexy.",
                    List.of("/images/test_female/skirt-black.jpg", "/images/test_female/skirt-grey.jpg"),
                    new Object[][]{
                        {"Đen Cá Tính (S)", "#18181b", 260, 30, "/images/test_female/skirt-black.jpg"},
                        {"Xám Tro Hiện Đại (S)", "#71717a", 260, 25, "/images/test_female/skirt-grey.jpg"}
                    });
        }
    }

    private void seedTestApparelItem(String sku, String name, Brand brand, int priceK, int oldPriceK, int stock, int sold,
                                     String material, boolean featured, String desc, List<String> imgs, Object[][] vRows) {
        Product p = new Product();
        p.sku = sku;
        p.name = name;
        p.brand = brand;
        p.type = "APPAREL";
        p.grp = "ACCESSORY";
        p.scale = "Free";
        p.ageMin = 16;
        p.price = (long) priceK * K;
        p.oldPrice = oldPriceK > 0 ? (long) oldPriceK * K : null;
        p.stock = stock;
        p.sold = sold;
        p.warrantyMonths = 0;
        p.material = material;
        p.featured = featured;
        p.isNew = true;
        p.description = desc;
        p.images.addAll(imgs);
        p.specs.put("Thương hiệu", brand.name);
        p.specs.put("Phân loại", "Thời trang nữ quyến rũ (Mẫu test)");
        p.specs.put("Chất liệu", material);
        p.specs.put("Mã SKU", p.sku);
        p.specs.put("Ghi chú", "Mẫu thử nghiệm hệ thống (Có thể xoá nhanh)");
        p = products.save(p);
        variants(p, vRows);
    }


    

        private void seedHistoricalOrders() {
        User k = users.findByEmailIgnoreCase("khach@ares.vn").orElse(null);
        if (k == null) return;
        List<Product> catalogPool = products.findAll().stream().filter(p -> p.active && p.price > 0).limit(10).toList();
        if (catalogPool.isEmpty()) return;

        String[] buyers = {"Trần Đức Anh", "Lê Thuỳ Trang", "Nguyễn Hoàng Nam", "Vũ Minh Quân", "Phạm Quốc Bảo", "Đặng Hải Yến", "Ngô Gia Huy"};
        String[] phones = {"0912888999", "0987654321", "0903112233", "0934567890", "0978123456", "0945678901", "0967890123"};
        String[] cities = {"Hải Phòng", "Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng", "Cần Thơ"};

        int[][] pattern = {
            {2, 14}, {3, 11}, {2, 16}, {4, 10}, {5, 15}, {3, 9}, {2, 13},
            {4, 17}, {3, 11}, {5, 14}, {4, 10}, {6, 16}, {5, 19}
        };

        long orderSeq = 1000L;
        for (int dayBack = 13; dayBack >= 1; dayBack--) {
            int pIdx = 13 - dayBack;
            int numOrders = pattern[pIdx][0];
            int startHour = pattern[pIdx][1];
            LocalDate date = LocalDate.now().minusDays(dayBack);

            for (int j = 0; j < numOrders; j++) {
                orderSeq++;
                LocalDateTime dt = date.atTime(Math.min(22, startHour + j * 2), (j * 17) % 60);
                CustomerOrder o = new CustomerOrder();
                o.code = "TS" + date.toString().replace("-", "").substring(2) + String.format("%04d", orderSeq);
                o.user = k;
                o.buyerName = buyers[(j + dayBack) % buyers.length];
                o.phone = phones[(j + dayBack) % phones.length];
                o.email = "khach" + orderSeq + "@gmail.com";
                o.city = cities[(j + dayBack) % cities.length];
                o.shippingAddress = "Số " + (j * 15 + 12) + " đường Trần Phú, " + o.city;
                o.paymentMethod = (j % 2 == 0) ? Enums.PaymentMethod.BANK_QR : Enums.PaymentMethod.COD;
                o.status = (j == 0 && dayBack % 4 == 0) ? Enums.OrderStatus.CANCELLED : Enums.OrderStatus.COMPLETED;
                o.paymentStatus = (o.status == Enums.OrderStatus.COMPLETED) ? Enums.PaymentStatus.PAID : Enums.PaymentStatus.UNPAID;
                o.createdAt = dt;
                o.timeline.put("PENDING", dt);
                if (o.status != Enums.OrderStatus.CANCELLED) {
                    o.timeline.put("CONFIRMED", dt.plusHours(1));
                    o.timeline.put("SHIPPING", dt.plusHours(4));
                    o.timeline.put("COMPLETED", dt.plusDays(1));
                    o.paidAt = dt.plusHours(1);
                }

                int numItems = 1 + (j % 3);
                long subtotal = 0;
                for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                    Product prod = catalogPool.get((j + itemIdx + dayBack) % catalogPool.size());
                    List<ProductVariant> vList = variants.findByProductIdOrderByIdAsc(prod.id);
                    ProductVariant varItem = vList.isEmpty() ? null : vList.get(0);
                    int qty = 1 + (itemIdx % 2);

                    OrderItem oi = new OrderItem();
                    oi.order = o;
                    oi.productId = prod.id;
                    oi.variantId = varItem != null ? varItem.id : null;
                    oi.productName = prod.name;
                    oi.variantName = varItem != null ? varItem.name : null;
                    oi.sku = prod.sku;
                    oi.image = (prod.images != null && !prod.images.isEmpty()) ? prod.images.get(0) : "";
                    oi.unitPrice = varItem != null ? varItem.price : prod.price;
                    oi.quantity = qty;
                    oi.warrantyMonths = prod.warrantyMonths;
                    o.items.add(oi);
                    subtotal += (oi.unitPrice * qty);
                }
                o.subtotal = subtotal;
                o.shippingFee = subtotal >= 500000 ? 0 : 35000;
                o.total = o.subtotal + o.shippingFee;
                o.paidAmount = (o.paymentStatus == Enums.PaymentStatus.PAID) ? o.total : 0;
                orders.save(o);
            }
        }
    }

    private void ensureStaffAndPosSeed() {
        // Cập nhật thông tin chi tiết cho nhân viên
        users.findByEmailIgnoreCase("admin@ares.vn").ifPresent(u -> {
            boolean changed = false;
            if (u.phone == null || u.phone.isBlank()) {
                u.phone = "0901234567";
                changed = true;
            }
            if (u.tier == null || "Đồng".equals(u.tier)) {
                u.tier = "Titan";
                changed = true;
            }
            if (u.staffCode == null) {
                u.staffCode = "QL-001";
                u.department = "Quản lý điều hành Showroom";
                u.commissionRate = 1.5;
                u.monthlySalesTarget = 100_000_000L;
                changed = true;
            }
            if (changed) users.save(u);
        });

        users.findByEmailIgnoreCase("staff@ares.vn").ifPresent(u -> {
            if (u.staffCode == null) {
                u.staffCode = "NV-001";
                u.department = "Thu ngân & Bán hàng tại quầy";
                u.commissionRate = 3.0;
                u.monthlySalesTarget = 60_000_000L;
                users.save(u);
            }
        });

        if (!users.existsByEmailIgnoreCase("ngan.cashier@ares.vn")) {
            User u = new User();
            u.email = "ngan.cashier@ares.vn";
            u.fullName = "Trần Thu Ngân";
            u.phone = "0988776655";
            u.password = enc.encode("staff123");
            u.role = Enums.Role.STAFF;
            u.staffCode = "NV-002";
            u.department = "Thu ngân quầy A";
            u.commissionRate = 2.5;
            u.monthlySalesTarget = 50_000_000L;
            u.employmentStatus = "ACTIVE";
            users.save(u);
        }

        if (!users.existsByEmailIgnoreCase("duc.sales@ares.vn")) {
            User u = new User();
            u.email = "duc.sales@ares.vn";
            u.fullName = "Hoàng Minh Đức";
            u.phone = "0977665544";
            u.password = enc.encode("staff123");
            u.role = Enums.Role.STAFF;
            u.staffCode = "NV-003";
            u.department = "Tư vấn kỹ thuật & RC";
            u.commissionRate = 3.5;
            u.monthlySalesTarget = 80_000_000L;
            u.employmentStatus = "ACTIVE";
            users.save(u);
        }

        // Mở ca làm việc mẫu nếu chưa có ca nào mở
        if (posShifts != null && posShifts.findFirstByStatusOrderByStartTimeDesc("OPEN").isEmpty()) {
            User staff = users.findByEmailIgnoreCase("staff@ares.vn").orElse(null);
            com.ares.shop.model.PosShift s = new com.ares.shop.model.PosShift();
            s.shiftCode = "CA-" + java.time.format.DateTimeFormatter.ofPattern("yyMMdd-0830").format(java.time.LocalDateTime.now());
            s.staffId = staff != null ? staff.id : 1L;
            s.staffName = staff != null ? staff.fullName : "Lê Nhân Viên";
            s.staffEmail = staff != null ? staff.email : "staff@ares.vn";
            s.openingCash = 2_000_000L;
            s.expectedCash = 2_000_000L;
            s.status = "OPEN";
            s.notes = "Ca sáng: Tiền lẻ đầu ca 2.000.000đ (két A)";
            posShifts.save(s);
        }

        // Tạo một số đơn POS mẫu để phân tách sản lượng On & Off
        if (orders.findByChannelOrderByCreatedAtDesc("POS").isEmpty()) {
            User staff = users.findByEmailIgnoreCase("staff@ares.vn").orElse(null);
            var prodList = products.findAll();
            if (!prodList.isEmpty()) {
                Product p1 = prodList.get(0);
                Product p2 = prodList.size() > 1 ? prodList.get(1) : p1;

                // Đơn POS 1: Tiền mặt
                CustomerOrder o1 = new CustomerOrder();
                o1.code = "POS" + java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss").format(java.time.LocalDateTime.now()) + "01";
                o1.channel = "POS";
                o1.buyerName = "Trần Anh Quân (Khách mua trực tiếp)";
                o1.phone = "0934567890";
                o1.email = "quan.pos@gmail.com";
                o1.shippingAddress = "Nhận tại showroom ARES Toys";
                o1.city = "Hải Phòng";
                o1.staffId = staff != null ? staff.id : null;
                o1.staffName = staff != null ? staff.fullName : "Lê Nhân Viên";
                o1.paymentMethod = Enums.PaymentMethod.COD;
                o1.posPaymentMethod = "CASH";
                o1.paymentStatus = Enums.PaymentStatus.PAID;
                o1.status = Enums.OrderStatus.COMPLETED;
                o1.createdAt = java.time.LocalDateTime.now().minusHours(4);
                o1.paidAt = o1.createdAt;
                o1.subtotal = p1.price;
                o1.total = p1.price;
                o1.paidAmount = p1.price;
                o1.cashGiven = (p1.price / 100000 + 1) * 100000L;
                o1.cashChange = o1.cashGiven - o1.total;
                o1.commissionRate = 3.0;
                o1.commissionAmount = Math.round(o1.total * 0.03);

                OrderItem i1 = new OrderItem();
                i1.order = o1; i1.productId = p1.id; i1.productName = p1.name; i1.unitPrice = p1.price; i1.quantity = 1;
                i1.image = p1.images != null && !p1.images.isEmpty() ? p1.images.get(0) : "";
                o1.items.add(i1);
                orders.save(o1);

                // Đơn POS 2: Chuyển khoản VietQR
                CustomerOrder o2 = new CustomerOrder();
                o2.code = "POS" + java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss").format(java.time.LocalDateTime.now()) + "02";
                o2.channel = "POS";
                o2.buyerName = "Hoàng Văn Tuấn (Quẹt QR tại quầy)";
                o2.phone = "0945678901";
                o2.email = "tuan.pos@gmail.com";
                o2.shippingAddress = "Nhận tại showroom ARES Toys";
                o2.city = "Hải Phòng";
                o2.staffId = staff != null ? staff.id : null;
                o2.staffName = staff != null ? staff.fullName : "Lê Nhân Viên";
                o2.paymentMethod = Enums.PaymentMethod.BANK_QR;
                o2.posPaymentMethod = "BANK_QR";
                o2.paymentStatus = Enums.PaymentStatus.PAID;
                o2.status = Enums.OrderStatus.COMPLETED;
                o2.createdAt = java.time.LocalDateTime.now().minusHours(2);
                o2.paidAt = o2.createdAt;
                o2.subtotal = p2.price * 2;
                o2.total = p2.price * 2;
                o2.paidAmount = o2.total;
                o2.cashGiven = o2.total;
                o2.cashChange = 0L;
                o2.commissionRate = 3.0;
                o2.commissionAmount = Math.round(o2.total * 0.03);

                OrderItem i2 = new OrderItem();
                i2.order = o2; i2.productId = p2.id; i2.productName = p2.name; i2.unitPrice = p2.price; i2.quantity = 2;
                i2.image = p2.images != null && !p2.images.isEmpty() ? p2.images.get(0) : "";
                o2.items.add(i2);
                orders.save(o2);
            }
        }
    }
}
