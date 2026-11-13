package com.ares.shop.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Dịch vụ tính toán khoảng cách và cước vận chuyển tích hợp API/quy chuẩn của các đơn vị giao hàng hàng đầu:
 * - Giao Hàng Nhanh (GHN)
 * - Giao Hàng Tiết Kiệm (GHTK)
 * - Viettel Post
 * - AhaMove / Hỏa tốc nội thành
 *
 * Điểm xuất phát (Origin Hub): Tổng kho ARES Central Hub (123 Nguyễn Văn Linh, P. Tân Phong, Quận 7, TP.HCM).
 */
@Service
public class CarrierShippingService {

    public static final String ORIGIN_NAME = "Showroom & Tổng kho ARES Hoàng Huy Commerce";
    public static final String ORIGIN_ADDRESS = "Chung cư Hoàng Huy Commerce, Võ Nguyên Giáp, P. Kênh Dương, Q. Lê Chân, TP. Hải Phòng";
    public static final double ORIGIN_LAT = 20.8285;
    public static final double ORIGIN_LNG = 106.6925;
    public static final long FREE_SHIP_THRESHOLD = OrderService.FREE_SHIP_FROM; // 500.000đ

    // Bản đồ khoảng cách đường bộ (km) từ Chung cư Hoàng Huy Commerce tới các quận/huyện Hải Phòng
    private static final Map<String, Double> HP_DISTRICT_DISTANCES = new LinkedHashMap<>();
    // Bản đồ khoảng cách đường bộ (km) từ Quận 7 tới các quận/huyện TP.HCM
    private static final Map<String, Double> HCM_DISTRICT_DISTANCES = new LinkedHashMap<>();
    // Bản đồ khoảng cách đường bộ (km) từ Hải Phòng tới 63 tỉnh/thành phố toàn quốc
    private static final Map<String, Double> PROVINCE_DISTANCES = new LinkedHashMap<>();

