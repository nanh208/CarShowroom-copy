package com.ares.shop.config;
import com.ares.shop.model.*; import com.ares.shop.repo.*;
import org.springframework.boot.CommandLineRunner; import org.springframework.core.annotation.Order; import org.springframework.stereotype.Component;
import java.util.*;
/** Dữ liệu mẫu v11 (chạy được trên DB cũ): combo, mã COMEBACK5. Idempotent. */
@Component @Order(50)
public class V11Seeder implements CommandLineRunner {
    private final BundleRepo bundles; private final ProductRepo products; private final PromotionRepo promos;
    public V11Seeder(BundleRepo bundles, ProductRepo products, PromotionRepo promos) { this.bundles = bundles; this.products = products; this.promos = promos; }
    @Override public void run(String... a) {
        Map<String, Long> sku = new HashMap<>(); products.findAll().forEach(p -> sku.put(p.sku, p.id));
        if (bundles.count() == 0 && !sku.isEmpty()) {
            mk("Mua 2 tặng 1 — xe 1:64", "Chọn 3 xe 1:64 bất kỳ trong nhóm, tặng chiếc rẻ nhất. Áp dụng lặp lại (6 xe tặng 2).", "BUY_X_GET_Y", 2, 1, 0, sku,
                    "AR-HW-R34", "AR-HW-911", "AR-HW-MONSTER", "AR-MINIGT-HURACAN", "AR-MINIGT-SUPRA", "AR-TOMICA-LC", "AR-TOMICA-NSX");
            mk("Combo Crawler sẵn sàng chạy", "WLtoys 12428 + pin LiPo 2S + sạc cân bằng SkyRC — mở hộp là chạy.", "FIXED", 0, 0, 2_790_000, sku, "AR-WLTOYS-CRAWLER", "AR-LIPO-2S", "AR-CHARGER");
            mk("Combo trưng bày 1:64", "Hộp mica 20 ô + 2 xe Skyline R34 & GR Supra.", "FIXED", 0, 0, 690_000, sku, "AR-DISPLAY-CASE", "AR-HW-R34", "AR-MINIGT-SUPRA");
        }
        if (promos.findByCodeIgnoreCase("COMEBACK5").isEmpty()) {
            Promotion p = new Promotion(); p.code = "COMEBACK5"; p.title = "Quay lại hoàn tất giỏ hàng"; p.discountPercent = 5; p.maxDiscount = 200_000; p.isPublic = false;
            p.description = "Gửi kèm email nhắc giỏ hàng bị bỏ quên."; promos.save(p);
        }
    }
    private void mk(String t, String d, String kind, int buy, int get, long price, Map<String, Long> sku, String... skus) {
        Bundle b = new Bundle(); b.title = t; b.description = d; b.kind = kind; b.buyQty = buy == 0 ? 2 : buy; b.getQty = get == 0 ? 1 : get; b.bundlePrice = price;
        for (String s : skus) if (sku.containsKey(s)) b.productIds.add(sku.get(s));
        b.sortOrder = (int) bundles.count(); if (b.productIds.size() >= 2) bundles.save(b);
    }
}
