package dao;

import connectDatabase.ConnectDatabase;
import model.entities.TienNghi;
import model.utils.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TienNghiDAO {
    
    public List<TienNghi> getAll() {
        List<TienNghi> list = new ArrayList<>();
        String sql = "SELECT * FROM TienNghi WHERE daXoa = 0";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new TienNghi(
                        rs.getString("maTN"),
                        rs.getString("tenTN"),
                        rs.getString("moTa"),
                        rs.getBoolean("trangThai")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public boolean insert(TienNghi tn) {
        String sql = "INSERT INTO TienNghi(maTN, tenTN, moTa, trangThai) VALUES (?, ?, ?, ?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, tn.getMaTienNghi());
            pstmt.setString(2, tn.getTenTienNghi());
            pstmt.setString(3, tn.getMoTa());
            pstmt.setBoolean(4, tn.isTrangThai());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean update(TienNghi tn) {
        String sql = "UPDATE TienNghi SET tenTN = ?, moTa = ?, trangThai = ? WHERE maTN = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, tn.getTenTienNghi());
            pstmt.setString(2, tn.getMoTa());
            pstmt.setBoolean(3, tn.isTrangThai());
            pstmt.setString(4, tn.getMaTienNghi());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean delete(String maTN) {
        // Soft delete bằng daXoa
        String sql = "UPDATE TienNghi SET daXoa = 1 WHERE maTN = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maTN);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public String generateNewMaTN() {
        return IdGenerator.randomId("TN", 8);
    }
}
