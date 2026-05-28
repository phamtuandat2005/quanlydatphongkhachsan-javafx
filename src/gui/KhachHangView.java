package gui;

import dao.KhachHangDAO;
import model.entities.KhachHang;
import model.utils.DimOverlay;
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
import javafx.stage.Modality;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * KhachHangView – JavaFX
 *
 * Thay thế KhachHangPanel (Swing) cũ.
 * Hiển thị: thẻ thống kê + thanh tìm kiếm + bảng danh sách khách hàng.
 * Thêm / Sửa → ủy quyền cho {@link ThemSuaKhachHangDialog}.
 *
 * Màu sắc kế thừa từ TrangChuView / MainFrameView.
 */
public class KhachHangView extends BorderPane {

    /* ── Bảng màu (đồng bộ TrangChuView & MainFrameView) ───────────── */
    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final String C_RED = "#dc2626";
    private static final String C_GOLD = "#d97706";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /* ── DAO & dữ liệu ─────────────────────────────────────────────── */
    private final KhachHangDAO dao = new KhachHangDAO();
    private ObservableList<KhachHang> masterData = FXCollections.observableArrayList();
    private FilteredList<KhachHang> filteredData;

    /* ── Controls ───────────────────────────────────────────────────── */
    private TableView<KhachHang> table;
    private TextField txtSearch;
    private Label lblTongKH;
    private Label lblSinhNhat;

    /* ──PHÂN QUYỀN ────────────────────────────────────────────── */
    private final boolean isAdmin;

    /* ──CONSTRUCTOR ────────────────────────────────────────────── */
    public KhachHangView(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildTableCard());

