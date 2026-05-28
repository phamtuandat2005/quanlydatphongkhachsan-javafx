package dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import connectDatabase.ConnectDatabase;
import model.entities.DatPhong;
import model.entities.KhachHang;
import model.utils.IdGenerator;

public class DatPhongDAO {

    /**
     * Tìm khách hàng dựa trên mã đặt phòng
     */
    public KhachHang findKhachHangByIdDatPhong(String maDat) {
        String sql = "SELECT maKH FROM DatPhong WHERE maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maDat);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new KhachHangDAO().findKhachHangByID(rs.getString("maKH"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách mã đặt phòng cần Check-in trong hôm nay
     */
    public List<String> getMaDatPhongCheckInHomNay() {
        List<String> dsMa = new ArrayList<>();
        String sql = "SELECT maDat FROM DatPhong " +
                "WHERE CAST(ngayCheckIn AS DATE) <= CAST(GETDATE() AS DATE) " +
                "AND ngayCheckIn IS NOT NULL " +
                "AND trangThai = N'DA_XACNHAN'";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dsMa.add(rs.getString("maDat"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dsMa;
    }

    /**
     * Lấy danh sách đơn đặt phòng cần Check-in theo ngày cụ thể.
     * Mỗi Object[]: {maDat, tenKH, dsPhong (comma-separated), tongSoNguoi}
     * @param includeLate true = hiện cả đơn trễ (ngayCheckIn < date), false = chỉ đúng ngày
     */
    public List<Object[]> getDonCheckInByDate(LocalDate date, boolean includeLate) {
        List<Object[]> ds = new ArrayList<>();
        String dateCondition = includeLate
                ? "CAST(dp.ngayCheckIn AS DATE) < ?"
                : "CAST(dp.ngayCheckIn AS DATE) = ?";
        String sql = "SELECT dp.maDat, kh.tenKH, kh.soDT, kh.soCCCD, " +
                "STRING_AGG(ctdp.maPhong, ', ') AS dsPhong, " +
                "SUM(ctdp.soNguoi) AS tongSoNguoi " +
                "FROM DatPhong dp " +
                "JOIN KH kh ON dp.maKH = kh.maKH " +
                "LEFT JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "WHERE " + dateCondition + " " +
                "AND dp.ngayCheckIn IS NOT NULL " +
                "AND dp.trangThai = N'DA_XACNHAN' " +
                "GROUP BY dp.maDat, kh.tenKH, kh.soDT, kh.soCCCD " +
                "ORDER BY dp.maDat";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ds.add(new Object[]{
                        rs.getString("maDat"),
                        rs.getString("tenKH"),
                        rs.getString("dsPhong") != null ? rs.getString("dsPhong") : "---",
                        rs.getObject("tongSoNguoi") != null ? rs.getInt("tongSoNguoi") : 0,
                        rs.getString("soDT") != null ? rs.getString("soDT") : "",
                        rs.getString("soCCCD") != null ? rs.getString("soCCCD") : ""
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Tìm kiếm tương đối đơn đặt phòng cần Check-in theo từ khóa.
     * Mỗi Object[]: {maDat, tenKH, dsPhong (comma-separated), tongSoNguoi}
     */
    public List<Object[]> searchDonCheckIn(String keyword) {
        List<Object[]> ds = new ArrayList<>();
        String sql = "SELECT dp.maDat, kh.tenKH, " +
                "STRING_AGG(ctdp.maPhong, ', ') AS dsPhong, " +
                "SUM(ctdp.soNguoi) AS tongSoNguoi " +
                "FROM DatPhong dp " +
                "JOIN KH kh ON dp.maKH = kh.maKH " +
                "LEFT JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "WHERE (dp.maDat LIKE ? OR kh.soDT LIKE ? OR kh.soCCCD LIKE ? OR kh.tenKH LIKE ?) " +
                "AND dp.ngayCheckIn IS NOT NULL " +
                "AND dp.trangThai = N'DA_XACNHAN' " +
                "GROUP BY dp.maDat, kh.tenKH " +
                "ORDER BY dp.maDat";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ds.add(new Object[]{
                        rs.getString("maDat"),
                        rs.getString("tenKH"),
                        rs.getString("dsPhong") != null ? rs.getString("dsPhong") : "---",
                        rs.getObject("tongSoNguoi") != null ? rs.getInt("tongSoNguoi") : 0
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Tìm thông tin đơn đặt phòng bất kể trạng thái (Dùng để kiểm tra khi tìm kiếm)
     */
    public DatPhong findDatPhongAllStatus(String keyword) {
        String sql = "SELECT dp.*, kh.tenKH, kh.soDT, kh.soCCCD " +
                "FROM DatPhong dp JOIN KH kh ON dp.maKH = kh.maKH " +
                "WHERE (dp.maDat = ? OR kh.soDT = ? OR kh.soCCCD = ?)";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, keyword);
            pstmt.setString(2, keyword);
            pstmt.setString(3, keyword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                KhachHang kh = new KhachHang(
                        rs.getString("maKH"),
                        rs.getString("tenKH"),
                        rs.getString("soCCCD"),
                        rs.getString("soDT"));

                DatPhong dp = new DatPhong();
                dp.setMaDat(rs.getString("maDat"));
                dp.setNgayDat(rs.getTimestamp("ngayDat") != null ? rs.getTimestamp("ngayDat").toLocalDateTime() : null);
                dp.setKhachHang(kh);
                dp.setTrangThai(rs.getString("trangThai"));
                dp.setNgayCheckIn(rs.getTimestamp("ngayCheckIn") != null ? rs.getTimestamp("ngayCheckIn").toLocalDateTime() : null);
                dp.setNgayCheckOut(rs.getTimestamp("ngayCheckOut") != null ? rs.getTimestamp("ngayCheckOut").toLocalDateTime() : null);

                return dp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Tìm thông tin tổng hợp để hiển thị lên giao diện CheckInPanel.
     * Hỗ trợ tìm theo: mã đặt phòng, số điện thoại, hoặc số CCCD.
     * Chỉ trả về đơn còn chờ check-in.
     */
    public DatPhong findDatPhongDetail(String keyword) {
        String sql = "SELECT dp.*, kh.tenKH, kh.soDT, kh.soCCCD " +
                "FROM DatPhong dp JOIN KH kh ON dp.maKH = kh.maKH " +
                "WHERE (dp.maDat = ? OR kh.soDT = ? OR kh.soCCCD = ?) " +
                "AND dp.trangThai = N'DA_XACNHAN'";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, keyword);
            pstmt.setString(2, keyword);
            pstmt.setString(3, keyword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                KhachHang kh = new KhachHang(
                        rs.getString("maKH"),
                        rs.getString("tenKH"),
                        rs.getString("soCCCD"),
                        rs.getString("soDT"));

                DatPhong dp = new DatPhong();
                dp.setMaDat(rs.getString("maDat"));
                dp.setNgayDat(rs.getTimestamp("ngayDat") != null ? rs.getTimestamp("ngayDat").toLocalDateTime() : null);
                dp.setKhachHang(kh);
                dp.setTrangThai(rs.getString("trangThai"));

                dp.setNgayCheckIn(
                        rs.getTimestamp("ngayCheckIn") != null ? rs.getTimestamp("ngayCheckIn").toLocalDateTime()
                                : null);
                dp.setNgayCheckOut(
                        rs.getTimestamp("ngayCheckOut") != null ? rs.getTimestamp("ngayCheckOut").toLocalDateTime()
                                : null);
                dp.setSoNguoi(rs.getInt("so_nguoi"));
                dp.setNgayThanhToan(rs.getTimestamp("ngay_thanh_toan") != null ? rs.getTimestamp("ngay_thanh_toan").toLocalDateTime() : null);

                return dp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Insert đơn đặt phòng mới
     */
    public boolean insert(DatPhong dp) {
        String sql = "INSERT INTO DatPhong(maDat, ngayDat, maKH, ngayCheckIn, ngayCheckOut, trangThai, so_nguoi, ngay_thanh_toan) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, dp.getMaDat());
            pstmt.setTimestamp(2, dp.getNgayDat() != null ? Timestamp.valueOf(dp.getNgayDat())
                    : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(3, dp.getKhachHang().getMaKH());

            // SỬA Ở ĐÂY: Lưu nguyên LocalDateTime xuống dạng Timestamp
            pstmt.setTimestamp(4,
                    dp.getNgayCheckIn() != null ? Timestamp.valueOf(dp.getNgayCheckIn()) : null);
            pstmt.setTimestamp(5,
                    dp.getNgayCheckOut() != null ? Timestamp.valueOf(dp.getNgayCheckOut()) : null);
            
            pstmt.setString(6, (dp.getTrangThai() != null && !dp.getTrangThai().isEmpty()) 
                               ? dp.getTrangThai() : "CHO_XACNHAN");
            pstmt.setInt(7, dp.getSoNguoi() > 0 ? dp.getSoNguoi() : 1);
            pstmt.setTimestamp(8, dp.getNgayThanhToan() != null ? Timestamp.valueOf(dp.getNgayThanhToan()) : null);

            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy tên khách hàng đang ở trong phòng
     */
    public String getTenKhachHienTai(String maPhong) {
        String sql = "SELECT kh.tenKH FROM KH kh " +
                "JOIN DatPhong dp ON kh.maKH = dp.maKH " +
                "JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE ctdp.maPhong = ? AND p.tinhTrang = N'DANGSUDUNG' AND dp.trangThai = N'DA_CHECKIN' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC)";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("tenKH");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Lấy mã hóa đơn đang hoạt động của một phòng
     */
    public String getMaHDByMaPhong(String maPhong) {
        String sql = "SELECT h.maHD FROM HoaDon h " +
                "JOIN DatPhong dp ON h.maDat = dp.maDat " +
                "JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE ctdp.maPhong = ? AND p.tinhTrang = N'DANGSUDUNG' AND dp.trangThai = N'DA_CHECKIN' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("maHD");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Query tổng hợp checkout
     */
    public Object[] findCheckOutInfoByMaPhong(String maPhong) {
        String sql = "SELECT dp.maDat, dp.ngayDat, dp.ngayCheckIn, dp.ngayCheckOut, " +
                "kh.maKH, kh.tenKH, kh.soDT, kh.soCCCD, " +
                "ctdp.giaCoc, lp.gia AS giaPhong, p.maPhong " +
                "FROM Phong p " +
                "JOIN ChiTietDatPhong ctdp ON p.maPhong = ctdp.maPhong " +
                "JOIN DatPhong dp ON ctdp.maDat = dp.maDat " +
                "JOIN KH kh ON dp.maKH = kh.maKH " +
                "JOIN LoaiPhong lp ON p.loaiPhong = lp.maLoaiPhong " +
                "WHERE p.maPhong = ? AND p.tinhTrang = N'DANGSUDUNG' AND dp.trangThai = N'DA_CHECKIN' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC)";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, maPhong);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                KhachHang kh = new KhachHang(
                        rs.getString("maKH"),
                        rs.getString("tenKH"),
                        rs.getString("soCCCD"),
                        rs.getString("soDT"));

                DatPhong dp = new DatPhong();
                dp.setMaDat(rs.getString("maDat"));
                dp.setNgayDat(rs.getTimestamp("ngayDat") != null
                        ? rs.getTimestamp("ngayDat").toLocalDateTime()
                        : null);
                dp.setKhachHang(kh);

                // SỬA Ở ĐÂY: Dùng Timestamp để lấy đủ ngày giờ (14:00, 12:00) thay vì 00:00
                dp.setNgayCheckIn(rs.getTimestamp("ngayCheckIn") != null
                        ? rs.getTimestamp("ngayCheckIn").toLocalDateTime()
                        : null);
                dp.setNgayCheckOut(rs.getTimestamp("ngayCheckOut") != null
                        ? rs.getTimestamp("ngayCheckOut").toLocalDateTime()
                        : null);

                double giaCoc = rs.getDouble("giaCoc");
                double giaPhong = rs.getDouble("giaPhong");
                String maPhongResult = rs.getString("maPhong");

                return new Object[] { dp, giaCoc, giaPhong, maPhongResult };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getDonDangSuDung() {
        List<String> ds = new ArrayList<>();
        String sql = "SELECT DISTINCT dp.maDat FROM DatPhong dp " +
                "JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE p.tinhTrang = N'DANGSUDUNG' AND dp.trangThai = N'DA_CHECKIN' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ds.add(rs.getString("maDat"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ds;
    }

    public List<String> getPhongDangSuDung() {
        List<String> ds = new ArrayList<>();
        String sql = "SELECT maPhong FROM Phong WHERE tinhTrang = N'DANGSUDUNG'";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ds.add(rs.getString("maPhong"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ds;
    }

    public boolean saveServiceOrder(String maPhong, java.util.Map<model.entities.DichVu, Integer> cart) {
        String maCTDP = getMaCTDPDangSuDungByMaPhong(maPhong);
        if (maCTDP == null)
            return false;

        String sql = "MERGE INTO DichVuSuDung AS target " +
                "USING (VALUES (?, ?, ?, ?)) AS source (maDV, maCTDP, soLuong, giaDV) " +
                "ON target.maDV = source.maDV AND target.maCTDP = source.maCTDP " +
                "WHEN MATCHED THEN " +
                "    UPDATE SET target.soLuong = target.soLuong + source.soLuong, " +
                "              target.ngaySuDung = GETDATE() " +
                "WHEN NOT MATCHED THEN " +
                "    INSERT (maDV, maCTDP, ngaySuDung, soLuong, giaDV, trangThai) " +
                "    VALUES (source.maDV, source.maCTDP, GETDATE(), source.soLuong, source.giaDV, 0);";

        try (Connection con = ConnectDatabase.getInstance().getConnection()) {
            if (con == null)
                return false;
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    for (java.util.Map.Entry<model.entities.DichVu, Integer> entry : cart.entrySet()) {
                        if (entry.getKey().getGia() == null)
                            continue;
                        ps.setString(1, entry.getKey().getMaDV());
                        ps.setString(2, maCTDP);
                        ps.setInt(3, entry.getValue());
                        ps.setDouble(4, entry.getKey().getGia());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getMaCTDPDangSuDungByMaPhong(String maPhong) {
        String sql = "SELECT ctdp.maCTDP FROM ChiTietDatPhong ctdp " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "JOIN DatPhong dp ON ctdp.maDat = dp.maDat " +
                "WHERE ctdp.maPhong = ? AND p.tinhTrang = N'DANGSUDUNG' AND dp.trangThai = N'DA_CHECKIN' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("maCTDP");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách đơn đặt phòng cần Check-out theo ngày cụ thể.
     * Mỗi Object[]: {maDat, tenKH, dsPhong (comma-separated), tongSoNguoi}
     * @param includeLate true = hiện cả đơn trễ (ngayCheckOut < date), false = chỉ đúng ngày
     */
    public List<Object[]> getDonCheckOutByDate(LocalDate date, boolean includeLate) {
        List<Object[]> ds = new ArrayList<>();
        String dateCondition = includeLate
                ? "CONVERT(DATE, dp.ngayCheckOut) <= ?"
                : "CONVERT(DATE, dp.ngayCheckOut) = ?";
        String sql = "SELECT dp.maDat, kh.tenKH, " +
                "STRING_AGG(ctdp.maPhong, ', ') AS dsPhong, " +
                "SUM(ctdp.soNguoi) AS tongSoNguoi " +
                "FROM DatPhong dp " +
                "JOIN KH kh ON dp.maKH = kh.maKH " +
                "LEFT JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE " + dateCondition + " " +
                "AND dp.trangThai = N'DA_CHECKIN' " +
                "AND p.tinhTrang = N'DANGSUDUNG' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC) " +
                "GROUP BY dp.maDat, kh.tenKH " +
                "ORDER BY dp.maDat";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ds.add(new Object[]{
                        rs.getString("maDat"),
                        rs.getString("tenKH"),
                        rs.getString("dsPhong") != null ? rs.getString("dsPhong") : "---",
                        rs.getObject("tongSoNguoi") != null ? rs.getInt("tongSoNguoi") : 0
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Tìm đơn đặt phòng đã check-in (DA_CHECKIN) theo mã đặt, SĐT hoặc CCCD.
     * Dùng cho luồng checkout.
     */
    public DatPhong findDatPhongForCheckOut(String keyword) {
        String sql = "SELECT dp.*, kh.tenKH, kh.soDT, kh.soCCCD " +
                "FROM DatPhong dp JOIN KH kh ON dp.maKH = kh.maKH " +
                "WHERE (dp.maDat = ? OR kh.soDT = ? OR kh.soCCCD = ?) " +
                "AND dp.trangThai = N'DA_CHECKIN'";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, keyword);
            pstmt.setString(2, keyword);
            pstmt.setString(3, keyword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                KhachHang kh = new KhachHang(
                        rs.getString("maKH"),
                        rs.getString("tenKH"),
                        rs.getString("soCCCD"),
                        rs.getString("soDT"));

                DatPhong dp = new DatPhong();
                dp.setMaDat(rs.getString("maDat"));
                dp.setNgayDat(rs.getTimestamp("ngayDat") != null ? rs.getTimestamp("ngayDat").toLocalDateTime() : null);
                dp.setKhachHang(kh);
                dp.setTrangThai(rs.getString("trangThai"));
                dp.setNgayCheckIn(
                        rs.getTimestamp("ngayCheckIn") != null ? rs.getTimestamp("ngayCheckIn").toLocalDateTime() : null);
                dp.setNgayCheckOut(
                        rs.getTimestamp("ngayCheckOut") != null ? rs.getTimestamp("ngayCheckOut").toLocalDateTime() : null);
                dp.setSoNguoi(rs.getInt("so_nguoi"));
                dp.setNgayThanhToan(rs.getTimestamp("ngay_thanh_toan") != null ? rs.getTimestamp("ngay_thanh_toan").toLocalDateTime() : null);
                return dp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String generateMaDat() {
        return IdGenerator.randomId("DP", 8);
    }

    // Các hàm dưới đây nhận trực tiếp LocalDate từ UI Dialog truyền xuống nên không
    // cần sửa
    public boolean insertWithConnection(Connection con, String maDat, String maKH,
            LocalDate checkIn, LocalDate checkOut, int soNguoi) throws SQLException {
        return insertWithConnection(con, maDat, maKH, checkIn, checkOut, "CHO_XACNHAN", soNguoi);
    }

    /** Insert đơn đặt phòng với trangThai tuỳ chọn */
    public boolean insertWithConnection(Connection con, String maDat, String maKH,
            LocalDate checkIn, LocalDate checkOut,
            String trangThai, int soNguoi) throws SQLException {
        String sql = "INSERT INTO DatPhong(maDat, ngayDat, maKH, ngayCheckIn, ngayCheckOut, trangThai, so_nguoi) VALUES(?,GETDATE(),?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ps.setString(2, maKH);
            ps.setTimestamp(3, Timestamp.valueOf(checkIn.atTime(14, 0)));
            ps.setTimestamp(4, Timestamp.valueOf(checkOut.atTime(12, 0)));
            ps.setString(5, trangThai);
            ps.setInt(6, soNguoi);
            return ps.executeUpdate() > 0;
        }
    }

    // /** Update trangThai dùng chung Connection (trong transaction) */
    // public boolean updateTrangThaiWithCon(Connection con, String maDat, String
    // trangThai) throws SQLException {
    // String sql = "UPDATE DatPhong SET trangThai = ? WHERE maDat = ?";
    // try (PreparedStatement ps = con.prepareStatement(sql)) {
    // ps.setString(1, trangThai);
    // ps.setString(2, maDat);
    // return ps.executeUpdate() > 0;
    // }
    // }

    public boolean updateNgayCheckInOut(Connection con, String maDat,
            LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = "UPDATE DatPhong SET ngayCheckIn=?, ngayCheckOut=? WHERE maDat=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(checkIn));
            ps.setDate(2, java.sql.Date.valueOf(checkOut));
            ps.setString(3, maDat);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * LẤY CHI TIẾT ĐƠN ĐẶT PHÒNG ĐỂ POPULATE FORM SỬA
     */
    /**
     * Cập nhật trạng thái đơn đặt phòng
     */
    // public boolean updateTrangThai(String maDat, String trangThai) {
    // String sql = "UPDATE DatPhong SET trangThai = ? WHERE maDat = ?";
    // try (Connection con = ConnectDatabase.getInstance().getConnection();
    // PreparedStatement ps = con.prepareStatement(sql)) {
    // ps.setString(1, trangThai);
    // ps.setString(2, maDat);
    // return ps.executeUpdate() > 0;
    // } catch (Exception e) {
    // e.printStackTrace();
    // return false;
    // }
    // }

    /**
     * Kiểm tra xem tất cả phòng trong đơn đã checkout chưa.
     * (Tất cả phòng trong CTDP có trạng thái CONTRONG → đơn đủ điều kiện
     * DA_CHECKOUT)
     */
    public boolean isAllRoomsCheckedOut(String maDat) {
        String sql = "SELECT COUNT(*) FROM ChiTietDatPhong ctdp " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE ctdp.maDat = ? AND p.tinhTrang = N'DANGSUDUNG'";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) == 0; // không còn phòng DANGSUDUNG nào
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy thông tin checkout của toàn đơn (tất cả phòng đang DANGSUDUNG trong đơn)
     * Trả về: [DatPhong, List<Object[]>] trong đó mỗi Object[] = {maCTDP, maPhong,
     * giaCoc, giaPhong}
     */
    public Object[] findCheckOutInfoByMaDat(String maDat) {
        String sqlDp = "SELECT dp.*, kh.maKH, kh.tenKH, kh.soDT, kh.soCCCD " +
                "FROM DatPhong dp JOIN KH kh ON dp.maKH = kh.maKH " +
                "WHERE dp.maDat = ?";
        DatPhong dp = null;
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sqlDp)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                KhachHang kh = new KhachHang(
                        rs.getString("maKH"), rs.getString("tenKH"),
                        rs.getString("soCCCD"), rs.getString("soDT"));
                dp = new DatPhong();
                dp.setMaDat(rs.getString("maDat"));
                dp.setKhachHang(kh);
                dp.setTrangThai(rs.getString("trangThai"));
                dp.setNgayCheckIn(rs.getTimestamp("ngayCheckIn") != null
                        ? rs.getTimestamp("ngayCheckIn").toLocalDateTime()
                        : null);
                dp.setNgayCheckOut(rs.getTimestamp("ngayCheckOut") != null
                        ? rs.getTimestamp("ngayCheckOut").toLocalDateTime()
                        : null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dp == null)
            return null;

        // Lấy tất cả phòng DANGSUDUNG của đơn
        String sqlRooms = "SELECT ctdp.maCTDP, ctdp.maPhong, ctdp.giaCoc, lp.gia AS giaPhong " +
                "FROM ChiTietDatPhong ctdp " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "JOIN LoaiPhong lp ON p.loaiPhong = lp.maLoaiPhong " +
                "WHERE ctdp.maDat = ? AND p.tinhTrang = N'DANGSUDUNG' " +
                "AND ctdp.maCTDP = (SELECT TOP 1 c2.maCTDP FROM ChiTietDatPhong c2 JOIN DatPhong d2 ON c2.maDat = d2.maDat WHERE c2.maPhong = ctdp.maPhong AND d2.trangThai = N'DA_CHECKIN' ORDER BY d2.ngayCheckIn DESC)";
        java.util.List<Object[]> roomList = new java.util.ArrayList<>();
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sqlRooms)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roomList.add(new Object[] {
                        rs.getString("maCTDP"),
                        rs.getString("maPhong"),
                        rs.getDouble("giaCoc"),
                        rs.getDouble("giaPhong")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Object[] { dp, roomList };
    }

    /**
     * Lấy chi tiết đơn đặt phòng để populate form sửa
     * data[0]=tenKH, [1]=soDT, [2]=soCCCD, [3]=ngaySinh(LocalDate),
     * [4]=ngayCheckIn(LocalDate), [5]=ngayCheckOut(LocalDate),
     * [6]=maPhong(String, nhiều phòng cách nhau ","),
     * [7]=soNguoi(int), [8]=giaCoc(double), [9]=ghiChu, [10]=loaiPhong
     */
    public Object[] findEditDetail(String maDat) {
        // Lấy nhiều phòng nếu có
        String sqlPhongs = """
                SELECT kh.tenKH, kh.soDT, kh.soCCCD, kh.ngaySinh,
                       dp.ngayCheckIn, dp.ngayCheckOut,
                       STRING_AGG(ctdp.maPhong, ',') AS dsPhong,
                       SUM(ctdp.soNguoi) AS soNguoi,
                       SUM(ctdp.giaCoc) AS giaCoc,
                       MAX(ctdp.ghiChu) AS ghiChu,
                       MAX(p.loaiPhong) AS loaiPhong,
                       MAX(dp.trangThai) AS trangThai
                FROM DatPhong dp JOIN KH kh ON dp.maKH = kh.maKH
                LEFT JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat
                LEFT JOIN Phong p ON ctdp.maPhong = p.maPhong
                WHERE dp.maDat = ?
                GROUP BY kh.tenKH, kh.soDT, kh.soCCCD, kh.ngaySinh,
                         dp.ngayCheckIn, dp.ngayCheckOut
                """;
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sqlPhongs)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Object[] {
                        rs.getString("tenKH"),
                        rs.getString("soDT"),
                        rs.getString("soCCCD"),
                        rs.getDate("ngaySinh") != null ? rs.getDate("ngaySinh").toLocalDate() : null,
                        rs.getDate("ngayCheckIn") != null ? rs.getDate("ngayCheckIn").toLocalDate() : null,
                        rs.getDate("ngayCheckOut") != null ? rs.getDate("ngayCheckOut").toLocalDate() : null,
                        rs.getString("dsPhong"), // data[6]: "P101,P102"
                        rs.getObject("soNguoi") != null ? rs.getInt("soNguoi") : 1, // data[7]
                        rs.getObject("giaCoc") != null ? rs.getDouble("giaCoc") : 0.0, // data[8]
                        rs.getString("ghiChu"), // data[9]
                        rs.getString("loaiPhong"), // data[10]
                        rs.getString("trangThai") // data[11]
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String findMaCTDPByMaPhong(String maDat, String maPhong) {
        String sql = "SELECT maCTDP FROM ChiTietDatPhong WHERE maDat = ? AND maPhong = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ps.setString(2, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("maCTDP");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateTrangThaiWithCon(Connection con, String maDat, String trangThai) {
        String sql = "UPDATE DatPhong SET trangThai = ? WHERE maDat = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trangThai);
            ps.setString(2, maDat);
            boolean ok = ps.executeUpdate() > 0;
            
            // Đồng bộ trạng thái hóa đơn nếu là HỦY
            if (ok && "DA_HUY".equals(trangThai)) {
                new HoaDonDAO().updateTrangThaiByMaDatWithCon(con, maDat, "DA_HUY");
            }
            
            return ok;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTrangThai(String maDat, String trangThai) {
        try (Connection con = ConnectDatabase.getInstance().getConnection()) {
            return updateTrangThaiWithCon(con, maDat, trangThai);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}