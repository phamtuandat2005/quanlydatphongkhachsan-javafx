package model.enums;

public enum trinhDo {
	THPT("Trung học phổ thông"),
	TRUNGCAP("Trung cấp"),
	CAODANG("Cao đẳng"),
	DAIHOC("Đại học"),
	SAU_DAIHOC("Sau đại học");

	private final String displayName;

	trinhDo(String displayName) {
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