    static {
        // Quận/huyện Hải Phòng (xuất phát từ Chung cư Hoàng Huy Commerce, Q. Lê Chân)
        HP_DISTRICT_DISTANCES.put("le chan", 1.2);
        HP_DISTRICT_DISTANCES.put("ngo quyen", 3.5);
        HP_DISTRICT_DISTANCES.put("hong bang", 4.2);
        HP_DISTRICT_DISTANCES.put("hai an", 5.0);
        HP_DISTRICT_DISTANCES.put("kien an", 6.5);
        HP_DISTRICT_DISTANCES.put("duong kinh", 7.0);
        HP_DISTRICT_DISTANCES.put("an duong", 10.5);
        HP_DISTRICT_DISTANCES.put("thuy nguyen", 12.0);
        HP_DISTRICT_DISTANCES.put("kien thuy", 14.0);
        HP_DISTRICT_DISTANCES.put("an lao", 16.0);
        HP_DISTRICT_DISTANCES.put("do son", 18.0);
        HP_DISTRICT_DISTANCES.put("tien lang", 26.0);
        HP_DISTRICT_DISTANCES.put("vinh bao", 38.0);
        HP_DISTRICT_DISTANCES.put("cat hai", 28.0);
        HP_DISTRICT_DISTANCES.put("bach long vi", 130.0);

        // Quận/huyện TP.HCM
        HCM_DISTRICT_DISTANCES.put("quan 7", 2.8);
        HCM_DISTRICT_DISTANCES.put("nha be", 6.5);
        HCM_DISTRICT_DISTANCES.put("quan 4", 5.2);
        HCM_DISTRICT_DISTANCES.put("quan 8", 6.8);
        HCM_DISTRICT_DISTANCES.put("quan 1", 7.2);
        HCM_DISTRICT_DISTANCES.put("quan 5", 8.5);
        HCM_DISTRICT_DISTANCES.put("quan 3", 8.9);
        HCM_DISTRICT_DISTANCES.put("quan 10", 9.6);
        HCM_DISTRICT_DISTANCES.put("quan 6", 10.5);
        HCM_DISTRICT_DISTANCES.put("quan 11", 11.2);
        HCM_DISTRICT_DISTANCES.put("phu nhuan", 12.0);
        HCM_DISTRICT_DISTANCES.put("binh thanh", 12.5);
        HCM_DISTRICT_DISTANCES.put("tan binh", 13.8);
        HCM_DISTRICT_DISTANCES.put("tan phu", 14.5);
        HCM_DISTRICT_DISTANCES.put("thu duc", 14.0);
        HCM_DISTRICT_DISTANCES.put("quan 2", 11.5);
        HCM_DISTRICT_DISTANCES.put("quan 9", 18.5);
        HCM_DISTRICT_DISTANCES.put("binh tan", 15.5);
        HCM_DISTRICT_DISTANCES.put("go vap", 17.0);
        HCM_DISTRICT_DISTANCES.put("quan 12", 21.0);
        HCM_DISTRICT_DISTANCES.put("binh chanh", 16.0);
        HCM_DISTRICT_DISTANCES.put("hoc mon", 24.5);
        HCM_DISTRICT_DISTANCES.put("cu chi", 42.0);
        HCM_DISTRICT_DISTANCES.put("can gio", 48.0);

        // 63 tỉnh thành Việt Nam (tính cự ly từ Hải Phòng)
        PROVINCE_DISTANCES.put("hai phong", 4.0);
        PROVINCE_DISTANCES.put("hai duong", 45.0);
        PROVINCE_DISTANCES.put("quang ninh", 55.0);
        PROVINCE_DISTANCES.put("thai binh", 65.0);
        PROVINCE_DISTANCES.put("hung yen", 80.0);
        PROVINCE_DISTANCES.put("ha noi", 105.0);
        PROVINCE_DISTANCES.put("bac ninh", 95.0);
        PROVINCE_DISTANCES.put("bac giang", 115.0);
        PROVINCE_DISTANCES.put("nam dinh", 85.0);
        PROVINCE_DISTANCES.put("ha nam", 95.0);
        PROVINCE_DISTANCES.put("ninh binh", 115.0);
        PROVINCE_DISTANCES.put("vinh phuc", 135.0);
        PROVINCE_DISTANCES.put("thai nguyen", 165.0);
        PROVINCE_DISTANCES.put("phu tho", 185.0);
        PROVINCE_DISTANCES.put("lang son", 195.0);
        PROVINCE_DISTANCES.put("thanh hoa", 195.0);
        PROVINCE_DISTANCES.put("nghe an", 340.0);
        PROVINCE_DISTANCES.put("ha tinh", 390.0);
        PROVINCE_DISTANCES.put("quang binh", 530.0);
        PROVINCE_DISTANCES.put("quang tri", 630.0);
        PROVINCE_DISTANCES.put("thua thien hue", 700.0);
        PROVINCE_DISTANCES.put("hue", 700.0);
        PROVINCE_DISTANCES.put("da nang", 810.0);
        PROVINCE_DISTANCES.put("quang nam", 870.0);
        PROVINCE_DISTANCES.put("quang ngai", 950.0);
        PROVINCE_DISTANCES.put("binh dinh", 1070.0);
        PROVINCE_DISTANCES.put("phu yen", 1170.0);
        PROVINCE_DISTANCES.put("khanh hoa", 1280.0);
        PROVINCE_DISTANCES.put("nha trang", 1280.0);
        PROVINCE_DISTANCES.put("lam dong", 1350.0);
        PROVINCE_DISTANCES.put("da lat", 1350.0);
        PROVINCE_DISTANCES.put("binh thuan", 1520.0);
        PROVINCE_DISTANCES.put("ba ria vung tau", 1680.0);
        PROVINCE_DISTANCES.put("dong nai", 1670.0);
        PROVINCE_DISTANCES.put("binh duong", 1660.0);
        PROVINCE_DISTANCES.put("ho chi minh", 1710.0);
        PROVINCE_DISTANCES.put("long an", 1740.0);
        PROVINCE_DISTANCES.put("tien giang", 1770.0);
        PROVINCE_DISTANCES.put("can tho", 1860.0);
        PROVINCE_DISTANCES.put("ca mau", 2020.0);
        PROVINCE_DISTANCES.put("bac giang", 1775.0);
        PROVINCE_DISTANCES.put("vinh phuc", 1765.0);
        PROVINCE_DISTANCES.put("phu tho", 1795.0);
        PROVINCE_DISTANCES.put("thai nguyen", 1785.0);
        PROVINCE_DISTANCES.put("tuyen quang", 1845.0);
        PROVINCE_DISTANCES.put("yen bai", 1875.0);
        PROVINCE_DISTANCES.put("lang son", 1865.0);
        PROVINCE_DISTANCES.put("hoa binh", 1665.0);
        PROVINCE_DISTANCES.put("son la", 1885.0);
        PROVINCE_DISTANCES.put("dien bien", 2095.0);
        PROVINCE_DISTANCES.put("lai chau", 2115.0);
        PROVINCE_DISTANCES.put("lao cai", 2005.0);
        PROVINCE_DISTANCES.put("ha giang", 1985.0);
        PROVINCE_DISTANCES.put("cao bang", 1995.0);
        PROVINCE_DISTANCES.put("bac kan", 1875.0);
    }

