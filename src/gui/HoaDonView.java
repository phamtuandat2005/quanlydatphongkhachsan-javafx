package gui;

import dao.HoaDonDAO;
import dao.ChiTietHoaDonDAO;
import model.entities.HoaDon;
import model.utils.RevenueCalculator;
import model.utils.InvoiceExporter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HoaDonView – JavaFX
 */
public class HoaDonView extends BorderPane {

    /* ── Bảng màu ───────────────────────────────────────────────────── */
    private static final String C_BG = "#f8f9fa";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_GREEN = "#16a34a";

    /* ── DAO & Dữ liệu ────────────────────────────────────────────── */
    private final HoaDonDAO dao = new HoaDonDAO();
    private final ChiTietHoaDonDAO cthdDAO = new ChiTietHoaDonDAO();
    private ObservableList<HoaDon> masterData = FXCollections.observableArrayList();
    private FilteredList<HoaDon> filteredData = new FilteredList<>(masterData, p -> true);
    /* ── Controls ───────────────────────────────────────────────────── */
    private TableView<HoaDon> table;
    private TextField txtSearch;
    private Label lblTongDoanhThu, lblSoHoaDon;
    private ComboBox<String> cbGroupBy;
    private Label lblFrom, lblTo, lblMonth, lblQuarter, lblYear;
    private DatePicker dpFrom, dpTo;
    private ComboBox<Integer> cbMonth, cbQuarter, cbYear;

    /* ── Column filter state ────────────────────────────────────────── */
    private java.util.Set<String> selectedTrangThais = new java.util.HashSet<>();
    private TableColumn<HoaDon, String> colTrangThai;
    private Button btnFilterTT;
    private ContextMenu menuTrangThai;

    public HoaDonView() {
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildTableCard());

