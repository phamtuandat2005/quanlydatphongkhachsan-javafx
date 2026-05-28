package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import connectDatabase.ConnectDatabase;
import model.entities.DichVu;
import model.utils.IdGenerator;

public class DichVuDAO {

    /**
     * Lấy tất cả dịch vụ từ bảng DV
     */
    public List<DichVu> getAll() {
        List<DichVu> ds = new ArrayList<>();
        String sql = "SELECT d.*, l.tenLoaiDV FROM DV d LEFT JOIN LoaiDichVu l ON d.maLoaiDV = l.maLoaiDV WHERE d.daXoa = 0";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ds.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Lấy tất cả dịch vụ Đang phục vụ (Trạng thái = 0)
     */
    public List<DichVu> getAllActive() {
        List<DichVu> ds = new ArrayList<>();
        String sql = "SELECT d.*, l.tenLoaiDV FROM DV d LEFT JOIN LoaiDichVu l ON d.maLoaiDV = l.maLoaiDV WHERE d.trangThai = 0 AND d.daXoa = 0";

        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ds.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Lấy danh sách dịch vụ theo loại
     */
    public List<DichVu> getByType(String loaiDV) {
        List<DichVu> ds = new ArrayList<>();
        String sql = "SELECT d.*, l.tenLoaiDV FROM DV d LEFT JOIN LoaiDichVu l ON d.maLoaiDV = l.maLoaiDV WHERE d.maLoaiDV = ? AND d.daXoa = 0";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loaiDV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Lấy danh sách dịch vụ theo loại và đang phục vụ
     */
    public List<DichVu> getActiveByType(String loaiDV) {
        List<DichVu> ds = new ArrayList<>();
        String sql = "SELECT d.*, l.tenLoaiDV FROM DV d LEFT JOIN LoaiDichVu l ON d.maLoaiDV = l.maLoaiDV WHERE d.maLoaiDV = ? AND d.trangThai = 0 AND d.daXoa = 0";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loaiDV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ds.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Cập nhật thông tin dịch vụ
     */
    public boolean update(DichVu dv) {
        String sql = "UPDATE DV SET tenDV = ?, gia = ?, maLoaiDV = ?, mieuTa = ?, donVi = ?, trangThai = ? WHERE maDV = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, dv.getTenDV());
            if (dv.getGia() != null) {
                pstmt.setDouble(2, dv.getGia());
            } else {
                pstmt.setNull(2, java.sql.Types.DECIMAL);
            }
            pstmt.setString(3, dv.getLoaiDV());
            pstmt.setString(4, dv.getMieuTa());
            pstmt.setString(5, dv.getDonVi());
            pstmt.setInt(6, dv.getTrangThai());
            pstmt.setString(7, dv.getMaDV());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Thêm mới dịch vụ
     */
    public boolean insert(DichVu dv) {
        String sql = "INSERT INTO DV (maDV, tenDV, gia, maLoaiDV, mieuTa, donVi, trangThai) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, dv.getMaDV());
            pstmt.setString(2, dv.getTenDV());
            if (dv.getGia() != null) {
                pstmt.setDouble(3, dv.getGia());
            } else {
                pstmt.setNull(3, java.sql.Types.DECIMAL);
            }
            pstmt.setString(4, dv.getLoaiDV());
            pstmt.setString(5, dv.getMieuTa());
            pstmt.setString(6, dv.getDonVi());
            pstmt.setInt(7, 0); // Active by default

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy thông tin dịch vụ theo mã dịch vụ (ID)
     */
    public DichVu getServiceByID(String ma) {
        String sql = "SELECT d.*, l.tenLoaiDV FROM DV d LEFT JOIN LoaiDichVu l ON d.maLoaiDV = l.maLoaiDV WHERE d.maDV = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, ma);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private DichVu mapRow(ResultSet rs) throws SQLException {
        DichVu dv = new DichVu();
        dv.setMaDV(rs.getString("maDV"));
        dv.setTenDV(rs.getString("tenDV"));
        Object giaObj = rs.getObject("gia");
        dv.setGia(giaObj != null ? rs.getDouble("gia") : null);
        dv.setLoaiDV(rs.getString("maLoaiDV"));
        try {
            dv.setTenLoaiDV(rs.getString("tenLoaiDV"));
        } catch (SQLException ignore) {}
        dv.setMieuTa(rs.getString("mieuTa"));
        dv.setDonVi(rs.getString("donVi"));
        try {
            dv.setTrangThai(rs.getInt("trangThai"));
        } catch (SQLException e) {
            dv.setTrangThai(0); // Graceful fallback
        }
        return dv;
    }

    /**
     * Kiểm tra mã dịch vụ đã tồn tại trong CSDL chưa
     */
    public boolean exists(String maDV) {
        String sql = "SELECT COUNT(*) FROM DV WHERE maDV = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDV);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Kiểm tra tên dịch vụ đã tồn tại trong CSDL chưa
     */
    public boolean existsTenDV(String tenDV, String excludeMaDV) {
        String sql = "SELECT COUNT(*) FROM DV WHERE LOWER(LTRIM(RTRIM(tenDV))) = LOWER(?)";
        if (excludeMaDV != null && !excludeMaDV.isEmpty()) {
            sql += " AND maDV != ?";
        }
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tenDV.trim());
            if (excludeMaDV != null && !excludeMaDV.isEmpty()) {
                ps.setString(2, excludeMaDV);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Xóa dịch vụ vĩnh viễn khỏi CSDL
     */
    public boolean delete(String maDV) {
        String sql = "DELETE FROM DV WHERE maDV = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDV);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sinh mã dịch vụ ngẫu nhiên
     */
    public String generateNextMaDV() {
        return IdGenerator.randomId("DV", 8);
    }
}