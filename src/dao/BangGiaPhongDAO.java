package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import connectDatabase.ConnectDatabase;
import model.entities.BangGiaPhong;
import model.entities.LoaiPhong;

public class BangGiaPhongDAO {
    /**
     * Lấy thông tin giá phòng theo tên loại phòng (maLoaiPhong trong schema mới)
     */
    public BangGiaPhong getPriceByNameRoomType(String maLoai) {
        BangGiaPhong bg = null;
        String sql = "SELECT * FROM LoaiPhong WHERE maLoaiPhong = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
                    
            stmt.setString(1, maLoai);
            try (ResultSet rs = stmt.executeQuery()) {
                if(rs.next()) {
                    LoaiPhong lp = new LoaiPhong();
                    lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
                    lp.setGia(rs.getDouble("gia"));
                    lp.setSucChua(rs.getInt("sucChua"));
                    
                    bg = new BangGiaPhong(rs.getString("maLoaiPhong"), lp, rs.getDouble("gia"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return bg;
    }
}
