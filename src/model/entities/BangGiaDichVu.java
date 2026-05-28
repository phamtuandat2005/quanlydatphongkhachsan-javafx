package model.entities;

import java.sql.Date;

public class BangGiaDichVu {
	private String maBangGia;
    private String tenBangGia;
    private Date ngayApDung;
    private Date ngayHetHieuLuc;
    private int trangThai;
	public String getMaBangGia() {
		return maBangGia;
	}
	public void setMaBangGia(String maBangGia) {
		this.maBangGia = maBangGia;
	}
	public String getTenBangGia() {
		return tenBangGia;
	}
	public void setTenBangGia(String tenBangGia) {
		this.tenBangGia = tenBangGia;
	}
	public Date getNgayApDung() {
		return ngayApDung;
	}
	public void setNgayApDung(Date ngayApDung) {
		this.ngayApDung = ngayApDung;
	}
	public Date getNgayHetHieuLuc() {
		return ngayHetHieuLuc;
	}
	public void setNgayHetHieuLuc(Date ngayHetHieuLuc) {
		this.ngayHetHieuLuc = ngayHetHieuLuc;
	}
	public int getTrangThai() {
		return trangThai;
	}
	public void setTrangThai(int trangThai) {
		this.trangThai = trangThai;
	}
	public BangGiaDichVu(String maBangGia, String tenBangGia, Date ngayApDung, Date ngayHetHieuLuc, int trangThai) {
		super();
		this.maBangGia = maBangGia;
		this.tenBangGia = tenBangGia;
		this.ngayApDung = ngayApDung;
		this.ngayHetHieuLuc = ngayHetHieuLuc;
		this.trangThai = trangThai;
	}
	public BangGiaDichVu(String maBangGia) {
		super();
	}
    
	public BangGiaDichVu() {
		super();
	}

	/**
	 * Sinh mã bảng giá tự động dựa trên timestamp
	 */
	public static String createMaBangGia() {
		return "BG" + System.currentTimeMillis();
	}
}	
