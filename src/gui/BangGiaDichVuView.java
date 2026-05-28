package gui;

import dao.BangGiaDichVuDAO;
import model.entities.BangGiaDichVu;
import model.utils.BadgeUtils;
import model.entities.BangGiaDichVu_ChiTiet;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * BangGiaDichVuView – JavaFX
 * Thay thế BangGiaDichVuView (Swing JPanel) cũ.
 * Giữ nguyên toàn bộ logic nghiệp vụ, chỉ đổi giao diện sang JavaFX
 * đồng nhất với KhachHangView, DichVuView, NhanVienView...
 */
public class BangGiaDichVuView extends BorderPane {

    /* ── Bảng màu (đồng bộ KhachHangView & MainFrameView) ────────── */
    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final String C_RED = "#dc2626";
    private static final String C_GREEN = "#16a34a";

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    /* ── DAO & dữ liệu ────────────────────────────────────────────── */
    private final BangGiaDichVuDAO dao = new BangGiaDichVuDAO();
    private ObservableList<Object[]> masterData = FXCollections.observableArrayList();
    private FilteredList<Object[]> filteredData;

    /* ── Controls ──────────────────────────────────────────────────── */
    private TableView<Object[]> table;
    private TextField txtSearch;
    private Label lblTong, lblDangAD, lblNgungAD;

    /* ── Constructor ───────────────────────────────────────────────── */
    public BangGiaDichVuView() {
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildTableCard());

