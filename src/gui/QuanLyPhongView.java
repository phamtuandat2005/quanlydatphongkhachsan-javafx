package gui;

import dao.PhongDAO;
import model.entities.Phong;
import model.enums.TrangThaiPhong;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/**
 * QuanLyPhongView – JavaFX
 * Thay thế QuanLyPhongPanel (Swing).
 */
public class QuanLyPhongView extends BorderPane {

    /* ── Bảng màu ───────────────────────────────────────────────────── */
    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final String C_EMERALD = "#059669";
    private static final String C_AMBER = "#d97706";
    private static final String C_ROSE = "#e11d48";
    private static final String C_PURPLE = "#7c3aed";

    /* ── DAO & dữ liệu ─────────────────────────────────────────────── */
    private final PhongDAO dao = new PhongDAO();
    private ObservableList<Phong> masterData = FXCollections.observableArrayList();
    private FilteredList<Phong> filteredData;

    /* ── Controls ───────────────────────────────────────────────────── */
    private TableView<Phong> table;
    private TextField txtSearch;
    private Label lblTotal, lblAvailable, lblOccupied, lblDirty, lblMaintenance;

    /* ── Column filter state ────────────────────────────────────────── */
    private String filterLoaiPhong = null;   // null = tất cả
    private String filterTrangThai = null;
    private Integer filterTang = null;
    private TableColumn<Phong, String> colLoai, colTrangThai, colTang;

    /* ── PHÂN QUYỀN ────────────────────────────────────────────── */
    private final boolean isAdmin;

    public QuanLyPhongView(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildTableCard());

