USE LuciaHT;
GO

-- ==========================================
-- SCRIPT XÓA TOÀN BỘ DỮ LIỆU TRONG DATABASE
-- (Thực hiện xóa theo thứ tự ngược với khóa ngoại)
-- ==========================================

-- 1. Xóa dữ liệu Hóa Đơn và Chi Tiết
DELETE FROM ChiTietHoaDon;
DELETE FROM HoaDon;

-- 2. Xóa dữ liệu Dịch Vụ Sử Dụng (bởi Chi Tiết Đặt Phòng)
DELETE FROM DichVuSuDung;

-- 3. Xóa dữ liệu Đặt Phòng
DELETE FROM ChiTietDatPhong;
DELETE FROM DatPhong;

-- 4. Xóa dữ liệu Bảng giá Dịch vụ
DELETE FROM BangGiaDV_Detail;
DELETE FROM BangGiaDV_Header;

-- 5. Xóa dữ liệu Danh mục Dịch vụ và Phòng
DELETE FROM DV;
DELETE FROM LoaiDichVu;
DELETE FROM Phong;
DELETE FROM LoaiPhongTienNghi
DELETE FROM LoaiPhong;
DELETE FROM TienNghi

-- 6. Xóa dữ liệu Khách hàng & Nhân viên
DELETE FROM KH;
DELETE FROM NV;

PRINT N'✅ Đã xóa toàn bộ dữ liệu thành công! Bạn có thể chạy lại file Lucia_DuLieu.sql ngay bây giờ.';
GO
