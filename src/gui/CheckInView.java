package gui;

import dao.DatPhongDAO;
import dao.ChiTietDatPhongDAO;
import dao.ChiTietHoaDonDAO;
import dao.HoaDonDAO;
import dao.PhongDAO;
import model.entities.DatPhong;
import model.entities.HoaDon;
import model.entities.NhanVien;
import connectDatabase.ConnectDatabase;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CheckInView – Thủ tục nhận phòng.
 * Giao diện giống y chang CheckOutView để đồng bộ trải nghiệm.
 *
 * Luồng:
 * 1. Hiển thị danh sách đơn ĐÃ_XÁC_NHẬN sẵn sàng check-in
 * 2. Click 1 đơn → load chi tiết bên phải + danh sách phòng (kèm trạng thái)
 * 3. Bấm "Xác nhận nhận phòng" → cập nhật DB, tạo HoaDon, đổi trạng thái
 */
public class CheckInView extends BorderPane {

    // ===== COLOR PALETTE (đồng bộ CheckOutView) =====
    private static final String C_BG = "#f8fafc";
    private static final String C_BORDER = "#e5e7eb";
    private static final String C_BORDER_SOFT = "#f1f5f9";
    private static final String C_TEXT_DARK = "#0f172a";
    private static final String C_TEXT_GRAY = "#64748b";
    private static final String C_TEXT_LIGHT = "#94a3b8";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final String C_BLUE_LIGHT = "#eff6ff";
    private static final String C_GREEN = "#16a34a";
    private static final String C_ORANGE = "#f59e0b";
    private static final String C_RED = "#dc2626";

    // ===== DAOs =====
    private final DatPhongDAO datPhongDAO = new DatPhongDAO();
    private final ChiTietDatPhongDAO ctdpDAO = new ChiTietDatPhongDAO();
    private final ChiTietHoaDonDAO cthdDAO = new ChiTietHoaDonDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final PhongDAO phongDAO = new PhongDAO();

    // ===== UI =====
    private TextField txtSearch;
    private CheckBox chkLate;
    private DatePicker dpFilter;
    private VBox listItemsContainer;
    private VBox detailInfoBox;
    private VBox roomListBox;
    private Button btnConfirm;
    private Label lblRoomCount;

    // ===== STATE =====
    private final List<Object[]> allItems = new ArrayList<>();
    private Object[] selectedItem;
    private DatPhong currentDatPhong;
    private List<Object[]> currentRoomDetails; // {maPhong, maLoai}
    private NhanVien staff;

    // ===== CONSTRUCTORS =====
    public CheckInView() {
        this(null);
    }

    public CheckInView(NhanVien staff) {
        this.staff = staff;
        setStyle("-fx-background-color:" + C_BG + ";");
        setPadding(new Insets(24, 28, 24, 28));
        setTop(buildHeader());
        setCenter(buildMainContent());
        resetDetail();
        refreshList();
    }