        loadData();
    }

    private VBox buildHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(0, 0, 12, 0));

        /* ── Dòng 1: Tiêu đề + Nút thêm ─────────────────────────── */
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label lblTitle = new Label("Quản lý phòng sách phòng");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));
        Label lblSubtitle = new Label("Quản lý và theo dõi trạng thái danh sách phòng");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        titleRow.getChildren().add(titleBox);

        if (isAdmin) {
            Button btnAdd = new Button("＋  Thêm phòng mới");
            btnAdd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            btnAdd.setPrefHeight(40);
            btnAdd.setCursor(Cursor.HAND);
            styleButton(btnAdd, C_BLUE, "white", C_BLUE_HOVER);
            btnAdd.setOnAction(e -> openDialog(null));
            titleRow.getChildren().add(btnAdd);
        }

        /* ── Dòng 2: Thẻ thống kê ────────────────────────────────── */
        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        lblTotal = new Label("0");
        lblAvailable = new Label("0");
        lblOccupied = new Label("0");
        lblDirty = new Label("0");
        lblMaintenance = new Label("0");

        VBox c1 = createStatCard("🏢", "TỔNG SỐ PHÒNG", lblTotal, C_NAVY);
        VBox c2 = createStatCard("✅", "CÒN TRỐNG", lblAvailable, C_EMERALD);
        VBox c3 = createStatCard("🛏", "ĐÃ CÓ KHÁCH", lblOccupied, C_AMBER);
        VBox c4 = createStatCard("🗑", "PHÒNG BẨN", lblDirty, C_ROSE);
        VBox c5 = createStatCard("🔧", "BẢO TRÌ", lblMaintenance, C_PURPLE);

        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        HBox.setHgrow(c3, Priority.ALWAYS);
        HBox.setHgrow(c4, Priority.ALWAYS);
        HBox.setHgrow(c5, Priority.ALWAYS);
        statsRow.getChildren().addAll(c1, c2, c3, c4, c5);

        /* ── Dòng 3: Thanh tìm kiếm ──────────────────────────────── */
        txtSearch = new TextField();
        txtSearch.setPromptText("🔍  Tìm kiếm theo mã phòng hoặc loại phòng...");
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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Không có dữ liệu phòng"));

        // Cột STT
        TableColumn<Phong, String> colSTT = new TableColumn<>("STT");
        colSTT.setMinWidth(50);
        colSTT.setMaxWidth(60);
        colSTT.setStyle("-fx-alignment: CENTER;");
        colSTT.setCellValueFactory(
                p -> new SimpleStringProperty(String.valueOf(table.getItems().indexOf(p.getValue()) + 1)));

        // Cột Mã phòng
        TableColumn<Phong, String> colMa = new TableColumn<>("Mã phòng");
        colMa.setMinWidth(100);
        colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        colMa.setCellValueFactory(p -> new SimpleStringProperty(nvl(p.getValue().getMaPhong())));

        // Cột Loại phòng (có bộ lọc trong header)
        colLoai = new TableColumn<>("Loại phòng ▼");
        colLoai.setMinWidth(150);
        colLoai.setStyle("-fx-alignment: CENTER;");
        colLoai.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().getLoaiPhong() != null ? p.getValue().getLoaiPhong().toString() : ""));
        installColumnFilter(colLoai, "Loại phòng", buildLoaiPhongMenu());

        // Cột Đơn giá
        TableColumn<Phong, String> colGia = new TableColumn<>("Đơn giá");
        colGia.setMinWidth(130);
        colGia.setStyle("-fx-alignment: CENTER;");
        colGia.setCellValueFactory(p -> {
            if (p.getValue().getLoaiPhong() != null) {
                return new SimpleStringProperty(String.format("%,.0f đ", p.getValue().getLoaiPhong().getGia()));
            }
            return new SimpleStringProperty("");
        });

        // Cột Sức chứa
        TableColumn<Phong, String> colSucChua = new TableColumn<>("Sức chứa");
        colSucChua.setMinWidth(100);
        colSucChua.setStyle("-fx-alignment: CENTER;");
        colSucChua.setCellValueFactory(p -> {
            if (p.getValue().getLoaiPhong() != null) {
                return new SimpleStringProperty(p.getValue().getLoaiPhong().getSucChua() + " người");
            }
            return new SimpleStringProperty("");
        });

        // Cột Trạng thái (có bộ lọc trong header)
        colTrangThai = new TableColumn<>("Trạng thái ▼");
        colTrangThai.setMinWidth(150);
        colTrangThai.setStyle("-fx-alignment: CENTER;");
        colTrangThai.setCellValueFactory(p -> {
            TrangThaiPhong tt = p.getValue().getTrangThai();
            String val = (tt != null) ? tt.getLabel() : "Không xác định";
            return new SimpleStringProperty(val);
        });
        colTrangThai.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean isOccupied = TrangThaiPhong.DACOKHACH.getLabel().equals(item);

                    // Style dựa trên trạng thái
                    String bg, text;
                    if (TrangThaiPhong.CONTRONG.getLabel().equals(item)) {
                        bg = "#d1fae5";
                        text = "#065f46"; // Emerald
                    } else if (isOccupied) {
                        bg = "#fef3c7";
                        text = "#92400e"; // Amber
                    } else {
                        bg = "#fee2e2";
                        text = "#991b1b"; // Rose
                    }

                    // Sử dụng Utility để tạo Badge đồng nhất
                    HBox badge = BadgeUtils.createStatusBadge(item, bg, text, !isOccupied);

                    // Menu chọn trạng thái: CHỈ cho phép Còn trống / Đang bảo trì
                    if (!isOccupied) {
                        ContextMenu statusMenu = new ContextMenu();
                        MenuItem m1 = new MenuItem("✅  " + TrangThaiPhong.CONTRONG.getLabel());
                        m1.setOnAction(ev -> updateStatus((Phong) getTableRow().getItem(), TrangThaiPhong.CONTRONG));
                        MenuItem m2 = new MenuItem("🗑   " + TrangThaiPhong.BAN.getLabel());
                        m2.setOnAction(ev -> updateStatus((Phong) getTableRow().getItem(), TrangThaiPhong.BAN));
                        MenuItem m3 = new MenuItem("🔧   " + TrangThaiPhong.BAOTRI.getLabel());
                        m3.setOnAction(ev -> updateStatus((Phong) getTableRow().getItem(), TrangThaiPhong.BAOTRI));
                        statusMenu.getItems().addAll(m1, m2, m3);

                        badge.setOnMouseClicked(e -> {
                            if (e.getButton() == MouseButton.PRIMARY) {
                                statusMenu.show(badge, e.getScreenX(), e.getScreenY());
                            }
                        });
                    }

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        installColumnFilter(colTrangThai, "Trạng thái", buildTrangThaiMenu());

        // Cột Tầng (có bộ lọc trong header)
        colTang = new TableColumn<>("Tầng ▼");
        colTang.setMinWidth(80);
        colTang.setStyle("-fx-alignment: CENTER;");
        colTang.setCellValueFactory(p -> new SimpleStringProperty(String.valueOf(p.getValue().getSoTang())));

        table.getColumns().add(colSTT);
        table.getColumns().add(colMa);
        table.getColumns().add(colLoai);
        table.getColumns().add(colGia);
        table.getColumns().add(colSucChua);
        table.getColumns().add(colTrangThai);
        table.getColumns().add(colTang);

        // Ràng buộc chiều rộng để các cột tự giãn và chặn kéo tay
        colMa.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colLoai.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.25));
        colGia.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colSucChua.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.15));
        colTrangThai.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.20));
        colTang.prefWidthProperty().bind(table.widthProperty().subtract(65).multiply(0.10));

        for (TableColumn<Phong, ?> c : table.getColumns()) {
            c.setReorderable(false);
            c.setSortable(false);
            c.setResizable(false);
        }

        // ContextMenu (Nhấn phải chuột)
        if (isAdmin) {
            ContextMenu ctxMenu = new ContextMenu();
            MenuItem miEdit = new MenuItem("✏  Cập nhật phòng");
            miEdit.setStyle("-fx-font-size: 13px;");
            miEdit.setOnAction(e -> {
                Phong p = table.getSelectionModel().getSelectedItem();
                if (p != null) {
                    if (p.getTrangThai() == TrangThaiPhong.DACOKHACH) {
                        showAlert(Alert.AlertType.WARNING, "Không thể chỉnh sửa",
                                "Phòng đang có khách. Vui lòng thực hiện trả phòng trước.");
                    } else {
                        openDialog(p);
                    }
                }
            });

            MenuItem miDelete = new MenuItem("🗑  Xóa phòng");
            miDelete.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
            miDelete.setOnAction(e -> {
                Phong p = table.getSelectionModel().getSelectedItem();
                if (p != null) {
                    if (p.getTrangThai() == TrangThaiPhong.DACOKHACH) {
                        showAlert(Alert.AlertType.WARNING, "Không thể xóa",
                                "Phòng đang có khách. Vui lòng thực hiện trả phòng trước.");
                    } else {
                        handleDelete(p);
                    }
                }
            });

            ctxMenu.getItems().addAll(miEdit, new SeparatorMenuItem(), miDelete);

            table.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY && table.getSelectionModel().getSelectedItem() != null) {
                    ctxMenu.show(table, e.getScreenX(), e.getScreenY());
                } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    Phong p = table.getSelectionModel().getSelectedItem();
                    if (p != null) {
                        if (p.getTrangThai() == TrangThaiPhong.DACOKHACH) {
                            showAlert(Alert.AlertType.WARNING, "Không thể chỉnh sửa",
                                    "Phòng đang có khách. Vui lòng thực hiện trả phòng trước.");
                        } else {
                            openDialog(p);
                        }
                    }
                } else {
                    ctxMenu.hide();
                }
            });
        }

        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().add(table);
        return card;
    }

    public void loadData() {
        try {
            List<Phong> ds = dao.getAll();
            masterData.setAll(ds);

            filteredData = new FilteredList<>(masterData, p -> true);
            table.setItems(filteredData);

            // Cập nhật thống kê
            long total = masterData.size();
            long available = masterData.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.CONTRONG).count();
            long occupied = masterData.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.DACOKHACH).count();
            long dirty = masterData.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.BAN).count();
            long maintenance = masterData.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.BAOTRI).count();

            if (lblTotal != null) lblTotal.setText(String.valueOf(total));
            if (lblAvailable != null) lblAvailable.setText(String.valueOf(available));
            if (lblOccupied != null) lblOccupied.setText(String.valueOf(occupied));
            if (lblDirty != null) lblDirty.setText(String.valueOf(dirty));
            if (lblMaintenance != null) lblMaintenance.setText(String.valueOf(maintenance));

            // Rebuild Tầng menu dynamically after data load
            if (colTang != null) {
                installColumnFilter(colTang, "Tầng", buildTangMenu());
                if (filterTang != null) {
                    updateColumnHeader(colTang, "Tầng", String.valueOf(filterTang));
                }
            }

            applyFilter(txtSearch != null ? txtSearch.getText() : "");
        } catch (Exception ex) {
            // Không làm gì nếu lỗi db
        }
    }

    /* ── Column-header filter helpers ──────────────────────────────── */

    /**
     * Gắn ContextMenu vào header của cột. Click vào header sẽ mở menu lọc.
     */
    private void installColumnFilter(TableColumn<Phong, String> col, String baseName, ContextMenu menu) {
        // Tìm Label trong header và gắn sự kiện click
        col.setContextMenu(null); // clear old
        // Sử dụng graphic label cho header để bắt sự kiện click
        Label headerLabel = new Label(baseName + " ▼");
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setCursor(Cursor.HAND);
        headerLabel.setStyle("-fx-font-size: 13px;");
        headerLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                menu.show(headerLabel, Side.BOTTOM, 0, 0);
            }
        });
        col.setGraphic(headerLabel);
        col.setText("");
    }

    private ContextMenu buildLoaiPhongMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem all = new MenuItem("Tất cả loại phòng");
        all.setOnAction(e -> { filterLoaiPhong = null; updateColumnHeader(colLoai, "Loại phòng", null); applyFilter(txtSearch.getText()); });
        menu.getItems().add(all);
        menu.getItems().add(new SeparatorMenuItem());
        for (model.enums.TenLoaiPhong tlp : model.enums.TenLoaiPhong.values()) {
            MenuItem mi = new MenuItem(tlp.getDisplayName());
            mi.setOnAction(e -> { filterLoaiPhong = tlp.getDisplayName(); updateColumnHeader(colLoai, "Loại phòng", tlp.getDisplayName()); applyFilter(txtSearch.getText()); });
            menu.getItems().add(mi);
        }
        return menu;
    }

    private ContextMenu buildTrangThaiMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem all = new MenuItem("Tất cả trạng thái");
        all.setOnAction(e -> { filterTrangThai = null; updateColumnHeader(colTrangThai, "Trạng thái", null); applyFilter(txtSearch.getText()); });
        menu.getItems().add(all);
        menu.getItems().add(new SeparatorMenuItem());
        for (TrangThaiPhong tt : TrangThaiPhong.values()) {
            MenuItem mi = new MenuItem(tt.getLabel());
            mi.setOnAction(e -> { filterTrangThai = tt.getLabel(); updateColumnHeader(colTrangThai, "Trạng thái", tt.getLabel()); applyFilter(txtSearch.getText()); });
            menu.getItems().add(mi);
        }
        return menu;
    }

    private ContextMenu buildTangMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem all = new MenuItem("Tất cả tầng");
        all.setOnAction(e -> { filterTang = null; updateColumnHeader(colTang, "Tầng", null); applyFilter(txtSearch.getText()); });
        menu.getItems().add(all);
        menu.getItems().add(new SeparatorMenuItem());
        masterData.stream().map(Phong::getSoTang).distinct().sorted().forEach(t -> {
            MenuItem mi = new MenuItem("Tầng " + t);
            mi.setOnAction(e -> { filterTang = t; updateColumnHeader(colTang, "Tầng", String.valueOf(t)); applyFilter(txtSearch.getText()); });
            menu.getItems().add(mi);
        });
        return menu;
    }

    /**
     * Cập nhật text hiển thị trên header cột.
     * Nếu value == null → hiển thị tên gốc (tất cả). Ngược lại → "Tên: giá trị".
     */
    private void updateColumnHeader(TableColumn<Phong, String> col, String baseName, String value) {
        String display;
        if (value == null) {
            display = baseName + " ▼";
        } else if ("Tầng".equals(baseName)) {
            display = "Tầng " + value + " ▼";
        } else {
            display = baseName + ": " + value + " ▼";
        }
        Label lbl = (Label) col.getGraphic();
        if (lbl != null) lbl.setText(display);
    }

    private void applyFilter(String keyword) {
        if (filteredData == null)
            return;
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        filteredData.setPredicate(p -> {
            // Text filter
            if (!kw.isEmpty()) {
                String lp = p.getLoaiPhong() != null ? p.getLoaiPhong().toString().toLowerCase() : "";
                if (!nvl(p.getMaPhong()).toLowerCase().contains(kw) && !lp.contains(kw))
                    return false;
            }
            // Room type filter
            if (filterLoaiPhong != null) {
                String loaiStr = p.getLoaiPhong() != null ? p.getLoaiPhong().toString() : "";
                if (!loaiStr.equals(filterLoaiPhong))
                    return false;
            }
            // Status filter
            if (filterTrangThai != null) {
                String ttStr = p.getTrangThai() != null ? p.getTrangThai().getLabel() : "";
                if (!ttStr.equals(filterTrangThai))
                    return false;
            }
            // Floor filter
            if (filterTang != null) {
                if (p.getSoTang() != filterTang)
                    return false;
            }
            return true;
        });
    }

    private void openDialog(Phong p) {
        Window owner = getScene().getWindow();
        new ThemSuaPhongDialog(owner, p, dao, this::loadData).showDialog();
    }

    private void updateStatus(Phong p, TrangThaiPhong newStatus) {
        if (p == null || p.getTrangThai() == newStatus)
            return;
        p.setTrangThai(newStatus);
        if (dao.update(p)) {
            loadData();
        } else {
            showAlert(Alert.AlertType.ERROR, "Cập nhật thất bại", "Không thể cập nhật trạng thái phòng này.");
            loadData(); // reload to revert local change if db failed
        }
    }

    private void handleDelete(Phong p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa phòng [" + p.getMaPhong() + "]?");
        confirm.setContentText("Hành động này không thể hoàn tác.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (dao.delete(p.getMaPhong())) {
                showAlert(Alert.AlertType.INFORMATION, "Đã xóa", "Phòng " + p.getMaPhong() + " đã được xóa.");
                loadData();
            } else {
                showAlert(Alert.AlertType.ERROR, "Xóa thất bại",
                        "Kiểm tra lại xem phòng có đang dính dữ liệu hóa đơn không.");
            }
        }
    }

    /* ── Utilities ──────────────────────────────────────────────────── */

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
}
