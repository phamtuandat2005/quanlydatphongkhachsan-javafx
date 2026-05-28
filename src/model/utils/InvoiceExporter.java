package model.utils;

import model.entities.HoaDon;
import model.entities.DichVuSuDung;
import dao.DichVuSuDungDAO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InvoiceExporter {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        public static String exportToHTML(HoaDon hd, List<Object[]> dsPhong) {
                try {
                        StringBuilder html = new StringBuilder();
                        html.append("<!DOCTYPE html>\n<html lang='vi'>\n<head>\n");
                        html.append("<meta charset='UTF-8'>\n");
                        html.append("<title>Hóa đơn " + hd.getMaHD() + "</title>\n");
                        html.append("<style>\n");
                        html.append(
                                        "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; line-height: 1.6; padding: 40px; background: #f4f7f6; }\n");
                        html.append(
                                        ".invoice-box { max-width: 800px; margin: auto; padding: 30px; border: 1px solid #eee; background: #fff; box-shadow: 0 0 10px rgba(0,0,0,0.15); border-radius: 8px; }\n");
                        html.append(
                                        ".header { text-align: center; border-bottom: 2px solid #2563eb; padding-bottom: 20px; margin-bottom: 20px; }\n");
                        html.append(".header h1 { color: #2563eb; margin: 0; font-size: 28px; }\n");
                        html.append(".info-section { display: flex; justify-content: space-between; margin-bottom: 30px; }\n");
                        html.append(".info-col { width: 48%; }\n");
                        html.append(
                                        ".info-col h3 { border-bottom: 1px solid #eee; padding-bottom: 5px; margin-bottom: 10px; color: #666; font-size: 16px; }\n");
                        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }\n");
                        html.append(
                                        "table th { background: #f8fafc; color: #475569; text-align: left; padding: 12px; border-bottom: 2px solid #e2e8f0; }\n");
                        html.append("table td { padding: 12px; border-bottom: 1px solid #f1f5f9; }\n");
                        html.append(
                                        ".total-section { text-align: right; margin-top: 30px; border-top: 2px solid #eee; padding-top: 20px; }\n");
                        html.append(".total-row { display: flex; justify-content: flex-end; margin-bottom: 5px; }\n");
                        html.append(".total-label { font-weight: bold; width: 220px; }\n");
                        html.append(".total-value { width: 150px; text-align: right; }\n");
                        html.append(".grand-total { font-size: 20px; color: #2563eb; margin-top: 10px; }\n");
                        html.append(".footer { text-align: center; margin-top: 50px; color: #94a3b8; font-size: 12px; }\n");
                        html.append("</style>\n</head>\n<body>\n");

                        html.append("<div class='invoice-box'>\n");
                        html.append(
                                        "<div class='header'>\n<h1>LUCIA HOTEL</h1>\n<p>Hệ thống quản lý khách sạn chuyên nghiệp</p>\n</div>\n");

                        html.append("<div class='info-section'>\n");
                        html.append("<div class='info-col'>\n<h3>THÔNG TIN HÓA ĐƠN</h3>\n");
                        html.append("<p><b>Mã hóa đơn:</b> " + hd.getMaHD() + "</p>\n");
                        if (hd.getDatPhong() != null) {
                                java.time.LocalDateTime actualCheckIn = hd.getDatPhong().getNgayCheckIn();
                                java.time.LocalDateTime plannedCheckOut = hd.getDatPhong().getNgayCheckOut();
                                java.time.LocalDateTime actualCheckOut = hd.getNgayTaoHD();

                                java.time.LocalDateTime plannedCheckIn = actualCheckIn != null
                                                ? actualCheckIn.toLocalDate().atTime(14, 0)
                                                : null;
                                java.time.LocalDateTime expectedCheckOut = plannedCheckOut != null
                                                ? plannedCheckOut.toLocalDate().atTime(12, 0)
                                                : null;

                                html.append("<p><b>Ngày nhận dự kiến:</b> "
                                                + (plannedCheckIn != null ? plannedCheckIn.format(FMT) : "—")
                                                + "</p>\n");
                                html.append("<p><b>Ngày trả dự kiến:</b> "
                                                + (expectedCheckOut != null ? expectedCheckOut.format(FMT) : "—")
                                                + "</p>\n");
                                html.append("<p><b>Ngày nhận thực tế:</b> "
                                                + (actualCheckIn != null ? actualCheckIn.format(FMT) : "—") + "</p>\n");
                                html.append("<p><b>Ngày trả thực tế (Checkout):</b> "
                                                + (actualCheckOut != null ? actualCheckOut.format(FMT) : "—")
                                                + "</p>\n");
                        }
                        html.append("<p><b>Mã nhân viên lập:</b> "
                                        + (hd.getNhanVien() != null ? hd.getNhanVien().getMaNV() : "—")
                                        + "</p>\n");
                        html.append("<p><b>Tên nhân viên lập:</b> "
                                        + (hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "System")
                                        + "</p>\n");
                        html.append("</div>\n");

                        html.append("<div class='info-col'>\n<h3>THÔNG TIN KHÁCH HÀNG</h3>\n");
                        if (hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null) {
                                html.append("<p><b>Khách hàng:</b> " + hd.getDatPhong().getKhachHang().getTenKH()
                                                + "</p>\n");
                                html.append("<p><b>Số điện thoại:</b> " + hd.getDatPhong().getKhachHang().getSoDT()
                                                + "</p>\n");
                                html.append("<p><b>Số CCCD:</b> " + hd.getDatPhong().getKhachHang().getSoCCCD()
                                                + "</p>\n");
                        }
                        html.append("</div>\n</div>\n");

                        html.append(
                                        "<h3>DANH SÁCH PHÒNG</h3>\n<table>\n<thead>\n<tr>\n<th>Phòng</th>\n<th>Loại phòng</th>\n<th>Thời gian</th>\n<th>Tiền phòng</th>\n<th>Phí phụ thu</th>\n<th>Phí trả muộn</th>\n</tr>\n</thead>\n<tbody>\n");
                        for (Object[] row : dsPhong) {
                                double roomPhuThu = (double) row[7];
                                double roomPhuPhiTraMuon = (double) row[8];
                                html.append("<tr>\n");
                                html.append("<td>" + row[0] + " - " + row[1] + "</td>\n");
                                html.append("<td>" + row[2] + "</td>\n");
                                html.append("<td>" + row[3] + " đêm</td>\n");
                                html.append("<td>" + String.format("%,.0f đ", (Double) row[4]) + "</td>\n");
                                html.append("<td>" + String.format("%,.0f đ", roomPhuThu) + "</td>\n");
                                html.append("<td style='color: #ef4444;'>" + String.format("%,.0f đ", roomPhuPhiTraMuon)
                                                + "</td>\n");
                                html.append("</tr>\n");
                        }
                        html.append("</tbody>\n</table>\n");

                        // Dịch vụ
                        DichVuSuDungDAO dvsdDAO = new DichVuSuDungDAO();
                        html.append("<h3>CHI TIẾT DỊCH VỤ THEO PHÒNG</h3>\n");
                        for (Object[] row : dsPhong) {
                                String maPhong = (String) row[0];
                                String tenPhong = (String) row[1];
                                String maCTDP = (String) row[6];
                                double tienPhong = (double) row[4];
                                double phuThuPhong = (double) row[7];
                                double phuPhiTraMuonPhong = (double) row[8];

                                List<DichVuSuDung> listDV = dvsdDAO.findByMaCTDP(maCTDP);

                                html.append("<h4 style='color: #475569; margin-top: 15px; margin-bottom: 5px;'>Phòng: "
                                                + maPhong + " - " + tenPhong + "</h4>\n");
                                html.append("<table>\n<thead>\n<tr>\n<th>Tên dịch vụ</th>\n<th>Đơn giá</th>\n<th>Số lượng</th>\n<th>Thành tiền</th>\n</tr>\n</thead>\n<tbody>\n");
                                double tongTienDVPhong = 0;

                                if (listDV != null && !listDV.isEmpty()) {
                                        for (DichVuSuDung dv : listDV) {
                                                double thanhTienDV = dv.getGiaDV() * dv.getSoLuong();
                                                tongTienDVPhong += thanhTienDV;
                                                html.append("<tr>\n");
                                                html.append("<td>"
                                                                + (dv.getDichVu() != null ? dv.getDichVu().getTenDV()
                                                                                : "—")
                                                                + "</td>\n");
                                                html.append("<td>" + String.format("%,.0f đ", dv.getGiaDV())
                                                                + "</td>\n");
                                                html.append("<td style='text-align: center;'>" + dv.getSoLuong()
                                                                + "</td>\n");
                                                html.append("<td style='text-align: right;'>"
                                                                + String.format("%,.0f đ", thanhTienDV)
                                                                + "</td>\n");
                                                html.append("</tr>\n");
                                        }
                                } else {
                                        html.append("<tr>\n<td colspan='4' style='text-align: center; color: #999; font-style: italic;'>Không có dịch vụ</td>\n</tr>\n");
                                }

                                html.append("</tbody>\n");
                                html.append("<tfoot>\n<tr>\n");
                                html.append("<td colspan='3' style='text-align: right; font-weight: bold; color: #475569;'>Tổng tiền dịch vụ:</td>\n");
                                html.append("<td style='text-align: right; font-weight: bold; color: #2563eb;'>"
                                                + String.format("%,.0f đ", tongTienDVPhong) + "</td>\n");
                                html.append("</tr>\n");

                                double tongCuaPhong = (tienPhong + tongTienDVPhong + phuThuPhong + phuPhiTraMuonPhong)
                                                * (1 + hd.getThueVAT());
                                html.append("<tr>\n");
                                html.append("<td colspan='3' style='text-align: right; font-weight: bold; color: #1e293b;'>Tổng cộng phòng "
                                                + maPhong + ":</td>\n");
                                html.append("<td style='text-align: right; font-weight: bold; color: #2563eb;'>"
                                                + String.format("%,.0f đ", tongCuaPhong) + "</td>\n");
                                html.append("</tr>\n");

                                html.append("</tfoot>\n");
                                html.append("</table>\n");
                        }

                        double phuPhiTraMuon = hd.getPhuPhiTraMuon();
                        double phuThu = hd.getPhuThu();
                        double vatAmount = (hd.getTienPhong() + hd.getTienDV() + phuPhiTraMuon + phuThu)
                                        * hd.getThueVAT();

                        html.append("<div class='total-section'>\n");
                        html.append("<div class='total-row'><div class='total-label'>Tiền phòng:</div><div class='total-value'>"
                                        + String.format("%,.0f đ", hd.getTienPhong()) + "</div></div>\n");
                        html.append("<div class='total-row'><div class='total-label'>Tiền dịch vụ:</div><div class='total-value'>"
                                        + String.format("%,.0f đ", hd.getTienDV()) + "</div></div>\n");
                        html.append("<div class='total-row'><div class='total-label'>Phụ phí trả muộn:</div><div class='total-value' style='color: #ef4444;'>"
                                        + String.format("%,.0f đ", phuPhiTraMuon) + "</div></div>\n");
                        html.append("<div class='total-row'><div class='total-label'>Phí phụ thu:</div><div class='total-value'>"
                                        + String.format("%,.0f đ", phuThu) + "</div></div>\n");
                        html.append("<div class='total-row'><div class='total-label'>Tiền cọc (đã khấu trừ):</div><div class='total-value' style='color: #16a34a;'>- "
                                        + String.format("%,.0f đ", hd.getTienCoc()) + "</div></div>\n");
                        html.append("<div class='total-row'><div class='total-label'>Thuế VAT ("
                                        + String.format("%.0f", hd.getThueVAT() * 100)
                                        + "%):</div><div class='total-value'>"
                                        + String.format("%,.0f đ", vatAmount)
                                        + "</div></div>\n");
                        double totalPaid = hd.getTongDaTra();
                        html.append(
                                        "<div class='total-row grand-total'><div class='total-label'>TỔNG ĐÃ THANH TOÁN:</div><div class='total-value'>"
                                                        + String.format("%,.0f đ", totalPaid) + "</div></div>\n");
                        html.append("</div>\n");

                        html.append(
                                        "<div class='footer'>\n<p>Cảm ơn quý khách đã tin tưởng và sử dụng dịch vụ tại Lucia Hotel!</p>\n<p>Hẹn gặp lại quý khách lần sau.</p>\n</div>\n");
                        html.append("</div>\n</body>\n</html>");

                        // Dùng đường dẫn tuyệt đối từ user.dir để tránh lỗi thư mục làm việc
                        String baseDir = System.getProperty("user.dir");
                        File folder = new File(baseDir, "Hoa_Don");
                        if (!folder.exists()) {
                                folder.mkdirs();
                        }

                        String fileName = hd.getMaHD() + ".html";
                        File file = new File(folder, fileName);

                        // Dùng OutputStreamWriter với UTF-8 thay vì FileWriter (dùng charset mặc định
                        // hệ thống)
                        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                                        StandardCharsets.UTF_8)) {
                                writer.write(html.toString());
                                return file.getAbsolutePath();
                        }
                } catch (Exception e) {
                        System.err.println("Lỗi xuất hóa đơn HTML: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }
}