    public record CarrierQuote(
            String carrierId,
            String carrierName,
            String serviceType, // STANDARD, EXPRESS, INSTANT
            String serviceName,
            String logo,
            double distanceKm,
            long rawFee,
            long finalFee,
            boolean isFree,
            long discount,
            String etaText,
            LocalDate etaFrom,
            LocalDate etaTo,
            String description
    ) {}

    public record ShippingCalculationResult(
            String originName,
            String originAddress,
            double originLat,
            double originLng,
            String destinationProvince,
            String destinationDistrict,
            String destinationAddress,
            double distanceKm,
            long freeShipThreshold,
            boolean isFreeShip,
            boolean hasHazardous,
            long hazardousFee,
            String hazardousNotice,
            List<CarrierQuote> quotes
    ) {}

    /**
     * Chuẩn hoá chuỗi tiếng Việt để tìm kiếm khoảng cách
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccents = pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'd');
        return noAccents.replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\b(tinh|thanh pho|tp|quan|huyen|thi xa|tx|phuong|xa)\\b", "")
                .replaceAll("\\s+", " ").trim();
    }

    /**
     * Tính toán khoảng cách (km) từ Tổng kho ARES (Q.7, TP.HCM) đến địa chỉ người nhận
     */
    public double calculateDistanceKm(String provinceOrCity, String district, String address) {
        String pNorm = normalize(provinceOrCity);
        String dNorm = normalize(district);
        String aNorm = normalize(address);

        // Nếu người nhận ở Hải Phòng (cùng thành phố với Showroom Hoàng Huy Commerce)
        if (pNorm.contains("hai phong") || aNorm.contains("hai phong")) {
            for (Map.Entry<String, Double> e : HP_DISTRICT_DISTANCES.entrySet()) {
                if (!dNorm.isEmpty() && dNorm.contains(e.getKey())) return e.getValue();
                if (aNorm.contains(e.getKey())) return e.getValue();
            }
            return 4.0; // Nội thành Hải Phòng mặc định ~4km
        }

        // Nếu ở TP. Hồ Chí Minh
        if (pNorm.contains("ho chi minh") || pNorm.contains("hcm") || pNorm.contains("sai gon") || aNorm.contains("ho chi minh")) {
            return 1710.0;
        }

        // Tìm theo tỉnh thành (tính cự ly từ Hải Phòng)
        for (Map.Entry<String, Double> e : PROVINCE_DISTANCES.entrySet()) {
            if (pNorm.contains(e.getKey()) || aNorm.contains(e.getKey())) {
                return e.getValue();
            }
        }

        // Mặc định liên tỉnh nếu không xác định được
        return 120.0;
    }

    /**
     * Tính toán bảng cước chi tiết của tất cả các đơn vị vận chuyển dựa theo khoảng cách
     */
    public ShippingCalculationResult calculateQuotes(String province, String district, String ward, String address, long orderAmount, int weightGram) {
        return calculateQuotes(province, district, ward, address, orderAmount, weightGram, false);
    }

