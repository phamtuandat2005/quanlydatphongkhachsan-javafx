package model.entities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import model.enums.ChucVu;
import model.enums.trinhDo;
import model.enums.TrangThaiNV;

public class NhanVien {
    private String maNV, hoTen, diaChi, soDT, matKhau, cccd;
    private ChucVu role;
    private trinhDo trinhDo;
    private TrangThaiNV trangThai;
    private LocalDate ngaySinh, ngayVaoLamDate;
    private NhanVien quanLy;
    private boolean daXoa;

    // Sử dụng static để tránh tạo mới formatter cho mỗi object, tiết kiệm bộ nhớ
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- CONSTRUCTORS ---
    public NhanVien() {
        // Khởi tạo các giá trị mặc định hợp lệ để tránh NullPointerException
        this.maNV = "LUCIA000";
        this.hoTen = "Chưa xác định";
        this.diaChi = "Chưa có";
        this.soDT = "0900000000";
        this.matKhau = "123";
        this.cccd = "000000000000";
        this.role = ChucVu.NHAN_VIEN;
        this.trinhDo = trinhDo.THPT;
        this.trangThai = TrangThaiNV.CON_LAM;
        this.ngaySinh = LocalDate.now().minusYears(19); // Đảm bảo > 18 tuổi
        this.ngayVaoLamDate = LocalDate.now();
    }

    public NhanVien(String maNV, String hoTen, String diaChi, trinhDo trinhDo,
            LocalDate ngayVaoLamDate, ChucVu role) {
        this.setMaNV(maNV);
        this.setHoTen(hoTen);
        this.setDiaChi(diaChi);
        this.setTrinhDo(trinhDo);
        this.setTrangThai(TrangThaiNV.CON_LAM);
        this.setNgayVaoLamDate(ngayVaoLamDate);
        this.setRole(role);
    }

    public NhanVien(String maNV) {
        super();
        this.maNV = maNV;
    }

    // --- GETTERS & SETTERS ---

    public void setMaNV(String maNV) {
        if (maNV == null || !(maNV.equals("ADMIN") || maNV.matches("LUCIA\\d+")))
            throw new IllegalArgumentException("Mã NV phải in hoa 'LUCIA' và đi kèm số hoặc là 'ADMIN'");
        this.maNV = maNV;
    }

    public String getMaNV() {
        return maNV;
    }

    public NhanVien getQuanLy() {
        return quanLy;
    }

    public void setQuanLy(NhanVien quanLy) {
        this.quanLy = quanLy;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        if (matKhau == null || matKhau.isBlank())
            throw new IllegalArgumentException("Mật khẩu không được rỗng");
        this.matKhau = matKhau;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        if (ngaySinh != null && ChronoUnit.YEARS.between(ngaySinh, LocalDate.now()) < 18)
            throw new IllegalArgumentException("Nhân viên phải từ 18 tuổi trở lên");
        this.ngaySinh = ngaySinh;
    }

    public String getSoDT() {
        return soDT;
    }

    public void setSoDT(String soDT) {
        if (soDT != null && !soDT.isBlank() && !soDT.matches("0\\d{9}"))
            throw new IllegalArgumentException("SĐT phải gồm 10 chữ số và bắt đầu bằng số 0");
        this.soDT = soDT;
    }

    public String getCccd() {
        return cccd;
    }

    /**
     * CCCD hợp lệ: 9 chữ số (CMND cũ) hoặc 12 chữ số (CCCD mới).
     */
    public void setCccd(String cccd) {
        if (cccd != null && !cccd.isBlank() && !cccd.matches("\\d{9}(\\d{3})?"))
            throw new IllegalArgumentException("CCCD phải gồm 9 hoặc 12 chữ số");
        this.cccd = cccd;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        if (hoTen == null || hoTen.isBlank())
            throw new IllegalArgumentException("Họ tên không được rỗng");
        this.hoTen = hoTen;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public boolean isDaXoa() {
        return daXoa;
    }

    public void setDaXoa(boolean daXoa) {
        this.daXoa = daXoa;
    }

    public trinhDo getTrinhDo() {
        return trinhDo;
    }

    public void setTrinhDo(trinhDo trinhDo) {
        this.trinhDo = trinhDo;
    }

    public TrangThaiNV getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiNV trangThai) {
        if (trangThai == null)
            throw new IllegalArgumentException("Trạng thái không được để trống");
        this.trangThai = trangThai;
    }

    public ChucVu getRole() {
        return role;
    }

    public void setRole(ChucVu role) {
        if (role == null)
            throw new IllegalArgumentException("Chức vụ không được để trống");
        this.role = role;
    }

    public LocalDate getNgayVaoLamDate() {
        return ngayVaoLamDate;
    }

    public void setNgayVaoLamDate(LocalDate ngayVaoLamDate) {
        if (ngayVaoLamDate != null && ngayVaoLamDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày vào làm không được sau ngày hiện tại");
        }
        this.ngayVaoLamDate = ngayVaoLamDate;
    }

    // --- TO STRING ---
    @Override
    public String toString() {
        String sNgaySinh = (ngaySinh != null) ? ngaySinh.format(FMT) : "Chưa có";
        String sNgayVao = (ngayVaoLamDate != null) ? ngayVaoLamDate.format(FMT) : "Chưa có";

        return String.format("NV [Mã: %s, Tên: %s, SĐT: %s, CCCD: %s, Chức vụ: %s, Ngày vào: %s]",
                maNV, hoTen, soDT, cccd, role, sNgayVao);
    }
}