        loadData();
    }

    private VBox buildHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(0, 0, 24, 0));

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Danh sách hóa đơn");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));
        Label lblSubtitle = new Label("Quản lý và tra cứu toàn bộ danh sách hóa đơn đã lập");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        HBox statsRow = new HBox(20);
        lblTongDoanhThu = new Label("0 đ");
        lblSoHoaDon = new Label("0");

        statsRow.getChildren().addAll(
                createStatCard("💰", "TỔNG DOANH THU", lblTongDoanhThu, C_GREEN),
                createStatCard("📄", "TỔNG SỐ HÓA ĐƠN", lblSoHoaDon, C_NAVY));

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm kiếm theo mã hóa đơn, tên khách hoặc mã đặt phòng...");
        txtSearch.setPrefHeight(44);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + C_BORDER
                + "; -fx-padding: 0 16;");
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter(n));
        Label lblFilter = new Label("Xem theo:");
        lblFilter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblFilter.setTextFill(Color.web(C_TEXT_DARK));

        cbGroupBy = new ComboBox<>(FXCollections.observableArrayList(
                "Ngày", "Tháng", "Quý", "Năm"));
        cbGroupBy.setValue("Ngày");
        cbGroupBy.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 44; -fx-background-radius: 8;");

        lblFrom = new Label("Từ ngày:");
        lblFrom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblFrom.setTextFill(Color.web(C_TEXT_DARK));

        dpFrom = new DatePicker(java.time.LocalDate.now());
        dpFrom.setPrefWidth(140);
        dpFrom.setPrefHeight(44);
        dpFrom.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-radius: 8;");

        lblTo = new Label("Đến ngày:");
        lblTo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblTo.setTextFill(Color.web(C_TEXT_DARK));

        dpTo = new DatePicker(java.time.LocalDate.now());
        dpTo.setPrefWidth(140);
        dpTo.setPrefHeight(44);
        dpTo.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-radius: 8;");

        lblMonth = new Label("Tháng:");
        lblMonth.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblMonth.setTextFill(Color.web(C_TEXT_DARK));
        cbMonth = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        cbMonth.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 44; -fx-background-radius: 8;");

        lblQuarter = new Label("Quý:");
        lblQuarter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblQuarter.setTextFill(Color.web(C_TEXT_DARK));
        cbQuarter = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
        cbQuarter.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 44; -fx-background-radius: 8;");

        lblYear = new Label("Năm:");
        lblYear.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblYear.setTextFill(Color.web(C_TEXT_DARK));
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = java.time.LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear; i++)
            years.add(i);
        cbYear = new ComboBox<>(years);
        cbYear.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 44; -fx-background-radius: 8;");

        cbMonth.setValue(java.time.LocalDate.now().getMonthValue());
        cbQuarter.setValue((java.time.LocalDate.now().getMonthValue() - 1) / 3 + 1);
        cbYear.setValue(currentYear);

        cbGroupBy.valueProperty().addListener((obs, oldV, newV) -> {
            updateFilterUI(newV);
            applyFilter(txtSearch.getText());
        });
        dpFrom.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter(txtSearch.getText()));
        dpTo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter(txtSearch.getText()));
        cbMonth.valueProperty().addListener((obs, oldV, newV) -> applyFilter(txtSearch.getText()));
        cbQuarter.valueProperty().addListener((obs, oldV, newV) -> applyFilter(txtSearch.getText()));
        cbYear.valueProperty().addListener((obs, oldV, newV) -> applyFilter(txtSearch.getText()));

        HBox dateBox = new HBox(12, lblFilter, cbGroupBy, lblFrom, dpFrom, lblTo, dpTo, lblMonth, cbMonth, lblQuarter,
                cbQuarter, lblYear, cbYear);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        // Spacer đẩy dateBox sang bên phải
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        filterRow.getChildren().addAll(txtSearch, spacer, dateBox);

        header.getChildren().addAll(titleBox, statsRow, filterRow);

        updateFilterUI(cbGroupBy.getValue());

        return header;
    }

    private void updateFilterUI(String groupBy) {
        boolean isM = "Tháng".equals(groupBy);
        boolean isQ = "Quý".equals(groupBy);
        boolean isY = "Năm".equals(groupBy);
        boolean isNormal = "Ngày".equals(groupBy);

        lblFrom.setVisible(isNormal);
        lblFrom.setManaged(isNormal);
        dpFrom.setVisible(isNormal);
        dpFrom.setManaged(isNormal);
        lblTo.setVisible(isNormal);
        lblTo.setManaged(isNormal);
        dpTo.setVisible(isNormal);
        dpTo.setManaged(isNormal);

        lblMonth.setVisible(isM);
        lblMonth.setManaged(isM);
        cbMonth.setVisible(isM);
        cbMonth.setManaged(isM);

        lblQuarter.setVisible(isQ);
        lblQuarter.setManaged(isQ);
        cbQuarter.setVisible(isQ);
        cbQuarter.setManaged(isQ);

        lblYear.setVisible(isM || isQ || isY);
        lblYear.setManaged(isM || isQ || isY);
        cbYear.setVisible(isM || isQ || isY);
        cbYear.setManaged(isM || isQ || isY);
    }

    private VBox buildTableCard() {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: " + C_BORDER + ";");
        card.setEffect(new DropShadow(10, 0, 4, Color.web("#00000008")));

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        table.setPlaceholder(new Label("Không có dữ liệu"));
        table.setItems(filteredData);

        TableColumn<HoaDon, Void> colStt = new TableColumn<>("STT");
        colStt.setPrefWidth(50);
        colStt.setMinWidth(30);
        colStt.setMaxWidth(45);
        colStt.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        colStt.setCellFactory(col -> new TableCell<HoaDon, Void>() {
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        TableColumn<HoaDon, String> colMa = new TableColumn<>("Mã hóa đơn");
        colMa.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMaHD()));
        colMa.setPrefWidth(120);
        colMa.setMinWidth(40);
        colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        TableColumn<HoaDon, String> colKhach = new TableColumn<>("Khách hàng");
        colKhach.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getDatPhong() != null &&
                        p.getValue().getDatPhong().getKhachHang() != null
                                ? p.getValue().getDatPhong().getKhachHang().getTenKH()
                                : "—"));
        colKhach.setPrefWidth(200);
        colKhach.setMinWidth(50);

        TableColumn<HoaDon, String> colNgay = new TableColumn<>("Ngày lập");
        colNgay.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getNgayTaoHD() != null
                ? p.getValue().getNgayTaoHD().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—"));
        colNgay.setPrefWidth(160);
        colNgay.setMinWidth(50);
        colNgay.setStyle("-fx-alignment: CENTER;");

        TableColumn<HoaDon, String> colTongCP = new TableColumn<>("Tổng chi phí");
        colTongCP.setCellValueFactory(
                p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getTongCP())));
        colTongCP.setStyle("-fx-alignment: CENTER-RIGHT;");
        colTongCP.setPrefWidth(120);
        colTongCP.setMinWidth(50);

        TableColumn<HoaDon, String> colTienCoc = new TableColumn<>("Tiền cọc");
        colTienCoc.setCellValueFactory(
                p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getTienCoc())));
        colTienCoc.setStyle("-fx-alignment: CENTER-RIGHT;");
        colTienCoc.setPrefWidth(120);
        colTienCoc.setMinWidth(50);

        TableColumn<HoaDon, String> colPhuThu = new TableColumn<>("Phụ phí");
        colPhuThu.setCellValueFactory(
                p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getPhuThu())));
        colPhuThu.setStyle("-fx-alignment: CENTER-RIGHT;");
        colPhuThu.setPrefWidth(110);
        colPhuThu.setMinWidth(40);

        TableColumn<HoaDon, String> colPhuPhiTraMuon = new TableColumn<>("Phí trả muộn");
        colPhuPhiTraMuon.setCellValueFactory(
                p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getPhuPhiTraMuon())));
        colPhuPhiTraMuon.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #ef4444;");
        colPhuPhiTraMuon.setPrefWidth(120);
        colPhuPhiTraMuon.setMinWidth(50);
        colPhuPhiTraMuon.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
                }
            }
        });

        TableColumn<HoaDon, String> colTong = new TableColumn<>("Tổng thanh toán");
        colTong.setCellValueFactory(
                p -> new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getTongTien())));
        colTong.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        colTong.setPrefWidth(160);
        colTong.setMinWidth(60);

        // ── [ĐÃ SỬA] Cột Trạng thái TT có BADGE MÀU ────────────────
        colTrangThai = new TableColumn<>();
        colTrangThai.setPrefWidth(160);
        colTrangThai.setMinWidth(70);
        colTrangThai.setStyle("-fx-alignment: CENTER;");
        colTrangThai.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getTrangThaiThanhToan() != null ? p.getValue().getTrangThaiThanhToan() : ""));
        colTrangThai.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String trangThai, boolean empty) {
                super.updateItem(trangThai, empty);
                if (empty || trangThai == null || trangThai.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String text;
                String bg, fg;
                switch (trangThai) {
                    case "DA_THANH_TOAN" -> {
                        text = "Đã thanh toán";
                        bg = "#d1fae5";
                        fg = "#065f46";
                    }
                    case "DA_THANH_TOAN_COC" -> {
                        text = "Đã đặt cọc";
                        bg = "#fef3c7";
                        fg = "#92400e";
                    }
                    case "CHUA_THANH_TOAN" -> {
                        text = "Chưa thanh toán";
                        bg = "#fee2e2";
                        fg = "#b91c1c";
                    }
                    case "DA_HOAN_COC" -> {
                        text = "Đã hủy - hoàn cọc";
                        bg = "#e0e7ff";
                        fg = "#3730a3";
                    }
                    case "DA_MAT_COC" -> {
                        text = "Đã hủy - mất cọc";
                        bg = "#fce7f3";
                        fg = "#be185d";
                    }
                    default -> {
                        text = trangThai;
                        bg = "#f3f4f6";
                        fg = "#6b7280";
                    }
                }

                Label badge = new Label(text);
                badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                        "; -fx-padding: 4 12 4 12; -fx-background-radius: 12;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().add(colStt);
        table.getColumns().add(colMa);
        table.getColumns().add(colKhach);
        table.getColumns().add(colNgay);
        table.getColumns().add(colTongCP);
        table.getColumns().add(colTienCoc);
        table.getColumns().add(colPhuThu);
        table.getColumns().add(colPhuPhiTraMuon);
        table.getColumns().add(colTong);
        table.getColumns().add(colTrangThai);

        // Ràng buộc chiều rộng các cột để tự giãn đầy bảng và không cho kéo
        double sttW = 55;
        colMa.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.09));
        colKhach.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.14));
        colNgay.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.12));
        colTongCP.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.10));
        colTienCoc.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.08));
        colPhuThu.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.07));
        colPhuPhiTraMuon.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.09));
        colTong.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.13));
        colTrangThai.prefWidthProperty().bind(table.widthProperty().subtract(sttW).multiply(0.15));

        // Khóa cứng tất cả cột – không cho kéo thả hay co dãn
        for (TableColumn<HoaDon, ?> c : table.getColumns()) {
            c.setReorderable(false);
            c.setResizable(false);
        }
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setRowFactory(tv -> {
            TableRow<HoaDon> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showHoaDonDetail(row.getItem());
                }
            });
            return row;
        });

        Label lblHint = new Label(
                "💡 Mẹo: Nháy đúp chuột vào một hóa đơn bất kỳ để xem chi tiết tiền phòng, dịch vụ...");
        lblHint.setTextFill(Color.web(C_TEXT_GRAY));
        lblHint.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 13));
        lblHint.setPadding(new Insets(10, 0, 0, 0));

        installColumnFilter();

        card.getChildren().addAll(table, lblHint);
        return card;
    }

    private void installColumnFilter() {
        String baseName = "Trạng thái TT";
        menuTrangThai = new ContextMenu();

        MenuItem all = new MenuItem("Tất cả trạng thái");

        String[][] statuses = {
                { "DA_THANH_TOAN", "Đã thanh toán" },
                { "CHUA_THANH_TOAN", "Chưa thanh toán" },
                { "DA_THANH_TOAN_COC", "Đã đặt cọc" },
                { "DA_HOAN_COC", "Đã hủy - hoàn cọc" },
                { "DA_MAT_COC", "Đã hủy - mất cọc" }
        };

        java.util.List<CheckBox> checkBoxes = new java.util.ArrayList<>();

        // Nút "Tất cả trạng thái" sẽ làm mới lại danh sách
        all.setOnAction(e -> {
            selectedTrangThais.clear();
            for (CheckBox cb : checkBoxes) {
                cb.setSelected(false); // Bỏ tích toàn bộ CheckBox
            }
            btnFilterTT.setText(baseName + " ▼");
            applyFilter(txtSearch.getText());
        });

        menuTrangThai.getItems().add(all);
        menuTrangThai.getItems().add(new SeparatorMenuItem());

        // Tạo các tùy chọn lọc bằng CheckBox
        for (String[] st : statuses) {
            CheckBox cb = new CheckBox(st[1]);
            cb.setStyle("-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 4 8;");

            // CustomMenuItem giúp menu KHÔNG BỊ ĐÓNG khi click vào CheckBox
            CustomMenuItem cmi = new CustomMenuItem(cb);
            cmi.setHideOnClick(false);
            menuTrangThai.getItems().add(cmi);

            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedTrangThais.add(st[0]);
                } else {
                    selectedTrangThais.remove(st[0]);
                }

                // Cập nhật text cho Header Button
                if (selectedTrangThais.isEmpty()) {
                    btnFilterTT.setText(baseName + " ▼");
                } else {
                    btnFilterTT.setText("Đã chọn (" + selectedTrangThais.size() + ") ▼");
                }
                applyFilter(txtSearch.getText());
            });
            checkBoxes.add(cb);
        }

        btnFilterTT = new Button(baseName + " ▼");
        btnFilterTT.setStyle("-fx-font-size: 12px; -fx-background-color: transparent;"
                + " -fx-padding: 4 8; -fx-cursor: hand; -fx-font-weight: bold;");
        btnFilterTT.setMaxWidth(Double.MAX_VALUE);
        btnFilterTT.setOnAction(e -> menuTrangThai.show(btnFilterTT, javafx.geometry.Side.BOTTOM, 0, 0));

        colTrangThai.setGraphic(btnFilterTT);
        colTrangThai.setText("");
    }

    public static void showHoaDonDetail(HoaDon hd) {
        dao.ChiTietHoaDonDAO cthdDAO = new dao.ChiTietHoaDonDAO();
        java.util.List<Object[]> dsPhong = cthdDAO.getDanhSachPhongDaTra(hd.getMaHD());

        Stage detailStage = new Stage();
        detailStage.setTitle("Chi tiết hóa đơn: " + hd.getMaHD());
        detailStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: white;");
        root.setMinWidth(520);

        Label lblTitle = new Label("💳 Hóa đơn: " + hd.getMaHD());
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblTitle.setTextFill(Color.web(C_NAVY));

        VBox khachBox = new VBox(8);
        khachBox.setPadding(new Insets(12));
        khachBox.setStyle("-fx-background-color: #f0f9ff; -fx-background-radius: 8;");
        String tenKH = hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null
                ? hd.getDatPhong().getKhachHang().getTenKH()
                : "—";
        String soDT = hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null
                ? (hd.getDatPhong().getKhachHang().getSoDT() != null ? hd.getDatPhong().getKhachHang().getSoDT() : "—")
                : "—";
        String cccd = hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null
                ? (hd.getDatPhong().getKhachHang().getSoCCCD() != null ? hd.getDatPhong().getKhachHang().getSoCCCD()
                        : "—")
                : "—";
        String maDat = hd.getDatPhong() != null ? hd.getDatPhong().getMaDat() : "—";
        String maNV = hd.getNhanVien() != null ? hd.getNhanVien().getMaNV() : "—";
        String tenNV = hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "—";
        khachBox.getChildren().addAll(
                makeBillInfoRow("👤 Khách hàng:", tenKH),
                makeBillInfoRow("📞 Số điện thoại:", soDT),
                makeBillInfoRow("🆔 Số CCCD:", cccd),
                makeBillInfoRow("🏠 Mã đặt phòng:", maDat),
                makeBillInfoRow("👔 Mã nhân viên lập:", maNV),
                makeBillInfoRow("📝 Tên nhân viên:", tenNV));

        dao.DichVuSuDungDAO dvsdDAO = new dao.DichVuSuDungDAO();

        Label lblPhongHeader = new Label("🛏  Phòng đã trả (Nhấn để xem dịch vụ)");
        lblPhongHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblPhongHeader.setTextFill(Color.web(C_TEXT_DARK));

        VBox phongBox = new VBox(8);
        double tongTienPhong = 0;
        double tongCoc = 0;
        double dynamicTienDV = 0;

        if (dsPhong.isEmpty()) {
            phongBox.getChildren().add(new Label("— Chưa có dữ liệu phòng"));
        } else {
            for (Object[] row : dsPhong) {
                String maPhong = (String) row[0];
                String tenPhong = (String) row[1];
                String loaiPhong = (String) row[2];
                double sodem = (double) row[3];
                double thanhTien = (double) row[4];
                double giaCoc = (double) row[5];
                String maCTDP = (String) row[6];
                double roomPhuThu = (double) row[7];
                double roomPhuPhiTraMuon = (double) row[8];
                tongTienPhong += thanhTien;
                ;
                tongCoc += giaCoc;

                VBox roomContainer = new VBox();
                roomContainer.setStyle(
                        "-fx-background-color: #f9fafb; -fx-background-radius: 6; -fx-border-color: #e5e7eb; -fx-border-radius: 6;");

                java.util.List<model.entities.DichVuSuDung> dvList = dvsdDAO.findByMaCTDP(maCTDP);
                double roomTienDV = 0;
                for (model.entities.DichVuSuDung dv : dvList) {
                    roomTienDV += dv.getThanhTien();
                    dynamicTienDV += dv.getThanhTien();
                }

                HBox r = new HBox();
                r.setPadding(new Insets(8, 12, 8, 12));
                r.setStyle("-fx-cursor: hand;");

                String textRoom = "🛏 " + maPhong + " - " + tenPhong + " (" + loaiPhong + ")"
                        + "  •  " + (int) sodem + " đêm  ▼";

                Label lblP = new Label(textRoom);
                lblP.setFont(Font.font("Segoe UI", 13));
                lblP.setWrapText(true);
                HBox.setHgrow(lblP, Priority.ALWAYS);

                double tongPhong = thanhTien;
                Label lblAmt = new Label(String.format("%,.0f đ", tongPhong));
                lblAmt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                lblAmt.setTextFill(Color.web(C_BLUE));
                r.getChildren().addAll(lblP, lblAmt);

                VBox dvBox = new VBox(6);
                dvBox.setPadding(new Insets(0, 12, 10, 32));
                dvBox.setManaged(false);
                dvBox.setVisible(false);

                if (dvList.isEmpty() && roomPhuThu == 0 && roomPhuPhiTraMuon == 0) {
                    Label lblEmpty = new Label("— Không có phụ thu hay dịch vụ");
                    lblEmpty.setTextFill(Color.web(C_TEXT_GRAY));
                    lblEmpty.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 12));
                    dvBox.getChildren().add(lblEmpty);
                } else {
                    // Hiển thị phụ thu per-room
                    if (roomPhuThu > 0) {
                        Label lblPT = new Label("   💰 Phí phụ thu: " + String.format("%,.0f đ", roomPhuThu));
                        lblPT.setTextFill(Color.web("#d97706"));
                        lblPT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                        dvBox.getChildren().add(lblPT);
                    }
                    if (roomPhuPhiTraMuon > 0) {
                        Label lblLF = new Label(
                                "   ⏰ Phụ phí trả muộn: " + String.format("%,.0f đ", roomPhuPhiTraMuon));
                        lblLF.setTextFill(Color.web("#ef4444"));
                        lblLF.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                        dvBox.getChildren().add(lblLF);
                    }

                    if (!dvList.isEmpty()) {
                        Label lblPhuThuDV = new Label("Tiền dịch vụ: " + String.format("%,.0f đ", roomTienDV));
                        lblPhuThuDV.setTextFill(Color.web(C_TEXT_DARK));
                        lblPhuThuDV.setFont(Font.font("Segoe UI", 12));
                        dvBox.getChildren().add(lblPhuThuDV);

                        for (model.entities.DichVuSuDung dv : dvList) {
                            HBox dvRow = new HBox();
                            Label dvName = new Label(
                                    "   🍹 " + dv.getDichVu().getTenDV() + " (x" + dv.getSoLuong() + ")");
                            dvName.setTextFill(Color.web(C_TEXT_GRAY));
                            dvName.setFont(Font.font("Segoe UI", 12));
                            dvName.setWrapText(true);
                            HBox.setHgrow(dvName, Priority.ALWAYS);
                            Label dvPrice = new Label(String.format("%,.0f đ", dv.getThanhTien()));
                            dvPrice.setTextFill(Color.web(C_TEXT_DARK));
                            dvPrice.setFont(Font.font("Segoe UI", 12));
                            dvRow.getChildren().addAll(dvName, dvPrice);
                            dvBox.getChildren().add(dvRow);
                        }
                    }
                }

                r.setOnMouseClicked(e -> {
                    boolean isVis = dvBox.isVisible();
                    dvBox.setVisible(!isVis);
                    dvBox.setManaged(!isVis);
                    lblP.setText(lblP.getText().replace(isVis ? "▲" : "▼", isVis ? "▼" : "▲"));
                });

                roomContainer.getChildren().addAll(r, dvBox);
                phongBox.getChildren().add(roomContainer);
            }
        }

        ScrollPane scrollPhong = new ScrollPane(phongBox);
        scrollPhong.setFitToWidth(true);
        scrollPhong.setMinHeight(80);
        scrollPhong.setPrefHeight(120);
        scrollPhong.setMaxHeight(150);
        scrollPhong
                .setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: transparent;");
        scrollPhong.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPhong, Priority.ALWAYS);

        Separator sep = new Separator();
        VBox sumBox = new VBox(10);
        sumBox.setPadding(new Insets(8, 0, 0, 0));

        double tienDV = dynamicTienDV > 0 ? dynamicTienDV : hd.getTienDV();
        double tienCoc = tongCoc > 0 ? tongCoc : hd.getTienCoc();

        double phuPhiTraMuon = hd.getPhuPhiTraMuon();
        double phuThu = hd.getPhuThu();
        double vatRate = hd.getThueVAT() > 0 ? hd.getThueVAT() : 0.1;
        // VAT tính trên tổng tiền phòng + DV + phụ thu + phụ phí, sau đó trừ cọc
        double vatAmount = (tongTienPhong + tienDV) * vatRate;
        double tongTT = Math.max(0, (tongTienPhong + tienDV + phuPhiTraMuon + phuThu + vatAmount) - tienCoc);

        sumBox.getChildren().addAll(
                makeSumRow("Tiền phòng:", String.format("%,.0f đ", tongTienPhong), Color.web(C_TEXT_DARK)),
                makeSumRow("Tiền dịch vụ:", String.format("%,.0f đ", tienDV), Color.web(C_TEXT_DARK)));

        sumBox.getChildren()
                .add(makeSumRow("Phụ phí trả muộn:", String.format("%,.0f đ", phuPhiTraMuon), Color.web("#ef4444")));
        sumBox.getChildren().add(makeSumRow("Phí phụ thu:", String.format("%,.0f đ", phuThu), Color.web(C_TEXT_DARK)));

        sumBox.getChildren().addAll(
                makeSumRow("Tiền cọc (đã khấu trừ):", String.format("- %,.0f đ", tienCoc), Color.web(C_GREEN)),
                makeSumRow(String.format("Thuế VAT (%.0f%%):", vatRate * 100), String.format("%,.0f đ", vatAmount),
                        Color.web(C_TEXT_DARK)));

        double paidTotal = Math.max(0, tongTT + tienCoc);
        HBox totalRow = new HBox();
        Label lblTotalText = new Label("⭐ TỔNG ĐÃ THANH TOÁN:");
        lblTotalText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        HBox.setHgrow(lblTotalText, Priority.ALWAYS);
        Label lblTotalAmt = new Label(String.format("%,.0f đ", paidTotal));
        lblTotalAmt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTotalAmt.setTextFill(Color.web(C_BLUE));
        totalRow.getChildren().addAll(lblTotalText, lblTotalAmt);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        sumBox.getChildren().addAll(sep, totalRow);

        Button btnExport = new Button("📄  Xuất hóa đơn (HTML)");
        btnExport.setPrefHeight(38);
        btnExport.setStyle("-fx-background-color: " + C_BLUE
                + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 20;");
        final double finalTongTienPhong = tongTienPhong;
        btnExport.setOnAction(e -> {
            hd.setTienPhong(finalTongTienPhong);
            hd.setTienDV(tienDV);
            hd.setTienCoc(tienCoc);
            hd.setTongTien(tongTT);
            String path = InvoiceExporter.exportToHTML(hd, dsPhong);
            if (path != null) {
                // Tự động mở file HTML trong trình duyệt mặc định
                try {
                    java.awt.Desktop.getDesktop().browse(new java.io.File(path).toURI());
                } catch (Exception ex) {
                    // Nếu không mở được trình duyệt, hiện thông báo đường dẫn
                    Alert ok = new Alert(Alert.AlertType.INFORMATION, "Đã xuất hóa đơn tại:\n" + path);
                    ok.setHeaderText("Xuất thành công!");
                    ok.showAndWait();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Lỗi khi xuất hóa đơn!").showAndWait();
            }
        });

        Button btnClose = new Button("✕  Đóng");
        btnClose.setPrefHeight(38);
        btnClose.setStyle("-fx-background-color: " + C_NAVY
                + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 20;");
        btnClose.setOnAction(e -> detailStage.close());

        HBox btnRow = new HBox(12, btnExport, btnClose);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // === Layout: BorderPane ===
        // TOP: Tiêu đề + thông tin khách hàng + header phòng
        VBox topSection = new VBox(12, lblTitle, khachBox, lblPhongHeader);
        topSection.setPadding(new Insets(0, 0, 8, 0));
        root.setTop(topSection);

        // CENTER: Danh sách phòng (cuộn được)
        root.setCenter(scrollPhong);
        BorderPane.setMargin(scrollPhong, new Insets(4, 0, 4, 0));

        // BOTTOM: Tổng tiền + nút bấm (cố định)
        VBox bottomSection = new VBox(12, sumBox, btnRow);
        bottomSection.setPadding(new Insets(8, 0, 0, 0));
        root.setBottom(bottomSection);

        // Constrain popup size - dùng visualBounds để trừ taskbar
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double maxH = screenBounds.getHeight() * 0.85;

        Scene scene = new Scene(root, 580, Math.min(maxH, 720));
        detailStage.setScene(scene);
        detailStage.setMinWidth(540);
        detailStage.setMinHeight(500);
        detailStage.setMaxHeight(screenBounds.getHeight());
        detailStage.setResizable(true);
        detailStage.sizeToScene();
        detailStage.showAndWait();
    }

    private static HBox makeBillInfoRow(String label, String value) {
        HBox hb = new HBox(8);
        hb.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setMinWidth(160);
        l.setTextFill(Color.web(C_TEXT_GRAY));
        l.setFont(Font.font("Segoe UI", 13));
        Label v = new Label(value);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        v.setTextFill(Color.web(C_TEXT_DARK));
        hb.getChildren().addAll(l, v);
        return hb;
    }

    private static HBox makeSumRow(String label, String value, Color valColor) {
        HBox hb = new HBox();
        hb.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setTextFill(Color.web(C_TEXT_GRAY));
        l.setFont(Font.font("Segoe UI", 14));
        HBox.setHgrow(l, Priority.ALWAYS);
        Label v = new Label(value);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        v.setTextFill(valColor);
        hb.getChildren().addAll(l, v);
        return hb;
    }

    private void loadData() {
        // Show loading placeholder
        table.setPlaceholder(new Label("⏳ Đang tải dữ liệu hóa đơn..."));

        javafx.concurrent.Task<List<HoaDon>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<HoaDon> call() {
                List<HoaDon> list = dao.getAllWithKhachHang();
                dao.DichVuSuDungDAO dvsdDAO = new dao.DichVuSuDungDAO();

                for (HoaDon hd : list) {
                    if ("DA_THANH_TOAN_COC".equals(hd.getTrangThaiThanhToan()) &&
                            hd.getDatPhong() != null &&
                            "DA_CHECKIN".equals(hd.getDatPhong().getTrangThai())) {
                        hd.setTrangThaiThanhToan("CHUA_THANH_TOAN");
                    }

                    double currentSumPhong = dao.getTongTienPhongCurrent(hd.getMaHD());
                    double tongCocHD = new dao.ChiTietHoaDonDAO().getTongCocByMaHD(hd.getMaHD());
                    if (currentSumPhong > 0 || tongCocHD > 0)
                        hd.setTienCoc(tongCocHD);

                    java.util.List<model.entities.DichVuSuDung> listDV = dvsdDAO.findByMaHD(hd.getMaHD());
                    double totalTienDV = listDV.stream().mapToDouble(model.entities.DichVuSuDung::getThanhTien).sum();

                    double phuPhiTraMuon = hd.getPhuPhiTraMuon();
                    double phuThu = hd.getPhuThu();
                    double vatAmount = (currentSumPhong + totalTienDV + phuPhiTraMuon + phuThu) * hd.getThueVAT();
                    double tcp = currentSumPhong + totalTienDV + phuPhiTraMuon + phuThu + vatAmount;
                    double ttt = Math.max(0, tcp - hd.getTienCoc());

                    hd.setTienPhong(currentSumPhong);
                    hd.setTienDV(totalTienDV);
                    hd.setTongCP(tcp);
                    hd.setTongTien(ttt);
                    dao.tinhDoanhThu(hd);
                }

                list.sort((a, b) -> {
                    if (a.getNgayTaoHD() == null && b.getNgayTaoHD() == null)
                        return 0;
                    if (a.getNgayTaoHD() == null)
                        return 1;
                    if (b.getNgayTaoHD() == null)
                        return -1;
                    return b.getNgayTaoHD().compareTo(a.getNgayTaoHD());
                });

                return list;
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            table.setPlaceholder(new Label("Không có dữ liệu"));
            applyFilter(txtSearch != null ? txtSearch.getText() : "");
        });

        task.setOnFailed(e -> {
            table.setPlaceholder(new Label("❌ Lỗi tải dữ liệu hóa đơn"));
            task.getException().printStackTrace();
        });

        new Thread(task, "HoaDon-Loader").start();
    }

    private void applyFilter(String kw) {
        if (filteredData == null)
            return;
        String filter = kw == null ? "" : kw.toLowerCase().trim();

        java.time.LocalDate startDate = null, endDate = null;
        String groupBy = cbGroupBy.getValue();

        if ("Tháng".equals(groupBy)) {
            int m = cbMonth.getValue();
            int y = cbYear.getValue();
            startDate = java.time.LocalDate.of(y, m, 1);
            endDate = startDate.plusMonths(1).minusDays(1);
        } else if ("Quý".equals(groupBy)) {
            int q = cbQuarter.getValue();
            int y = cbYear.getValue();
            startDate = java.time.LocalDate.of(y, (q - 1) * 3 + 1, 1);
            endDate = startDate.plusMonths(3).minusDays(1);
        } else if ("Năm".equals(groupBy)) {
            int y = cbYear.getValue();
            startDate = java.time.LocalDate.of(y, 1, 1);
            endDate = java.time.LocalDate.of(y, 12, 31);
        } else {
            startDate = dpFrom.getValue();
            endDate = dpTo.getValue();
        }

        final java.time.LocalDate fStart = startDate;
        final java.time.LocalDate fEnd = endDate;

        filteredData.setPredicate(hd -> {
            // Filter by status (multi-select)
            if (!selectedTrangThais.isEmpty()) {
                String trangThai = hd.getTrangThaiThanhToan();
                if (trangThai == null || !selectedTrangThais.contains(trangThai)) {
                    return false;
                }
            }
            if (hd.getNgayTaoHD() != null) {
                java.time.LocalDate invoiceDate = hd.getNgayTaoHD().toLocalDate();
                if (fStart != null && invoiceDate.isBefore(fStart))
                    return false;
                if (fEnd != null && invoiceDate.isAfter(fEnd))
                    return false;
            } else {
                if (fStart != null || fEnd != null)
                    return false;
            }

            if (filter.isEmpty())
                return true;

            if (hd.getMaHD() != null && hd.getMaHD().toLowerCase().contains(filter))
                return true;
            if (hd.getDatPhong() != null && hd.getDatPhong().getMaDat() != null
                    && hd.getDatPhong().getMaDat().toLowerCase().contains(filter))
                return true;
            if (hd.getNhanVien() != null && hd.getNhanVien().getHoTen() != null
                    && hd.getNhanVien().getHoTen().toLowerCase().contains(filter))
                return true;
            if (hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null
                    && hd.getDatPhong().getKhachHang().getTenKH() != null
                    && hd.getDatPhong().getKhachHang().getTenKH().toLowerCase().contains(filter))
                return true;

            return false;
        });

        double tongDoanhThu = filteredData.stream()
                .mapToDouble(RevenueCalculator::calculateActualRevenue)
                .sum();
        lblTongDoanhThu.setText(String.format("%,.0f đ", tongDoanhThu));
        lblSoHoaDon.setText(String.valueOf(filteredData.size()));
    }

    private VBox createStatCard(String icon, String title, Label valueLbl, String accentHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setMinWidth(240);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + C_BORDER
                + "; -fx-background-radius: 10; -fx-border-radius: 10;");
        card.setEffect(new DropShadow(8, 0, 2, Color.web("#00000008")));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane badge = new StackPane();
        Rectangle badgeBg = new Rectangle(40, 40);
        badgeBg.setArcWidth(8);
        badgeBg.setArcHeight(8);
        Color accent = Color.web(accentHex);
        badgeBg.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.1));
        badge.getChildren().addAll(badgeBg, new Label(icon));

        VBox text = new VBox(2);
        Label lblT = new Label(title);
        lblT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblT.setTextFill(Color.web(C_TEXT_GRAY));

        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        valueLbl.setTextFill(accent);

        text.getChildren().addAll(lblT, valueLbl);
        topRow.getChildren().addAll(badge, text);
        card.getChildren().add(topRow);
        return card;
    }
}