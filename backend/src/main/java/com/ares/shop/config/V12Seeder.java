package com.ares.shop.config;
import com.ares.shop.model.*; import com.ares.shop.repo.*;
import org.springframework.boot.CommandLineRunner; import org.springframework.core.annotation.Order; import org.springframework.stereotype.Component;
import java.time.LocalDate; import java.util.*;
/** Dữ liệu mẫu v12 (chạy được trên DB cũ): ý định chatbot, bộ sưu tập, mẫu đặt trước. Idempotent. */
@Component @Order(60)
public class V12Seeder implements CommandLineRunner {
    private final ChatIntentRepo intents; private final LookbookRepo books; private final ProductRepo products;
    public V12Seeder(ChatIntentRepo intents, LookbookRepo books, ProductRepo products) { this.intents = intents; this.books = books; this.products = products; }
    @Override public void run(String... a) {
        if (intents.count() == 0) {
            in("Phí ship", "phi ship,ship,van chuyen,giao hang,phi giao,freeship,mien phi van chuyen", "Giao tiêu chuẩn 2–4 ngày (nội thành HCM/HN 1–2 ngày), miễn phí cho đơn từ 500.000đ. Giao nhanh +25.000đ, nhận sớm hơn 1–2 ngày. Ngày giao dự kiến hiện ngay ở bước thanh toán.", "Tra cứu đơn hàng,Mã giảm giá");
            in("Thời gian giao", "bao lau,khi nao nhan,may ngay,thoi gian giao,giao trong", "Thường 1–2 ngày nội thành HCM/HN, 2–4 ngày tỉnh khác. Chọn “Giao nhanh” ở bước thanh toán để nhận sớm hơn.", "Phí ship,Tra cứu đơn hàng");
            in("Đổi trả", "doi tra,tra hang,hoan tien,doi hang,bi loi,hong,vo,thieu", "Đổi trả trong 7 ngày nếu sản phẩm lỗi/không đúng mô tả, còn nguyên hộp. Vào Tài khoản › Đơn hàng › “Yêu cầu đổi trả” và tải ảnh lên — shop phản hồi trong 24 giờ.", "Gặp nhân viên,Tra cứu đơn hàng");
            in("Thanh toán", "thanh toan,momo,vnpay,chuyen khoan,cod,tra gop,the tin dung", "ARES nhận COD, chuyển khoản, MoMo, VNPay và trả góp 0% (đơn từ 3 triệu). Có thể dùng thêm điểm thưởng, thẻ quà và ví tín dụng.", "Thẻ quà tặng,Mã giảm giá");
            in("Bảo hành", "bao hanh,sua chua,linh kien,phu tung", "Xe RC và tay khiển bảo hành 6–12 tháng (xem tab Thông số). Mô hình tĩnh được đổi mới nếu lỗi sản xuất trong 7 ngày. Gửi phiếu hỗ trợ kèm mã đơn để được xử lý.", "Gặp nhân viên");
            in("Tỉ lệ & kích thước", "ti le,1:64,1:18,1:24,1:10,kich thuoc,scale,dai bao nhieu,to khong", "1:64 dài ~7cm (cỡ Hot Wheels), 1:43 ~10cm, 1:24 ~18cm, 1:18 ~25cm; xe RC 1:10 dài ~45cm. Dùng “Tìm tỉ lệ phù hợp” ở trang sản phẩm để được gợi ý.", "Gợi ý quà tặng");
            in("Gợi ý quà tặng", "qua tang,tang be,sinh nhat,tre em,qua cho,goi y", "Gợi ý nhanh: dưới 300k — xe 1:64 Hot Wheels/MiniGT; 300k–2tr — mô hình 1:24, LEGO nhỏ; trên 2tr — xe RC Traxxas/Kyosho. Bạn có thể gõ “xe RC dưới 3tr” hoặc “quà cho bé 6 tuổi” để mình tìm. Có gói quà + thiệp miễn phí lời chúc.", "Thẻ quà tặng,Bộ sưu tập");
            in("Thẻ quà tặng", "the qua,gift card,qua tang dien tu,voucher qua", "Thẻ quà ARES mệnh giá 100k–5tr, gửi qua email kèm lời nhắn. Người nhận nạp vào ví hoặc nhập mã ở bước thanh toán. Mua tại trang Thẻ quà tặng.", "Mã giảm giá");
            in("Điểm thưởng", "diem,tich diem,hang thanh vien,diem danh,vong quay,thanh vien", "Mỗi 10.000đ = 1 điểm, 100 điểm = 10.000đ khi thanh toán. Điểm danh hằng ngày và vòng quay may mắn ở Tài khoản › Phần thưởng; hạng Bạc/Vàng/Kim cương có thêm ưu đãi.", "Mã giảm giá");
            in("Đặt trước", "dat truoc,pre order,preorder,sap ra mat,hang sap ve,order truoc", "Các mẫu sắp ra mắt có nút “Đặt trước” — giữ suất, chỉ cọc khi hàng về và được báo qua email/thông báo. Xem danh sách tại trang Sắp ra mắt.", "Bộ sưu tập");
            in("Cửa hàng", "dia chi,cua hang,showroom,gio mo cua,o dau,hotline,so dien thoai", "Showroom: 123 Nguyễn Văn Linh, Q.7, TP.HCM — 9:00–21:00 mỗi ngày. Hotline 1900 6868.", "Gặp nhân viên");
            in("Chào hỏi", "xin chao,chao,hello,hi,alo,shop oi", "Chào bạn 👋 Mình là trợ lý ARES. Bạn có thể hỏi về đơn hàng, mã giảm giá, phí ship hoặc gõ tên mẫu xe để tìm nhé!", null);
        }
        Map<String, Product> sku = new HashMap<>(); products.findAll().forEach(p -> sku.put(p.sku, p));
        if (books.count() == 0 && !sku.isEmpty()) {
            bk("sieu-xe-y", "Siêu xe Ý", "Ferrari · Lamborghini · Pagani", "Những đường cong huyền thoại từ Maranello và Sant'Agata — tuyển chọn mô hình tĩnh tỉ lệ 1:18 đến 1:64 cho kệ trưng bày.", "#c62828", sku, "AR-BBURAGO-F40", "AR-BBURAGO-SF90", "AR-MAISTO-AVENTADOR", "AR-MINIGT-HURACAN", "AR-KYOSHO-COUNTACH");
            bk("jdm-legends", "JDM Legends", "Skyline · Supra · NSX", "Biểu tượng xe Nhật thập niên 90 – nay, từ Tomica đến MiniGT.", "#1565c0", sku, "AR-HW-R34", "AR-MINIGT-SUPRA", "AR-TOMICA-NSX", "AR-TOMICA-LC", "AR-TAMIYA-KIT-GTR");
            bk("off-road", "Off-road & Crawler", "Leo đá, băng đồi, phá bùn", "Xe RC địa hình bền bỉ cho cuối tuần ngoài trời, kèm pin sạc sẵn sàng chạy.", "#2e7d32", sku, "AR-TRAXXAS-TRX4", "AR-TRAXXAS-XMAXX", "AR-WLTOYS-CRAWLER", "AR-TAMIYA-SCORCHER", "AR-TRAXXAS-SLASH", "AR-LIPO-2S");
            bk("qua-cho-be", "Quà cho bé", "Từ 3 tuổi · an toàn · dễ chơi", "Xe mô hình nhỏ, bộ lắp ráp và RC tốc độ vừa phải phù hợp trẻ em.", "#ff6a00", sku, "AR-HW-MONSTER", "AR-HW-911", "AR-TAMIYA-MINI4WD", "AR-KYOSHO-MINIZ", "AR-LEGO-MUSTANG");
        }
        if (products.findAll().stream().noneMatch(p -> Boolean.TRUE.equals(p.preorder))) {
            up("AR-TAMIYA-SCORCHER", "AR-PRE-SCORCHER-50", "Tamiya Sand Scorcher — Bản kỷ niệm 50 năm", 45, 500_000, 8_900_000, sku);
            up("AR-LEGO-CHIRON", "AR-PRE-LEGO-2027", "LEGO Technic Supercar 2027 (đặt trước)", 75, 300_000, 10_500_000, sku);
        }
    }
    private void in(String n, String k, String ans, String q) { ChatIntent i = new ChatIntent(); i.name = n; i.keywords = k; i.answer = ans; i.quickReplies = q; i.sortOrder = (int) intents.count(); intents.save(i); }
    private void bk(String slug, String t, String sub, String d, String accent, Map<String, Product> sku, String... skus) {
        Lookbook b = new Lookbook(); b.slug = slug; b.title = t; b.subtitle = sub; b.description = d; b.accent = accent; b.sortOrder = (int) books.count();
        for (String s : skus) if (sku.containsKey(s)) b.productIds.add(sku.get(s).id);
        if (!b.productIds.isEmpty()) { b.cover = sku.get(skus[0]) != null && !sku.get(skus[0]).images.isEmpty() ? sku.get(skus[0]).images.get(0) : null; books.save(b); }
    }
    private void up(String base, String newSku, String name, int days, long deposit, long price, Map<String, Product> sku) {
        Product b = sku.get(base); if (b == null || sku.containsKey(newSku)) return;
        Product p = new Product(); p.name = name; p.sku = newSku; p.brand = b.brand; p.type = b.type; p.grp = b.grp; p.scale = b.scale; p.ageMin = b.ageMin; p.material = b.material;
        p.warrantyMonths = b.warrantyMonths; p.price = price; p.stock = 0; p.baseColor = b.baseColor; p.images = new ArrayList<>(b.images); p.specs = new LinkedHashMap<>(b.specs);
        p.description = "Mẫu sắp ra mắt — " + (b.description == null ? "" : b.description); p.isNew = true; p.preorder = true; p.releaseDate = LocalDate.now().plusDays(days); p.preorderDeposit = deposit;
        products.save(p);
    }
}
