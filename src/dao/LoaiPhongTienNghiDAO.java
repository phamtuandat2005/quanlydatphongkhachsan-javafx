package dao;

import connectDatabase.ConnectDatabase;
import model.entities.LoaiPhong_TienNghi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class LoaiPhongTienNghiDAO {
    
    public Map<String, Integer> getTienNghiMapByLoaiPhong(String maLoaiPhong) {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT maTN, soLuong FROM LoaiPhongTienNghi WHERE maLoaiPhong = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maLoaiPhong);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("maTN"), rs.getInt("soLuong"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
    
    public boolean updateTienNghiForLoaiPhong(String maLoaiPhong, Map<String, Integer> mapMaTNSoLuong) {
        String deleteSql = "DELETE FROM LoaiPhongTienNghi WHERE maLoaiPhong = ?";
        String insertSql = "INSERT INTO LoaiPhongTienNghi(maLoaiPhong, maTN, soLuong) VALUES (?, ?, ?)";
        
        Connection con = null;
        try {
            con = ConnectDatabase.getInstance().getConnection();
            con.setAutoCommit(false);
            
            // Delete old mapping
            try (PreparedStatement pstmtDel = con.prepareStatement(deleteSql)) {
                pstmtDel.setString(1, maLoaiPhong);
                pstmtDel.executeUpdate();
            }
            
            // Insert new mapping
            if (mapMaTNSoLuong != null && !mapMaTNSoLuong.isEmpty()) {
                try (PreparedStatement pstmtIns = con.prepareStatement(insertSql)) {
                    for (Map.Entry<String, Integer> entry : mapMaTNSoLuong.entrySet()) {
                        pstmtIns.setString(1, maLoaiPhong);
                        pstmtIns.setString(2, entry.getKey());
                        pstmtIns.setInt(3, entry.getValue());
                        pstmtIns.addBatch();
                    }
                    pstmtIns.executeBatch();
                }
            }
            
            con.commit();
            return true;
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
