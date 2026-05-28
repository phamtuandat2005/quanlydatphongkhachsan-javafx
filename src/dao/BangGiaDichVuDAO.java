package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import connectDatabase.ConnectDatabase;
import model.entities.BangGiaDichVu;
import model.entities.BangGiaDichVu_ChiTiet;
import model.entities.DichVu;
import model.utils.IdGenerator;

public class BangGiaDichVuDAO {

    /**
     * Lấy danh sách tất cả bảng giá từ bảng BangGiaDV_Header
     */
    public List<BangGiaDichVu> getAllBangGia() {
        List<BangGiaDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGiaDV_Header WHERE daXoa = 0 ORDER BY ngayApDung DESC";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapBangGia(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy chi tiết của một bảng giá từ bảng BangGiaDV_Detail
     */
    public List<BangGiaDichVu_ChiTiet> getChiTietByMa(String maBG) {
        List<BangGiaDichVu_ChiTiet> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGiaDV_Detail WHERE maBangGia = ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maBG);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapChiTiet(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Thêm mới bảng giá thông tin
     */
    public boolean insertBangGia(BangGiaDichVu bg) {
        String sql = "INSERT INTO BangGiaDV_Header (maBangGia, tenBangGia, ngayApDung, ngayHetHieuLuc, trangThai) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bg.getMaBangGia());
            ps.setString(2, bg.getTenBangGia());
            ps.setDate(3, new java.sql.Date(bg.getNgayApDung().getTime()));
            ps.setDate(4, new java.sql.Date(bg.getNgayHetHieuLuc().getTime()));
            ps.setInt(5, bg.getTrangThai());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Thêm chi tiết bảng giá
     */
    public boolean insertChiTiet(BangGiaDichVu_ChiTiet ct) {
        String sql = "INSERT INTO BangGiaDV_Detail (maBangGia, maDV, giaDV) VALUES (?, ?, ?)";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ct.getMaBangGia().getMaBangGia());
            ps.setString(2, ct.getMaDichVu().getMaDV());
            if (ct.getGiaDichVu() != null) {
                ps.setDouble(3, ct.getGiaDichVu());
            } else {
                ps.setNull(3, java.sql.Types.DECIMAL);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa bảng giá (Transaction: Xóa chi tiết trước)
     */
    public boolean deleteBangGia(String maBG) {
        String sqlChiTiet = "DELETE FROM BangGiaDV_Detail WHERE maBangGia = ?";
        String sqlThongTin = "DELETE FROM BangGiaDV_Header WHERE maBangGia = ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlChiTiet);
                    PreparedStatement ps2 = conn.prepareStatement(sqlThongTin)) {
                ps1.setString(1, maBG);
                ps1.executeUpdate();
                ps2.setString(1, maBG);
                ps2.executeUpdate();
                conn.commit();
                return true;
            } catch (Exception ex) {
                conn.rollback();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map dữ liệu từ ResultSet sang Object BangGiaDichVu
     */
    private BangGiaDichVu mapBangGia(ResultSet rs) throws SQLException {
        BangGiaDichVu bg = new BangGiaDichVu();
        bg.setMaBangGia(rs.getString("maBangGia"));
        bg.setTenBangGia(rs.getString("tenBangGia"));
        bg.setNgayApDung(rs.getDate("ngayApDung"));
        bg.setNgayHetHieuLuc(rs.getDate("ngayHetHieuLuc"));
        bg.setTrangThai(rs.getInt("trangThai"));
        return bg;
    }

    /**
     * Map dữ liệu từ ResultSet sang Object BangGiaDichVu_ChiTiet
     */
    private BangGiaDichVu_ChiTiet mapChiTiet(ResultSet rs) throws SQLException {
        BangGiaDichVu_ChiTiet ct = new BangGiaDichVu_ChiTiet();
        ct.setMaBangGia(new BangGiaDichVu(rs.getString("maBangGia")));
        ct.setMaDichVu(new DichVu(rs.getString("maDV")));
        Object giaObj = rs.getObject("giaDV");
        ct.setGiaDichVu(giaObj != null ? rs.getDouble("giaDV") : null);
        return ct;
    }

    /**
     * Lưu toàn bộ bảng giá và chi tiết dịch vụ đi kèm (FIX: đóng connection đúng
     * cách)
     */
    public boolean insertFullBangGia(BangGiaDichVu thongTin, List<BangGiaDichVu_ChiTiet> dsChiTiet) {
        try (Connection con = ConnectDatabase.getInstance().getConnection()) {
            con.setAutoCommit(false);

            String sqlThongTin = "INSERT INTO BangGiaDV_Header (maBangGia, tenBangGia, ngayApDung, ngayHetHieuLuc, trangThai) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps1 = con.prepareStatement(sqlThongTin)) {
                ps1.setString(1, thongTin.getMaBangGia());
                ps1.setString(2, thongTin.getTenBangGia());
                ps1.setDate(3, new java.sql.Date(thongTin.getNgayApDung().getTime()));
                ps1.setDate(4, new java.sql.Date(thongTin.getNgayHetHieuLuc().getTime()));
                ps1.setInt(5, thongTin.getTrangThai());
                ps1.executeUpdate();
            }

            String sqlChiTiet = "INSERT INTO BangGiaDV_Detail (maBangGia, maDV, giaDV) VALUES (?, ?, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(sqlChiTiet)) {
                for (BangGiaDichVu_ChiTiet ct : dsChiTiet) {
                    ps2.setString(1, thongTin.getMaBangGia());
                    ps2.setString(2, ct.getMaDichVu().getMaDV());
                    if (ct.getGiaDichVu() != null) {
                        ps2.setDouble(3, ct.getGiaDichVu());
                    } else {
                        ps2.setNull(3, java.sql.Types.DECIMAL);
                    }
                    ps2.addBatch();
                }
                ps2.executeBatch();
            }

            con.commit();
            syncActivePricesToDB();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật toàn bộ bảng giá và chi tiết dịch vụ đi kèm
     * FIX: thêm trangThai vào UPDATE + đóng connection đúng cách
     */
    public boolean updateFullBangGia(BangGiaDichVu bg, List<BangGiaDichVu_ChiTiet> dsChiTiet) {
        try (Connection con = ConnectDatabase.getInstance().getConnection()) {
            con.setAutoCommit(false);

            String sqlUpdateBG = "UPDATE BangGiaDV_Header SET tenBangGia = ?, ngayApDung = ?, ngayHetHieuLuc = ?, trangThai = ? WHERE maBangGia = ?";
            try (PreparedStatement ps1 = con.prepareStatement(sqlUpdateBG)) {
                ps1.setString(1, bg.getTenBangGia());
                ps1.setDate(2, new java.sql.Date(bg.getNgayApDung().getTime()));
                ps1.setDate(3, new java.sql.Date(bg.getNgayHetHieuLuc().getTime()));
                ps1.setInt(4, bg.getTrangThai());
                ps1.setString(5, bg.getMaBangGia());
                ps1.executeUpdate();
            }

            String sqlDeleteDetails = "DELETE FROM BangGiaDV_Detail WHERE maBangGia = ?";
            try (PreparedStatement ps2 = con.prepareStatement(sqlDeleteDetails)) {
                ps2.setString(1, bg.getMaBangGia());
                ps2.executeUpdate();
            }

            String sqlInsertDetails = "INSERT INTO BangGiaDV_Detail (maBangGia, maDV, giaDV) VALUES (?, ?, ?)";
            try (PreparedStatement ps3 = con.prepareStatement(sqlInsertDetails)) {
                for (BangGiaDichVu_ChiTiet ct : dsChiTiet) {
                    ps3.setString(1, bg.getMaBangGia());
                    ps3.setString(2, ct.getMaDichVu().getMaDV());
                    if (ct.getGiaDichVu() != null) {
                        ps3.setDouble(3, ct.getGiaDichVu());
                    } else {
                        ps3.setNull(3, java.sql.Types.DECIMAL);
                    }
                    ps3.addBatch();
                }
                ps3.executeBatch();
            }

            con.commit();
            syncActivePricesToDB();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy bảng giá theo mã
     */
    public BangGiaDichVu getBangGiaByMa(String maBG) {
        String sql = "SELECT * FROM BangGiaDV_Header WHERE maBangGia = ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maBG);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapBangGia(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cập nhật trạng thái bảng giá (bật/tắt)
     */
    public boolean updateTrangThai(String maBangGia, int trangThai) {
        String sql = "UPDATE BangGiaDV_Header SET trangThai = ? WHERE maBangGia = ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trangThai);
            ps.setString(2, maBangGia);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tìm các bảng giá bị xung đột thời gian (gối đầu) với một khoảng [ngayAD,
     * ngayHH]
     * Loại trừ bảng giá chính mình (theo maBG, dùng khi sửa)
     */
    public List<BangGiaDichVu> findConflicts(java.sql.Date ngayAD, java.sql.Date ngayHH, String excludeMaBG) {
        List<BangGiaDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGiaDV_Header WHERE trangThai = 0 AND daXoa = 0 AND ngayApDung <= ? AND ngayHetHieuLuc >= ?";
        if (excludeMaBG != null && !excludeMaBG.isEmpty()) {
            sql += " AND maBangGia <> ?";
        }
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, ngayHH);
            ps.setDate(2, ngayAD);
            if (excludeMaBG != null && !excludeMaBG.isEmpty()) {
                ps.setString(3, excludeMaBG);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBangGia(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Sinh mã bảng giá tiếp theo (BG001, BG002, ...)
     */
    public String generateNextMaBangGia() {
        return IdGenerator.randomId("BG", 8);
    }

    /**
     * Soft delete: set trangThai = 0 thay vì xóa vật lý
     */
    public boolean softDeleteBangGia(String maBG) {
        String sql = "UPDATE BangGiaDV_Header SET trangThai = 1, daXoa = 1 WHERE maBangGia = ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maBG);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra mã bảng giá đã tồn tại chưa
     */
    public boolean exists(String maBG) {
        // Sử dụng UPPER và TRIM để đối soát tuyệt đối chính xác trong mọi trường hợp
        // collation/padding
        String sql = "SELECT COUNT(*) FROM BangGiaDV_Header WHERE UPPER(LTRIM(RTRIM(maBangGia))) = UPPER(?)";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maBG.trim());
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
     * Kiểm tra xung đột thời gian khi thêm mới bảng giá
     */
    public boolean checkOverlap(java.sql.Date ngayAD, java.sql.Date ngayHH) {
        String sql = "SELECT COUNT(*) FROM BangGiaDV_Header WHERE trangThai = 0 AND daXoa = 0 AND ngayApDung <= ? AND ngayHetHieuLuc >= ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, ngayHH);
            ps.setDate(2, ngayAD);
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
     * Kiểm tra xung đột thời gian khi cập nhật bảng giá (loại trừ chính nó)
     */
    public boolean checkOverlapUpdate(String maBG, java.sql.Date ngayAD, java.sql.Date ngayHH) {
        String sql = "SELECT COUNT(*) FROM BangGiaDV_Header WHERE trangThai = 0 AND daXoa = 0 AND ngayApDung <= ? AND ngayHetHieuLuc >= ? AND maBangGia <> ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, ngayHH);
            ps.setDate(2, ngayAD);
            ps.setString(3, maBG);
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
     * Tìm các bảng giá xung đột thời gian NGHIÊM NGẶT (trangThai != -1, tức chưa
     * xóa mềm).
     * Dùng cho logic chặn cứng: không cho phép tồn tại 2 bảng giá chồng lấn thời
     * gian.
     * 
     * @param excludeMaBG mã bảng giá cần loại trừ (khi sửa), null nếu thêm mới
     */
    public List<BangGiaDichVu> findStrictConflicts(java.sql.Date ngayAD, java.sql.Date ngayHH, String excludeMaBG) {
        List<BangGiaDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGiaDV_Header WHERE trangThai = 0 AND daXoa = 0 AND ngayApDung <= ? AND ngayHetHieuLuc >= ?";
        if (excludeMaBG != null && !excludeMaBG.isEmpty()) {
            sql += " AND maBangGia <> ?";
        }
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, ngayHH);
            ps.setDate(2, ngayAD);
            if (excludeMaBG != null && !excludeMaBG.isEmpty()) {
                ps.setString(3, excludeMaBG);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBangGia(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy giá dịch vụ từ bảng giá đang hiệu lực (active) tại thời điểm hiện tại.
     * Trả về Map<maDV, giaDV>. Nếu không có bảng giá active, trả map rỗng.
     */
    public java.util.Map<String, Double> getActivePriceMap() {
        java.util.Map<String, Double> map = new java.util.LinkedHashMap<>();
        // Tìm bảng giá có trangThai = 0 (Đang áp dụng) và hôm nay nằm trong
        // [ngayApDung, ngayHetHieuLuc]
        String sql = "SELECT d.maDV, d.giaDV, h.maBangGia, h.tenBangGia FROM BangGiaDV_Detail d " +
                "INNER JOIN BangGiaDV_Header h ON d.maBangGia = h.maBangGia " +
                "WHERE h.trangThai = 0 AND h.daXoa = 0 AND ? >= h.ngayApDung AND ? < h.ngayHetHieuLuc";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            ps.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("maDV"), rs.getDouble("giaDV"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Cập nhật trạng thái bảng giá dựa trên tên trạng thái từ ComboBox UI
     * 
     * @param maBG         Mã bảng giá cần cập nhật
     * @param tenTrangThai "Đang áp dụng", "Chờ áp dụng", hoặc "Ngưng áp dụng"
     * @return true nếu cập nhật thành công
     */
    public boolean updateTrangThaiTheoTen(String maBG, String tenTrangThai) {
        int trangThaiInt = 0;
        boolean isActivating = false;

        if (tenTrangThai.equals("Ngưng áp dụng")) {
            trangThaiInt = 1;
        } else if (tenTrangThai.equals("Đang áp dụng") || tenTrangThai.equals("Chờ áp dụng")) {
            trangThaiInt = 0;
            isActivating = true;
        }

        try (Connection conn = ConnectDatabase.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            // 1. Cập nhật trạng thái bit
            String sqlStatus = "UPDATE BangGiaDV_Header SET trangThai = ? WHERE maBangGia = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(sqlStatus)) {
                ps1.setInt(1, trangThaiInt);
                ps1.setString(2, maBG);
                ps1.executeUpdate();
            }

            // 2. Không cần chốt ngày kết thúc tự động nữa (Theo yêu cầu: giữ nguyên ngày đã
            // set)

            conn.commit();
            syncActivePricesToDB();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đồng bộ giá từ bảng giá đang active vào cột gia trong bảng DV.
     * Gọi sau khi thêm/sửa/kích hoạt bảng giá.
     */
    public void syncActivePricesToDB() {
        java.util.Map<String, Double> activePrices = getActivePriceMap();
        String resetSql = "UPDATE DV SET gia = NULL";
        String updateSql = "UPDATE DV SET gia = ? WHERE maDV = ?";

        try (Connection conn = ConnectDatabase.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psReset = conn.prepareStatement(resetSql)) {
                psReset.executeUpdate();
            }

            if (!activePrices.isEmpty()) {
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    for (java.util.Map.Entry<String, Double> entry : activePrices.entrySet()) {
                        psUpdate.setDouble(1, entry.getValue());
                        psUpdate.setString(2, entry.getKey());
                        psUpdate.addBatch();
                    }
                    psUpdate.executeBatch();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lấy giá hiện tại của một dịch vụ từ bảng giá đang hiệu lực.
     */
    public double getGiaHienTai(String maDV) {
        String sql = "SELECT d.giaDV FROM BangGiaDV_Detail d " +
                "INNER JOIN BangGiaDV_Header h ON d.maBangGia = h.maBangGia " +
                "WHERE h.trangThai = 0 AND h.daXoa = 0 AND CAST(GETDATE() AS DATE) >= h.ngayApDung AND CAST(GETDATE() AS DATE) < h.ngayHetHieuLuc " +
                "AND d.maDV = ?";
        try (Connection conn = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maDV);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("giaDV");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
