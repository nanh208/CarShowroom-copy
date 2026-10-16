package com.ares.shop.service;

import com.ares.shop.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Nhãn vận chuyển GHN / GHTK khổ A6 (105×148mm) dạng PDF, font DejaVu (đủ dấu tiếng Việt), mã vạch Code128. */
@Service
public class LabelPdfService {
    static final float W = 297.6f, H = 419.5f; // A6 pt
    final BaseFont reg, bold;
    public LabelPdfService() throws Exception {
        reg = BaseFont.createFont("fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes("fonts/DejaVuSans.ttf"), null);
        bold = BaseFont.createFont("fonts/DejaVuSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes("fonts/DejaVuSans-Bold.ttf"), null);
    }
    static byte[] bytes(String r) throws Exception { try (var in = LabelPdfService.class.getClassLoader().getResourceAsStream(r)) { return in.readAllBytes(); } }

    public record Label(CustomerOrder order, String carrier, String trackingNo, String service, int weightGrams, long cod, String sortCode) {}

    public byte[] render(List<Label> labels, String shopName, String shopPhone, String shopAddress) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(new Rectangle(W, H), 0, 0, 0, 0);
        PdfWriter w = PdfWriter.getInstance(doc, out); doc.addTitle("Shipping labels"); doc.addCreator("ARES Toys"); doc.open();
        for (int i = 0; i < labels.size(); i++) { if (i > 0) doc.newPage(); draw(w.getDirectContent(), labels.get(i), shopName, shopPhone, shopAddress); }
        if (labels.isEmpty()) { doc.add(new Paragraph(" ")); }
        doc.close(); return out.toByteArray();
    }

    void text(PdfContentByte c, BaseFont f, float size, float x, float y, String s) { c.beginText(); c.setFontAndSize(f, size); c.setTextMatrix(x, y); c.showText(s == null ? "" : s); c.endText(); }
    /** Ngắt dòng đơn giản theo độ rộng; trả về y sau khi in. */
    float wrap(PdfContentByte c, BaseFont f, float size, float x, float y, float maxW, String s, int maxLines) {
        if (s == null) return y; String[] words = s.split("\\s+"); StringBuilder line = new StringBuilder(); int n = 0;
        for (String wd : words) {
            String t = line.isEmpty() ? wd : line + " " + wd;
            if (f.getWidthPoint(t, size) > maxW && !line.isEmpty()) { text(c, f, size, x, y, line.toString()); y -= size + 2; n++; line = new StringBuilder(wd); if (n >= maxLines) return y; }
            else line = new StringBuilder(t);
        }
        if (!line.isEmpty() && n < maxLines) { text(c, f, size, x, y, line.toString()); y -= size + 2; }
        return y;
    }
    void barcode(PdfContentByte c, String code, float x, float y, float bw, float bh, boolean showText) {
        Barcode128 b = new Barcode128(); b.setCode(code); b.setBarHeight(bh); b.setX(0.9f); if (!showText) b.setFont(null); else { b.setFont(reg); b.setSize(7); }
        Image img = b.createImageWithBarcode(c, Color.BLACK, Color.BLACK); float sc = Math.min(1f, bw / img.getWidth()); img.scaleAbsolute(img.getWidth() * sc, img.getHeight());
        img.setAbsolutePosition(x + (bw - img.getScaledWidth()) / 2, y); try { c.addImage(img); } catch (DocumentException e) { throw new RuntimeException(e); }
    }

    void draw(PdfContentByte c, Label l, String shopName, String shopPhone, String shopAddress) {
        CustomerOrder o = l.order(); boolean ghtk = "GHTK".equals(l.carrier()); float m = 10, x2 = W - m;
        Color brand = ghtk ? new Color(0, 133, 62) : new Color(242, 101, 34);
        // khung
        c.setLineWidth(1f); c.rectangle(m / 2, m / 2, W - m, H - m); c.stroke();
        // header hãng
        c.setColorFill(brand); c.rectangle(m / 2, H - 42, W - m, 37.5f); c.fill(); c.setColorFill(Color.WHITE);
        text(c, bold, ghtk ? 13 : 17, m + 2, H - 30, ghtk ? "GIAO HÀNG TIẾT KIỆM" : "GHN EXPRESS");
        text(c, reg, 7, m + 2, H - 39, ghtk ? "ghtk.vn · 1900 6092" : "ghn.vn · 1900 636677");
        c.beginText(); c.setFontAndSize(bold, 12); c.showTextAligned(Element.ALIGN_RIGHT, l.sortCode(), x2 - 2, H - 30, 0); c.endText();
        c.beginText(); c.setFontAndSize(reg, 7); c.showTextAligned(Element.ALIGN_RIGHT, "STANDARD".equals(l.service()) ? "Chuẩn" : "Hoả tốc", x2 - 2, H - 39, 0); c.endText();
        c.setColorFill(Color.BLACK);
        // mã vận đơn
        barcode(c, l.trackingNo(), m, H - 92, W - 2 * m, 40, false);
        c.beginText(); c.setFontAndSize(bold, 12); c.showTextAligned(Element.ALIGN_CENTER, l.trackingNo(), W / 2, H - 105, 0); c.endText();
        line(c, H - 112);
        // gửi / nhận
        float y = H - 124;
        text(c, bold, 7, m, y, "TỪ (NGƯỜI GỬI)"); y -= 10; text(c, bold, 8.5f, m, y, shopName + " · " + shopPhone); y -= 10; y = wrap(c, reg, 7.5f, m, y, W - 2 * m, shopAddress, 2);
        line(c, y + 2); y -= 10;
        text(c, bold, 7, m, y, "ĐẾN (NGƯỜI NHẬN)"); y -= 13; text(c, bold, 11, m, y, o.buyerName); y -= 12; text(c, bold, 10, m, y, mask(o.phone)); y -= 12;
        y = wrap(c, reg, 8.5f, m, y, W - 2 * m, o.shippingAddress + (o.city != null && (o.shippingAddress == null || !o.shippingAddress.contains(o.city)) ? ", " + o.city : ""), 3);
        line(c, y + 2); y -= 4;
        // ô thông tin
        float bx = m, bw = (W - 2 * m) / 3, top = y, bh = 34;
        String[][] cells = {{"THU HỘ COD", vnd(l.cod())}, {"KHỐI LƯỢNG", l.weightGrams() >= 1000 ? String.format("%.1f kg", l.weightGrams() / 1000.0) : l.weightGrams() + " g"}, {"NGÀY TẠO", o.createdAt == null ? "" : o.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}};
        for (int i = 0; i < 3; i++) { c.rectangle(bx + i * bw, top - bh, bw, bh); c.stroke(); text(c, reg, 6.5f, bx + i * bw + 4, top - 10, cells[i][0]); text(c, bold, i == 0 ? 11 : 9.5f, bx + i * bw + 4, top - 26, cells[i][1]); }
        y = top - bh - 11;
        // nội dung hàng
        text(c, bold, 7, m, y, "NỘI DUNG HÀNG (" + o.items.stream().mapToInt(i -> i.quantity).sum() + " sp)"); y -= 10;
        int shown = 0;
        for (OrderItem it : o.items) { if (shown == 4) { text(c, reg, 7.5f, m, y, "… và " + (o.items.size() - 4) + " mặt hàng khác"); y -= 10; break; }
            y = wrap(c, reg, 7.5f, m, y, W - 2 * m, it.quantity + " × " + it.productName + (it.variantName != null ? " (" + it.variantName + ")" : ""), 1); shown++; }
        // chân: mã đơn + ghi chú
        float fy = 40; line(c, fy + 22);
        barcode(c, o.code, m, fy - 6, 130, 22, true);
        float ny = fy + 12; ny = wrap(c, bold, 7, 150, ny, x2 - 150, "Cho xem hàng, không cho thử", 1);
        ny = wrap(c, reg, 6.5f, 150, ny, x2 - 150, o.note == null || o.note.isBlank() ? "Hàng dễ vỡ — nhẹ tay" : "Ghi chú: " + o.note, 2);
        if (Boolean.TRUE.equals(o.giftWrap)) wrap(c, bold, 6.5f, 150, ny, x2 - 150, "GÓI QUÀ", 1);
    }
    void line(PdfContentByte c, float y) { c.setLineWidth(0.6f); c.moveTo(10, y); c.lineTo(W - 10, y); c.stroke(); }
    static String vnd(long v) { return String.format("%,d", v).replace(',', '.') + "đ"; }
    /** Che bớt SĐT trên nhãn (bảo vệ dữ liệu cá nhân): 0901***678 */
    static String mask(String p) { if (p == null || p.length() < 7) return p; return p.substring(0, 4) + "***" + p.substring(p.length() - 3); }
}
