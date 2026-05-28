package gui;

import dao.BangGiaDichVuDAO;
import dao.DichVuDAO;
import java.util.HashMap;
import java.util.Map;
import dao.LoaiDichVuDAO;
import model.entities.DichVu;
import model.entities.LoaiDichVu;
import model.utils.BadgeUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * QuanLyDichVuView – JavaFX
 * Quản lý danh mục tất cả dịch vụ (Thêm, Xóa, Sửa).
 * Đồng bộ phong cách thiết kế với hệ thống Lucia Hotel.
 */
public class QuanLyDichVuView extends BorderPane {

    /* ── Bảng màu chuẩn hệ thống ────────────────────────────────────── */
    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final String C_RED = "#dc2626";
    private static final String C_GREEN = "#16a34a";

    /* ── Dữ liệu & DAO ─────────────────────────────────────────────── */
    private final DichVuDAO dao = new DichVuDAO();
    private final BangGiaDichVuDAO bgDAO = new BangGiaDichVuDAO();
    private Map<String, Double> activePriceMap = new HashMap<>();
    private ObservableList<DichVu> masterData = FXCollections.observableArrayList();
    private FilteredList<DichVu> filteredData;

    /* ── UI Controls ────────────────────────────────────────────────── */
    private TableView<DichVu> table;
    private TextField txtSearch;
    // private ComboBox<String> cbCategory;
    private String selectedStatusFilter = "Đang phục vụ";
    private String selectedCategoryFilter = "Tất cả";
    private final boolean isAdmin;

    public QuanLyDichVuView(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildTableCard());

