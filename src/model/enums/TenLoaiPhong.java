package model.enums;

public enum TenLoaiPhong {
	SINGLE("Phòng Đơn"),
	DOUBLE("Phòng Đôi"),
	TWIN("Phòng 2 Giường"),
	TRIPLE("Phòng 3 Người"),
	FAMILY("Phòng Gia Đình");

	private final String displayName;

	TenLoaiPhong(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
