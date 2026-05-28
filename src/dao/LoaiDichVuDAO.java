package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import connectDatabase.ConnectDatabase;
import model.entities.LoaiDichVu;
import model.utils.IdGenerator;

public class LoaiDichVuDAO {

    public List<LoaiDichVu> getAll() {
        List<LoaiDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM LoaiDichVu";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new LoaiDichVu(rs.getString("maLoaiDV"), rs.getString("tenLoaiDV")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public LoaiDichVu getById(String id) {
        String sql = "SELECT * FROM LoaiDichVu WHERE maLoaiDV = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new LoaiDichVu(rs.getString("maLoaiDV"), rs.getString("tenLoaiDV"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(LoaiDichVu loaiDV) {
        String sql = "INSERT INTO LoaiDichVu (maLoaiDV, tenLoaiDV) VALUES (?, ?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, loaiDV.getMaLoaiDV());
            ps.setString(2, loaiDV.getTenLoaiDV());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String generateNextMaLoaiDV() {
        return IdGenerator.randomId("LDV", 8);
    }
}