    // ============================================================
    // HEADER
    // ============================================================
    private VBox buildHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(0, 0, 18, 0));

        Label t = new Label("Thủ tục nhận phòng");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        t.setTextFill(Color.web(C_TEXT_DARK));

        Label s = new Label("Tiếp nhận khách check-in – kiểm tra trạng thái phòng và bàn giao");
        s.setFont(Font.font("Segoe UI", 13));
        s.setTextFill(Color.web(C_TEXT_GRAY));

        header.getChildren().addAll(t, s);
        return header;
    }

    // ============================================================
    // MAIN CONTENT
    // ============================================================
    private HBox buildMainContent() {
        HBox main = new HBox(20);
        main.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = buildLeftColumn();
        VBox rightCol = buildRightColumn();

        leftCol.setMinWidth(540);
        leftCol.setPrefWidth(640);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        rightCol.setMinWidth(420);
        rightCol.setPrefWidth(470);
        rightCol.setMaxWidth(520);

        main.getChildren().addAll(leftCol, rightCol);
        return main;
    }

    // ============================================================
    // LEFT COLUMN
    // ============================================================
    private VBox buildLeftColumn() {
        VBox col = new VBox(16);

        HBox topRow = buildSearchRow();
        VBox listPanel = buildListPanel();
        VBox roomsPanel = buildRoomsPanel();

        VBox.setVgrow(listPanel, Priority.SOMETIMES);
        VBox.setVgrow(roomsPanel, Priority.ALWAYS);

        col.getChildren().addAll(topRow, listPanel, roomsPanel);
        return col;
    }

    // ----- Search bar -----
    private HBox buildSearchRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Tìm kiếm:");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(C_TEXT_GRAY));

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm theo mã đặt, tên khách, SĐT, CCCD...");
        txtSearch.setPrefHeight(40);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        String base = "-fx-background-color:white;" +
                "-fx-background-radius:10;" +
                "-fx-border-color:" + C_BORDER + ";" +
                "-fx-border-radius:10;" +
                "-fx-border-width:1;" +
                "-fx-font-size:13px;" +
                "-fx-padding:0 14;";
        String focused = base.replace(C_BORDER, C_BLUE).replace("-fx-border-width:1;", "-fx-border-width:1.5;");
        txtSearch.setStyle(base);
        txtSearch.focusedProperty().addListener((obs, o, n) -> txtSearch.setStyle(n ? focused : base));
        txtSearch.textProperty().addListener((obs, o, n) -> renderList());

        row.getChildren().addAll(lbl, txtSearch);
        return row;
    }

    // ----- List panel: danh sách đơn đang chờ check-in -----
    private VBox buildListPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(18, 20, 18, 20));
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-border-width:1;");
        panel.setEffect(new DropShadow(8, 0, 2, Color.web("#0f172a0a")));

        Label lblTitle = new Label("Đơn chờ check-in");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));

        listItemsContainer = new VBox(8);
        listItemsContainer.setPadding(new Insets(2));

        ScrollPane scroll = new ScrollPane(listItemsContainer);
        scroll.setFitToWidth(true);
        scroll.setMinHeight(160);
        scroll.setPrefHeight(200);
        scroll.setMaxHeight(240);
        scroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(lblTitle, scroll);
        return panel;
    }

    // ----- Rooms panel: danh sách phòng trong đơn được chọn -----
    private VBox buildRoomsPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(18, 20, 18, 20));
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-border-width:1;");
        panel.setEffect(new DropShadow(8, 0, 2, Color.web("#0f172a0a")));

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Phòng trong đơn");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lbl.setTextFill(Color.web(C_TEXT_DARK));
        lblRoomCount = new Label("");
        lblRoomCount.setFont(Font.font("Segoe UI", 12));
        lblRoomCount.setTextFill(Color.web(C_TEXT_LIGHT));
        titleRow.getChildren().addAll(lbl, lblRoomCount);

        roomListBox = new VBox(8);
        Label noRoom = new Label("Chọn 1 đơn để xem danh sách phòng");
        noRoom.setTextFill(Color.web(C_TEXT_LIGHT));
        noRoom.setFont(Font.font("Segoe UI", 13));
        noRoom.setPadding(new Insets(20));
        roomListBox.getChildren().add(noRoom);

        ScrollPane scroll = new ScrollPane(roomListBox);
        scroll.setFitToWidth(true);
        scroll.setMinHeight(180);
        scroll.setPrefHeight(280);
        scroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:" + C_BORDER_SOFT + ";" +
                        "-fx-border-radius:8;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(titleRow, scroll);
        return panel;
    }

    // ============================================================
    // RIGHT COLUMN
    // ============================================================
    private VBox buildRightColumn() {
        VBox col = new VBox(16);
        col.getChildren().addAll(buildBookingInfoPanel(), buildActionPanel());
        return col;
    }

    private VBox buildBookingInfoPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(20, 22, 22, 22));
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-border-width:1;");
        panel.setEffect(new DropShadow(8, 0, 2, Color.web("#0f172a0a")));

        Label lbl = new Label("Thông tin đặt phòng");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lbl.setTextFill(Color.web(C_NAVY));

        detailInfoBox = new VBox(10);
        Label empty = new Label("Chưa có thông tin đơn đặt phòng");
        empty.setTextFill(Color.web(C_TEXT_LIGHT));
        empty.setFont(Font.font("Segoe UI", 13));
        detailInfoBox.getChildren().add(empty);

        panel.getChildren().addAll(lbl, detailInfoBox);
        return panel;
    }

    private VBox buildActionPanel() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(20, 22, 20, 22));
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-border-width:1;");
        panel.setEffect(new DropShadow(8, 0, 2, Color.web("#0f172a0a")));

        Label lbl = new Label("Bàn giao phòng");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lbl.setTextFill(Color.web(C_NAVY));

        Label hint = new Label(
                "• Kiểm tra trạng thái các phòng trong danh sách bên trái\n" +
                        "• Phòng có tình trạng \"Sẵn sàng\" mới được phép bàn giao\n" +
                        "• Hệ thống sẽ tạo hóa đơn nháp khi bàn giao thành công");
        hint.setFont(Font.font("Segoe UI", 12));
        hint.setTextFill(Color.web(C_TEXT_GRAY));
        hint.setWrapText(true);

        btnConfirm = new Button("🔑   XÁC NHẬN NHẬN PHÒNG");
        btnConfirm.setPrefHeight(48);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setCursor(Cursor.HAND);
        btnConfirm.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        styleButton(btnConfirm, C_NAVY, "white", C_BLUE_HOVER);
        btnConfirm.setVisible(false);
        btnConfirm.setManaged(false);
        btnConfirm.setOnAction(e -> handleCheckIn());

        panel.getChildren().addAll(lbl, hint, btnConfirm);
        return panel;
    }

    // ============================================================
    // LIST RENDERING
    // ============================================================
    private void refreshList() {
        allItems.clear();
        // Lấy danh sách đơn ĐÃ_XÁC_NHẬN có thể check-in (default: hôm nay, không lọc)
        List<Object[]> rows = datPhongDAO.getDonCheckInByDate(LocalDate.now(), false);
        if (rows == null)
            rows = new ArrayList<>();
        for (Object[] r : rows) {
            // r = {maDat, tenKH, dsPhongStr, soPhong}
            String maDat = (String) r[0];
            String tenKH = (String) r[1];
            String dsPhong = (String) r[2];
            int soNguoi = r[3] instanceof Integer ? (Integer) r[3] : 0;

            DatPhong dp = datPhongDAO.findDatPhongAllStatus(maDat);
            String sdt = dp != null && dp.getKhachHang() != null && dp.getKhachHang().getSoDT() != null
                    ? dp.getKhachHang().getSoDT()
                    : "---";
            String cccd = dp != null && dp.getKhachHang() != null && dp.getKhachHang().getSoCCCD() != null
                    ? dp.getKhachHang().getSoCCCD()
                    : "";
            LocalDate ngayNhan = dp != null && dp.getNgayCheckIn() != null
                    ? dp.getNgayCheckIn().toLocalDate()
                    : null;

            allItems.add(new Object[] { maDat, tenKH, sdt, cccd, dsPhong, soNguoi, ngayNhan });
        }
        renderList();
    }

    private void renderList() {
        listItemsContainer.getChildren().clear();
        String q = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase();
        boolean filterByDate = chkLate != null && chkLate.isSelected() && dpFilter != null
                && dpFilter.getValue() != null;
        LocalDate filterDate = filterByDate ? dpFilter.getValue() : null;

        List<Object[]> filtered = allItems.stream().filter(it -> {
            if (!q.isEmpty()) {
                boolean match = ((String) it[0]).toLowerCase().contains(q)
                        || ((String) it[1]).toLowerCase().contains(q)
                        || ((String) it[2]).toLowerCase().contains(q)
                        || ((String) it[3]).toLowerCase().contains(q);
                if (!match)
                    return false;
            }
            if (filterByDate) {
                LocalDate ngayNhan = (LocalDate) it[6];
                if (ngayNhan == null || !ngayNhan.equals(filterDate))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            String msg;
            if (allItems.isEmpty())
                msg = "Không có đơn nào chờ check-in";
            else if (filterByDate && q.isEmpty())
                msg = "Không có đơn nào nhận ngày " + filterDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            else
                msg = "Không tìm thấy kết quả phù hợp";
            Label empty = new Label(msg);
            empty.setTextFill(Color.web(C_TEXT_LIGHT));
            empty.setFont(Font.font("Segoe UI", 13));
            empty.setPadding(new Insets(20));
            listItemsContainer.getChildren().add(empty);
            return;
        }

        for (Object[] it : filtered) {
            listItemsContainer.getChildren().add(createListItem(it));
        }
    }

    private HBox createListItem(Object[] it) {
        String maDat = (String) it[0];
        String tenKH = (String) it[1];
        String sdt = (String) it[2];
        String cccd = (String) it[3];
        int soPhong = (int) it[5];

        boolean isSelected = (selectedItem == it);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 12));
        row.setCursor(Cursor.HAND);

        String selStyle = "-fx-background-color:" + C_BLUE_LIGHT + ";" +
                "-fx-background-radius:10;" +
                "-fx-border-color:" + C_BLUE + ";" +
                "-fx-border-radius:10;" +
                "-fx-border-width:1.5;";
        String normalStyle = "-fx-background-color:white;" +
                "-fx-background-radius:10;" +
                "-fx-border-color:" + C_BORDER_SOFT + ";" +
                "-fx-border-radius:10;" +
                "-fx-border-width:1;";
        String hoverStyle = "-fx-background-color:#f8fafc;" +
                "-fx-background-radius:10;" +
                "-fx-border-color:" + C_BORDER + ";" +
                "-fx-border-radius:10;" +
                "-fx-border-width:1;";
        row.setStyle(isSelected ? selStyle : normalStyle);
        row.setOnMouseEntered(e -> {
            if (selectedItem != it)
                row.setStyle(hoverStyle);
        });
        row.setOnMouseExited(e -> {
            if (selectedItem != it)
                row.setStyle(normalStyle);
        });

        Label pill = new Label(maDat);
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pill.setTextFill(Color.web(C_ORANGE));
        pill.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:18;" +
                        "-fx-border-color:" + C_ORANGE + ";" +
                        "-fx-border-radius:18;" +
                        "-fx-border-width:1.5;" +
                        "-fx-padding:5 14;");
        pill.setMinWidth(80);
        pill.setAlignment(Pos.CENTER);

        // Tên khách + CCCD + số người cùng 1 dòng (in đậm, rõ nét)
        HBox info = new HBox(6);
        info.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(tenKH);
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblName.setTextFill(Color.web(C_TEXT_DARK));
        info.getChildren().add(lblName);
        if (!cccd.isEmpty()) {
            Label dot1 = new Label("•");
            dot1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            dot1.setTextFill(Color.web(C_TEXT_GRAY));
            Label lblCccd = new Label("CCCD: " + cccd);
            lblCccd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblCccd.setTextFill(Color.web(C_TEXT_DARK));
            info.getChildren().addAll(dot1, lblCccd);
        }
        {
            Label dot2 = new Label("•");
            dot2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            dot2.setTextFill(Color.web(C_TEXT_GRAY));
            Label lblSoNguoi = new Label(soPhong + " người");
            lblSoNguoi.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblSoNguoi.setTextFill(Color.web(C_TEXT_DARK));
            info.getChildren().addAll(dot2, lblSoNguoi);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox phoneBox = new HBox(4);
        phoneBox.setAlignment(Pos.CENTER_RIGHT);
        Label phoneIcon = new Label("📞");
        phoneIcon.setFont(Font.font(12));
        Label lblPhone = new Label(sdt);
        lblPhone.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblPhone.setTextFill(Color.web(C_TEXT_DARK));
        phoneBox.getChildren().addAll(phoneIcon, lblPhone);

        row.getChildren().addAll(pill, info, spacer, phoneBox);

        row.setOnMouseClicked(e -> {
            selectedItem = it;
            renderList();
            loadOrderDetail(maDat);
        });
        return row;
    }

    // ============================================================
    // LOAD DETAIL
    // ============================================================
    private void loadOrderDetail(String maDat) {
        DatPhong dp = datPhongDAO.findDatPhongAllStatus(maDat);
        if (dp == null) {
            new Alert(Alert.AlertType.WARNING, "Không tìm thấy đơn đặt phòng.", ButtonType.OK).showAndWait();
            return;
        }
        currentDatPhong = dp;
        currentRoomDetails = ctdpDAO.getPhongDetailsByMaDat(maDat);
        if (currentRoomDetails == null)
            currentRoomDetails = new ArrayList<>();

        updateDetailInfoUI();
        updateRoomListUI();

        boolean hasRooms = !currentRoomDetails.isEmpty();
        btnConfirm.setVisible(hasRooms);
        btnConfirm.setManaged(hasRooms);
    }

    private void updateDetailInfoUI() {
        detailInfoBox.getChildren().clear();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        String maDat = currentDatPhong.getMaDat();
        String tenKH = safe(currentDatPhong.getKhachHang() != null ? currentDatPhong.getKhachHang().getTenKH() : null);
        String sdt = safe(currentDatPhong.getKhachHang() != null ? currentDatPhong.getKhachHang().getSoDT() : null);
        String cccd = safe(currentDatPhong.getKhachHang() != null ? currentDatPhong.getKhachHang().getSoCCCD() : null);
        String ngayNhan = currentDatPhong.getNgayCheckIn() != null ? currentDatPhong.getNgayCheckIn().format(dtf)
                : "---";
        String ngayTra = currentDatPhong.getNgayCheckOut() != null ? currentDatPhong.getNgayCheckOut().format(dtf)
                : "---";

        String trangThaiStr = safe(currentDatPhong.getTrangThai());
        try {
            trangThaiStr = model.enums.TrangThaiDatPhong.valueOf(currentDatPhong.getTrangThai()).getThongTinTrangThai();
        } catch (Exception ignored) {
        }

        grid.add(infoCell("Mã đặt phòng", maDat, false), 0, 0);
        grid.add(infoCell("Số phòng", currentRoomDetails.size() + " phòng", false), 1, 0);
        grid.add(infoCell("Khách hàng", tenKH, false), 0, 1);
        grid.add(infoCell("Số điện thoại", sdt, false), 1, 1);
        grid.add(infoCell("Ngày nhận dự kiến", ngayNhan, false), 0, 2);
        grid.add(infoCell("Ngày trả dự kiến", ngayTra, false), 1, 2);
        grid.add(infoCell("CCCD", cccd, false), 0, 3);
        grid.add(infoCell("Trạng thái", trangThaiStr, true), 1, 3);

        detailInfoBox.getChildren().add(grid);
    }

    private VBox infoCell(String label, String value, boolean valueGreen) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web(C_TEXT_GRAY));
        Label val = new Label(value != null && !value.isEmpty() ? value : "---");
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        val.setTextFill(Color.web(valueGreen ? C_GREEN : C_TEXT_DARK));
        val.setWrapText(true);
        box.getChildren().addAll(lbl, val);
        return box;
    }

    // ============================================================
    // ROOM LIST UI (với trạng thái phòng)
    // ============================================================
    private void updateRoomListUI() {
        roomListBox.getChildren().clear();

        if (currentRoomDetails == null || currentRoomDetails.isEmpty()) {
            Label empty = new Label("Đơn này chưa có phòng nào được phân công");
            empty.setTextFill(Color.web(C_ORANGE));
            empty.setWrapText(true);
            empty.setPadding(new Insets(20));
            roomListBox.getChildren().add(empty);
            lblRoomCount.setText("");
            return;
        }

        // Lấy trạng thái thực tế của các phòng
        List<String> listMa = new ArrayList<>();
        for (Object[] r : currentRoomDetails)
            listMa.add((String) r[0]);
        java.util.Map<String, String> mapStatus = phongDAO.getTrangThaiMapByMaPhongs(listMa);

        int readyCount = 0;
        for (Object[] r : currentRoomDetails) {
            String mp = (String) r[0];
            String status = mapStatus.getOrDefault(mp, "CONTRONG");
            if ("CONTRONG".equals(status))
                readyCount++;
        }
        lblRoomCount.setText("(" + readyCount + "/" + currentRoomDetails.size() + " sẵn sàng)");

        for (Object[] r : currentRoomDetails) {
            roomListBox.getChildren().add(createRoomCard(r, mapStatus));
        }
    }

    private HBox createRoomCard(Object[] r, java.util.Map<String, String> mapStatus) {
        String maPhong = (String) r[0];
        String maLoai = (String) r[1];
        String status = mapStatus.getOrDefault(maPhong, "CONTRONG");

        boolean isOccupied = "DANGSUDUNG".equals(status);
        boolean isDirty = "BAN".equals(status);
        boolean isMaintenance = "BAOTRI".equals(status);
        boolean isReady = !isOccupied && !isDirty && !isMaintenance;

        String tenLoai = maLoai;
        try {
            tenLoai = model.enums.TenLoaiPhong.valueOf(maLoai.toUpperCase()).getDisplayName();
        } catch (Exception ignored) {
        }

        // Card style
        String bg, border, statusBg, statusFg, statusText, pillFg;
        if (isReady) {
            bg = "#f0fdf4";
            border = C_GREEN;
            statusBg = "#dcfce7";
            statusFg = C_GREEN;
            statusText = "✓ Sẵn sàng";
            pillFg = C_GREEN;
        } else if (isOccupied) {
            bg = "#fef2f2";
            border = C_RED;
            statusBg = "#fee2e2";
            statusFg = C_RED;
            statusText = "⛔ Đang có khách";
            pillFg = C_RED;
        } else if (isDirty) {
            bg = "#fff7ed";
            border = "#fb923c";
            statusBg = "#ffedd5";
            statusFg = "#ea580c";
            statusText = "❌ Cần dọn";
            pillFg = "#ea580c";
        } else {
            bg = "#fef3c7";
            border = "#f59e0b";
            statusBg = "#fde68a";
            statusFg = "#92400e";
            statusText = "❌ Bảo trì";
            pillFg = "#92400e";
        }

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 12));
        card.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-radius:10;" +
                        "-fx-border-width:1.5;");

        Label pill = new Label(maPhong);
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        pill.setTextFill(Color.web(pillFg));
        pill.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:18;" +
                        "-fx-border-color:" + pillFg + ";" +
                        "-fx-border-radius:18;" +
                        "-fx-border-width:1.5;" +
                        "-fx-padding:5 14;");
        pill.setMinWidth(80);
        pill.setAlignment(Pos.CENTER);

        VBox info = new VBox(2);
        Label lblType = new Label(tenLoai);
        lblType.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblType.setTextFill(Color.web(C_TEXT_DARK));
        Label lblHint = new Label("Phòng " + maPhong);
        lblHint.setFont(Font.font("Segoe UI", 11));
        lblHint.setTextFill(Color.web(C_TEXT_LIGHT));
        info.getChildren().addAll(lblType, lblHint);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblStatus = new Label(statusText);
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblStatus.setTextFill(Color.web(statusFg));
        lblStatus.setStyle(
                "-fx-background-color:" + statusBg + ";" +
                        "-fx-padding:4 10;" +
                        "-fx-background-radius:8;");

        card.getChildren().addAll(pill, info, sp, lblStatus);
        return card;
    }

    // ============================================================
    // CHECK-IN EXECUTION
    // ============================================================
    private void handleCheckIn() {
        if (currentDatPhong == null || currentRoomDetails == null || currentRoomDetails.isEmpty())
            return;

        List<String> listMaPhong = new ArrayList<>();
        for (Object[] r : currentRoomDetails)
            listMaPhong.add((String) r[0]);

        // Kiểm tra trạng thái phòng
        java.util.Map<String, String> mapStatus = phongDAO.getTrangThaiMapByMaPhongs(listMaPhong);
        List<String> dirtyRooms = new ArrayList<>();
        List<String> maintenanceRooms = new ArrayList<>();
        List<String> occupiedRooms = new ArrayList<>();

        for (String mp : listMaPhong) {
            String status = mapStatus.getOrDefault(mp, "CONTRONG");
            if ("BAN".equals(status))
                dirtyRooms.add(mp);
            else if ("BAOTRI".equals(status))
                maintenanceRooms.add(mp);
            else if ("DANGSUDUNG".equals(status))
                occupiedRooms.add(mp);
        }

        if (!dirtyRooms.isEmpty() || !maintenanceRooms.isEmpty() || !occupiedRooms.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Không thể nhận phòng do có phòng chưa sẵn sàng:\n");
            if (!occupiedRooms.isEmpty())
                errorMsg.append("\n⛔ Phòng [").append(String.join(", ", occupiedRooms))
                        .append("] hiện đang CÓ KHÁCH ĐANG Ở.");
            if (!dirtyRooms.isEmpty())
                errorMsg.append("\n❌ Phòng [").append(String.join(", ", dirtyRooms)).append("] đang BẨN.");
            if (!maintenanceRooms.isEmpty())
                errorMsg.append("\n❌ Phòng [").append(String.join(", ", maintenanceRooms)).append("] đang BẢO TRÌ.");
            errorMsg.append("\n\nVui lòng yêu cầu dọn dẹp hoặc đổi phòng trước khi tiếp tục.");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi Check-in");
            alert.setHeaderText("Phòng chưa sẵn sàng!");
            alert.setContentText(errorMsg.toString());
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận Check-in");
        confirm.setHeaderText("Xác nhận cho khách " + currentDatPhong.getKhachHang().getTenKH()
                + " nhận " + listMaPhong.size() + " phòng?");
        confirm.setContentText(
                "Bàn giao phòng: " + String.join(", ", listMaPhong) +
                        "\n\nHệ thống sẽ:\n" +
                        " • Cập nhật trạng thái phòng → Đang sử dụng\n" +
                        " • Cập nhật đơn → Đã nhận phòng\n" +
                        " • Tạo hóa đơn nháp");
        confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK)
                return;
            performCheckIn(listMaPhong);
        });
    }

    private void performCheckIn(List<String> listMaPhong) {
        try (Connection con = ConnectDatabase.getInstance().getConnection()) {
            if (con == null)
                return;
            con.setAutoCommit(false);
            try {
                // 1. Tạo HoaDon nếu chưa có
                HoaDon hd = hoaDonDAO.getByMaDat(currentDatPhong.getMaDat());
                if (hd == null) {
                    hd = new HoaDon();
                    hd.setMaHD(hoaDonDAO.generateMaHD());
                    hd.setDatPhong(currentDatPhong);
                    hd.setNhanVien(staff != null ? staff : new NhanVien("LUCIA001"));
                    hd.setTienPhong(0.0);
                    hd.setTienDV(0.0);
                    hd.setTienCoc(0.0);
                    hd.setThueVAT(0.1);
                    hd.setTongTien(0.0);
                    hd.setLoaiHD("HOA_DON_PHONG");
                    hd.setTrangThaiThanhToan("CHUA_THANH_TOAN");
                    hoaDonDAO.insertWithConnection(con, hd);
                }

                // 2. Sinh mã CTHD bắt đầu
                String baseMaCTHD = cthdDAO.generateMaCTHD();
                int lastNum = 0;
                if (baseMaCTHD != null && baseMaCTHD.length() > 4) {
                    try {
                        lastNum = Integer.parseInt(baseMaCTHD.substring(4));
                    } catch (Exception ignored) {
                    }
                }

                // 3. Update trạng thái phòng + tạo CTHD
                java.util.Map<String, String> mapMaCTDP = ctdpDAO.getMaCTDPMapByMaDat(currentDatPhong.getMaDat());
                for (String maPhong : listMaPhong) {
                    phongDAO.updateTrangThaiWithCon(con, maPhong, "DANGSUDUNG");
                    String maCTDP = mapMaCTDP.get(maPhong);
                    if (maCTDP != null && hd != null) {
                        String maCTHD = String.format("CTHD%03d", lastNum++);
                        cthdDAO.insertWithConnection(con, maCTHD, hd.getMaHD(), maCTDP, 0, 0, 0, 0);
                    }
                }

                // 4. Update đơn → DA_CHECKIN
                datPhongDAO.updateTrangThaiWithCon(con, currentDatPhong.getMaDat(), "DA_CHECKIN");
                
                // Cập nhật lại ngày giờ nhận phòng thực tế là NGAY LÚC NÀY
                try (java.sql.PreparedStatement psUpdateCI = con.prepareStatement(
                        "UPDATE DatPhong SET ngayCheckIn = ? WHERE maDat = ?")) {
                    psUpdateCI.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    psUpdateCI.setString(2, currentDatPhong.getMaDat());
                    psUpdateCI.executeUpdate();
                }

                con.commit();

                Alert ok = new Alert(Alert.AlertType.INFORMATION,
                        "Các phòng [" + String.join(", ", listMaPhong) + "] đã bàn giao thành công.",
                        ButtonType.OK);
                ok.setTitle("Check-in thành công");
                ok.setHeaderText("Check-in hoàn tất!");
                ok.showAndWait();

                resetDetail();
                refreshList();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Lỗi Check-in: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    // ============================================================
    // RESET / HELPERS
    // ============================================================
    private void resetDetail() {
        if (txtSearch != null)
            txtSearch.clear();
        currentDatPhong = null;
        currentRoomDetails = null;
        selectedItem = null;

        if (btnConfirm != null) {
            btnConfirm.setVisible(false);
            btnConfirm.setManaged(false);
        }
        if (detailInfoBox != null) {
            detailInfoBox.getChildren().clear();
            Label lbl = new Label("Chưa có thông tin đơn đặt phòng");
            lbl.setTextFill(Color.web(C_TEXT_LIGHT));
            lbl.setFont(Font.font("Segoe UI", 13));
            detailInfoBox.getChildren().add(lbl);
        }
        if (roomListBox != null) {
            roomListBox.getChildren().clear();
            Label lbl = new Label("Chọn 1 đơn để xem danh sách phòng");
            lbl.setTextFill(Color.web(C_TEXT_LIGHT));
            lbl.setFont(Font.font("Segoe UI", 13));
            lbl.setPadding(new Insets(20));
            roomListBox.getChildren().add(lbl);
        }
        if (lblRoomCount != null)
            lblRoomCount.setText("");
    }

    private String safe(String s) {
        return s != null && !s.isEmpty() ? s : "---";
    }

    private void styleButton(Button btn, String bg, String fg, String hoverBg) {
        String base = "-fx-background-color:" + bg + ";"
                + "-fx-text-fill:" + fg + ";"
                + "-fx-background-radius:10;"
                + "-fx-padding:10 20;"
                + "-fx-cursor:hand;"
                + "-fx-font-weight:bold;";
        String hover = base.replace("-fx-background-color:" + bg, "-fx-background-color:" + hoverBg);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }
}