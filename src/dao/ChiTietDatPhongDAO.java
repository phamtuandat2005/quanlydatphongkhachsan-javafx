package dao;

import java.sql.*;
import connectDatabase.ConnectDatabase;
import model.entities.ChiTietDatPhong;
import model.entities.DatPhong;
import model.entities.Phong;
import model.utils.IdGenerator;

public class ChiTietDatPhongDAO {

    /**
     * TÃ¬m chi tiáº¿t Ä‘áº·t phÃ²ng dá»±a trÃªn mÃ£ Ä‘áº·t vÃ  mÃ£ phÃ²ng
     */
    public ChiTietDatPhong findChiTietDatPhong(String maDat, String maPhong) {
        String sql = "SELECT * FROM ChiTietDatPhong WHERE maDat = ? AND maPhong = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, maDat);
            pstmt.setString(2, maPhong);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ThÃªm má»›i chi tiáº¿t Ä‘áº·t phÃ²ng
     */
    public boolean insert(ChiTietDatPhong ctdp) {
        String sql = "INSERT INTO ChiTietDatPhong (maCTDP, maPhong, maDat, giaCoc, soNguoi, ghiChu) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, ctdp.getMaCTDP());
            pstmt.setString(2, ctdp.getPhong().getMaPhong());
            pstmt.setString(3, ctdp.getDatPhong().getMaDat());
            pstmt.setDouble(4, ctdp.getGiaCoc());
            pstmt.setInt(5, ctdp.getSoNguoi());
            pstmt.setString(6, ctdp.getGhiChu());

            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private ChiTietDatPhong mapRow(ResultSet rs) throws SQLException {
        ChiTietDatPhong ctdp = new ChiTietDatPhong();
        ctdp.setMaCTDP(rs.getString("maCTDP"));
        String maPhong = rs.getString("maPhong");
        ctdp.setPhong(new PhongDAO().getAll().stream()
                .filter(p -> p.getMaPhong().equals(maPhong))
                .findFirst().orElse(new Phong(maPhong)));
        ctdp.setDatPhong(new DatPhong(rs.getString("maDat")));
        ctdp.setGiaCoc(rs.getDouble("giaCoc"));
        ctdp.setSoNguoi(rs.getInt("soNguoi"));
        ctdp.setGhiChu(rs.getString("ghiChu"));
        return ctdp;
    }

    /**
     * Tá»± Ä‘á»™ng phÃ¡t sinh mÃ£ chi tiáº¿t Ä‘áº·t phÃ²ng
     */
    public String generateMaCTDP() {
        return IdGenerator.randomId("CTDP", 8);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // INSERT (dÃ¹ng chung Connection cho transaction)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean insertWithConnection(Connection con, String maCTDP, String maPhong,
            String maDat, double giaCoc, int soNguoi, String ghiChu) throws SQLException {
        String sql = "INSERT INTO ChiTietDatPhong(maCTDP, maPhong, maDat, giaCoc, soNguoi, ghiChu) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maCTDP);
            ps.setString(2, maPhong);
            ps.setString(3, maDat);
            ps.setDouble(4, giaCoc);
            ps.setInt(5, soNguoi);
            ps.setString(6, ghiChu);
            return ps.executeUpdate() > 0;
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Cáº¬P NHáº¬T CHI TIáº¾T Äáº¶T PHÃ’NG THEO MÃƒ Äáº¶T (dÃ¹ng chung
    // Connection)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean updateByMaDat(Connection con, String maDat, String maPhong,
            double giaCoc, int soNguoi, String ghiChu) throws SQLException {
        String sql = "UPDATE ChiTietDatPhong SET maPhong=?, giaCoc=?, soNguoi=?, ghiChu=? WHERE maDat=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ps.setDouble(2, giaCoc);
            ps.setInt(3, soNguoi);
            ps.setString(4, ghiChu);
            ps.setString(5, maDat);
            return ps.executeUpdate() > 0;
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Cáº¬P NHáº¬T THÃ”NG TIN CHUNG (KHÃ”NG Äá»”I MÃƒ PHÃ’NG) CHO MULTI-ROOM
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean updateInfoByMaDat(Connection con, String maDat,
            double giaCoc, int soNguoi, String ghiChu) throws SQLException {
        int roomCount = 1;
        try (PreparedStatement psCount = con.prepareStatement("SELECT COUNT(*) FROM ChiTietDatPhong WHERE maDat=?")) {
            psCount.setString(1, maDat);
            ResultSet rsCount = psCount.executeQuery();
            if (rsCount.next())
                roomCount = Math.max(1, rsCount.getInt(1));
        }

        double cdpCoc = giaCoc / roomCount;
        int cdpNguoi = soNguoi / roomCount;

        String sql = "UPDATE ChiTietDatPhong SET giaCoc=?, soNguoi=?, ghiChu=? WHERE maDat=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, cdpCoc);
            ps.setInt(2, cdpNguoi);
            ps.setString(3, ghiChu);
            ps.setString(4, maDat);
            return ps.executeUpdate() > 0;
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // [THÃŠM Má»šI] Láº¤Y Tá»”NG TIá»€N Cá»ŒC Cá»¦A Má»˜T ÄÆ N Äáº¶T PHÃ’NG
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public double getTongCocByMaDat(String maDat) {
        String sql = "SELECT SUM(giaCoc) FROM ChiTietDatPhong WHERE maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maDat);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // [THÃŠM Má»šI] Láº¤Y KIá»‚U PHÃ’NG THEO MÃƒ Äáº¶T
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public java.util.List<String> getMaPhongByMaDat(String maDat) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT maPhong FROM ChiTietDatPhong WHERE maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maDat);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                list.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // [THÃŠM Má»šI] XÃ“A TOÃ€N Bá»˜ CHI TIáº¾T Äáº¶T PHÃ’NG Dá»°A TRÃŠN MÃƒ Äáº¶T
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean deleteByMaDat(String maDat) {
        String sql = "DELETE FROM ChiTietDatPhong WHERE maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, maDat);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lay ds phong trong don kem gia/dem tu LoaiPhong.
     * Object[]: {maCTDP, maPhong, giaPhong, giaCoc, soNguoi}
     */
    public java.util.List<Object[]> getPhongWithPriceByMaDat(String maDat) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT ctdp.maCTDP, ctdp.maPhong, lp.gia AS giaPhong, ctdp.giaCoc, ctdp.soNguoi " +
                "FROM ChiTietDatPhong ctdp " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "JOIN LoaiPhong lp ON p.loaiPhong = lp.maLoaiPhong " +
                "WHERE ctdp.maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[] {
                        rs.getString("maCTDP"),
                        rs.getString("maPhong"),
                        rs.getDouble("giaPhong"),
                        rs.getDouble("giaCoc"),
                        rs.getInt("soNguoi")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lay ds phong trong don kem ten loai phong.
     * Object[]: {maPhong, tenLoaiPhong}
     */
    public java.util.List<Object[]> getPhongDetailsByMaDat(String maDat) {
        java.util.List<Object[]> list = new java.util.ArrayList<>();
        String sql = "SELECT ctdp.maPhong, p.loaiPhong " +
                "FROM ChiTietDatPhong ctdp " +
                "JOIN Phong p ON ctdp.maPhong = p.maPhong " +
                "WHERE ctdp.maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[] {
                        rs.getString("maPhong"),
                        rs.getString("loaiPhong") // Day la ma loai (SINGLE, DOUBLE...)
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public java.util.Map<String, String> getMaCTDPMapByMaDat(String maDat) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        String sql = "SELECT maPhong, maCTDP FROM ChiTietDatPhong WHERE maDat = ?";
        try (Connection con = ConnectDatabase.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maDat);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("maPhong"), rs.getString("maCTDP"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}