package com.ares.shop.web;

import com.ares.shop.model.*;
import com.ares.shop.repo.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Trợ lý ảo (rule/keyword-based, tiếng Việt). Thứ tự xử lý: mã đơn → mã giảm giá → ý định do admin cấu hình
 * → tìm sản phẩm → không hiểu (gợi ý gặp nhân viên). Mọi lượt hỏi được ghi BotLog để admin xem lại.
 */
@RestController
public class BotController {
    private final ChatIntentRepo intents; private final BotLogRepo logs; private final ProductRepo products; private final PromotionRepo promos;
    private final OrderRepo orders; private final TicketRepo tickets; private final ChatRepo chats;
    public BotController(ChatIntentRepo intents, BotLogRepo logs, ProductRepo products, PromotionRepo promos, OrderRepo orders, TicketRepo tickets, ChatRepo chats) {
        this.intents = intents; this.logs = logs; this.products = products; this.promos = promos; this.orders = orders; this.tickets = tickets; this.chats = chats;
    }

    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd').replaceAll("[^a-z0-9:/ ]", " ").replaceAll("\\s+", " ").trim();
    }
    static final List<String> DEFAULT_QUICK = List.of("Tra cứu đơn hàng", "Mã giảm giá", "Phí ship", "Đổi trả", "Gợi ý quà tặng", "Gặp nhân viên");
    public static final Set<String> STOP = Set.of("cho", "toi", "minh", "ban", "shop", "co", "khong", "la", "gi", "nao", "the", "voi", "mot", "can", "muon", "tim", "mua", "xem", "ve", "cua", "va", "hay", "duoc", "nhe", "a", "oi", "em", "anh", "chi", "hoi", "o", "dau", "bao", "nhieu", "gia", "san", "pham", "loai");
    static final Pattern ORDER = Pattern.compile("\\b([a-z]{2}\\d{8,})\\b");
    static final Pattern PHONE = Pattern.compile("\\b(0\\d{9})\\b");

    public record Ask(String text) {}
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.I18nService i18n;
    /** Câu hỏi tiếng Anh → bổ sung cụm tiếng Việt tương đương để khớp ý định/từ khoá (giữ nguyên câu gốc). */
    static final Map<String, String> EN = new LinkedHashMap<>();
    static {
        String[][] m = {{"discount code", "ma giam"}, {"discount", "giam gia"}, {"promo", "khuyen mai"}, {"coupon", "coupon"}, {"track order", "tra cuu don"}, {"track my order", "tra cuu don"},
            {"order status", "trang thai don"}, {"my order", "don cua toi"}, {"talk to staff", "gap nhan vien"}, {"talk to a person", "gap nhan vien"}, {"human", "nhan vien"}, {"agent", "nhan vien"}, {"staff", "nhan vien"},
            {"shipping fee", "phi ship"}, {"shipping", "phi ship"}, {"delivery time", "thoi gian giao"}, {"how long", "bao lau"}, {"return", "doi tra"}, {"refund", "hoan tien"}, {"exchange", "doi hang"},
            {"payment", "thanh toan"}, {"installment", "tra gop"}, {"instalment", "tra gop"}, {"warranty", "bao hanh"}, {"repair", "sua chua"}, {"spare part", "linh kien"}, {"scale", "ti le"}, {"size", "kich thuoc"},
            {"gift card", "the qua"}, {"gift idea", "goi y qua tang"}, {"gift", "qua tang"}, {"birthday", "sinh nhat"}, {"kid", "tre em"}, {"points", "tich diem"}, {"reward", "diem"}, {"membership", "hang thanh vien"},
            {"pre order", "dat truoc"}, {"preorder", "dat truoc"}, {"coming soon", "sap ra mat"}, {"store", "cua hang"}, {"address", "dia chi"}, {"opening hours", "gio mo cua"}, {"hotline", "hotline"}, {"phone number", "so dien thoai"},
            {"hello", "xin chao"}, {"hi", "chao"}, {"rc car", "xe rc"}, {"remote control", "dieu khien"}, {"car", "xe"}, {"cars", "xe"}, {"battery", "pin"}, {"charger", "sac"}, {"under", "duoi"}, {"see more products", "xem them san pham"}};
        for (String[] x : m) EN.put(x[0], x[1]);
    }
    static String enrich(String n) {
        StringBuilder extra = new StringBuilder();
        for (var e : EN.entrySet()) if ((" " + n + " ").contains(" " + e.getKey() + " ")) extra.append(' ').append(e.getValue());
        return extra.length() == 0 ? n : n + extra;
    }
    private List<String> quick(String lang, List<String> q) { return q.stream().map(x -> i18n.bot(lang, x)).toList(); }

    @PostMapping("/api/public/bot/{session}") @Transactional
    public Map<String, Object> ask(@PathVariable String session, @RequestBody Ask a, @RequestHeader(value = "Accept-Language", required = false) String al) {
        String lang = com.ares.shop.service.I18nService.langOf(al);
        String q = Optional.ofNullable(a.text()).orElse("").trim();
        if (q.isEmpty() || q.length() > 500) throw ApiException.bad("Câu hỏi trống hoặc quá dài");
        if (session.length() > 64) throw ApiException.bad("Phiên không hợp lệ");
        String n = enrich(norm(q)); User u = Current.userOrNull();
        Map<String, Object> r = new LinkedHashMap<>(); r.put("quick", quick(lang, DEFAULT_QUICK)); r.put("lang", lang); String intent = null;

        Matcher om = ORDER.matcher(n);
        boolean wantsHuman = has(n, "nhan vien", "nguoi that", "tu van vien", "gap nguoi", "khieu nai");
        if (ai.enabled() && !wantsHuman) {
            try {
                List<String[]> hist = new ArrayList<>(); var past = logs.findTop100BySessionOrderByIdAsc(session);
                for (BotLog b : past.subList(Math.max(0, past.size() - 6), past.size())) hist.add(new String[]{b.question, b.answer});
                var rep = ai.chat(q, hist, lang, u);
                intent = "ai"; r.put("text", rep.text()); r.put("ai", true); r.put("tools", rep.toolsUsed());
                if (!rep.products().isEmpty()) r.put("products", rep.products());
                if (!rep.coupons().isEmpty()) r.put("coupons", rep.coupons());
                if (rep.order() != null) r.put("order", rep.order());
            } catch (Exception e) { r.put("aiError", e.getClass().getSimpleName()); } // rơi về bot từ khoá
        }
        if ("ai".equals(intent)) { /* đã trả lời bằng AI */ }
        else if (om.find()) { intent = "order"; orderLookup(om.group(1).toUpperCase(), n, u, r, lang); }
        else if (has(n, "ma giam", "giam gia", "coupon", "voucher", "khuyen mai", "ma uu dai", "sale")) {
            intent = "coupon";
            var list = promos.findByActiveTrue().stream().filter(p -> p.isPublic && p.isValidNow()).limit(5).map(p -> {
                Map<String, Object> m = new LinkedHashMap<>(); m.put("code", p.code); m.put("title", p.title);
                m.put("value", p.kind == Enums.VoucherKind.PERCENT ? i18n.bot(lang, "Giảm {0}%", p.discountPercent) : i18n.bot(lang, "Giảm {0}", com.ares.shop.service.MailService.vnd(p.amount)));
                m.put("minOrder", p.minOrder); m.put("endDate", p.endDate); return m; }).toList();
            r.put("text", i18n.bot(lang, list.isEmpty() ? "Hiện chưa có mã giảm giá công khai. Bạn đăng ký nhận tin để được báo sớm nhé!" : "Đây là các mã đang dùng được — chạm để sao chép. Ở trang thanh toán, hệ thống cũng tự áp mã có lợi nhất cho bạn."));
            r.put("coupons", list);
        } else if (has(n, "tra cuu don", "don hang cua toi", "kiem tra don", "don cua toi", "trang thai don")) {
            intent = "order-help"; r.put("text", i18n.bot(lang, "Gõ mã đơn (VD: TS2610151234…) kèm số điện thoại đặt hàng để mình tra cứu. Nếu đã đăng nhập, chọn đơn ngay bên dưới."));
            r.put("askOrder", true);
        } else if (has(n, "nhan vien", "nguoi that", "tu van vien", "gap nguoi", "support", "khieu nai")) {
            intent = "handoff"; r.put("text", i18n.bot(lang, "Mình sẽ chuyển bạn tới nhân viên ARES. Bấm “Chuyển nhân viên” để gửi kèm nội dung đã trao đổi.")); r.put("handoff", true);
        } else {
            ChatIntent best = null; int bestScore = 0;
            for (ChatIntent it : intents.findAllByOrderBySortOrderAscIdAsc()) {
                if (!Boolean.TRUE.equals(it.active) || it.keywords == null) continue;
                int sc = 0; for (String k : it.keywords.split(",")) { String kk = norm(k); if (!kk.isEmpty() && (" " + n + " ").contains(" " + kk + " ") || (!kk.isEmpty() && kk.contains(" ") && n.contains(kk))) sc += kk.split(" ").length; }
                if (sc > bestScore) { bestScore = sc; best = it; }
            }
            var ent = searchSvc.parse(n);
            List<Map<String, Object>> entHits = (ent.scale() != null || ent.brand() != null) ? search(n, true) : List.of();
            if (!entHits.isEmpty()) {
                intent = "product-search"; r.put("products", entHits);
                r.put("text", i18n.bot(lang, "Mình tìm thấy {0} mẫu", entHits.size()) + (ent.brand() != null ? " " + ent.brand() : "") + (ent.scale() != null ? i18n.bot(lang, " tỉ lệ {0}", ent.scale()) : "")
                    + (ent.terms().isEmpty() ? "" : i18n.bot(lang, " khớp “{0}”", String.join(" ", ent.terms()))) + ":");
                r.put("entities", Map.of("scale", String.valueOf(ent.scale()), "brand", String.valueOf(ent.brand()), "terms", ent.terms()));
                r.put("quick", quick(lang, List.of("Xem thêm sản phẩm", "Mã giảm giá", "Phí ship")));
            } else if (best != null) {
                intent = best.name; best.hits = Optional.ofNullable(best.hits).orElse(0) + 1; intents.save(best);
                boolean en = "en".equals(lang);
                r.put("text", en && best.answerEn != null && !best.answerEn.isBlank() ? best.answerEn : i18n.bot(lang, best.answer));
                if (en && best.quickRepliesEn != null && !best.quickRepliesEn.isBlank()) r.put("quick", Arrays.stream(best.quickRepliesEn.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
                else if (best.quickReplies != null && !best.quickReplies.isBlank()) r.put("quick", quick(lang, Arrays.stream(best.quickReplies.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()));
            } else {
                var found = search(n, false);
                if (!found.isEmpty()) { intent = "product-search"; r.put("text", i18n.bot(lang, "Mình tìm thấy {0} sản phẩm phù hợp:", found.size())); r.put("products", found); }
                else { r.put("text", i18n.bot(lang, "Xin lỗi, mình chưa hiểu câu hỏi. Bạn thử diễn đạt khác, chọn gợi ý bên dưới, hoặc chuyển sang nhân viên nhé.")); r.put("handoff", true); }
            }
        }
        BotLog l = new BotLog(); l.session = session; l.userId = u == null ? null : u.id; l.question = q; l.intent = intent; l.matched = intent != null;
        l.answer = String.valueOf(r.get("text")); logs.save(l);
        r.put("intent", intent); r.put("id", l.id); return r;
    }
    private static boolean has(String n, String... ks) { for (String k : ks) if (n.contains(k)) return true; return false; }

    private void orderLookup(String code, String n, User u, Map<String, Object> r, String lang) {
        var o = orders.findByCode(code).orElse(null);
        Matcher pm = PHONE.matcher(n);
        boolean mine = o != null && u != null && o.user != null && o.user.id.equals(u.id);
        boolean phoneOk = o != null && pm.find() && pm.group(1).equals(o.phone);
        if (o == null) { r.put("text", i18n.bot(lang, "Không tìm thấy đơn {0}. Bạn kiểm tra lại mã đơn giúp mình nhé.", code)); return; }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("code", o.code); m.put("status", o.status); m.put("paymentStatus", o.paymentStatus);
        m.put("etaFrom", o.etaFrom); m.put("etaTo", o.etaTo); m.put("createdAt", o.createdAt);
        if (mine || phoneOk) { m.put("total", o.total); m.put("timeline", o.timeline); m.put("items", o.items.stream().map(i -> i.quantity + " × " + i.productName).toList()); }
        r.put("order", m);
        r.put("text", mine || phoneOk ? i18n.bot(lang, "Thông tin đơn {0}:", code) : i18n.bot(lang, "Trạng thái đơn {0} (gửi kèm số điện thoại đặt hàng để xem chi tiết):", code));
        r.put("quick", quick(lang, List.of("Đổi trả", "Gặp nhân viên")));
    }

    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.SearchService searchSvc;
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.AiService ai;
    @GetMapping("/api/admin/ai/status") public Map<String, Object> aiStatus() { return ai.status(); }
    @GetMapping("/api/public/ai/status") public Map<String, Object> aiPublic() { return Map.of("enabled", ai.enabled()); }
    private List<Map<String, Object>> search(String n, boolean strict) {
        var ps = searchSvc.parse(n);
        if (ps.terms().isEmpty() && ps.scale() == null && ps.brand() == null && ps.maxPrice() == null && ps.minPrice() == null && ps.age() == null) return List.of();
        return searchSvc.search(n, true, strict).stream().limit(4).map(p -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.id); m.put("name", p.name); m.put("price", p.price);
                m.put("image", p.images.isEmpty() ? "" : p.images.get(0)); m.put("stock", p.stock); m.put("scale", p.scale); return m; }).collect(Collectors.toList());
    }

    @org.springframework.beans.factory.annotation.Autowired org.springframework.messaging.simp.SimpMessagingTemplate wsT;
    @GetMapping("/api/public/bot/{session}")
    public List<BotLog> history(@PathVariable String session) { return logs.findTop100BySessionOrderByIdAsc(session); }

    /** Chuyển nhân viên: khách đăng nhập → tạo phiếu hỗ trợ kèm bản ghi; khách vãng lai → mở chat trực tiếp. */
    @PostMapping("/api/public/bot/{session}/handoff") @Transactional
    public Map<String, Object> handoff(@PathVariable String session, @RequestBody(required = false) Map<String, String> b) {
        var hist = logs.findTop100BySessionOrderByIdAsc(session);
        String transcript = hist.stream().skip(Math.max(0, hist.size() - 8)).map(l -> "Khách: " + l.question + "\nBot: " + l.answer).collect(Collectors.joining("\n"));
        if (transcript.isBlank()) transcript = "(chưa có hội thoại)";
        User u = Current.userOrNull();
        if (u != null) {
            Ticket t = new Ticket(); t.user = u; t.subject = "Chuyển từ trợ lý ảo: " + (hist.isEmpty() ? "cần hỗ trợ" : hist.get(hist.size() - 1).question);
            if (t.subject.length() > 120) t.subject = t.subject.substring(0, 120);
            t.category = "Trợ lý ảo"; t.code = "HT" + String.valueOf(System.currentTimeMillis()).substring(5);
            TicketMessage m = new TicketMessage(); m.ticket = t; m.author = u.fullName; m.content = "[Bản ghi trợ lý ảo]\n" + transcript; t.messages.add(m);
            tickets.save(t); return Map.of("ticket", t.code, "mode", "ticket");
        }
        ChatMessage c = new ChatMessage(); c.sessionId = session; c.customerName = b == null ? null : b.get("name");
        c.content = "[Từ trợ lý ảo]\n" + (transcript.length() > 1900 ? transcript.substring(transcript.length() - 1900) : transcript); chats.save(c);
        wsT.convertAndSend("/topic/admin/chat", Map.of("sessionId", c.sessionId, "message", c)); wsT.convertAndSend("/topic/chat/" + c.sessionId, c);
        return Map.of("mode", "chat");
    }

    // ===== Admin: cấu hình ý định + bản ghi hội thoại =====
    @GetMapping("/api/admin/bot/intents") public List<ChatIntent> list() { return intents.findAllByOrderBySortOrderAscIdAsc(); }
    @PostMapping("/api/admin/bot/intents") public ChatIntent create(@RequestBody ChatIntent i) { i.id = null; return intents.save(check(i)); }
    @PutMapping("/api/admin/bot/intents/{id}") public ChatIntent update(@PathVariable Long id, @RequestBody ChatIntent i) {
        ChatIntent old = intents.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy ý định")); i.id = id; i.hits = old.hits; return intents.save(check(i)); }
    @DeleteMapping("/api/admin/bot/intents/{id}") public void del(@PathVariable Long id) { intents.deleteById(id); }
    private ChatIntent check(ChatIntent i) {
        if (i.name == null || i.name.isBlank() || i.keywords == null || i.keywords.isBlank() || i.answer == null || i.answer.isBlank()) throw ApiException.bad("Cần tên, từ khoá và câu trả lời");
        i.updatedAt = LocalDateTime.now(); if (i.hits == null) i.hits = 0; if (i.active == null) i.active = true; return i;
    }
    /** Thử câu hỏi ở trang cấu hình (ghi vào phiên admin-test, không tính thống kê). */
    @PostMapping("/api/admin/bot/test")
    public Map<String, Object> test(@RequestBody Ask a, @RequestHeader(value = "Accept-Language", required = false) String al) { return ask("admin-test", a, al); }

    @GetMapping("/api/admin/bot/transcripts")
    public List<Map<String, Object>> transcripts() {
        Map<String, List<BotLog>> by = new LinkedHashMap<>();
        for (BotLog l : logs.findTop1000ByOrderByIdDesc()) if (!"admin-test".equals(l.session)) by.computeIfAbsent(l.session, k -> new ArrayList<>()).add(l);
        return by.entrySet().stream().map(e -> { var ls = new ArrayList<>(e.getValue()); Collections.reverse(ls);
            Map<String, Object> m = new LinkedHashMap<>(); m.put("session", e.getKey()); m.put("userId", ls.get(0).userId); m.put("count", ls.size());
            m.put("unmatched", ls.stream().filter(l -> !Boolean.TRUE.equals(l.matched)).count()); m.put("last", ls.get(ls.size() - 1).createdAt); m.put("messages", ls); return m; }).toList();
    }
    @GetMapping("/api/admin/bot/stats")
    public Map<String, Object> stats() {
        var all = logs.findTop1000ByOrderByIdDesc().stream().filter(l -> !"admin-test".equals(l.session)).toList();
        Map<String, Long> byIntent = all.stream().filter(l -> l.intent != null).collect(Collectors.groupingBy(l -> l.intent, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> unmatched = all.stream().filter(l -> !Boolean.TRUE.equals(l.matched)).collect(Collectors.groupingBy(l -> norm(l.question), Collectors.counting()));
        return Map.of("total", all.size(), "matchedRate", all.isEmpty() ? 0 : Math.round(all.stream().filter(l -> Boolean.TRUE.equals(l.matched)).count() * 100.0 / all.size()),
            "sessions", all.stream().map(l -> l.session).distinct().count(),
            "byIntent", byIntent.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).map(e -> Map.of("intent", e.getKey(), "count", e.getValue())).toList(),
            "unmatched", unmatched.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(15).map(e -> Map.of("q", e.getKey(), "count", e.getValue())).toList());
    }
}