        loadData();
    }

    /* ══════════════════ HEADER ══════════════════ */
    private VBox buildHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(0, 0, 20, 0));

        // Dòng 1: Tiêu đề + Nút thêm
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label lblTitle = new Label("Danh mục dịch vụ");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));
        Label lblSubtitle = new Label("Quản lý danh sách dịch vụ và giá bán đang áp dụng");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        titleRow.getChildren().add(titleBox);

        if (isAdmin) {
            Button btnAdd = new Button("＋  Thêm dịch vụ mới");
            btnAdd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            btnAdd.setPrefHeight(40);
            btnAdd.setCursor(Cursor.HAND);
            styleButton(btnAdd, C_BLUE, "white", C_BLUE_HOVER);
            btnAdd.setOnAction(e -> openDialog(null));
            titleRow.getChildren().add(btnAdd);
        }

        // Dòng 2: Thanh tìm kiếm + Bộ lọc loại
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm kiếm theo mã dịch vụ hoặc tên dịch vụ...");
        txtSearch.setFont(Font.font("Segoe UI", 14));
        txtSearch.setPrefHeight(45);
        txtSearch.setMaxWidth(500);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 0 16 0 16;");
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        filterBar.getChildren().addAll(txtSearch);

        header.getChildren().addAll(titleRow, filterBar);
        return header;
    }

    /* ══════════════════ TABLE CARD ══════════════════ */
    private VBox buildTableCard() {
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;");
        card.setEffect(new DropShadow(10, 0, 4, Color.web("#00000008")));

        table = new TableView<>();
        table.setFixedCellSize(52);
        table.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-selection-bar: #f3f6ff;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Không có dữ liệu"));

        // 1. Cột STT
        TableColumn<DichVu, String> colSTT = new TableColumn<>("STT");
        colSTT.setPrefWidth(60);
        colSTT.setMaxWidth(60);
        colSTT.setStyle("-fx-alignment: CENTER;");
        colSTT.setCellValueFactory(
                p -> new SimpleStringProperty(String.valueOf(table.getItems().indexOf(p.getValue()) + 1)));

        // 2. Cột Mã DV
        TableColumn<DichVu, String> colMa = new TableColumn<>("Mã dịch vụ");
        colMa.setPrefWidth(120);
        colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        colMa.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMaDV()));

        // 3. Cột Tên DV
        TableColumn<DichVu, String> colTen = new TableColumn<>("Tên dịch vụ");
        colTen.setPrefWidth(250);
        colTen.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 15;");
        colTen.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTenDV()));

        // 4. Cột Loại DV
        TableColumn<DichVu, String> colLoai = new TableColumn<>();
        colLoai.setPrefWidth(150);
        colLoai.setStyle("-fx-alignment: CENTER;");
        colLoai.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTenLoaiDV()));

        Label lblHeaderPL = new Label("Phân loại ▼");
        lblHeaderPL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblHeaderPL.setTextFill(Color.web(C_TEXT_DARK));
        lblHeaderPL.setCursor(Cursor.HAND);
        colLoai.setGraphic(lblHeaderPL);

        ContextMenu filterTypeMenu = new ContextMenu();
        filterTypeMenu.getStyleClass().add("filter-menu");

        ToggleGroup tgLoai = new ToggleGroup();

        RadioMenuItem miAllLoai = new RadioMenuItem("Tất cả");
        miAllLoai.setToggleGroup(tgLoai);
        miAllLoai.setSelected(true);
        miAllLoai.setOnAction(e -> {
            selectedCategoryFilter = "Tất cả";
            lblHeaderPL.setText("Phân loại ▼");
            applyFilter();
        });
        filterTypeMenu.getItems().add(miAllLoai);

        LoaiDichVuDAO ldvDAO = new LoaiDichVuDAO();
        List<LoaiDichVu> loaiList = ldvDAO.getAll();

        for (LoaiDichVu l : loaiList) {
            RadioMenuItem item = new RadioMenuItem(l.getTenLoaiDV());
            item.setToggleGroup(tgLoai);
            item.setOnAction(e -> {
                selectedCategoryFilter = l.getTenLoaiDV();
                lblHeaderPL.setText("Phân loại (Lọc) ▼");
                applyFilter();
            });
            filterTypeMenu.getItems().add(item);
        }

        lblHeaderPL.setOnMouseClicked(e -> filterTypeMenu.show(lblHeaderPL, Side.BOTTOM, 0, 5));

        // 5. Cột Đơn giá
        TableColumn<DichVu, String> colGia = new TableColumn<>("Giá áp dụng");
        colGia.setPrefWidth(160);
        colGia.setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 15 0 0; -fx-font-weight: bold; -fx-text-fill: "
                + C_BLUE + ";");
        colGia.setCellValueFactory(p -> {
            DichVu dv = p.getValue();
            Double activePrice = activePriceMap.get(dv.getMaDV());
            if (activePrice != null) {
                return new SimpleStringProperty(String.format("%,.0f đ", activePrice));
            } else {
                return new SimpleStringProperty("Chưa thiết lập");
            }
        });

        // Add Tooltip and Colors
        colGia.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setStyle("");
                } else {
                    setText(item);
                    DichVu dv = getTableRow().getItem();
                    if (dv != null) {
                        if (dv.getGia() == null) {
                            // Chưa thiết lập giá
                            setStyle("-fx-text-fill: " + C_RED + "; -fx-font-style: italic;"
                                    + " -fx-alignment: CENTER_RIGHT; -fx-padding: 0 15 0 0; -fx-font-weight: bold;");
                            setTooltip(new Tooltip("Dịch vụ chưa được thiết lập giá"));
                        } else {
                            // Hiện giá (màu xanh lá cây)
                            setStyle("-fx-text-fill: " + C_GREEN
                                    + "; -fx-alignment: CENTER_RIGHT; -fx-padding: 0 15 0 0; -fx-font-weight: bold;");
                            setTooltip(new Tooltip("Giá đang áp dụng hiện tại"));
                        }
                    }
                }
            }
        });

        // 6. Cột Đơn vị
        TableColumn<DichVu, String> colDonVi = new TableColumn<>("Đơn vị");
        colDonVi.setPrefWidth(100);
        colDonVi.setStyle("-fx-alignment: CENTER;");
        colDonVi.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getDonVi()));

        // 7. Cột Trạng thái (Tích hợp bộ lọc vào Header)
        TableColumn<DichVu, String> colTrangThai = new TableColumn<>();
        colTrangThai.setPrefWidth(160);
        colTrangThai.setStyle("-fx-alignment: CENTER;");
        colTrangThai.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTrangThaiLabel()));

        // Thiết kế Header có nút lọc
        Label lblHeader = new Label("Trạng thái (Lọc) ▼");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblHeader.setTextFill(Color.web(C_TEXT_DARK));
        lblHeader.setCursor(Cursor.HAND);
        colTrangThai.setGraphic(lblHeader);

        ContextMenu filterMenu = new ContextMenu();
        filterMenu.getStyleClass().add("filter-menu");

        RadioMenuItem miAll = new RadioMenuItem("Tất cả trạng thái");
        RadioMenuItem miActive = new RadioMenuItem("Đang phục vụ");
        RadioMenuItem miSuspended = new RadioMenuItem("Tạm ngưng phục vụ");
        ToggleGroup group = new ToggleGroup();
        miAll.setToggleGroup(group);
        miActive.setToggleGroup(group);
        miSuspended.setToggleGroup(group);
        miActive.setSelected(true);

        miAll.setOnAction(e -> {
            selectedStatusFilter = "Tất cả trạng thái";
            lblHeader.setText("Trạng thái ▼");
            applyFilter();
        });
        miActive.setOnAction(e -> {
            selectedStatusFilter = "Đang phục vụ";
            lblHeader.setText("Trạng thái (Lọc) ▼");
            applyFilter();
        });
        miSuspended.setOnAction(e -> {
            selectedStatusFilter = "Tạm ngưng phục vụ";
            lblHeader.setText("Trạng thái (Lọc) ▼");
            applyFilter();
        });

        filterMenu.getItems().addAll(miAll, miActive, miSuspended);
        lblHeader.setOnMouseClicked(e -> filterMenu.show(lblHeader, Side.BOTTOM, 0, 5));

        colTrangThai.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String bg = item.equals("Đang phục vụ") ? "#d1fae5" : "#fee2e2";
                    String text = item.equals("Đang phục vụ") ? "#065f46" : "#991b1b";

                    HBox badge = BadgeUtils.createStatusBadge(item, bg, text, true);

                    // Menu phụ để đổi trạng thái nhanh (nếu là Admin)
                    if (isAdmin) {
                        ContextMenu quickMenu = new ContextMenu();
                        MenuItem qActive = new MenuItem("✅  Đang phục vụ");
                        qActive.setOnAction(
                                e -> handleToggleStatus((DichVu) getTableRow().getItem(), DichVu.DANG_PHUC_VU));
                        MenuItem qSuspended = new MenuItem("🚫  Tạm ngưng phục vụ");
                        qSuspended.setOnAction(
                                e -> handleToggleStatus((DichVu) getTableRow().getItem(), DichVu.TAM_NGUNG));
                        quickMenu.getItems().addAll(qActive, qSuspended);

                        badge.setOnMouseClicked(e -> {
                            if (e.getButton() == MouseButton.PRIMARY) {
                                quickMenu.show(badge, Side.BOTTOM, 0, 0);
                            }
                        });
                    }

                    setGraphic(badge);
                }
            }
        });

        table.getColumns().add(colSTT);
        table.getColumns().add(colMa);
        table.getColumns().add(colTen);
        table.getColumns().add(colLoai);
        table.getColumns().add(colGia);
        table.getColumns().add(colDonVi);
        table.getColumns().add(colTrangThai);

        colMa.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colTen.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.25));
        colLoai.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colGia.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colDonVi.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.10));
        colTrangThai.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.20));

        // Chặn kéo cột, sắp xếp
        table.getColumns().forEach(c -> {
            c.setReorderable(false);
            c.setSortable(false);
            c.setResizable(false);
        });

        // Context Menu (Chuột phải)
        if (isAdmin) {
            ContextMenu ctxMenu = new ContextMenu();
            MenuItem miEdit = new MenuItem("✏  Chỉnh sửa dịch vụ");
            miEdit.setOnAction(e -> {
                DichVu dv = table.getSelectionModel().getSelectedItem();
                if (dv != null)
                    openDialog(dv);
            });

            ctxMenu.getItems().addAll(miEdit);

            table.setRowFactory(tv -> {
                TableRow<DichVu> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                        ctxMenu.show(row, event.getScreenX(), event.getScreenY());
                    } else if (event.getClickCount() == 2 && !row.isEmpty()) {
                        openDialog(row.getItem());
                    } else {
                        ctxMenu.hide();
                    }
                });
                return row;
            });
        }

        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().add(table);
        return card;
    }

    /* ══════════════════ LOGIC ══════════════════ */
    public void loadData() {
        bgDAO.syncActivePricesToDB(); // Sync prices first
        activePriceMap = bgDAO.getActivePriceMap(); // Update local map
        List<DichVu> list = dao.getAll();
        masterData.setAll(list);

        filteredData = new FilteredList<>(masterData, p -> true);
        table.setItems(filteredData);
    }

    private void applyFilter() {
        if (filteredData == null)
            return;
        String kw = txtSearch.getText().toLowerCase().trim();

        filteredData.setPredicate(dv -> {
            boolean matchesText = kw.isEmpty()
                    || (dv.getMaDV() != null && dv.getMaDV().toLowerCase().contains(kw))
                    || (dv.getTenDV() != null && dv.getTenDV().toLowerCase().contains(kw));

            boolean matchesCategory = selectedCategoryFilter.equals("Tất cả")
                    || (dv.getTenLoaiDV() != null && dv.getTenLoaiDV().equalsIgnoreCase(selectedCategoryFilter));

            String statusFilter = selectedStatusFilter;
            boolean matchesStatus = statusFilter.equals("Tất cả trạng thái")
                    || (dv.getTrangThaiLabel() != null && dv.getTrangThaiLabel().equals(statusFilter));

            return matchesText && matchesCategory && matchesStatus;
        });
    }

    private void handleToggleStatus(DichVu dv, int targetStatus) {
        if (dv == null || dv.getTrangThai() == targetStatus)
            return;

        dv.setTrangThai(targetStatus);
        if (dao.update(dv)) {
            showNotify(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã đổi trạng thái dịch vụ [" + dv.getTenDV() + "] sang: " + dv.getTrangThaiLabel());
            loadData(); // Load lại bảng
        } else {
            showNotify(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái!");
        }
    }

    private void openDialog(DichVu dv) {
        Window owner = getScene().getWindow();
        new ThemSuaDichVuDialog(owner, dv, this::loadData).showDialog();
    }



    /* ── Utilities ──────────────────────────────────────────────────── */
    private void styleButton(Button btn, String bg, String fg, String hoverBg) {
        String base = "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 20; -fx-cursor: hand;";
        String hover = base.replace("-fx-background-color: " + bg, "-fx-background-color: " + hoverBg);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private void showNotify(Alert.AlertType type, String header, String msg) {
        Alert a = new Alert(type);
        a.setTitle(type == Alert.AlertType.ERROR ? "Lỗi" : "Thông báo");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.showAndWait();
    }

}
