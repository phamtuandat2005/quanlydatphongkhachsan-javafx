package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.LocalDate;

import connectDatabase.ConnectDatabase;
import model.entities.DatPhong;
import model.entities.HoaDon;
import model.entities.NhanVien;
import model.utils.IdGenerator;

public class HoaDonDAO {

    /**
     * Lấy toàn bộ hóa đơn từ bảng HoaDon
     */
    public List<HoaDon> getAll() {
        List<HoaDon> dsHoaDon = new ArrayList<>();
        String sql = "SELECT * FROM HoaDon";
        Connection con = ConnectDatabase.getInstance().getConnection();
        try (Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dsHoaDon.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách hóa đơn: " + e.getMessage());
        }

        // Fetch NhanVien details after ResultSet is closed to avoid MARS exception
        NhanVienDAO nvDao = new NhanVienDAO();
        for (HoaDon hd : dsHoaDon) {
            if (hd.getNhanVien() != null && hd.getNhanVien().getMaNV() != null) {
                hd.setNhanVien(nvDao.getById(hd.getNhanVien().getMaNV()));
            }
        }

        return dsHoaDon;
    }

    /**
     * Tìm hóa đơn theo mã
     */
    public HoaDon getById(String maHD) {
        String sql = "SELECT * FROM HoaDon WHERE maHD = ?";
        HoaDon hd = null;
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maHD);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                hd = mapRow(rs);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy hóa đơn theo mã: " + e.getMessage());
        }
        if (hd != null && hd.getNhanVien() != null && hd.getNhanVien().getMaNV() != null) {
            hd.setNhanVien(new NhanVienDAO().getById(hd.getNhanVien().getMaNV()));
        }
        return hd; // Fix: trước đây return null
    }

    /**
     * Tìm hóa đơn theo mã đặt phòng
     */
    public HoaDon getByMaDat(String maDat) {
        String sql = "SELECT * FROM HoaDon WHERE maDat = ?";
        HoaDon hd = null;
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                hd = mapRow(rs);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy hóa đơn theo mã đặt phòng: " + e.getMessage());
        }

        if (hd != null && hd.getNhanVien() != null && hd.getNhanVien().getMaNV() != null) {
            hd.setNhanVien(new NhanVienDAO().getById(hd.getNhanVien().getMaNV()));
        }

        return hd;
    }

    /**
     * Thêm mới hóa đơn
     */
    public boolean insert(HoaDon hd) {
        String sql = "INSERT INTO HoaDon (maHD, maDat, maNV, ngayTaoHD, tienPhong, tienDV, tienCoc, thueVAT, tongTien, doanhThu, loaiHD, trangThaiThanhToan, ngayThanhToan, ghiChuThanhToan, phu_thu, phu_phi_tra_muon) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, hd.getMaHD());
            ps.setString(2, hd.getDatPhong().getMaDat());
            ps.setString(3, hd.getNhanVien().getMaNV());
            ps.setTimestamp(4, hd.getNgayTaoHD() != null ? Timestamp.valueOf(hd.getNgayTaoHD())
                    : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setDouble(5, hd.getTienPhong());
            ps.setDouble(6, hd.getTienDV());
            ps.setDouble(7, hd.getTienCoc());
            ps.setDouble(8, hd.getThueVAT());
            ps.setDouble(9, hd.getTongTien());
            ps.setDouble(10, hd.getDoanhThu());
            ps.setString(11, hd.getLoaiHD() != null ? hd.getLoaiHD() : "HOA_DON_PHONG");
            ps.setString(12, hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "CHUA_THANH_TOAN");
            ps.setTimestamp(13, hd.getNgayThanhToan() != null ? Timestamp.valueOf(hd.getNgayThanhToan()) : null);
            ps.setString(14, hd.getGhiChuThanhToan());
            ps.setDouble(15, hd.getPhuThu());
            ps.setDouble(16, hd.getPhuPhiTraMuon());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm hóa đơn: " + e.getMessage());
            return false;
        }
    }

    private HoaDon mapRow(ResultSet rs) throws SQLException {
        HoaDon hd = new HoaDon();
        hd.setMaHD(rs.getString("maHD"));
        hd.setNgayTaoHD(rs.getTimestamp("ngayTaoHD") != null ? rs.getTimestamp("ngayTaoHD").toLocalDateTime() : null);
        hd.setDatPhong(new DatPhong(rs.getString("maDat")));

        NhanVien placeholderNV = new NhanVien();
        placeholderNV.setMaNV(rs.getString("maNV"));
        hd.setNhanVien(placeholderNV);

        hd.setTienPhong(rs.getDouble("tienPhong"));
        hd.setTienDV(rs.getDouble("tienDV"));
        hd.setTienCoc(rs.getDouble("tienCoc"));
        hd.setThueVAT(rs.getDouble("thueVAT"));
        hd.setTongTien(rs.getDouble("tongTien"));
        hd.setDoanhThu(rs.getDouble("doanhThu"));
        try {
            hd.setPhuThu(rs.getDouble("phu_thu"));
            hd.setPhuPhiTraMuon(rs.getDouble("phu_phi_tra_muon"));
        } catch (SQLException ignored) {
        }
        hd.setLoaiHD(rs.getString("loaiHD"));
        hd.setTrangThaiThanhToan(rs.getString("trangThaiThanhToan"));
        hd.setNgayThanhToan(
                rs.getTimestamp("ngayThanhToan") != null ? rs.getTimestamp("ngayThanhToan").toLocalDateTime() : null);
        hd.setGhiChuThanhToan(rs.getString("ghiChuThanhToan"));
        return hd;
    }

    /**
     * Tự động phát sinh mã hóa đơn
     */
    public String generateMaHD() {
        return IdGenerator.randomId("HD", 8);
    }

    /**
     * Cập nhật đầy đủ hóa đơn khi checkout (tienPhong, tienDV, tienCoc, trangThai,
     * phương thức...)
     */
    public boolean updateTongTien(HoaDon hd) {
        String sql = "UPDATE HoaDon SET tienPhong=?, tienDV=?, tienCoc=?, thueVAT=?, tongTien=?, doanhThu=?," +
                " ngayTaoHD=?, trangThaiThanhToan=?, ngayThanhToan=?, phu_thu=?, phu_phi_tra_muon=? WHERE maHD=?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, hd.getTienPhong());
            ps.setDouble(2, hd.getTienDV());
            ps.setDouble(3, hd.getTienCoc());
            ps.setDouble(4, hd.getThueVAT());
            ps.setDouble(5, hd.getTongTien());
            ps.setDouble(6, hd.getDoanhThu());
            ps.setTimestamp(7, hd.getNgayTaoHD() != null
                    ? Timestamp.valueOf(hd.getNgayTaoHD())
                    : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(8, hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "DA_THANH_TOAN");
            ps.setTimestamp(9, hd.getNgayThanhToan() != null ? Timestamp.valueOf(hd.getNgayThanhToan()) : null);
            ps.setDouble(10, hd.getPhuThu());
            ps.setDouble(11, hd.getPhuPhiTraMuon());
            ps.setString(12, hd.getMaHD());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy hóa đơn theo maCTDP (tìm HoaDon của 1 phòng cụ thể trong đơn)
     */
    public HoaDon getByMaCTDP(String maCTDP) {
        String sql = "SELECT h.* FROM HoaDon h " +
                "JOIN ChiTietHoaDon cthd ON h.maHD = cthd.maHD " +
                "WHERE cthd.maCTDP = ?";
        HoaDon hd = null;
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maCTDP);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                hd = mapRow(rs);
        } catch (Exception e) {
            System.err.println("Lỗi getByMaCTDP: " + e.getMessage());
        }
        return hd;
    }

    /**
     * Lấy toàn bộ hóa đơn kèm tên khách hàng (JOIN thêm KH)
     */
    public List<HoaDon> getAllWithKhachHang() {
        List<HoaDon> dsHoaDon = new ArrayList<>();
        String sql = "SELECT h.*, kh.tenKH, kh.soDT, kh.soCCCD, dp.trangThai AS dp_trangThai, dp.ngayCheckIn AS dp_ngayCheckIn, dp.ngayCheckOut AS dp_ngayCheckOut FROM HoaDon h " +
                "JOIN DatPhong dp ON h.maDat = dp.maDat " +
                "JOIN KH kh ON dp.maKH = kh.maKH " +
                "ORDER BY h.maHD ASC";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                HoaDon hd = mapRow(rs);
                model.entities.KhachHang kh = new model.entities.KhachHang();
                kh.setTenKH(rs.getString("tenKH"));
                kh.setSoDT(rs.getString("soDT"));
                kh.setSoCCCD(rs.getString("soCCCD"));
                hd.getDatPhong().setKhachHang(kh);
                try {
                    hd.getDatPhong().setTrangThai(rs.getString("dp_trangThai"));
                    if (rs.getTimestamp("dp_ngayCheckIn") != null) {
                        hd.getDatPhong().setNgayCheckIn(rs.getTimestamp("dp_ngayCheckIn").toLocalDateTime());
                    }
                    if (rs.getTimestamp("dp_ngayCheckOut") != null) {
                        hd.getDatPhong().setNgayCheckOut(rs.getTimestamp("dp_ngayCheckOut").toLocalDateTime());
                    }
                } catch (SQLException ignored) {
                }
                dsHoaDon.add(hd);
            }
        } catch (Exception e) {
            System.err.println("Lỗi getAllWithKhachHang: " + e.getMessage());
        }
        NhanVienDAO nvDao = new NhanVienDAO();
        for (HoaDon hd : dsHoaDon) {
            if (hd.getNhanVien() != null && hd.getNhanVien().getMaNV() != null)
                hd.setNhanVien(nvDao.getById(hd.getNhanVien().getMaNV()));
        }
        return dsHoaDon;
    }

    /** Tạo hoá đơn dùng chung Connection (transaction) */
    public boolean insertWithConnection(Connection con, HoaDon hd) throws SQLException {
        String sql = "INSERT INTO HoaDon (maHD, maDat, maNV, ngayTaoHD, tienPhong, tienDV, tienCoc, thueVAT, tongTien, doanhThu, loaiHD, trangThaiThanhToan, ngayThanhToan, ghiChuThanhToan, phu_thu, phu_phi_tra_muon) VALUES (?,?,?,GETDATE(),?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hd.getMaHD());
            ps.setString(2, hd.getDatPhong().getMaDat());
            ps.setString(3, hd.getNhanVien() != null ? hd.getNhanVien().getMaNV() : "LUCIA001");
            ps.setDouble(4, hd.getTienPhong());
            ps.setDouble(5, hd.getTienDV());
            ps.setDouble(6, hd.getTienCoc());
            ps.setDouble(7, hd.getThueVAT());
            ps.setDouble(8, hd.getTongTien());
            ps.setDouble(9, hd.getDoanhThu());
            ps.setString(10, hd.getLoaiHD() != null ? hd.getLoaiHD() : "HOA_DON_PHONG");
            ps.setString(11, hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "CHUA_THANH_TOAN");
            ps.setTimestamp(12, hd.getNgayThanhToan() != null ? Timestamp.valueOf(hd.getNgayThanhToan()) : null);
            ps.setString(13, hd.getGhiChuThanhToan());
            ps.setDouble(14, hd.getPhuThu());
            ps.setDouble(15, hd.getPhuPhiTraMuon());
            return ps.executeUpdate() > 0;
        }
    }

    /** Cập nhật tiền hóa đơn sau mỗi lần phòng được trả (dùng chung Connection) */
    public boolean updateAmounts(Connection con, String maHD,
            double tienPhong, double tienDV, double tienCoc, double tongTien,
            String trangThai) throws SQLException {
        String sql = "UPDATE HoaDon SET tienPhong=?, tienDV=?, tienCoc=?, tongTien=?, trangThaiThanhToan=? WHERE maHD=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, tienPhong);
            ps.setDouble(2, tienDV);
            ps.setDouble(3, tienCoc);
            ps.setDouble(4, tongTien);
            ps.setString(5, trangThai);
            ps.setString(6, maHD);
            return ps.executeUpdate() > 0;
        }
    }

    /** Lấy tổng tienPhong hiện tại của 1 hóa đơn (sum CTHD.thanhTien) */
    public double getTongTienPhongCurrent(String maHD) {
        String sql = "SELECT ISNULL(SUM(thanhTien), 0) FROM ChiTietHoaDon WHERE maHD = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maHD);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getDouble(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateTrangThaiByMaDatWithCon(Connection con, String maDat, String trangThai) throws SQLException {
        String sql = "UPDATE HoaDon SET trangThaiThanhToan = ? WHERE maDat = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThai);
            ps.setString(2, maDat);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateTrangThaiByMaDat(String maDat, String trangThai) {
        try (Connection con = ConnectDatabase.getInstance().getConnection()) {
            return updateTrangThaiByMaDatWithCon(con, maDat, trangThai);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public double tinhTongTien(HoaDon hd) {
        // Tiền cọc = trả trước đêm đầu, trừ thẳng vào tổng
        double subtotal = hd.getTienPhong() + hd.getTienDV() + hd.getPhuThu() + hd.getPhuPhiTraMuon();
        double giaVAT = subtotal * hd.getThueVAT();
        double tongTien = Math.max(0, subtotal + giaVAT - hd.getTienCoc());
        hd.setTongTien(tongTien);
        return tongTien;
    }

    public double tinhDoanhThu(HoaDon hd) {
        String status = hd.getTrangThaiThanhToan();
        double doanhThu = 0;

        if ("DA_THANH_TOAN".equals(status)) {
            // Doanh thu = tổng tiền phòng + tiền DV + phụ phí trả muộn + phụ thu
            doanhThu = hd.getTienPhong() + hd.getTienDV() + hd.getPhuPhiTraMuon() + hd.getPhuThu();
        } else if ("DA_MAT_COC".equals(status)) {
            doanhThu = hd.getTienCoc();
        } else {
            doanhThu = 0;
        }

        hd.setDoanhThu(doanhThu);
        return doanhThu;
    }

    public Map<String, Double> getSoNgaySuDungTheoLoaiPhong(LocalDate start, LocalDate end) {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT p.loaiPhong, ISNULL(SUM(cthd.thoiGianLuuTru), 0) as totalDays " +
                "FROM ChiTietHoaDon cthd " +
                "JOIN HoaDon hd ON cthd.maHD = hd.maHD " +
                "JOIN ChiTietDatPhong ctdp ON cthd.maCTDP = ctdp.maCTDP " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE CAST(hd.ngayTaoHD AS DATE) >= ? AND CAST(hd.ngayTaoHD AS DATE) <= ? " +
                "GROUP BY p.loaiPhong";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(start));
            ps.setDate(2, java.sql.Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("loaiPhong"), rs.getDouble("totalDays"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
