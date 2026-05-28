package dao;

import java.util.ArrayList;
import java.util.List;
import model.entities.DichVuSuDung;

public class DichVuSuDungDAO {
    // Boilerplate DAO cho bảng vừa mới tạo
    /**
     * Lấy danh sách dịch vụ sử dụng theo mã hóa đơn
     */
    public List<DichVuSuDung> findByMaHD(String maHD) {
        List<DichVuSuDung> ds = new ArrayList<>();
        String sql = "SELECT dv.maDV, dv.soLuong, dv.giaDV, dv.ngaySuDung, d.tenDV, d.donVi " +
                "FROM DichVuSuDung dv " +
                "JOIN DV d ON dv.maDV = d.maDV " +
                "JOIN ChiTietHoaDon cthd ON dv.maCTDP = cthd.maCTDP " +
                "WHERE cthd.maHD = ?";

        try (java.sql.Connection con = connectDatabase.ConnectDatabase.getInstance().getConnection();
                java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maHD);
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.entities.DichVu d = new model.entities.DichVu();
                d.setMaDV(rs.getString("maDV"));
                d.setTenDV(rs.getString("tenDV"));
                d.setGia(rs.getObject("giaDV") != null ? rs.getDouble("giaDV") : null);
                d.setDonVi(rs.getString("donVi"));

                DichVuSuDung dvsd = new DichVuSuDung();
                dvsd.setDichVu(d);
                dvsd.setGiaDV(rs.getDouble("giaDV"));
                dvsd.setSoLuong(rs.getInt("soLuong"));
                java.sql.Date sqlDate = rs.getDate("ngaySuDung");
                dvsd.setNgaySuDung(sqlDate != null ? sqlDate.toLocalDate() : null);
                ds.add(dvsd);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }

    /**
     * Lấy danh sách dịch vụ sử dụng theo mã chi tiết đặt phòng
     */
    public List<DichVuSuDung> findByMaCTDP(String maCTDP) {
        List<DichVuSuDung> ds = new ArrayList<>();
        String sql = "SELECT dv.maDV, dv.soLuong, dv.giaDV, dv.ngaySuDung, d.tenDV, d.donVi " +
                "FROM DichVuSuDung dv " +
                "JOIN DV d ON dv.maDV = d.maDV " +
                "WHERE dv.maCTDP = ?";

        try (java.sql.Connection con = connectDatabase.ConnectDatabase.getInstance().getConnection();
                java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maCTDP);
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.entities.DichVu d = new model.entities.DichVu();
                d.setMaDV(rs.getString("maDV"));
                d.setTenDV(rs.getString("tenDV"));
                d.setGia(rs.getObject("giaDV") != null ? rs.getDouble("giaDV") : null);
                d.setDonVi(rs.getString("donVi"));

                DichVuSuDung dvsd = new DichVuSuDung();
                dvsd.setDichVu(d);
                dvsd.setGiaDV(rs.getDouble("giaDV"));
                dvsd.setSoLuong(rs.getInt("soLuong"));
                java.sql.Date sqlDate = rs.getDate("ngaySuDung");
                dvsd.setNgaySuDung(sqlDate != null ? sqlDate.toLocalDate() : null);
                ds.add(dvsd);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return ds;
    }
}
