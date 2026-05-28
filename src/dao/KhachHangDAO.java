package dao;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import connectDatabase.ConnectDatabase;
import model.entities.KhachHang;
import model.utils.IdGenerator;

/**
 * DAO cho bảng KH (KhachHang).
 */
public class KhachHangDAO {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // LẤY TOÀN BỘ
    // ─────────────────────────────────────────────────────────────────────────
    public List<KhachHang> getAll() {
        List<KhachHang> ds = new ArrayList<>();
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            ResultSet rs = con.createStatement()
                    .executeQuery("SELECT * FROM KH WHERE daXoa = 0");
            while (rs.next())
                ds.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("getAll KH: " + e.getMessage());
        }
        return ds;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // THÊM
    // ─────────────────────────────────────────────────────────────────────────
    public boolean insert(KhachHang kh) {
        String sql = "INSERT INTO KH "
                + "(maKH, tenKH, soDT, ngaySinh, soCCCD) "
                + "VALUES (?,?,?,?,?)";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, kh.getMaKH());
            pstmt.setString(2, kh.getTenKH());
            pstmt.setString(3, kh.getSoDT());
            if (kh.getNgaySinh() != null)
                pstmt.setDate(4, Date.valueOf(kh.getNgaySinh()));
            else
                pstmt.setNull(4, Types.DATE);
            pstmt.setString(5, kh.getSoCCCD());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("insert KH: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CẬP NHẬT
    // ─────────────────────────────────────────────────────────────────────────
    public boolean update(KhachHang kh) {
        String sql = "UPDATE KH "
                + "SET tenKH=?, soDT=?, ngaySinh=?, soCCCD=? "
                + "WHERE maKH=?";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, kh.getTenKH());
            pstmt.setString(2, kh.getSoDT());
            if (kh.getNgaySinh() != null)
                pstmt.setDate(3, Date.valueOf(kh.getNgaySinh()));
            else
                pstmt.setNull(3, Types.DATE);
            pstmt.setString(4, kh.getSoCCCD());
            pstmt.setString(5, kh.getMaKH());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("update KH: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XÓA
    // ─────────────────────────────────────────────────────────────────────────
    public boolean delete(String maKH) {
        if (hasActiveBooking(maKH)) {
            return false;
        }
        String sql = "UPDATE KH SET daXoa = 1 WHERE maKH = ?";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, maKH);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("delete KH: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // KIỂM TRA ĐƠN ĐẶT PHÒNG HOẠT ĐỘNG
    // ─────────────────────────────────────────────────────────────────────────
    public boolean hasActiveBooking(String maKH) {
        String sql = "SELECT COUNT(*) FROM DatPhong WHERE maKH = ? AND trangThai IN (N'CHO_XACNHAN', N'DA_XACNHAN', N'DA_CHECKIN')";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, maKH);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("hasActiveBooking: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÌM THEO CCCD
    // ─────────────────────────────────────────────────────────────────────────
    public KhachHang findByCCCD(String cccd) {
        String sql = "SELECT * FROM KH WHERE soCCCD = ? AND daXoa = 0";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, cccd);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÌM THEO SỐ ĐIỆN THOẠI
    // ─────────────────────────────────────────────────────────────────────────
    public KhachHang findBySDT(String soDT) {
        String sql = "SELECT * FROM KH WHERE soDT = ? AND daXoa = 0";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, soDT);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÌM THEO MÃ (LIKE)
    // ─────────────────────────────────────────────────────────────────────────
    public KhachHang findKhachHangByID(String keyword) {
        String sql = "SELECT * FROM KH WHERE maKH LIKE ? AND daXoa = 0";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SINH MÃ KH TIẾP THEO
    // ─────────────────────────────────────────────────────────────────────────
    public String getNextMaKH() {
        return IdGenerator.randomId("KH", 8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐẾM KHÁCH CÓ SINH NHẬT HÔM NAY
    // ─────────────────────────────────────────────────────────────────────────
    public int getBirthdayTodayCount() {
        String sql = "SELECT COUNT(*) FROM KH "
                + "WHERE DAY(ngaySinh)   = DAY(GETDATE()) "
                + "  AND MONTH(ngaySinh) = MONTH(GETDATE())";
        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            ResultSet rs = con.createStatement().executeQuery(sql);
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DANH SÁCH KHÁCH SINH NHẬT HÔM NAY + PHÒNG ĐANG Ở
    // ─────────────────────────────────────────────────────────────────────────
    public List<String[]> getBirthdayTodayWithRoom() {
        Map<String, String[]> custMap = new LinkedHashMap<>();
        Map<String, List<String>> roomMap = new HashMap<>();

        String sql = "SELECT kh.maKH, kh.tenKH, kh.soDT, kh.ngaySinh, " +
                "       p.maPhong " +
                "FROM   KH kh " +
                "LEFT JOIN DatPhong dp " +
                "       ON  kh.maKH        = dp.maKH " +
                "       AND dp.ngayCheckIn  IS NOT NULL " +
                "       AND dp.ngayCheckOut IS NULL " +
                "LEFT JOIN ChiTietDatPhong ctdp ON dp.maDat = ctdp.maDat " +
                "LEFT JOIN Phong           p    ON ctdp.maPhong  = p.maPhong " +
                "WHERE DAY(kh.ngaySinh)   = DAY(GETDATE()) " +
                "  AND MONTH(kh.ngaySinh) = MONTH(GETDATE())";

        try {
            Connection con = ConnectDatabase.getInstance().getConnection();
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                String maKH = rs.getString("maKH");
                if (!custMap.containsKey(maKH)) {
                    Date d = rs.getDate("ngaySinh");
                    String nsStr = d != null
                            ? d.toLocalDate().format(DATE_FMT)
                            : "";
                    custMap.put(maKH, new String[] {
                            maKH,
                            rs.getString("tenKH"),
                            rs.getString("soDT"),
                            nsStr
                    });
                    roomMap.put(maKH, new ArrayList<>());
                }
                String maPhong = rs.getString("maPhong");
                if (maPhong != null)
                    roomMap.get(maKH).add(maPhong);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<String[]> result = new ArrayList<>();
        for (String maKH : custMap.keySet()) {
            String[] data = custMap.get(maKH);
            List<String> rooms = roomMap.get(maKH);
            String roomStr = rooms.isEmpty() ? "Chưa nhận phòng"
                    : String.join(", ", rooms);
            result.add(new String[] { data[0], data[1], data[2], data[3], roomStr });
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: ánh xạ ResultSet → KhachHang
    // ─────────────────────────────────────────────────────────────────────────
    private KhachHang mapRow(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang(
                rs.getString("maKH"),
                rs.getString("tenKH"),
                rs.getString("soCCCD"),
                rs.getString("soDT"));
        Date d = rs.getDate("ngaySinh");
        if (d != null)
            kh.setNgaySinh(d.toLocalDate());
        
        try {
            kh.setDaXoa(rs.getBoolean("daXoa"));
        } catch (SQLException ignored) {}
        
        return kh;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CẬP NHẬT KH THEO MÃ ĐẶT PHÒNG (dùng chung Connection cho transaction)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean updateByMaDat(Connection con, String maDat, String ten,
            String sdt, String cccd, LocalDate ngaySinh) throws java.sql.SQLException {
        String sql = "UPDATE KH SET tenKH=?, soDT=?, soCCCD=?, ngaySinh=? WHERE maKH=(SELECT maKH FROM DatPhong WHERE maDat=?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ten);
            ps.setString(2, sdt);
            ps.setString(3, cccd);
            if (ngaySinh != null)
                ps.setDate(4, Date.valueOf(ngaySinh));
            else
                ps.setNull(4, Types.DATE);
            ps.setString(5, maDat);
            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÌM HOẶC TẠO MỚI KH (dùng chung Connection cho transaction)
    // Tìm theo CCCD hoặc SĐT → nếu có thì cập nhật, nếu chưa thì insert mới
    // ─────────────────────────────────────────────────────────────────────────
    public String findOrCreate(Connection con, String ten, String sdt, String cccd,
            LocalDate ngaySinh, String preGenMaKH) throws java.sql.SQLException {
        for (String col : new String[] { "soCCCD", "soDT" }) {
            try (PreparedStatement ps = con.prepareStatement("SELECT maKH FROM KH WHERE " + col + " = ?")) {
                ps.setString(1, col.equals("soCCCD") ? cccd : sdt);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String existingMaKH = rs.getString("maKH");
                    try (PreparedStatement psU = con.prepareStatement(
                            "UPDATE KH SET tenKH=?, soDT=?, soCCCD=?, ngaySinh=? WHERE maKH=?")) {
                        psU.setString(1, ten);
                        psU.setString(2, sdt);
                        psU.setString(3, cccd);
                        if (ngaySinh != null)
                            psU.setDate(4, Date.valueOf(ngaySinh));
                        else
                            psU.setNull(4, Types.DATE);
                        psU.setString(5, existingMaKH);
                        psU.executeUpdate();
                    }
                    return existingMaKH;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO KH(maKH, tenKH, soDT, soCCCD, ngaySinh) VALUES(?,?,?,?,?)")) {
            ps.setString(1, preGenMaKH);
            ps.setString(2, ten);
            ps.setString(3, sdt);
            ps.setString(4, cccd);
            if (ngaySinh != null)
                ps.setDate(5, Date.valueOf(ngaySinh));
            else
                ps.setNull(5, Types.DATE);
            ps.executeUpdate();
        }
        return preGenMaKH;
    }
}