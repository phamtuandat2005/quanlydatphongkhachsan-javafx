package gui;

import dao.LoaiPhongDAO;
import dao.LoaiPhongTienNghiDAO;
import dao.TienNghiDAO;
import model.entities.LoaiPhong;
import model.entities.TienNghi;
import model.utils.BadgeUtils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QuanLyTienNghiView extends BorderPane {

    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";

    private final TienNghiDAO tnDAO = new TienNghiDAO();
    private final LoaiPhongDAO lpDAO = new LoaiPhongDAO();
    private final LoaiPhongTienNghiDAO lptnDAO = new LoaiPhongTienNghiDAO();

    private ObservableList<TienNghi> masterData = FXCollections.observableArrayList();
    private ObservableList<TienNghiWrapper> allWrappers = FXCollections.observableArrayList();
    private FilteredList<TienNghiWrapper> filteredWrappers;
    private TableView<TienNghiWrapper> table;
    private ListView<LoaiPhong> listLoaiPhong;

    private Map<String, SimpleBooleanProperty> checkboxStates = new HashMap<>();
    private Map<String, SimpleStringProperty> soLuongStates = new HashMap<>();
    private boolean isAutoSavingEnabled = false;

    private TextField txtSearch;
    private String selectedStatusFilter = "Đang sử dụng";

    public QuanLyTienNghiView() {
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(32));

        setTop(buildHeader());
        setCenter(buildMainContent());

        loadDataTienNghi();
        loadLoaiPhong();
    }

    private VBox buildHeader() {
        VBox header = new VBox(20);
        header.setPadding(new Insets(0, 0, 12, 0));

        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label lblTitle = new Label("Quản lý Tiện nghi");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));
        Label lblSubtitle = new Label("Quản lý danh sách tiện nghi và gán tiện nghi cho từng loại phòng");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        Button btnAdd = new Button("＋  Thêm tiện nghi");
        btnAdd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnAdd.setPrefHeight(40);
        btnAdd.setCursor(Cursor.HAND);
        styleButton(btnAdd, C_BLUE, "white", C_BLUE_HOVER);
        btnAdd.setOnAction(e -> openDialog(null));

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Tìm kiếm mã hoặc tên tiện nghi...");
        txtSearch.setPrefWidth(450);
        txtSearch.setPrefHeight(40);
        txtSearch.setStyle(
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-background-radius: 8; -fx-border-color: "
                        + C_BORDER + "; -fx-border-radius: 8; -fx-padding: 8 12;");
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter());

        titleRow.getChildren().addAll(titleBox, txtSearch, new Label("  "), btnAdd);
        header.getChildren().addAll(titleRow);
        return header;
    }

    private void applyFilter() {
        if (filteredWrappers == null) return;
        String keyword = txtSearch.getText();
        filteredWrappers.setPredicate(wrapper -> {
            boolean matchesSearch = true;
            if (keyword != null && !keyword.isEmpty()) {
                String lowerCaseFilter = keyword.toLowerCase();
                matchesSearch = wrapper.getTienNghi().getMaTienNghi().toLowerCase().contains(lowerCaseFilter)
                        || wrapper.getTienNghi().getTenTienNghi().toLowerCase().contains(lowerCaseFilter);
            }

            boolean matchesStatus = true;
            if (!"Tất cả trạng thái".equals(selectedStatusFilter)) {
                boolean isActive = "Đang sử dụng".equals(selectedStatusFilter);
                matchesStatus = wrapper.getTienNghi().isTrangThai() == isActive;
            }

            return matchesSearch && matchesStatus;
        });
    }

    private HBox buildMainContent() {
        HBox mainBox = new HBox(20);

        VBox leftCard = new VBox(10);
        leftCard.setPrefWidth(250);
        leftCard.setStyle("-fx-background-color: " + C_CARD_BG + ";" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 10; -fx-background-radius: 10;");
        leftCard.setEffect(new DropShadow(8, 0, 2, Color.web("#00000010")));
        leftCard.setPadding(new Insets(16));

        Label lblLoaiPhong = new Label("Chọn Loại Phòng");
        lblLoaiPhong.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblLoaiPhong.setTextFill(Color.web(C_TEXT_DARK));

        listLoaiPhong = new ListView<>();
        listLoaiPhong.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        listLoaiPhong.setCellFactory(lv -> new ListCell<LoaiPhong>() {
            @Override
            protected void updateItem(LoaiPhong item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setFont(Font.font("Segoe UI", 14));
                    setPadding(new Insets(10));
                }
            }
        });
        listLoaiPhong.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null)
                loadConfigForLoaiPhong(n.getMaLoaiPhong());
        });
        VBox.setVgrow(listLoaiPhong, Priority.ALWAYS);

        leftCard.getChildren().addAll(lblLoaiPhong, listLoaiPhong);

        VBox rightCard = new VBox(0);
        HBox.setHgrow(rightCard, Priority.ALWAYS);
        rightCard.setStyle("-fx-background-color: " + C_CARD_BG + ";" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 10; -fx-background-radius: 10;");
        rightCard.setEffect(new DropShadow(8, 0, 2, Color.web("#00000010")));

        table = new TableView<>();
        table.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-table-cell-border-color: "
                + C_BORDER + ";");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Không có dữ liệu"));
        table.setEditable(true);

        TableColumn<TienNghiWrapper, Boolean> colCheck = new TableColumn<>();
        CheckBox cbSelectAll = new CheckBox("Chọn");
        cbSelectAll.setStyle("-fx-text-fill: #111827; -fx-font-weight: bold;");
        cbSelectAll.setOnAction(e -> {
            isAutoSavingEnabled = false;
            boolean selected = cbSelectAll.isSelected();
            for (TienNghiWrapper w : table.getItems())
                w.selectedProperty().set(selected);
            isAutoSavingEnabled = true;
            handleSaveConfig();
        });
        colCheck.setGraphic(cbSelectAll);
        colCheck.setMinWidth(80);
        colCheck.setMaxWidth(100);
        colCheck.setStyle("-fx-alignment: CENTER;");
        colCheck.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colCheck.setCellFactory(CheckBoxTableCell.forTableColumn(colCheck));
        colCheck.setEditable(true);

        TableColumn<TienNghiWrapper, String> colMa = new TableColumn<>("Mã TN");
        colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        colMa.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTienNghi().getMaTienNghi()));

        TableColumn<TienNghiWrapper, String> colSoLuong = new TableColumn<>("Số lượng");
        colSoLuong.setStyle("-fx-alignment: CENTER;");
        colSoLuong.setCellValueFactory(p -> p.getValue().soLuongProperty());
        colSoLuong.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        colSoLuong.setOnEditCommit(e -> {
            if (e.getNewValue().matches("\\d+")) {
                e.getRowValue().soLuongProperty().set(e.getNewValue());
            } else {
                e.getRowValue().soLuongProperty().set(e.getOldValue());
                table.refresh();
            }
            handleSaveConfig();
        });

        TableColumn<TienNghiWrapper, String> colTen = new TableColumn<>("Tên Tiện Nghi");
        colTen.setStyle("-fx-alignment: CENTER_LEFT;");
        colTen.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTienNghi().getTenTienNghi()));
        colTen.setEditable(false);

        // Cột Mô tả
        TableColumn<TienNghiWrapper, String> colMoTa = new TableColumn<>("Mô tả");
        colMoTa.setStyle("-fx-alignment: CENTER_LEFT;");
        colMoTa.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTienNghi().getMoTa()));
        colMoTa.setEditable(false);

        TableColumn<TienNghiWrapper, String> colTrangThai = new TableColumn<>();
        colTrangThai.setStyle("-fx-alignment: CENTER;");

        Label lblHeader = new Label("Trạng thái (Lọc) ▼");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblHeader.setTextFill(Color.web(C_TEXT_DARK));
        lblHeader.setCursor(Cursor.HAND);
        colTrangThai.setGraphic(lblHeader);

        ContextMenu filterMenu = new ContextMenu();
        RadioMenuItem miAll = new RadioMenuItem("Tất cả trạng thái");
        RadioMenuItem miActive = new RadioMenuItem("Đang sử dụng");
        RadioMenuItem miSuspended = new RadioMenuItem("Ngưng");
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
            selectedStatusFilter = "Đang sử dụng";
            lblHeader.setText("Trạng thái (Lọc) ▼");
            applyFilter();
        });
        miSuspended.setOnAction(e -> {
            selectedStatusFilter = "Ngưng";
            lblHeader.setText("Trạng thái (Lọc) ▼");
            applyFilter();
        });

        filterMenu.getItems().addAll(miAll, miActive, miSuspended);
        lblHeader.setOnMouseClicked(e -> filterMenu.show(lblHeader, Side.BOTTOM, 0, 5));
        colTrangThai.setCellValueFactory(
                p -> new SimpleStringProperty(p.getValue().getTienNghi().isTrangThai() ? "Sử dụng" : "Ngưng"));
        colTrangThai.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    boolean active = "Sử dụng".equals(item);
                    HBox badge = BadgeUtils.createStatusBadge(item, active ? "#d1fae5" : "#fee2e2",
                            active ? "#065f46" : "#991b1b", true);

                    ContextMenu quickMenu = new ContextMenu();
                    MenuItem qActive = new MenuItem("✅  Sử dụng");
                    qActive.setOnAction(e -> {
                        TienNghiWrapper tw = (TienNghiWrapper) getTableRow().getItem();
                        if (tw != null && !tw.getTienNghi().isTrangThai()) {
                            tw.getTienNghi().setTrangThai(true);
                            if (tnDAO.update(tw.getTienNghi())) loadDataTienNghi();
                        }
                    });
                    MenuItem qSuspended = new MenuItem("🚫  Ngưng");
                    qSuspended.setOnAction(e -> {
                        TienNghiWrapper tw = (TienNghiWrapper) getTableRow().getItem();
                        if (tw != null && tw.getTienNghi().isTrangThai()) {
                            tw.getTienNghi().setTrangThai(false);
                            if (tnDAO.update(tw.getTienNghi())) loadDataTienNghi();
                        }
                    });
                    quickMenu.getItems().addAll(qActive, qSuspended);

                    badge.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            quickMenu.show(badge, Side.BOTTOM, 0, 0);
                        }
                    });

                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(colCheck, colMa, colTen, colSoLuong, colMoTa, colTrangThai);
        // Bind % chiều rộng để co giãn theo kích thước cửa sổ
        colCheck.prefWidthProperty().bind(table.widthProperty().multiply(0.07));
        colMa.prefWidthProperty().bind(table.widthProperty().multiply(0.09));
        colTen.prefWidthProperty().bind(table.widthProperty().multiply(0.24));
        colSoLuong.prefWidthProperty().bind(table.widthProperty().multiply(0.09));
        colMoTa.prefWidthProperty().bind(table.widthProperty().multiply(0.38));
        colTrangThai.prefWidthProperty().bind(table.widthProperty().multiply(0.12));
        // Cố định thứ tự cột, không cho kéo di chuyển và co giãn
        for (TableColumn<?, ?> col : table.getColumns()) {
            col.setReorderable(false);
            col.setResizable(false);
        }

        // Context menu và double-click để Sửa/Xóa tiện nghi
        ContextMenu ctxMenu = new ContextMenu();
        MenuItem miEdit = new MenuItem("✏  Cập nhật tiện nghi");
        miEdit.setStyle("-fx-font-size: 13px;");
        miEdit.setOnAction(e -> {
            TienNghiWrapper tw = table.getSelectionModel().getSelectedItem();
            if (tw != null)
                openDialog(tw.getTienNghi());
        });
        ctxMenu.getItems().addAll(miEdit);

        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY && table.getSelectionModel().getSelectedItem() != null) {
                ctxMenu.show(table, e.getScreenX(), e.getScreenY());
            } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                TienNghiWrapper tw = table.getSelectionModel().getSelectedItem();
                if (tw != null && !table.getSelectionModel().getSelectedCells().isEmpty()
                        && table.getSelectionModel().getSelectedCells().get(0).getColumn() != 0) {
                    openDialog(tw.getTienNghi());
                }
            } else {
                ctxMenu.hide();
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        rightCard.getChildren().add(table);
        mainBox.getChildren().addAll(leftCard, rightCard);
        return mainBox;
    }

    private void loadLoaiPhong() {
        List<LoaiPhong> lps = lpDAO.getAll();
        listLoaiPhong.setItems(FXCollections.observableArrayList(lps));
        if (!lps.isEmpty())
            listLoaiPhong.getSelectionModel().selectFirst();
    }

    private void loadDataTienNghi() {
        masterData.setAll(tnDAO.getAll());
        rebuildTableWrappers();
    }

    private void rebuildTableWrappers() {
        isAutoSavingEnabled = false;
        allWrappers.clear();
        for (TienNghi tn : masterData) {
            SimpleBooleanProperty prop = checkboxStates.computeIfAbsent(tn.getMaTienNghi(), k -> {
                SimpleBooleanProperty p = new SimpleBooleanProperty(false);
                p.addListener((obs, old, n) -> {
                    if (isAutoSavingEnabled)
                        handleSaveConfig();
                });
                return p;
            });
            SimpleStringProperty slProp = soLuongStates.computeIfAbsent(tn.getMaTienNghi(),
                    k -> new SimpleStringProperty("1"));
            allWrappers.add(new TienNghiWrapper(tn, prop, slProp));
        }
        if (filteredWrappers == null) {
            filteredWrappers = new FilteredList<>(allWrappers, p -> true);
            table.setItems(filteredWrappers);
        }
        isAutoSavingEnabled = true;
        applyFilter();
    }

    private void loadConfigForLoaiPhong(String maLoaiPhong) {
        isAutoSavingEnabled = false;
        for (SimpleBooleanProperty prop : checkboxStates.values())
            prop.set(false);
        for (SimpleStringProperty prop : soLuongStates.values())
            prop.set("1");
        Map<String, Integer> assigned = lptnDAO.getTienNghiMapByLoaiPhong(maLoaiPhong);
        for (Map.Entry<String, Integer> entry : assigned.entrySet()) {
            if (checkboxStates.containsKey(entry.getKey()))
                checkboxStates.get(entry.getKey()).set(true);
            if (soLuongStates.containsKey(entry.getKey()))
                soLuongStates.get(entry.getKey()).set(String.valueOf(entry.getValue()));
        }
        isAutoSavingEnabled = true;
    }

    private void handleSaveConfig() {
        if (!isAutoSavingEnabled)
            return;
        LoaiPhong lp = listLoaiPhong.getSelectionModel().getSelectedItem();
        if (lp == null)
            return;
        Map<String, Integer> selectedMaTN = new HashMap<>();
        for (TienNghi tn : masterData) {
            SimpleBooleanProperty prop = checkboxStates.get(tn.getMaTienNghi());
            if (prop != null && prop.get()) {
                int sl = 1;
                try {
                    sl = Integer.parseInt(soLuongStates.get(tn.getMaTienNghi()).get());
                } catch (Exception ignored) {
                }
                selectedMaTN.put(tn.getMaTienNghi(), sl);
            }
        }
        lptnDAO.updateTienNghiForLoaiPhong(lp.getMaLoaiPhong(), selectedMaTN);
    }

    private void openDialog(TienNghi tn) {
        Window owner = getScene().getWindow();
        new ThemSuaTienNghiDialog(owner, tn, tnDAO, this::loadDataTienNghi).showDialog();
    }



    private void showAlert(Alert.AlertType type, String header, String msg) {
        Alert a = new Alert(type);
        a.setTitle(type == Alert.AlertType.ERROR ? "Lỗi" : "Thông báo");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void styleButton(Button btn, String bg, String fg, String hoverBg) {
        String base = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(bg, hoverBg)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    public static class TienNghiWrapper {
        private final TienNghi tienNghi;
        private final SimpleBooleanProperty selected;
        private final SimpleStringProperty soLuong;

        public TienNghiWrapper(TienNghi tn, SimpleBooleanProperty prop, SimpleStringProperty slProp) {
            this.tienNghi = tn;
            this.selected = prop;
            this.soLuong = slProp;
        }

        public TienNghi getTienNghi() {
            return tienNghi;
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public SimpleStringProperty soLuongProperty() {
            return soLuong;
        }
    }
}