    public ShippingCalculationResult calculateQuotes(String province, String district, String ward, String address, long orderAmount, int weightGram, boolean hasHazardous) {
        double distKm = calculateDistanceKm(province, district, address);
        boolean isFreeShip = orderAmount >= FREE_SHIP_THRESHOLD;

        List<CarrierQuote> list = new ArrayList<>();

        // 1. Giao Hàng Nhanh (GHN) - Tiêu chuẩn
        long ghnRaw = computeGhnStandard(distKm);
        long ghnFinal = isFreeShip ? 0 : ghnRaw;
        var ghnEta = computeEta(distKm, "STANDARD");
        list.add(new CarrierQuote(
                "GHN",
                "Giao Hàng Nhanh (GHN)",
                "STANDARD",
                "GHN Tiêu chuẩn (Đường bộ)",
                "GHN",
                distKm,
                ghnRaw,
                ghnFinal,
                isFreeShip,
                ghnRaw - ghnFinal,
                formatEta(ghnEta[0], ghnEta[1]),
                ghnEta[0],
                ghnEta[1],
                "Vận chuyển đường bộ theo km, chống sốc mô hình xe"
        ));

        // 2. Giao Hàng Tiết Kiệm (GHTK) - Tiết kiệm
        long ghtkRaw = computeGhtkStandard(distKm);
        long ghtkFinal = isFreeShip ? 0 : ghtkRaw;
        var ghtkEta = computeEta(distKm, "STANDARD");
        list.add(new CarrierQuote(
                "GHTK",
                "Giao Hàng Tiết Kiệm (GHTK)",
                "STANDARD",
                "GHTK Tiết Kiệm",
                "GHTK",
                distKm,
                ghtkRaw,
                ghtkFinal,
                isFreeShip,
                ghtkRaw - ghtkFinal,
                formatEta(ghtkEta[0], ghtkEta[1]),
                ghtkEta[0],
                ghtkEta[1],
                "Cước phí tiết kiệm theo cự ly khoảng cách"
        ));

        // 3. Viettel Post
        long vtpRaw = computeViettelPost(distKm);
        long vtpFinal = isFreeShip ? 0 : vtpRaw;
        var vtpEta = computeEta(distKm, "STANDARD");
        list.add(new CarrierQuote(
                "VIETTEL_POST",
                "Viettel Post",
                "STANDARD",
                "Viettel Chuyển phát Tiết kiệm",
                "VIETTEL_POST",
                distKm,
                vtpRaw,
                vtpFinal,
                isFreeShip,
                vtpRaw - vtpFinal,
                formatEta(vtpEta[0], vtpEta[1]),
                vtpEta[0],
                vtpEta[1],
                "Phủ sóng 100% bưu cục toàn quốc, bảo hiểm hàng dễ vỡ"
        ));

        // 4. GHN Hỏa tốc (Express - Đường bay)
        long ghnExpRaw = computeGhnExpress(distKm);
        long ghnExpFinal = isFreeShip ? Math.max(0, ghnExpRaw - 30_000) : ghnExpRaw;
        var ghnExpEta = computeEta(distKm, "EXPRESS");
        list.add(new CarrierQuote(
                "GHN_EXPRESS",
                "Giao Hàng Nhanh (GHN Express)",
                "EXPRESS",
                "GHN Hỏa Tốc (Đường bay)",
                "GHN",
                distKm,
                ghnExpRaw,
                ghnExpFinal,
                ghnExpFinal == 0,
                ghnExpRaw - ghnExpFinal,
                formatEta(ghnExpEta[0], ghnExpEta[1]),
                ghnExpEta[0],
                ghnExpEta[1],
                "Ưu tiên bay hỏa tốc liên tỉnh nhận sớm"
        ));

        // 5. AhaMove Hỏa tốc nội thành (Chỉ hiển thị nếu khoảng cách <= 40 km)
        if (distKm <= 40.0) {
            long ahaRaw = computeAhamove(distKm);
            long ahaFinal = isFreeShip ? Math.max(0, ahaRaw - 25_000) : ahaRaw;
            var today = LocalDate.now();
            list.add(new CarrierQuote(
                    "AHAMOVE",
                    "AhaMove Siêu Tốc 2H",
                    "INSTANT",
                    "AhaMove Giao ngay trong 2 giờ",
                    "AHAMOVE",
                    distKm,
                    ahaRaw,
                    ahaFinal,
                    ahaFinal == 0,
                    ahaRaw - ahaFinal,
                    "Nhận hàng trong 2 giờ hôm nay",
                    today,
                    today,
                    "Shipper giao trực tiếp bằng xe máy trong 2 giờ"
            ));
        }

        long hazFee = hasHazardous ? 25_000L : 0L;
        String hazNotice = hasHazardous 
                ? "Đơn hàng chứa sản phẩm pin LiPo / dễ cháy nổ. Đã áp dụng quy chuẩn đóng gói chuyên dụng chống cháy & vận chuyển đường bộ an toàn (+25.000đ)." 
                : "";

        return new ShippingCalculationResult(
                ORIGIN_NAME,
                ORIGIN_ADDRESS,
                ORIGIN_LAT,
                ORIGIN_LNG,
                province != null ? province : "Hồ Chí Minh",
                district != null ? district : "",
                address != null ? address : "",
                Math.round(distKm * 10.0) / 10.0,
                FREE_SHIP_THRESHOLD,
                isFreeShip,
                hasHazardous,
                hazFee,
                hazNotice,
                list
        );
    }