        loadData();
    }

    public KhachHangView() {
        this(true);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * HEADER: Tiêu đề + Nút thêm + Thẻ thống kê + Thanh tìm kiếm
     * ══════════════════════════════════════════════════════════════════
     */
    private VBox buildHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(0, 0, 12, 0));

        /* ── Dòng 1: Tiêu đề + Nút thêm ─────────────────────────── */
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label lblTitle = new Label("Quản lý khách hàng");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));
        Label lblSubtitle = new Label("Quản lý thông tin và hồ sơ khách hàng");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        titleRow.getChildren().addAll(titleBox);

        /* ── Dòng 2: Thẻ thống kê ────────────────────────────────── */
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        lblTongKH = new Label("0");
        lblSinhNhat = new Label("0");

        VBox c1 = createStatCard("👥", "TỔNG KHÁCH HÀNG", lblTongKH, C_NAVY);
        VBox c2 = createStatCard("🎂", "SINH NHẬT HÔM NAY", lblSinhNhat, C_GOLD);

        // Bấm vào thẻ sinh nhật → mở dialog danh sách
        c2.setCursor(Cursor.HAND);
        c2.setOnMouseClicked(e -> showBirthdayDialog());

        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        statsRow.getChildren().addAll(c1, c2);

        /* ── Dòng 3: Thanh tìm kiếm ──────────────────────────────── */
        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm kiếm theo tên, SĐT, mã KH...");
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

        /* ── TableView ────────────────────────────────────────────── */

        /* ── TableView ────────────────────────────────────────────── */
        table = new TableView<>();
        table.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-table-cell-border-color: " + C_BORDER + ";");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Không có dữ liệu"));

        // ── Định nghĩa các cột ──────────────────────────────────────

        TableColumn<KhachHang, Void> colSTT = new TableColumn<>("STT");
        colSTT.setMinWidth(60);
        colSTT.setMaxWidth(60);
        colSTT.setResizable(false);
        colSTT.setStyle("-fx-alignment: CENTER;");
        colSTT.setCellFactory(col -> new TableCell<>() {
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

        TableColumn<KhachHang, String> colMa = new TableColumn<>("Mã KH");
        colMa.setMinWidth(100);
        colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        colMa.setCellValueFactory(p -> new SimpleStringProperty(nvl(p.getValue().getMaKH())));

        TableColumn<KhachHang, String> colTen = new TableColumn<>("Họ và tên");
        colTen.setMinWidth(220);
        colTen.setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 20;");
        colTen.setCellValueFactory(p -> {
            String ten = nvl(p.getValue().getTenKH());
            return new SimpleStringProperty(toTitleCaseLocal(ten));
        });

        TableColumn<KhachHang, String> colCCCD = new TableColumn<>("Số CCCD");
        colCCCD.setMinWidth(140);
        colCCCD.setStyle("-fx-alignment: CENTER;");
        colCCCD.setCellValueFactory(p -> new SimpleStringProperty(nvl(p.getValue().getSoCCCD())));

        TableColumn<KhachHang, String> colSDT = new TableColumn<>("Số điện thoại");
        colSDT.setMinWidth(130);
        colSDT.setStyle("-fx-alignment: CENTER;");
        colSDT.setCellValueFactory(p -> new SimpleStringProperty(nvl(p.getValue().getSoDT())));

        TableColumn<KhachHang, String> colNS = new TableColumn<>("Ngày sinh");
        colNS.setMinWidth(130);
        colNS.setStyle("-fx-alignment: CENTER;");
        colNS.setCellValueFactory(p -> {
            LocalDate ns = p.getValue().getNgaySinh();
            return new SimpleStringProperty(ns != null ? ns.format(DATE_FMT) : "");
        });

        // ── Custom Header Labels (Bold & Aligned) ──────────────────
        for (TableColumn<KhachHang, ?> c : List.of(colSTT, colMa, colTen, colCCCD, colSDT, colNS)) {
            Label lblHeader = new Label(c.getText());
            lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: " + C_TEXT_DARK + ";");
            if (c == colTen) {
                lblHeader.setPadding(new Insets(0, 0, 0, 15));
                c.setGraphic(new StackPane(lblHeader));
                ((StackPane) c.getGraphic()).setAlignment(Pos.CENTER_LEFT);
            } else {
                c.setGraphic(new StackPane(lblHeader));
                ((StackPane) c.getGraphic()).setAlignment(Pos.CENTER);
            }
            c.setText(""); // Xóa text gốc để hiển thị Graphic
        }

        // Ràng buộc chiều rộng các cột để tự giãn đầy bảng và không cho kéo
        colMa.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colTen.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.30));
        colCCCD.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.20));
        colSDT.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.17));
        colNS.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.18));

        // Khóa cứng tất cả cột – không cho kéo thả
        for (TableColumn<KhachHang, ?> c : List.of(colSTT, colMa, colTen, colCCCD, colSDT, colNS)) {
            c.setReorderable(false);
            c.setSortable(false);
            c.setResizable(false);
        }

        table.getColumns().addAll(colSTT, colMa, colTen, colCCCD, colSDT, colNS);

        // ── Chuột phải → ContextMenu (Cập nhật / Xóa) ──────────────
        ContextMenu ctxMenu = new ContextMenu();

        MenuItem miEdit = new MenuItem("✏  Cập nhật thông tin");
        miEdit.setStyle("-fx-font-size: 13px;");
        miEdit.setOnAction(e -> {
            KhachHang kh = table.getSelectionModel().getSelectedItem();
            if (kh != null)
                openDialog(kh);
        });

        ctxMenu.getItems().add(miEdit);

        if (isAdmin) {
            MenuItem miDelete = new MenuItem("🗑  Xóa khách hàng");
            miDelete.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
            miDelete.setOnAction(e -> {
                KhachHang kh = table.getSelectionModel().getSelectedItem();
                if (kh != null)
                    handleDelete(kh);
            });
            ctxMenu.getItems().addAll(new SeparatorMenuItem(), miDelete);
        }

        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY
                    && table.getSelectionModel().getSelectedItem() != null) {
                ctxMenu.show(table, e.getScreenX(), e.getScreenY());
            } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                KhachHang kh = table.getSelectionModel().getSelectedItem();
                if (kh != null)
                    openDialog(kh);
            } else {
                ctxMenu.hide();
            }
        });

        // ── Tô màu hàng sinh nhật ───────────────────────────────────
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(KhachHang kh, boolean empty) {
                super.updateItem(kh, empty);
                if (empty || kh == null) {
                    setStyle("");
                } else if (kh.isBirthdayToday()) {
                    setStyle("-fx-background-color: #fefce8;");
                } else {
                    setStyle("");
                }
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().add(table);
        return card;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * LOAD & FILTER DỮ LIỆU
     * ══════════════════════════════════════════════════════════════════
     */
    public void loadData() {
        try {
            List<KhachHang> ds = dao.getAll();
            masterData.setAll(ds);

            filteredData = new FilteredList<>(masterData, p -> true);
            table.setItems(filteredData);

            long bdToday = ds.stream().filter(KhachHang::isBirthdayToday).count();
            lblTongKH.setText(String.valueOf(ds.size()));
            lblSinhNhat.setText(String.valueOf(bdToday));
            lblSinhNhat.setTextFill(bdToday > 0
                    ? Color.web(C_GOLD)
                    : Color.web(C_TEXT_DARK));
        } catch (Exception ex) {
            lblTongKH.setText("—");
            lblSinhNhat.setText("—");
        }
    }

    private void applyFilter(String keyword) {
        if (filteredData == null)
            return;
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        filteredData.setPredicate(kh -> {
            if (kw.isEmpty())
                return true;
            return nvl(kh.getTenKH()).toLowerCase().contains(kw)
                    || nvl(kh.getSoDT()).contains(kw)
                    || nvl(kh.getMaKH()).toLowerCase().contains(kw)
                    || nvl(kh.getSoCCCD()).contains(kw);
        });
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * MỞ DIALOG THÊM / SỬA
     * ══════════════════════════════════════════════════════════════════
     */
    private void openDialog(KhachHang kh) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        new ThemSuaKhachHangDialog(owner, kh, dao, this::loadData).showDialog();
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * XÓA KHÁCH HÀNG
     * ══════════════════════════════════════════════════════════════════
     */
    private void handleDelete(KhachHang kh) {
        if (dao.hasActiveBooking(kh.getMaKH())) {
            showAlert(Alert.AlertType.WARNING, "Không thể xóa",
                    "Không thể xóa khách hàng đang có đơn đặt phòng chưa hoàn tất (Chờ xác nhận, Đã xác nhận hoặc Đang ở)!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa khách hàng " + kh.getMaKH() + "?");
        confirm.setContentText("Hành động này không thể hoàn tác.\n"
                + "Họ tên: " + nvl(kh.getTenKH()));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (dao.delete(kh.getMaKH())) {
                showAlert(Alert.AlertType.INFORMATION, "Đã xóa",
                        "Khách hàng " + kh.getMaKH() + " đã được xóa.");
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Xóa thất bại",
                        "Đã xảy ra lỗi khi xóa khách hàng. Có thể do ràng buộc dữ liệu.");
            }
        }
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * DIALOG SINH NHẬT HÔM NAY (+ phòng đang ở)
     * Dữ liệu từ dao.getBirthdayTodayWithRoom()
     * → List<String[]>: [maKH, hoTen, soDienThoai, ngaySinh, dsPhong]
     * ══════════════════════════════════════════════════════════════════
     */
    @SuppressWarnings("unchecked")
    private void showBirthdayDialog() {
        List<String[]> list = dao.getBirthdayTodayWithRoom();

        Window owner = getScene() != null ? getScene().getWindow() : null;

        // ── Dim overlay ──────────────────────────────────────────────
        Region dimOverlay = DimOverlay.show(owner);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("🎂  Khách có sinh nhật hôm nay");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null)
            dialog.initOwner(owner);

        /* ── Header ───────────────────────────────────────────────── */
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(22, 28, 16, 28));
        headerBox.setStyle("-fx-background-color: " + C_NAVY + ";");

        Label h1 = new Label("🎂  Danh sách khách có sinh nhật hôm nay");
        h1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        h1.setTextFill(Color.WHITE);

        LocalDate today = LocalDate.now();
        Label h2 = new Label(String.format("Ngày %02d / %02d / %04d  –  %d khách",
                today.getDayOfMonth(), today.getMonthValue(), today.getYear(), list.size()));
        h2.setFont(Font.font("Segoe UI", 13));
        h2.setTextFill(Color.web("#93c5fd"));

        headerBox.getChildren().addAll(h1, h2);

        /* ── Bảng ─────────────────────────────────────────────────── */
        TableView<String[]> bdTable = new TableView<>();
        bdTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bdTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent;");

        String[] colNames = { "STT", "Mã KH", "Họ và tên", "Số điện thoại",
                "Ngày sinh", "Phòng đang ở" };

        for (int i = 0; i < colNames.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(colNames[i]);
            col.setReorderable(false);
            col.setSortable(false);
            col.setResizable(false);
            col.setStyle("-fx-alignment: CENTER;");

            if (idx == 0) {
                // STT
                col.setCellValueFactory(p -> {
                    int row = bdTable.getItems().indexOf(p.getValue()) + 1;
                    return new SimpleStringProperty(String.valueOf(row));
                });
                col.setMaxWidth(55);
            } else {
                // Dữ liệu: index 0→maKH, 1→hoTen, 2→sdt, 3→ngaySinh, 4→dsPhong
                final int dataIdx = idx - 1;
                col.setCellValueFactory(p -> {
                    String[] row = p.getValue();
                    String val = (dataIdx < row.length) ? row[dataIdx] : "";
                    return new SimpleStringProperty(val != null ? val : "");
                });
                // Cột "Họ và tên" canh trái
                if (idx == 2)
                    col.setStyle("-fx-alignment: CENTER-LEFT;");
                // Cột "Phòng đang ở" hiện "Chưa đặt phòng" nếu rỗng
                if (idx == 5) {
                    col.setCellFactory(c -> new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setText(null);
                                setStyle("-fx-alignment: CENTER;");
                            } else if (item == null || item.isBlank()) {
                                setText("Chưa nhận phòng");
                                setStyle("-fx-alignment: CENTER; -fx-text-fill: #9ca3af;");
                            } else {
                                setText(item);
                                setStyle(
                                        "-fx-alignment: CENTER; -fx-text-fill: " + C_BLUE + "; -fx-font-weight: bold;");
                            }
                        }
                    });
                }
            }
            bdTable.getColumns().add(col);
        }
        
        var cols = bdTable.getColumns();
        cols.get(1).prefWidthProperty().bind(bdTable.widthProperty().subtract(65).multiply(0.15));
        cols.get(2).prefWidthProperty().bind(bdTable.widthProperty().subtract(65).multiply(0.25));
        cols.get(3).prefWidthProperty().bind(bdTable.widthProperty().subtract(65).multiply(0.20));
        cols.get(4).prefWidthProperty().bind(bdTable.widthProperty().subtract(65).multiply(0.20));
        cols.get(5).prefWidthProperty().bind(bdTable.widthProperty().subtract(65).multiply(0.20));

        ObservableList<String[]> bdData = FXCollections.observableArrayList();
        if (!list.isEmpty())
            bdData.addAll(list);
        bdTable.setItems(bdData);

        if (list.isEmpty()) {
            bdTable.setPlaceholder(new Label("Không có khách nào sinh nhật hôm nay 🎂"));
        }

        /* ── Layout ───────────────────────────────────────────────── */
        VBox content = new VBox(0, headerBox, bdTable);
        content.setPrefSize(750, 420);
        VBox.setVgrow(bdTable, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-padding: 0;");
        dialog.getDialogPane().getButtonTypes().add(
                new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE));

        dialog.getDialogPane().setMinWidth(750);
        dialog.getDialogPane().setMinHeight(480);
        dialog.showAndWait();

        // ── Gỡ dim overlay ──────────────────────────────────────────
        DimOverlay.hide(owner, dimOverlay);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * THẺ THỐNG KÊ (giống TrangChuView)
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

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static String toTitleCaseLocal(String s) {
        if (s == null || s.isBlank())
            return s;
        String[] words = s.trim().toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!sb.isEmpty())
                sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }
}