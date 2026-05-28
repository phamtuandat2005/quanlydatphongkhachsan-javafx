package model.entities;

import java.time.LocalDateTime;

public class BangGiaPhong {
	private String maBangGia;
	private LoaiPhong loaiPhong;
	private LocalDateTime ngayBatDau;
	private LocalDateTime ngayKetThuc;
	private double donGia;
	
	// --- RÀNG BUỘC NGHIỆP VỤ ---
	public boolean isHopLe() {
		// Giá trị phải dương và ngày bắt đầu phải nhỏ hơn ngày kết thúc
		if (this.donGia <= 0) return false;
		if (this.ngayBatDau != null && this.ngayKetThuc != null) {
			return this.ngayBatDau.isBefore(this.ngayKetThuc);
		}
		return true;
	}

	public BangGiaPhong(String maBangGia, LoaiPhong loaiPhong, LocalDateTime ngayBatDau, LocalDateTime ngayKetThuc, double donGia) {
		this.maBangGia = maBangGia;
		this.loaiPhong = loaiPhong;
		this.ngayBatDau = ngayBatDau;
		this.ngayKetThuc = ngayKetThuc;
		this.donGia = (donGia > 0) ? donGia : 0; // Đảm bảo đơn giá không âm
	}

	public BangGiaPhong(String maBangGia) {
		this.maBangGia = maBangGia;
	}

	public BangGiaPhong(String maBangGia, LoaiPhong loaiPhong, double donGia) {
		this.maBangGia = maBangGia;
		this.loaiPhong = loaiPhong;
		this.donGia = (donGia > 0) ? donGia : 0;
	}

	public BangGiaPhong() {
	}
	
	public LoaiPhong getLoaiPhong() { return loaiPhong; }
	public void setLoaiPhong(LoaiPhong loaiPhong) { this.loaiPhong = loaiPhong; }
	public LocalDateTime getNgayBatDau() { return ngayBatDau; }
	public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }
	public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
	public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
	public double getDonGia() { return donGia; }
	public void setDonGia(double donGia) { this.donGia = (donGia > 0) ? donGia : 0; }
	public String getMaBangGia() { return maBangGia; }
	public void setMaBangGia(String maBangGia) { this.maBangGia = maBangGia; }
}