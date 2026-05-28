package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import connectDatabase.ConnectDatabase;
import model.entities.LoaiPhong;
import model.enums.TenLoaiPhong;

public class LoaiPhongDAO {

    private TenLoaiPhong findEnumByString(String loaiPhong) {
        loaiPhong = loaiPhong.toUpperCase();
        if (loaiPhong.equals("DOUBLE")) {
            return TenLoaiPhong.DOUBLE;
        } else if (loaiPhong.equals("FAMILY"))
            return TenLoaiPhong.FAMILY;
        else if (loaiPhong.equals("SINGLE"))
            return TenLoaiPhong.SINGLE;
        else if (loaiPhong.equals("TRIPLE"))
            return TenLoaiPhong.TRIPLE;
        else if (loaiPhong.equals("TWIN"))
            return TenLoaiPhong.TWIN;
        else
            return null;
    }

    /**
     * Lấy toàn bộ tên loại phòng để hiển thị lên ComboBox
     */
    public String[] fetchAllRoomTypeNames() {
        List<String> list = new ArrayList<>();
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT maLoaiPhong FROM LoaiPhong")) {

            while (rs.next()) {
                list.add(findEnumByString(rs.getString("maLoaiPhong")).getDisplayName());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tất cả loại phòng: " + e.getMessage());
        }
        return list.toArray(new String[0]);
    }

    /**
     * Lấy toàn bộ thực thể LoaiPhong
     */
    public List<LoaiPhong> getAll() {
        List<LoaiPhong> ds = new ArrayList<>();
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM LoaiPhong")) {

            while (rs.next()) {
                ds.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Tìm theo mã loại phòng
     */
    public LoaiPhong findByID(String maLoaiPhong) {
        String sql = "SELECT * FROM LoaiPhong WHERE maLoaiPhong = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maLoaiPhong);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private LoaiPhong mapRow(ResultSet rs) throws SQLException {
        LoaiPhong lp = new LoaiPhong();
        lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
        lp.setGia(rs.getDouble("gia"));
        lp.setSucChua(rs.getInt("sucChua"));
        return lp;
    }

    /**
     * Lấy danh sách mã loại phòng (dùng cho ComboBox đặt phòng)
     */
    public List<String> getAllMaLoaiPhong() {
        List<String> list = new ArrayList<>();
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT maLoaiPhong FROM LoaiPhong")) {
            while (rs.next())
                list.add(rs.getString("maLoaiPhong"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy giá phòng theo mã loại
     */
    public double getGiaByMaLoai(String maLoaiPhong) {
        LoaiPhong lp = findByID(maLoaiPhong);
        return lp != null ? lp.getGia() : 0;
    }

    /**
     * Thêm loại phòng mới
     */
    public boolean insertLoaiPhong(LoaiPhong loaiPhong) {
        String sql = "INSERT INTO LoaiPhong(maLoaiPhong, gia, sucChua) VALUES (?, ?, ?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, loaiPhong.getMaLoaiPhong());
            pstmt.setDouble(2, loaiPhong.getGia());
            pstmt.setInt(3, loaiPhong.getSucChua());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Cập nhật giá phòng cho một loại phòng
     */
    public boolean updateGiaByMaLoai(String maLoaiPhong, double gia) {
        String sql = "UPDATE LoaiPhong SET gia = ? WHERE maLoaiPhong = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setDouble(1, gia);
            pstmt.setString(2, maLoaiPhong);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}