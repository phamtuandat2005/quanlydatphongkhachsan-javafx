package gui;

import dao.DatPhongDAO;
import dao.DichVuSuDungDAO;
import dao.HoaDonDAO;
import dao.ChiTietHoaDonDAO;
import dao.PhongDAO;
import dao.ChiTietDatPhongDAO;
import model.entities.DatPhong;
import model.entities.DichVuSuDung;
import model.entities.HoaDon;
import model.entities.NhanVien;

import javafx.beans.property.SimpleStringProperty;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CheckOutView extends BorderPane {
    // ===== COLOR PALETTE =====
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

    private static final double VAT_RATE = 0.1;

    // ===== DAOs =====
    private final DatPhongDAO datPhongDAO = new DatPhongDAO();
    private final DichVuSuDungDAO dvsdDAO = new DichVuSuDungDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final ChiTietHoaDonDAO cthdDAO = new ChiTietHoaDonDAO();
    private final PhongDAO phongDAO = new PhongDAO();
    private final ChiTietDatPhongDAO ctdpDAO = new ChiTietDatPhongDAO();

    // ===== UI ELEMENTS =====
    private TextField txtSearch, txtLateFee;
    private ToggleButton btnModePhong, btnModeDon;
    private VBox roomItemsContainer;
    private Label lblListTitle;
    private CheckBox chkLate;
    private DatePicker dpFilter;
    private TableView<DichVuSuDung> serviceTable;
    private Label lblTotalDV;
    private VBox detailInfoBox, billingBox, bookingInfoPanel;
    private Label lblTongTien, lblVAT;
    private Button btnConfirm;

    // ===== STATE =====
    private final List<Object[]> allItems = new ArrayList<>();
    private Object[] selectedItem;
    private DatPhong currentDatPhong;
    private List<Object[]> currentRoomList;
    private double currentTienPhong = 0, currentTienDV = 0, currentTienCoc = 0,
            currentLateFee = 0, currentPhuThu = 0, currentTongTien = 0;
    private double minLateFee = 0; // phí trả muộn tối thiểu (tính tự động)
    private long currentSoDem = 0;
    private long currentSoNgayTre = 0;
    private boolean isLateCheckout = false;
    private boolean isModeDon = true; // mặc định: theo đơn đặt
    private NhanVien staff;
    // Per-room surcharge maps (key = maCTDP)
    private Map<String, Double> roomPhuThuMap = new HashMap<>();
    private Map<String, Double> roomLateFeeMap = new HashMap<>();

    // ===== CONSTRUCTORS =====
    public CheckOutView() {
        this(null);
    }

    public CheckOutView(NhanVien staff) {
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

        Label t = new Label("Thủ tục trả phòng");
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        t.setTextFill(Color.web(C_TEXT_DARK));

        Label s = new Label("Thanh toán và trả phòng cho khách – theo từng phòng hoặc toàn đơn");
        s.setFont(Font.font("Segoe UI", 13));
        s.setTextFill(Color.web(C_TEXT_GRAY));

        header.getChildren().addAll(t, s);
        return header;
    }

    // ============================================================
    // MAIN CONTENT (LEFT + RIGHT)
    // ============================================================
    private HBox buildMainContent() {
        HBox main = new HBox(24);
        main.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = buildLeftColumn();
        VBox rightCol = buildRightColumn();

        leftCol.setMinWidth(420);
        leftCol.setPrefWidth(600);
        leftCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        rightCol.setMinWidth(420);
        rightCol.setPrefWidth(600);
        rightCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        main.getChildren().addAll(leftCol, rightCol);
        return main;
    }

    // ============================================================
    // LEFT COLUMN
    // ============================================================
    private VBox buildLeftColumn() {
        VBox col = new VBox(16);

        HBox topRow = buildModeAndSearch();
        VBox listPanel = buildListPanel();
        VBox svcPanel = buildServicePanel();

        VBox.setVgrow(listPanel, Priority.SOMETIMES);
        VBox.setVgrow(svcPanel, Priority.ALWAYS);

        col.getChildren().addAll(topRow, listPanel, svcPanel);
        return col;
    }

    // ----- Mode toggle + search bar (cùng 1 hàng) -----
    private HBox buildModeAndSearch() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblMode = new Label("Chế độ:");
        lblMode.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblMode.setTextFill(Color.web(C_TEXT_GRAY));

        // Toggle group bo cùng 1 viền nền trắng
        HBox toggleBox = new HBox(0);
        toggleBox.setAlignment(Pos.CENTER_LEFT);
        toggleBox.setPadding(new Insets(3));
        toggleBox.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:10;" +
                        "-fx-border-width:1;");

        btnModePhong = makeModeButton("🚪  Theo phòng");
        btnModeDon = makeModeButton("📋  Theo đơn đặt");

        ToggleGroup tg = new ToggleGroup();
        btnModePhong.setToggleGroup(tg);
        btnModeDon.setToggleGroup(tg);
        btnModeDon.setSelected(true);
        applyModeStyles();

        tg.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                o.setSelected(true);
                return;
            }
            isModeDon = (n == btnModeDon);
            applyModeStyles();
            updatePromptsForMode();
            resetDetail();
            refreshList();
        });

        toggleBox.getChildren().addAll(btnModePhong, btnModeDon);

        // Ô tìm kiếm: nhỏ, không có nút "Tìm kiếm", filter live theo input
        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm theo mã đặt, tên khách, SĐT, CCCD...");
        txtSearch.setPrefHeight(40);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:10;" +
                        "-fx-border-width:1;" +
                        "-fx-font-size:13px;" +
                        "-fx-padding:0 14;");
        txtSearch.focusedProperty().addListener((obs, o, n) -> {
            if (n)
                txtSearch.setStyle(
                        "-fx-background-color:white;" +
                                "-fx-background-radius:10;" +
                                "-fx-border-color:" + C_BLUE + ";" +
                                "-fx-border-radius:10;" +
                                "-fx-border-width:1.5;" +
                                "-fx-font-size:13px;" +
                                "-fx-padding:0 14;");
            else
                txtSearch.setStyle(
                        "-fx-background-color:white;" +
                                "-fx-background-radius:10;" +
                                "-fx-border-color:" + C_BORDER + ";" +
                                "-fx-border-radius:10;" +
                                "-fx-border-width:1;" +
                                "-fx-font-size:13px;" +
                                "-fx-padding:0 14;");
        });
        txtSearch.textProperty().addListener((obs, o, n) -> renderList());

        row.getChildren().addAll(lblMode, toggleBox, txtSearch);
        return row;
    }

    private ToggleButton makeModeButton(String text) {
        ToggleButton b = new ToggleButton(text);
        b.setCursor(Cursor.HAND);
        b.setPrefHeight(34);
        b.setMinWidth(140);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        return b;
    }

    private void applyModeStyles() {
        String active = "-fx-background-color:" + C_BLUE + ";"
                + "-fx-text-fill:white;"
                + "-fx-background-radius:8;"
                + "-fx-border-color:transparent;";
        String inactive = "-fx-background-color:transparent;"
                + "-fx-text-fill:" + C_TEXT_GRAY + ";"
                + "-fx-background-radius:8;"
                + "-fx-border-color:transparent;";
        btnModePhong.setStyle(isModeDon ? inactive : active);
        btnModeDon.setStyle(isModeDon ? active : inactive);
    }

    private void updatePromptsForMode() {
        if (isModeDon) {
            txtSearch.setPromptText("🔍  Tìm theo mã đặt, tên khách, SĐT, CCCD...");
            lblListTitle.setText("Đơn đang sử dụng");
        } else {
            txtSearch.setPromptText("🔍  Tìm theo mã phòng, tên khách, SĐT, CCCD...");
            lblListTitle.setText("Phòng đang có khách");
        }
    }

    // ----- Panel danh sách phòng / đơn (có scroll dọc) -----
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

        lblListTitle = new Label("Đơn đang sử dụng");
        lblListTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblListTitle.setTextFill(Color.web(C_TEXT_DARK));

        roomItemsContainer = new VBox(8);
        roomItemsContainer.setPadding(new Insets(2, 2, 2, 2));

        ScrollPane scroll = new ScrollPane(roomItemsContainer);
        scroll.setFitToWidth(true);
        scroll.setMinHeight(160);
        scroll.setPrefHeight(200);
        scroll.setMaxHeight(240);
        scroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(lblListTitle, scroll);
        return panel;
    }

    /** Hàng filter: checkbox bật/tắt + date picker + nav buttons (◀ Hôm nay ▶) */

    // ----- Panel dịch vụ đã sử dụng (scroll trong bảng) -----
    private VBox buildServicePanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(18, 20, 18, 20));
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-border-width:1;");
        panel.setEffect(new DropShadow(8, 0, 2, Color.web("#0f172a0a")));

        Label lblSvc = new Label("Dịch vụ đã sử dụng");
        lblSvc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblSvc.setTextFill(Color.web(C_TEXT_DARK));

        // Bảng dịch vụ - thanh trượt là của bảng
        serviceTable = new TableView<>();
        serviceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        serviceTable.setPlaceholder(new Label("Chưa có dịch vụ nào"));
        serviceTable.setMinHeight(140);
        serviceTable.setPrefHeight(280);
        serviceTable.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-border-color:" + C_BORDER_SOFT + ";" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;" +
                        "-fx-font-size:12px;");

        TableColumn<DichVuSuDung, String> colPhong = new TableColumn<>("Phòng");
        colPhong.setCellValueFactory(p -> {
            if (p.getValue().getChiTietDatPhong() != null) {
                String maCTDP = p.getValue().getChiTietDatPhong().getMaCTDP();
                if (currentRoomList != null) {
                    for (Object[] r : currentRoomList) {
                        if (maCTDP.equals((String) r[0]))
                            return new SimpleStringProperty((String) r[1]);
                    }
                }
            }
            return new SimpleStringProperty("");
        });
        colPhong.setMinWidth(70);
        colPhong.setMaxWidth(80);
        colPhong.setStyle("-fx-alignment:CENTER-LEFT; -fx-padding: 0 0 0 15; -fx-font-weight:bold; -fx-text-fill:" + C_NAVY + ";");

        TableColumn<DichVuSuDung, String> colNgay = new TableColumn<>("Ngày");
        colNgay.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getNgaySuDung() != null ? p.getValue().getNgaySuDung().toString() : ""));
        colNgay.setMinWidth(90);

        TableColumn<DichVuSuDung, String> colTen = new TableColumn<>("Dịch vụ");
        colTen.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getDichVu() != null ? p.getValue().getDichVu().getTenDV() : ""));
        colTen.setMinWidth(130);

        TableColumn<DichVuSuDung, String> colDonVi = new TableColumn<>("Đơn vị");
        colDonVi.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getDichVu() != null && p.getValue().getDichVu().getDonVi() != null
                        ? p.getValue().getDichVu().getDonVi()
                        : ""));
        colDonVi.setMaxWidth(70);
        colDonVi.setStyle("-fx-alignment:CENTER;");

        TableColumn<DichVuSuDung, String> colSl = new TableColumn<>("SL");
        colSl.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getSoLuong())));
        colSl.setMaxWidth(55);
        colSl.setStyle("-fx-alignment:CENTER;");

        TableColumn<DichVuSuDung, String> colGia = new TableColumn<>("Đơn giá");
        colGia.setCellValueFactory(p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getGiaDV())));
        colGia.setStyle("-fx-alignment:CENTER-RIGHT;");

        TableColumn<DichVuSuDung, String> colTT = new TableColumn<>("Thành tiền");
        colTT.setCellValueFactory(p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getThanhTien())));
        colTT.setStyle("-fx-alignment:CENTER-RIGHT;-fx-font-weight:bold;");

        serviceTable.getColumns().addAll(colPhong, colNgay, colTen, colDonVi, colSl, colGia, colTT);
        for (TableColumn<DichVuSuDung, ?> c : serviceTable.getColumns()) {
            c.setReorderable(false);
            c.setSortable(false);
        }
        VBox.setVgrow(serviceTable, Priority.ALWAYS);

        // Hàng tổng tiền dịch vụ (cuối cùng, không scroll cùng bảng)
        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setPadding(new Insets(10, 14, 10, 14));
        totalRow.setStyle("-fx-background-color:" + C_BLUE_LIGHT + ";-fx-background-radius:8;");
        Label lblL = new Label("Tổng tiền dịch vụ");
        lblL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblL.setTextFill(Color.web(C_TEXT_GRAY));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        lblTotalDV = new Label("0 đ");
        lblTotalDV.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTotalDV.setTextFill(Color.web(C_BLUE));
        totalRow.getChildren().addAll(lblL, sp, lblTotalDV);

        panel.getChildren().addAll(lblSvc, serviceTable, totalRow);
        return panel;
    }

    // ============================================================
    // RIGHT COLUMN: Booking info fixed on top, billing scrollable
    // ============================================================
    private VBox buildRightColumn() {
        VBox col = new VBox(16);

        // Booking info panel stays fixed (no scroll)
        bookingInfoPanel = buildBookingInfoPanel();

        // Billing panel wrapped in ScrollPane
        VBox billingPanel = buildBillingPanel();
        ScrollPane billingScroll = new ScrollPane(billingPanel);
        billingScroll.setFitToWidth(true);
        billingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        billingScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        billingScroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;" +
                        "-fx-padding:0;");
        VBox.setVgrow(billingScroll, Priority.ALWAYS);

        col.getChildren().addAll(bookingInfoPanel, billingScroll);
        return col;
    }

    // ----- Panel "Thông tin đặt phòng" -----
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

    // ----- Panel "Chi tiết hóa đơn" (no scroll) -----
    private VBox buildBillingPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16, 22, 16, 22));
        panel.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-border-width:1;");
        panel.setEffect(new DropShadow(8, 0, 2, Color.web("#0f172a0a")));

        Label lbl = new Label("Chi tiết hóa đơn");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lbl.setTextFill(Color.web(C_NAVY));

        billingBox = new VBox(10);
        Label empty = new Label("Chọn phòng / đơn đặt để xem chi tiết");
        empty.setTextFill(Color.web(C_TEXT_LIGHT));
        empty.setFont(Font.font("Segoe UI", 13));
        billingBox.getChildren().add(empty);

        btnConfirm = new Button("💳   XÁC NHẬN THANH TOÁN VÀ TRẢ PHÒNG");
        btnConfirm.setPrefHeight(48);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setCursor(Cursor.HAND);
        btnConfirm.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        styleButton(btnConfirm, C_NAVY, "white", C_BLUE_HOVER);
        btnConfirm.setVisible(false);
        btnConfirm.setManaged(false);
        btnConfirm.setOnAction(e -> handleCheckOut());

        panel.getChildren().addAll(lbl, billingBox, btnConfirm);
        return panel;
    }

    // ============================================================
    // LIST RENDERING & SEARCH
    // ============================================================
    private void refreshList() {
        allItems.clear();
        if (isModeDon) {
            // Theo đơn đặt
            List<String> dsDon = datPhongDAO.getDonDangSuDung();
            for (String maDat : dsDon) {
                DatPhong dp = datPhongDAO.findDatPhongForCheckOut(maDat);
                if (dp == null)
                    continue;
                String tenKH = dp.getKhachHang() != null ? dp.getKhachHang().getTenKH() : "---";
                String sdt = dp.getKhachHang() != null && dp.getKhachHang().getSoDT() != null
                        ? dp.getKhachHang().getSoDT()
                        : "---";
                String cccd = dp.getKhachHang() != null && dp.getKhachHang().getSoCCCD() != null
                        ? dp.getKhachHang().getSoCCCD()
                        : "";
                int soPhong = 0;
                Object[] info = datPhongDAO.findCheckOutInfoByMaDat(maDat);
                if (info != null) {
                    @SuppressWarnings("unchecked")
                    List<Object[]> rooms = (List<Object[]>) info[1];
                    soPhong = rooms.size();
                }
                LocalDate ngayTra = dp.getNgayCheckOut() != null ? dp.getNgayCheckOut().toLocalDate() : null;
                allItems.add(new Object[] { maDat, tenKH, sdt, cccd, maDat, soPhong, ngayTra });
            }
        } else {
            // Theo phòng
            List<String> dsPhong = datPhongDAO.getPhongDangSuDung();
            for (String maPhong : dsPhong) {
                Object[] info = datPhongDAO.findCheckOutInfoByMaPhong(maPhong);
                if (info == null)
                    continue;
                DatPhong dp = (DatPhong) info[0];
                String tenKH = dp.getKhachHang() != null ? dp.getKhachHang().getTenKH() : "---";
                String sdt = dp.getKhachHang() != null && dp.getKhachHang().getSoDT() != null
                        ? dp.getKhachHang().getSoDT()
                        : "---";
                String cccd = dp.getKhachHang() != null && dp.getKhachHang().getSoCCCD() != null
                        ? dp.getKhachHang().getSoCCCD()
                        : "";
                LocalDate ngayTra = dp.getNgayCheckOut() != null ? dp.getNgayCheckOut().toLocalDate() : null;
                allItems.add(new Object[] { maPhong, tenKH, sdt, cccd, dp.getMaDat(), 1, ngayTra });
            }
        }
        renderList();
    }

    private void renderList() {
        roomItemsContainer.getChildren().clear();
        String q = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase();
        boolean filterByDate = chkLate != null && chkLate.isSelected() && dpFilter != null
                && dpFilter.getValue() != null;
        LocalDate filterDate = filterByDate ? dpFilter.getValue() : null;

        List<Object[]> filtered = allItems.stream().filter(it -> {
            // Text search filter
            if (!q.isEmpty()) {
                boolean match = ((String) it[0]).toLowerCase().contains(q)
                        || ((String) it[1]).toLowerCase().contains(q)
                        || ((String) it[2]).toLowerCase().contains(q)
                        || ((String) it[3]).toLowerCase().contains(q);
                if (!match)
                    return false;
            }
            // Date filter (theo ngày trả dự kiến)
            if (filterByDate) {
                LocalDate ngayTra = (LocalDate) it[6];
                if (ngayTra == null || !ngayTra.equals(filterDate))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            String emptyMsg;
            if (allItems.isEmpty()) {
                emptyMsg = isModeDon ? "Không có đơn nào đang sử dụng" : "Không có phòng nào đang có khách";
            } else if (filterByDate && q.isEmpty()) {
                emptyMsg = "Không có " + (isModeDon ? "đơn" : "phòng") + " nào trả ngày "
                        + filterDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else {
                emptyMsg = "Không tìm thấy kết quả phù hợp";
            }
            Label empty = new Label(emptyMsg);
            empty.setTextFill(Color.web(C_TEXT_LIGHT));
            empty.setFont(Font.font("Segoe UI", 13));
            empty.setPadding(new Insets(20));
            roomItemsContainer.getChildren().add(empty);
            return;
        }

        for (Object[] it : filtered) {
            roomItemsContainer.getChildren().add(createListItem(it));
        }
    }

    private HBox createListItem(Object[] it) {
        String pillText = (String) it[0];
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

        // Pill (mã phòng / mã đặt) - viền cam
        Label pill = new Label(pillText);
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

        // Tên khách + CCCD + số phòng cùng 1 dòng (in đậm, rõ nét)
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
        if (isModeDon && soPhong > 0) {
            Label dot2 = new Label("•");
            dot2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            dot2.setTextFill(Color.web(C_TEXT_GRAY));
            Label lblSoPhong = new Label(soPhong + " phòng");
            lblSoPhong.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lblSoPhong.setTextFill(Color.web(C_TEXT_DARK));
            info.getChildren().addAll(dot2, lblSoPhong);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // SĐT với icon điện thoại
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
            renderList(); // re-render để cập nhật highlight
            loadItemDetail(it);
        });
        return row;
    }

    // ============================================================
    // LOAD DETAIL WHEN AN ITEM IS CLICKED
    // ============================================================
    private void loadItemDetail(Object[] it) {
        String maDat = (String) it[4];
        String maPhong = isModeDon ? null : (String) it[0];

        Object[] result = datPhongDAO.findCheckOutInfoByMaDat(maDat);
        if (result == null) {
            new Alert(Alert.AlertType.WARNING, "Không tìm thấy thông tin checkout.", ButtonType.OK).showAndWait();
            return;
        }
        currentDatPhong = (DatPhong) result[0];
        @SuppressWarnings("unchecked")
        List<Object[]> rooms = (List<Object[]>) result[1];

        if (!isModeDon && maPhong != null) {
            final String mp = maPhong;
            rooms = rooms.stream().filter(r -> mp.equals((String) r[1])).collect(Collectors.toList());
        }
        currentRoomList = rooms;
        if (currentRoomList.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Đơn này không có phòng nào đang sử dụng.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        updateDetailInfoUI();
        loadAllServices();
        calculateBilling();
        updateBillingUI();
        btnConfirm.setVisible(true);
        btnConfirm.setManaged(true);
    }

    // ----- Update right-top: Thông tin đặt phòng -----
    private void updateDetailInfoUI() {
        detailInfoBox.getChildren().clear();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Build 2-column grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        c1.setPercentWidth(40);
        c2.setPercentWidth(60);
        grid.getColumnConstraints().addAll(c1, c2);

        String maDat = currentDatPhong.getMaDat();
        String tenKH = safe(currentDatPhong.getKhachHang() != null ? currentDatPhong.getKhachHang().getTenKH() : null);
        String sdt = safe(currentDatPhong.getKhachHang() != null ? currentDatPhong.getKhachHang().getSoDT() : null);
        String cccd = safe(currentDatPhong.getKhachHang() != null ? currentDatPhong.getKhachHang().getSoCCCD() : null);
        String ngayNhan = currentDatPhong.getNgayCheckIn() != null ? currentDatPhong.getNgayCheckIn().format(dtf)
                : "---";
        String ngayTra = currentDatPhong.getNgayCheckOut() != null ? currentDatPhong.getNgayCheckOut().format(dtf)
                : "---";

        double sumCoc = 0;
        for (Object[] r : currentRoomList) {
            sumCoc += (double) r[2];
        }

        // Left column
        VBox leftBox = new VBox(15);
        leftBox.getChildren().add(infoCell("Mã đặt phòng", maDat, false));
        leftBox.getChildren().add(phongInfoCell(currentRoomList));
        
        // Right column: use a nested GridPane for the customer info
        GridPane rightGrid = new GridPane();
        rightGrid.setHgap(15);
        rightGrid.setVgap(10);
        ColumnConstraints rc1 = new ColumnConstraints();
        rc1.setPercentWidth(50);
        ColumnConstraints rc2 = new ColumnConstraints();
        rc2.setPercentWidth(50);
        rightGrid.getColumnConstraints().addAll(rc1, rc2);

        rightGrid.add(infoCell("Khách hàng", tenKH, false), 0, 0);
        rightGrid.add(infoCell("Số điện thoại", sdt, false), 1, 0);
        rightGrid.add(infoCell("Ngày nhận", ngayNhan, false), 0, 1);
        rightGrid.add(infoCell("Ngày trả dự kiến", ngayTra, false), 1, 1);
        rightGrid.add(infoCell("CCCD", cccd, false), 0, 2);
        rightGrid.add(infoCell("Tiền cọc (phòng)", String.format("%,.0f đ", sumCoc), true), 1, 2);

        grid.add(leftBox, 0, 0);
        grid.add(rightGrid, 1, 0);

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

    /**
     * Cell hiển thị danh sách phòng:
     * - 1 phòng: inline như cell thường
     * - Nhiều phòng: cuộn dọc trong khung cao cố định 64px → các thành phần khác
     * KHÔNG bị dịch chuyển
     */
    private VBox phongInfoCell(List<Object[]> rooms) {
        VBox box = new VBox(4);

        Label lbl = new Label(rooms.size() > 1
                ? "Phòng  (" + rooms.size() + " phòng)"
                : "Phòng");
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web(C_TEXT_GRAY));

        if (rooms.size() == 1) {
            Object[] r = rooms.get(0);
            String mp = (String) r[1];
            Label val = new Label(mp);
            val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            val.setTextFill(Color.web(C_TEXT_DARK));
            box.getChildren().addAll(lbl, val);
            box.setMinHeight(135);
            return box;
        }

        // Nhiều phòng: hiện dạng chip trên FlowPane, tự ngắt dòng
        FlowPane chips = new FlowPane(6, 6);
        chips.setAlignment(Pos.CENTER_LEFT);
        for (Object[] r : rooms) {
            String mp = (String) r[1];

            Label chip = new Label(mp);
            chip.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            chip.setTextFill(Color.web(C_NAVY));
            chip.setPadding(new Insets(3, 10, 3, 10));
            chip.setStyle(
                    "-fx-background-color:" + C_BLUE_LIGHT + ";" +
                            "-fx-background-radius:6;");
            chips.getChildren().add(chip);
        }

        ScrollPane vScroll = new ScrollPane(chips);
        vScroll.setFitToWidth(true);
        vScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        vScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        vScroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;" +
                        "-fx-padding:0;");
        vScroll.setMinHeight(115);
        vScroll.setMaxHeight(160);

        box.getChildren().addAll(lbl, vScroll);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    // ============================================================
    // SERVICES + BILLING CALCULATION
    // ============================================================
    private void loadAllServices() {
        List<DichVuSuDung> allDV = new ArrayList<>();
        for (Object[] room : currentRoomList) {
            String maCTDP = (String) room[0];
            List<DichVuSuDung> dvRoom = dvsdDAO.findByMaCTDP(maCTDP);
            if (dvRoom != null) {
                for (DichVuSuDung dv : dvRoom) {
                    if (dv.getChiTietDatPhong() == null) {
                        model.entities.ChiTietDatPhong ctdp = new model.entities.ChiTietDatPhong();
                        ctdp.setMaCTDP(maCTDP);
                        dv.setChiTietDatPhong(ctdp);
                    }
                }
                allDV.addAll(dvRoom);
            }
        }
        serviceTable.setItems(FXCollections.observableArrayList(allDV));
        currentTienDV = allDV.stream().mapToDouble(DichVuSuDung::getThanhTien).sum();
        lblTotalDV.setText(String.format("%,.0f đ", currentTienDV));
    }

    private void calculateBilling() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkIn = currentDatPhong.getNgayCheckIn();
        LocalDateTime plannedOut = currentDatPhong.getNgayCheckOut();
        if (plannedOut == null)
            plannedOut = now;

        // 1. Tính số đêm dự kiến (Chỉ dựa vào ngày, KHÔNG phụ thuộc giờ check-in thực tế)
        long plannedNights = java.time.temporal.ChronoUnit.DAYS.between(checkIn.toLocalDate(), plannedOut.toLocalDate());
        if (plannedNights < 1) plannedNights = 1;

        // 2. Phụ phí tính theo SỐ NGÀY TRỄ so với dự kiến (Tiền phạt)
        currentSoNgayTre = 0;
        isLateCheckout = false;
        if (now.isAfter(plannedOut)) {
            long lateMins = Duration.between(plannedOut, now).toMinutes();
            
            // Từ đủ 30 phút trở đi tính là 1 ngày trễ
            if (lateMins >= 30) {
                isLateCheckout = true;
                currentSoNgayTre = (lateMins - 30 + 1440) / 1440; 
            }
        }

        // Vẫn tính luôn tiền phòng ngày trễ đó
        currentSoDem = plannedNights + currentSoNgayTre;

        currentTienPhong = 0;
        currentTienCoc = 0;
        currentLateFee = 0;
        minLateFee = 0;
        // Tính phụ phí trả muộn per-room
        for (Object[] room : currentRoomList) {
            String maCTDP = (String) room[0];
            double giaCoc = (double) room[2];
            double giaPhong = (double) room[3];
            currentTienPhong += giaPhong * currentSoDem;
            currentTienCoc += giaCoc;
            // Phụ thu phí trả muộn = 50% giá tiền phòng * số ngày trễ
            double roomLate = giaPhong * 0.5 * currentSoNgayTre;
            roomLateFeeMap.put(maCTDP, roomLate);
            roomPhuThuMap.putIfAbsent(maCTDP, 0.0);
            currentLateFee += roomLate;
        }
        minLateFee = currentLateFee;
        currentPhuThu = roomPhuThuMap.values().stream().mapToDouble(Double::doubleValue).sum();
        double base = currentTienPhong + currentLateFee + currentTienDV + currentPhuThu;
        currentTongTien = Math.max(0, base * (1 + VAT_RATE) - currentTienCoc);
    }

    // ----- Update right-bottom: Chi tiết hóa đơn (no scroll) -----
    private void updateBillingUI() {
        billingBox.getChildren().clear();

        VBox rows = new VBox(10);

        // Tiền phòng - hiển thị đêm × giá
        String tienPhongLbl;
        if (currentRoomList.size() == 1) {
            double giaPhong = (double) currentRoomList.get(0)[3];
            tienPhongLbl = String.format("Tiền phòng (%d đêm × %,.0f đ)", currentSoDem, giaPhong);
        } else {
            tienPhongLbl = String.format("Tiền phòng (%d đêm × %d phòng)", currentSoDem, currentRoomList.size());
        }
        rows.getChildren()
                .add(billRow(tienPhongLbl, String.format("%,.0f đ", currentTienPhong), Color.web(C_TEXT_DARK)));
        rows.getChildren()
                .add(billRow("Tiền dịch vụ", String.format("%,.0f đ", currentTienDV), Color.web(C_TEXT_DARK)));

        // ===== PHỤ THU THEO PHÒNG (per-room) =====
        rows.getChildren().add(buildPerRoomSurchargeSection());

        rows.getChildren()
                .add(billRow("Tiền cọc (đã khấu trừ)", String.format("%,.0f đ", -currentTienCoc), Color.web(C_GREEN)));

        // VAT label (instance field for live update)
        HBox vatRow = new HBox();
        vatRow.setAlignment(Pos.CENTER_LEFT);
        Label vatLbl = new Label(String.format("Thuế VAT (%.0f%%)", VAT_RATE * 100));
        vatLbl.setTextFill(Color.web(C_TEXT_GRAY));
        vatLbl.setFont(Font.font("Segoe UI", 13));
        Region vatSp = new Region();
        HBox.setHgrow(vatSp, Priority.ALWAYS);
        lblVAT = new Label(String.format("%,.0f đ",
                (currentTienPhong + currentLateFee + currentTienDV + currentPhuThu) * VAT_RATE));
        lblVAT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblVAT.setTextFill(Color.web(C_TEXT_DARK));
        vatRow.getChildren().addAll(vatLbl, vatSp, lblVAT);
        rows.getChildren().add(vatRow);

        // Đường kẻ + Tổng
        Separator sep = new Separator();

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        totalRow.setPadding(new Insets(4, 0, 4, 0));
        Label lblT = new Label("TỔNG THANH TOÁN");
        lblT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblT.setTextFill(Color.web(C_TEXT_DARK));
        Region totSp = new Region();
        HBox.setHgrow(totSp, Priority.ALWAYS);
        lblTongTien = new Label(String.format("%,.0f đ", currentTongTien));
        lblTongTien.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTongTien.setTextFill(Color.web(C_BLUE));
        totalRow.getChildren().addAll(lblT, totSp, lblTongTien);

        billingBox.getChildren().addAll(rows, sep, totalRow);
    }

    /**
     * Xây dựng phần nhập phụ thu theo từng phòng.
     * Gồm header + 1 dòng cho mỗi phòng với 2 ô: Phí phụ thu (TextField) + Phụ phí
     * trả muộn (Label, tự tính).
     */
    private VBox buildPerRoomSurchargeSection() {
        VBox section = new VBox(6);
        section.setPadding(new Insets(8, 0, 8, 0));

        // Header
        Label header = new Label("Phụ thu theo phòng" + (isLateCheckout ? " (trễ " + currentSoNgayTre + " ngày)" : ""));
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        header.setTextFill(Color.web(C_NAVY));
        section.getChildren().add(header);

        // Khung viền chứa Header + List (tạo hiệu ứng table đẹp hơn)
        VBox tableBox = new VBox(0);
        tableBox.setStyle(
                "-fx-border-color:" + C_BORDER + ";" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;" +
                        "-fx-background-color:white;");

        // Column header row
        GridPane gridHeader = new GridPane();
        gridHeader.setHgap(8);
        gridHeader.setPadding(new Insets(8, 12, 8, 12));
        gridHeader.setStyle(
                "-fx-background-color:" + C_BORDER_SOFT + ";" +
                        "-fx-background-radius:8 8 0 0;");
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setMinWidth(50);
        cc1.setPrefWidth(55);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(45);
        cc2.setHalignment(javafx.geometry.HPos.RIGHT);
        ColumnConstraints cc3 = new ColumnConstraints();
        cc3.setPercentWidth(35);
        cc3.setHalignment(javafx.geometry.HPos.RIGHT);
        gridHeader.getColumnConstraints().addAll(cc1, cc2, cc3);

        Label h1 = new Label("Phòng");
        h1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        h1.setTextFill(Color.web(C_TEXT_GRAY));
        Label h2 = new Label("Phí phụ thu");
        h2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        h2.setTextFill(Color.web(C_TEXT_GRAY));
        h2.setMaxWidth(Double.MAX_VALUE);
        h2.setAlignment(Pos.CENTER_RIGHT);
        Label h3 = new Label("Phí trả muộn");
        h3.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        h3.setTextFill(Color.web(C_TEXT_GRAY));
        h3.setMaxWidth(Double.MAX_VALUE);
        h3.setAlignment(Pos.CENTER_RIGHT);
        gridHeader.add(h1, 0, 0);
        gridHeader.add(h2, 1, 0);
        gridHeader.add(h3, 2, 0);

        tableBox.getChildren().add(gridHeader);

        // Body container
        VBox rowsContainer = new VBox();
        rowsContainer.setPadding(new Insets(0));

        // Each room row
        boolean isEven = false;
        for (Object[] room : currentRoomList) {
            String maCTDP = (String) room[0];
            String maPhong = (String) room[1];
            double lateFee = roomLateFeeMap.getOrDefault(maCTDP, 0.0);

            GridPane rowGrid = new GridPane();
            rowGrid.setHgap(8);
            rowGrid.setPadding(new Insets(6, 12, 6, 12));
            rowGrid.getColumnConstraints().addAll(cc1, cc2, cc3);

            if (isEven) {
                rowGrid.setStyle("-fx-background-color: #fafbfc;");
            } else {
                rowGrid.setStyle("-fx-background-color: white;");
            }
            isEven = !isEven;

            // Cột 1: Mã phòng
            Label lblRoom = new Label(maPhong);
            lblRoom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblRoom.setTextFill(Color.web(C_NAVY));

            // Cột 2: Phí phụ thu (TextField - cho nhập tay)
            TextField txtPT = new TextField(String.format("%.0f", roomPhuThuMap.getOrDefault(maCTDP, 0.0)));
            txtPT.setPrefWidth(100);
            txtPT.setStyle(
                    "-fx-alignment:CENTER-RIGHT;" +
                            "-fx-font-size:12px;" +
                            "-fx-font-weight:bold;" +
                            "-fx-text-fill:" + C_TEXT_DARK + ";" +
                            "-fx-border-color:" + C_BORDER + ";" +
                            "-fx-border-radius:6;" +
                            "-fx-background-radius:6;" +
                            "-fx-padding:4 8;");
            txtPT.setTextFormatter(new TextFormatter<>(change -> {
                String t = change.getControlNewText();
                if (t.isEmpty() || t.matches("[0-9]+"))
                    return change;
                return null;
            }));

            txtPT.focusedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    txtPT.setStyle(
                            txtPT.getStyle().replace("-fx-border-color:" + C_BORDER, "-fx-border-color:" + C_BLUE));
                } else {
                    txtPT.setStyle(
                            txtPT.getStyle().replace("-fx-border-color:" + C_BLUE, "-fx-border-color:" + C_BORDER));
                }
            });

            final String key = maCTDP;
            txtPT.textProperty().addListener((obs, o, n) -> {
                try {
                    double val = n.isEmpty() ? 0 : Double.parseDouble(n);
                    roomPhuThuMap.put(key, val);
                    currentPhuThu = roomPhuThuMap.values().stream().mapToDouble(Double::doubleValue).sum();
                    recalcTotal();
                } catch (NumberFormatException ignored) {
                }
            });

            // Cột 3: Phụ phí trả muộn (Label - tự động tính, không sửa)
            Label lblLate = new Label(String.format("%,.0f đ", lateFee));
            lblLate.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblLate.setTextFill(lateFee > 0 ? Color.web("#ef4444") : Color.web(C_TEXT_DARK));
            lblLate.setMaxWidth(Double.MAX_VALUE);
            lblLate.setAlignment(Pos.CENTER_RIGHT);

            rowGrid.add(lblRoom, 0, 0);
            rowGrid.add(txtPT, 1, 0);
            rowGrid.add(lblLate, 2, 0);

            rowsContainer.getChildren().add(rowGrid);
        }

        ScrollPane scroll = new ScrollPane(rowsContainer);
        scroll.setFitToWidth(true);
        scroll.setMinHeight(50);
        scroll.setPrefHeight(82);
        scroll.setMaxHeight(90);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background:transparent;" +
                        "-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;" +
                        "-fx-padding:0;");

        tableBox.getChildren().add(scroll);
        section.getChildren().add(tableBox);

        // Tổng dòng phụ thu
        HBox totalPhuThu = new HBox();
        totalPhuThu.setAlignment(Pos.CENTER_LEFT);
        totalPhuThu.setPadding(new Insets(4, 0, 0, 0));
        Label lblTotalPT = new Label("Tổng phụ thu + trả muộn:");
        lblTotalPT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblTotalPT.setTextFill(Color.web(C_TEXT_GRAY));
        Region spPT = new Region();
        HBox.setHgrow(spPT, Priority.ALWAYS);
        Label lblTotalPTVal = new Label(String.format("%,.0f đ", currentPhuThu + currentLateFee));
        lblTotalPTVal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblTotalPTVal.setTextFill(currentPhuThu + currentLateFee > 0 ? Color.web("#ef4444") : Color.web(C_TEXT_DARK));
        totalPhuThu.getChildren().addAll(lblTotalPT, spPT, lblTotalPTVal);
        section.getChildren().add(totalPhuThu);

        return section;
    }

    private HBox billRow(String label, String value, Color valColor) {
        HBox hb = new HBox();
        hb.setMaxWidth(Double.MAX_VALUE);
        hb.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setTextFill(Color.web(C_TEXT_GRAY));
        lbl.setFont(Font.font("Segoe UI", 13));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        val.setTextFill(valColor);
        hb.getChildren().addAll(lbl, sp, val);
        return hb;
    }

    private void recalcTotal() {
        currentPhuThu = roomPhuThuMap.values().stream().mapToDouble(Double::doubleValue).sum();
        double base = currentTienPhong + currentLateFee + currentTienDV + currentPhuThu;
        currentTongTien = Math.max(0, base * (1 + VAT_RATE) - currentTienCoc);
        if (lblVAT != null)
            lblVAT.setText(String.format("%,.0f đ",
                    (currentTienPhong + currentLateFee + currentTienDV + currentPhuThu) * VAT_RATE));
        if (lblTongTien != null)
            lblTongTien.setText(String.format("%,.0f đ", currentTongTien));
    }

    // ============================================================
    // CHECKOUT EXECUTION
    // ============================================================
    private void handleCheckOut() {
        if (currentDatPhong == null || currentRoomList == null)
            return;

        // Logic kiểm tra phí trả muộn cũ đã bỏ
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thanh toán");
        confirm.setHeaderText("Checkout " + currentRoomList.size()
                + " phòng cho khách " + currentDatPhong.getKhachHang().getTenKH() + "?");
        confirm.setContentText("Tổng: " + String.format("%,.0f đ", currentTongTien));
        confirm.showAndWait().ifPresent(type -> {
            if (type == ButtonType.OK) {
                HoaDon hd = performCheckOut();
                if (hd != null) {
                    HoaDonView.showHoaDonDetail(hd);
                    resetDetail();
                    refreshList();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Lưu dữ liệu thất bại. Vui lòng thử lại.").showAndWait();
                }
            }
        });
    }

    private HoaDon performCheckOut() {
        try {
            LocalDateTime now = LocalDateTime.now();
            HoaDon hd = hoaDonDAO.getByMaDat(currentDatPhong.getMaDat());
            if (hd == null) {
                hd = new HoaDon();
                hd.setMaHD(hoaDonDAO.generateMaHD());
                hd.setDatPhong(currentDatPhong);
                hd.setNhanVien(staff != null ? staff : new NhanVien("LUCIA001"));
                hd.setNgayTaoHD(now);
                hd.setTienPhong(0);
                hd.setTienDV(0);
                hd.setTienCoc(0);
                hd.setThueVAT(VAT_RATE);
                hoaDonDAO.tinhTongTien(hd);
                hoaDonDAO.tinhDoanhThu(hd);
                hoaDonDAO.insert(hd);
            }
            hd.setDatPhong(currentDatPhong);
            for (Object[] room : currentRoomList) {
                String maCTDP = (String) room[0];
                String maPhong = (String) room[1];
                double giaPhong = (double) room[3];
                double tienPhong = currentSoDem * giaPhong;
                double roomPhuThu = roomPhuThuMap.getOrDefault(maCTDP, 0.0);
                double roomLateFee = roomLateFeeMap.getOrDefault(maCTDP, 0.0);
                String maCTHD = cthdDAO.getMaCTHDByMaCTDP(maCTDP);
                if (maCTHD != null) {
                    cthdDAO.updateLuuTruVaTien(maCTHD, currentSoDem, tienPhong, roomPhuThu, roomLateFee);
                } else {
                    maCTHD = cthdDAO.generateMaCTHD();
                    cthdDAO.insert(maCTHD, hd.getMaHD(), maCTDP, currentSoDem, tienPhong, roomPhuThu, roomLateFee);
                }
                phongDAO.updateTrangThai(maPhong, "BAN");
            }
            double sumPhong = hoaDonDAO.getTongTienPhongCurrent(hd.getMaHD());
            List<DichVuSuDung> listDV = dvsdDAO.findByMaHD(hd.getMaHD());
            double tienDV = listDV.stream().mapToDouble(DichVuSuDung::getThanhTien).sum();

            List<model.entities.ChiTietHoaDon> cthdList = cthdDAO.getByMaHD(hd.getMaHD());
            double sumPhuThu = cthdList.stream().mapToDouble(c -> c.getPhuThu()).sum();
            double sumLateFee = cthdList.stream().mapToDouble(c -> c.getPhuPhiTraMuon()).sum();
            double tongCoc = cthdDAO.getTongCocByMaHD(hd.getMaHD());

            hd.setTienPhong(sumPhong);
            hd.setTienDV(tienDV);
            hd.setPhuThu(sumPhuThu);
            hd.setPhuPhiTraMuon(sumLateFee);
            hd.setTienCoc(tongCoc);

            hd.setThueVAT(VAT_RATE);
            hd.setNgayTaoHD(now);

            // Tính toán tổng tiền mới (Tiền phòng + Tiền DV + Phụ thu + Phí trả muộn) * (1
            // + VAT) - Tiền cọc
            double base = hd.getTienPhong() + hd.getTienDV() + hd.getPhuThu() + hd.getPhuPhiTraMuon();
            double newTong = Math.max(0, base * (1 + VAT_RATE) - hd.getTienCoc());
            hd.setTongTien(newTong);
            // Doanh thu không bao gồm tiền cọc (đã tính trong tiền phòng)
            hd.setDoanhThu(base + (base * VAT_RATE));
            if (datPhongDAO.isAllRoomsCheckedOut(currentDatPhong.getMaDat())) {
                datPhongDAO.updateTrangThai(currentDatPhong.getMaDat(), "DA_CHECKOUT");
                hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            }
            if (hoaDonDAO.updateTongTien(hd))
                return hd;
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ============================================================
    // RESET / HELPERS
    // ============================================================
    private void resetDetail() {
        if (txtSearch != null)
            txtSearch.clear();
        currentDatPhong = null;
        currentRoomList = null;
        selectedItem = null;
        currentTienPhong = 0;
        currentTienDV = 0;
        currentTienCoc = 0;
        currentLateFee = 0;
        currentPhuThu = 0;
        minLateFee = 0;
        currentTongTien = 0;
        currentSoDem = 0;
        currentSoNgayTre = 0;
        roomPhuThuMap.clear();
        roomLateFeeMap.clear();

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
        if (billingBox != null) {
            billingBox.getChildren().clear();
            Label lbl = new Label("Chọn phòng / đơn để xem chi tiết");
            lbl.setTextFill(Color.web(C_TEXT_LIGHT));
            lbl.setFont(Font.font("Segoe UI", 13));
            billingBox.getChildren().add(lbl);
        }
        if (serviceTable != null)
            serviceTable.setItems(FXCollections.observableArrayList());
        if (lblTotalDV != null)
            lblTotalDV.setText("0 đ");
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