        loadDataFromDatabase();
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * HEADER: Tiêu đề + Nút thêm + Thẻ thống kê + Thanh tìm kiếm
     * ══════════════════════════════════════════════════════════════════
     */
    private VBox buildHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(0, 0, 12, 0));

        /* ── Dòng 1: Tiêu đề + Nút thêm ───────────────────────── */
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label lblTitle = new Label("Quản lý bảng giá dịch vụ");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));
        Label lblSubtitle = new Label("Thiết lập và quản lý các đợt giá dịch vụ khách sạn");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        Button btnAdd = new Button("＋  Thêm bảng giá");
        btnAdd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnAdd.setPrefHeight(40);
        btnAdd.setCursor(Cursor.HAND);
        styleButton(btnAdd, C_BLUE, "white", C_BLUE_HOVER);
        btnAdd.setOnAction(e -> {
            Window owner = getScene() != null ? getScene().getWindow() : null;
            new ThemSuaBangGiaDialog(owner, null, null, this::loadDataFromDatabase).show();
        });

        titleRow.getChildren().addAll(titleBox, btnAdd);

        /* ── Dòng 2: Thẻ thống kê ────────────────────────────── */
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        lblTong = new Label("0");
        lblDangAD = new Label("0");
        lblNgungAD = new Label("0");

        VBox c1 = createStatCard("📋", "TỔNG BẢNG GIÁ", lblTong, C_NAVY);
        VBox c2 = createStatCard("✅", "ĐANG ÁP DỤNG", lblDangAD, C_GREEN);
        VBox c3 = createStatCard("⏸", "NGƯNG ÁP DỤNG", lblNgungAD, C_RED);

        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        HBox.setHgrow(c3, Priority.ALWAYS);
        statsRow.getChildren().addAll(c1, c2, c3);

        /* ── Dòng 3: Thanh tìm kiếm ─────────────────────────── */
        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm kiếm theo mã, tên bảng giá...");
        txtSearch.setFont(Font.font("Segoe UI", 14));
        txtSearch.setPrefHeight(42);
        txtSearch.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 0 16 0 16;");
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter(n));

        header.getChildren().addAll(titleRow, statsRow, txtSearch);
        return header;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * BẢNG DỮ LIỆU
     * ══════════════════════════════════════════════════════════════════
     */
    @SuppressWarnings("unchecked")
    private VBox buildTableCard() {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;");
        card.setEffect(new DropShadow(8, 0, 2, Color.web("#00000010")));

        table = new TableView<>();
        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-table-cell-border-color: " + C_BORDER + ";");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Không có dữ liệu"));

        // ── Cột STT ──
        TableColumn<Object[], String> colSTT = new TableColumn<>("STT");
        colSTT.setMinWidth(50);
        colSTT.setMaxWidth(60);
        colSTT.setStyle("-fx-alignment: CENTER;");
        colSTT.setCellValueFactory(p -> {
            int idx = table.getItems().indexOf(p.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });

        // ── Cột Mã BG ──
        TableColumn<Object[], String> colMa = new TableColumn<>("Mã bảng giá");
        colMa.setMinWidth(120);
        colMa.setPrefWidth(120);
        colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        colMa.setCellValueFactory(p -> new SimpleStringProperty(str(p.getValue()[0])));

        // ── Cột Tên BG ──
        TableColumn<Object[], String> colTen = new TableColumn<>("Tên bảng giá");
        colTen.setMinWidth(200);
        colTen.setPrefWidth(400);
        colTen.setCellValueFactory(p -> new SimpleStringProperty(str(p.getValue()[1])));

        // ── Cột Ngày áp dụng ──
        TableColumn<Object[], String> colNgayAD = new TableColumn<>("Ngày áp dụng");
        colNgayAD.setMinWidth(120);
        colNgayAD.setPrefWidth(150);
        colNgayAD.setStyle("-fx-alignment: CENTER;");
        colNgayAD.setCellValueFactory(p -> new SimpleStringProperty(str(p.getValue()[2])));

        // ── Cột Ngày hết hạn ──
        TableColumn<Object[], String> colNgayHH = new TableColumn<>("Ngày hết hạn");
        colNgayHH.setMinWidth(120);
        colNgayHH.setPrefWidth(150);
        colNgayHH.setStyle("-fx-alignment: CENTER;");
        colNgayHH.setCellValueFactory(p -> new SimpleStringProperty(str(p.getValue()[3])));

        // ── Cột Trạng thái (tô màu) ──
        // ── Cột Trạng thái (Sử dụng ComboBox) ──
        TableColumn<Object[], String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setMinWidth(160);
        colTrangThai.setPrefWidth(180);
        colTrangThai.setStyle("-fx-alignment: CENTER;");
        colTrangThai.setCellValueFactory(p -> new SimpleStringProperty(str(p.getValue()[4])));

        colTrangThai.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // HBox Badge chứa Text + Arrow - Thu nhỏ khung cực bộ
                    String bgColor, textColor;
                    if (item.equals("Đang áp dụng")) {
                        bgColor = "#d1fae5";
                        textColor = "#065f46"; // Emerald
                    } else if (item.equals("Chờ áp dụng")) {
                        bgColor = "#dbeafe";
                        textColor = "#1e40af"; // Blue
                    } else {
                        bgColor = "#fee2e2";
                        textColor = "#b91c1c"; // Rose
                    }

                    // Sử dụng Utility đồng nhất
                    HBox badge = BadgeUtils.createStatusBadge(item, bgColor, textColor, true);

                    // Menu chọn trạng thái nhanh
                    ContextMenu statusMenu = new ContextMenu();
                    MenuItem m1 = new MenuItem("✅  Đang áp dụng");
                    m1.setOnAction(ev -> handleQuickStatusUpdate(str(getTableRow().getItem()[0]), "Đang áp dụng"));
                    MenuItem m2 = new MenuItem("⏸  Ngưng áp dụng");
                    m2.setOnAction(ev -> handleQuickStatusUpdate(str(getTableRow().getItem()[0]), "Ngưng áp dụng"));
                    statusMenu.getItems().addAll(m1, m2);

                    badge.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            statusMenu.show(badge, e.getScreenX(), e.getScreenY());
                        }
                    });

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colMa.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colTen.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.35));
        colNgayAD.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colNgayHH.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colTrangThai.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.20));

        // Khóa tất cả cột
        for (TableColumn<Object[], ?> c : List.of(colSTT, colMa, colTen, colNgayAD, colNgayHH, colTrangThai)) {
            c.setReorderable(false);
            c.setSortable(false);
            c.setResizable(false);
        }

        table.getColumns().addAll(colSTT, colMa, colTen, colNgayAD, colNgayHH, colTrangThai);

        // ── Context Menu (chuột phải) ──
        ContextMenu ctxMenu = new ContextMenu();

        MenuItem miEdit = new MenuItem("✏  Chỉnh sửa chi tiết");
        miEdit.setStyle("-fx-font-size: 13px;");
        miEdit.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row != null)
                handleEdit(row);
        });

        MenuItem miDelete = new MenuItem("🗑  Xóa bảng giá");
        miDelete.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
        miDelete.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row != null)
                handleDelete(row);
        });

        ctxMenu.getItems().addAll(miEdit, new SeparatorMenuItem(), miDelete);

        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY
                    && table.getSelectionModel().getSelectedItem() != null) {
                ctxMenu.show(table, e.getScreenX(), e.getScreenY());
            } else {
                ctxMenu.hide();
                // Double-click để sửa (giữ nguyên logic cũ)
                if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                    Object[] row = table.getSelectionModel().getSelectedItem();
                    if (row != null)
                        handleEdit(row);
                }
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().add(table);
        return card;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * LOAD DATA (giữ nguyên logic tính trạng thái từ Swing cũ)
     * ══════════════════════════════════════════════════════════════════
     */
    public void loadDataFromDatabase() {
        masterData.clear();
        List<BangGiaDichVu> list = dao.getAllBangGia();
        if (list == null)
            return;

        java.time.LocalDate today = java.time.LocalDate.now();
        int countDangAD = 0, countNgung = 0;

        for (BangGiaDichVu bg : list) {
            String trangThaiText;
            java.time.LocalDate ad = bg.getNgayApDung().toLocalDate();
            java.time.LocalDate hh = bg.getNgayHetHieuLuc().toLocalDate();

            if (bg.getTrangThai() == 1) {
                trangThaiText = "Ngưng áp dụng";
            } else {
                if (!today.isBefore(hh)) {
                    trangThaiText = "Ngưng áp dụng";
                } else if (today.isBefore(ad)) {
                    trangThaiText = "Chờ áp dụng";
                } else {
                    trangThaiText = "Đang áp dụng";
                }
            }

            if (trangThaiText.equals("Đang áp dụng"))
                countDangAD++;
            if (trangThaiText.equals("Ngưng áp dụng"))
                countNgung++;

            masterData.add(new Object[] {
                    bg.getMaBangGia(),
                    bg.getTenBangGia(),
                    SDF.format(bg.getNgayApDung()),
                    SDF.format(bg.getNgayHetHieuLuc()),
                    trangThaiText
            });
        }

        filteredData = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredData);

        // Cập nhật thẻ thống kê
        lblTong.setText(String.valueOf(list.size()));
        lblDangAD.setText(String.valueOf(countDangAD));
        lblNgungAD.setText(String.valueOf(countNgung));

        // Re-apply filter nếu có
        if (txtSearch != null && !txtSearch.getText().isEmpty()) {
            applyFilter(txtSearch.getText());
        }
    }

    private void applyFilter(String keyword) {
        if (filteredData == null)
            return;
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        filteredData.setPredicate(row -> {
            if (kw.isEmpty())
                return true;
            return str(row[0]).toLowerCase().contains(kw)
                    || str(row[1]).toLowerCase().contains(kw);
        });
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * HANDLE EDIT – giữ nguyên logic Swing cũ
     * ══════════════════════════════════════════════════════════════════
     */
    private void handleEdit(Object[] row) {
        String maBG = str(row[0]);
        String trangThai = str(row[4]);

        BangGiaDichVu bg = dao.getBangGiaByMa(maBG);
        List<BangGiaDichVu_ChiTiet> dsChiTiet = dao.getChiTietByMa(maBG);

        if (bg != null) {
            // Giữ nguyên logic: Bảng giá đã ngưng → hỏi trước khi sửa
            if (trangThai.equals("Ngưng áp dụng")) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Bảng giá này đã hết hạn. Bạn có muốn điều chỉnh lại ngày để tái sử dụng không?",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Thông báo");
                confirm.setHeaderText(null);
                var result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.YES)
                    return;
            }

            Window owner = getScene() != null ? getScene().getWindow() : null;
            new ThemSuaBangGiaDialog(owner, bg, dsChiTiet, this::loadDataFromDatabase).show();
        }
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * HANDLE DELETE – giữ nguyên logic Swing cũ
     * ══════════════════════════════════════════════════════════════════
     */
    private void handleDelete(Object[] row) {
        String maBG = str(row[0]);
        String trangThai = str(row[4]);

        // Giữ nguyên logic: CHẶN XÓA bảng giá đang áp dụng
        if (trangThai.equals("Đang áp dụng")) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("Cảnh báo");
            warn.setHeaderText("Không thể xóa bảng giá đang áp dụng!");
            warn.setContentText("Bạn chỉ có thể xóa bảng giá đã hết hạn hoặc đang chờ.");
            warn.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa vĩnh viễn bảng giá: " + maBG + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (dao.softDeleteBangGia(maBG)) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa bảng giá " + maBG);
                    loadDataFromDatabase();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi hệ thống: Không thể xóa dữ liệu!");
                }
            }
        });
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * THẺ THỐNG KÊ (giống KhachHangView)
     * ══════════════════════════════════════════════════════════════════
     */
    private VBox createStatCard(String icon, String title, Label valueLbl, String accentHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;");
        card.setEffect(new DropShadow(8, 0, 2, Color.web("#00000018")));

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblTitle.setTextFill(Color.web(C_TEXT_GRAY));

        valueLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        valueLbl.setTextFill(Color.web(C_TEXT_DARK));

        textBox.getChildren().addAll(lblTitle, valueLbl);

        StackPane badge = new StackPane();
        badge.setMinSize(46, 46);
        badge.setPrefSize(46, 46);
        Rectangle badgeBg = new Rectangle(46, 46);
        badgeBg.setArcWidth(10);
        badgeBg.setArcHeight(10);
        Color accent = Color.web(accentHex);
        badgeBg.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.12));
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font("Segoe UI Emoji", 22));
        badge.getChildren().addAll(badgeBg, iconLbl);

        topRow.getChildren().addAll(textBox, badge);

        Region bar = new Region();
        bar.setPrefHeight(3);
        bar.setStyle("-fx-background-color: " + accentHex + "; -fx-background-radius: 2;");

        card.getChildren().addAll(topRow, bar);
        return card;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * UTILITY
     * ══════════════════════════════════════════════════════════════════
     */
    private void styleButton(Button btn, String bg, String fg, String hoverBg) {
        String base = "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 20; -fx-cursor: hand;";
        String hover = base.replace(
                "-fx-background-color: " + bg,
                "-fx-background-color: " + hoverBg);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void showAlert(Alert.AlertType type, String header, String msg) {
        Alert a = new Alert(type);
        a.setTitle(type == Alert.AlertType.ERROR ? "Lỗi" : "Thông báo");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    /**
     * Cập nhật trạng thái nhanh từ ComboBox trên bảng
     */
    private void handleQuickStatusUpdate(String maBG, String tenTrangThaiMoi) {
        // Chuyển đổi tên hiển thị sang mã số (Trạng thái trong DB của bạn là int)
        // Giả sử: 0 = Đang áp dụng/Chờ, 1 = Ngưng áp dụng (Dựa theo logic code cũ của
        // bạn)
        int trangThaiInt = tenTrangThaiMoi.equals("Ngưng áp dụng") ? 1 : 0;

        // Lấy đối tượng từ DB để cập nhật
        BangGiaDichVu bg = dao.getBangGiaByMa(maBG);
        if (bg != null) {
            bg.setTrangThai(trangThaiInt);

            // Lưu vào DB
            if (dao.updateTrangThaiTheoTen(maBG, tenTrangThaiMoi)) {
                // Load lại để cập nhật Thẻ thống kê và màu sắc đồng bộ
                loadDataFromDatabase();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái cho " + maBG);
            }
        }
    }
}