    /**
     * Biểu phí GHN Tiêu chuẩn tính theo km
     */
    public long computeGhnStandard(double d) {
        if (d <= 10.0) return 18_000;
        if (d <= 30.0) return 22_000 + Math.round((d - 10.0) * 400);
        if (d <= 100.0) return 28_000 + Math.round((d - 30.0) * 120);
        if (d <= 300.0) return 34_000 + Math.round((d - 100.0) * 60);
        if (d <= 1000.0) return 40_000 + Math.round((d - 300.0) * 18);
        return 48_000 + Math.round((d - 1000.0) * 8);
    }

    /**
     * Biểu phí GHN Hỏa tốc tính theo km
     */
    public long computeGhnExpress(double d) {
        if (d <= 30.0) return 36_000;
        if (d <= 300.0) return 58_000;
        return 75_000 + Math.round((d - 300.0) * 12);
    }

    /**
     * Biểu phí GHTK Tiết kiệm tính theo km
     */
    public long computeGhtkStandard(double d) {
        if (d <= 10.0) return 16_500;
        if (d <= 30.0) return 20_000 + Math.round((d - 10.0) * 350);
        if (d <= 100.0) return 26_000 + Math.round((d - 30.0) * 100);
        if (d <= 300.0) return 32_000 + Math.round((d - 100.0) * 50);
        if (d <= 1000.0) return 38_000 + Math.round((d - 300.0) * 15);
        return 45_000 + Math.round((d - 1000.0) * 7);
    }

    /**
     * Biểu phí Viettel Post tính theo km
     */
    public long computeViettelPost(double d) {
        if (d <= 15.0) return 18_000;
        if (d <= 50.0) return 24_000 + Math.round((d - 15.0) * 200);
        if (d <= 300.0) return 32_000 + Math.round((d - 50.0) * 60);
        if (d <= 1000.0) return 42_000 + Math.round((d - 300.0) * 18);
        return 50_000 + Math.round((d - 1000.0) * 9);
    }

    /**
     * Biểu phí AhaMove tính trực tiếp theo số km
     */
    public long computeAhamove(double d) {
        if (d <= 4.0) return 22_000;
        return 22_000 + Math.round((d - 4.0) * 5_000);
    }

    /**
     * Tính toán thời gian dự kiến nhận hàng theo khoảng cách
     */
    public LocalDate[] computeEta(double distKm, String method) {
        boolean express = "EXPRESS".equalsIgnoreCase(method);
        int daysMin;
        int daysMax;
        if (express) {
            daysMin = distKm <= 50.0 ? 1 : (distKm <= 500.0 ? 1 : 2);
            daysMax = daysMin + 1;
        } else {
            if (distKm <= 20.0) {
                daysMin = 1; daysMax = 2;
            } else if (distKm <= 150.0) {
                daysMin = 2; daysMax = 3;
            } else if (distKm <= 600.0) {
                daysMin = 2; daysMax = 4;
            } else {
                daysMin = 3; daysMax = 5;
            }
        }
        LocalDate now = LocalDate.now();
        return new LocalDate[]{ now.plusDays(daysMin), now.plusDays(daysMax) };
    }

    private String formatEta(LocalDate from, LocalDate to) {
        if (from.equals(to)) {
            return from.getDayOfMonth() + "/" + from.getMonthValue();
        }
        return from.getDayOfMonth() + "/" + from.getMonthValue() + " – " + to.getDayOfMonth() + "/" + to.getMonthValue();
    }
}
