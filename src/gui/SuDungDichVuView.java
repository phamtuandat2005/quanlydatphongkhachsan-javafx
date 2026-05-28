package gui;

import dao.BangGiaDichVuDAO;
import dao.DatPhongDAO;
import dao.DichVuDAO;
import dao.DichVuSuDungDAO;
import dao.LoaiDichVuDAO;
import dao.PhongDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.entities.DichVu;
import model.entities.DichVuSuDung;
import model.entities.LoaiDichVu;
import model.entities.Phong;
import model.enums.TrangThaiPhong;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SuDungDichVuView extends BorderPane {

    private static final String C_SIDEBAR = "#1e3a8a";
    private static final String C_ACTIVE = "#1d4ed8";
    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_SOFT_BLUE = "#eff6ff";

    private final DatPhongDAO datPhongDAO = new DatPhongDAO();
    private final BangGiaDichVuDAO bangGiaDAO = new BangGiaDichVuDAO();
    private final LoaiDichVuDAO loaiDVDAO = new LoaiDichVuDAO();
    private final DichVuSuDungDAO dvsdDAO = new DichVuSuDungDAO();
    private final PhongDAO phongDAO = new PhongDAO();
    private final DichVuDAO dichVuDAO = new DichVuDAO();

    private final ObservableList<Phong> displayedRooms = FXCollections.observableArrayList();
    private final ObservableList<DichVu> displayedServices = FXCollections.observableArrayList();
    private final Map<DichVu, Integer> cart = new HashMap<>();
    private final Map<String, Double> activePrices = new HashMap<>();
    private final List<LoaiDichVu> categories = new ArrayList<>();

    private TilePane roomTilePane;
    private TilePane serviceTilePane;
    private VBox billContainer;
    private ComboBox<String> cboCategory;
    private Label lblRoomTitle;
    private Label lblRoomCount;
    private Label lblServiceCount;
    private Label lblTotal;
    private TextField txtRoomSearch;
    private TextField txtServiceSearch;

    private String selectedMaPhong = "";
    private LoaiDichVu currentCategory;
    private boolean usedServicesExpanded = false;

    public SuDungDichVuView() {
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(24));
        setCenter(buildBody());

        categories.addAll(loaiDVDAO.getAll());
        currentCategory = null;
        refreshCategoryOptions();
        refreshRooms();
        refreshServices();
        updateBillUI();
    }

    private HBox buildBody() {
        HBox body = new HBox(24);
        body.setAlignment(Pos.TOP_LEFT);

        VBox leftPane = buildLeftPane();
        leftPane.prefWidthProperty().bind(body.widthProperty().multiply(0.65));
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        VBox rightPane = buildRightPane();
        rightPane.prefWidthProperty().bind(body.widthProperty().multiply(0.35));
        rightPane.setMinWidth(320);

        body.getChildren().addAll(leftPane, rightPane);
        return body;
    }

    private VBox buildLeftPane() {
        VBox pane = new VBox(16);

        VBox roomBox = new VBox(10);
        roomBox.setPadding(new Insets(12, 15, 12, 15));
        roomBox.setStyle("-fx-background-color: " + C_CARD + ";" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;");
        roomBox.setEffect(new DropShadow(8, 0, 2, Color.web("#00000008")));
        roomBox.setPrefHeight(320);
        roomBox.setMinHeight(320);

        HBox roomHeader = new HBox(10);
        roomHeader.setAlignment(Pos.CENTER_LEFT);

        VBox roomText = new VBox(2);
        Label lblRoom = new Label("Phòng đang có khách");
        lblRoom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblRoom.setTextFill(Color.web(C_TEXT_DARK));
        Label lblRoomSub = new Label("Tìm theo mã phòng hoặc tên khách hàng");
        lblRoomSub.setFont(Font.font("Segoe UI", 12));
        lblRoomSub.setTextFill(Color.web(C_TEXT_GRAY));
        roomText.getChildren().addAll(lblRoom, lblRoomSub);

        lblRoomCount = new Label();
        lblRoomCount.setStyle("-fx-background-color: " + C_SOFT_BLUE + ";" +
                "-fx-text-fill: " + C_SIDEBAR + ";" +
                "-fx-background-radius: 999; -fx-padding: 6 12 6 12;" +
                "-fx-font-weight: bold; -fx-font-size: 12;");

        Region roomSpacer = new Region();
        HBox.setHgrow(roomSpacer, Priority.ALWAYS);
        roomHeader.getChildren().addAll(roomText, roomSpacer, lblRoomCount);

        txtRoomSearch = createSearchField("Nhập mã phòng / Tên khách hàng..");
        txtRoomSearch.textProperty().addListener((obs, oldVal, newVal) -> filterRooms());
        txtRoomSearch.setMaxWidth(500);

        roomTilePane = new TilePane();
        roomTilePane.setHgap(8);
        roomTilePane.setVgap(8);
        roomTilePane.setPrefColumns(5);
        roomTilePane.setTileAlignment(Pos.TOP_LEFT);
        roomTilePane.setStyle("-fx-background-color: transparent;");

        ScrollPane roomScroll = new ScrollPane(roomTilePane);
        roomScroll.setFitToWidth(true);
        roomScroll.setPrefViewportHeight(220);
        roomScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        roomBox.getChildren().addAll(roomHeader, txtRoomSearch, roomScroll);

        VBox serviceBox = new VBox(12);
        serviceBox.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(serviceBox, Priority.ALWAYS);

        HBox serviceHeader = new HBox(10);
        serviceHeader.setAlignment(Pos.CENTER_LEFT);
        VBox serviceText = new VBox(2);
        Label lblService = new Label("Danh sách dịch vụ");
        lblService.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblService.setTextFill(Color.web(C_TEXT_DARK));
        Label lblServiceSub = new Label("Lọc theo nhóm và tìm theo tên dịch vụ.");
        lblServiceSub.setFont(Font.font("Segoe UI", 12));
        lblServiceSub.setTextFill(Color.web(C_TEXT_GRAY));
        serviceText.getChildren().addAll(lblService, lblServiceSub);

        lblServiceCount = new Label();
        lblServiceCount.setStyle("-fx-background-color: " + C_SOFT_BLUE + ";" +
                "-fx-text-fill: " + C_SIDEBAR + ";" +
                "-fx-background-radius: 999; -fx-padding: 6 12 6 12;" +
                "-fx-font-weight: bold; -fx-font-size: 12;");
        Region serviceSpacer = new Region();
        HBox.setHgrow(serviceSpacer, Priority.ALWAYS);
        serviceHeader.getChildren().addAll(serviceText, serviceSpacer, lblServiceCount);

        txtServiceSearch = createSearchField("Tìm dịch vụ...");
        txtServiceSearch.textProperty().addListener((obs, oldVal, newVal) -> refreshServices());

        cboCategory = new ComboBox<>();
        cboCategory.setPrefWidth(200);
        cboCategory.setPrefHeight(34);

        cboCategory.setStyle("-fx-background-color: white;" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;" +
                "-fx-font-size: 12;");
        cboCategory.setOnAction(e -> {
            String selected = cboCategory.getValue();
            if (selected == null || "Tất cả".equals(selected)) {
                currentCategory = null;
            } else {
                for (LoaiDichVu category : categories) {
                    if (category.getTenLoaiDV().equals(selected)) {
                        currentCategory = category;
                        break;
                    }
                }
            }
            refreshServices();
        });

        HBox filterRow = new HBox(10, txtServiceSearch, cboCategory);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        serviceTilePane = new TilePane();
        serviceTilePane.setHgap(10);
        serviceTilePane.setVgap(10);
        serviceTilePane.setTileAlignment(Pos.TOP_LEFT);
        serviceTilePane.setStyle("-fx-background-color: transparent;");

        ScrollPane serviceScroll = new ScrollPane(serviceTilePane);
        serviceScroll.setFitToWidth(true);
        serviceScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(serviceScroll, Priority.ALWAYS);

        serviceBox.getChildren().addAll(serviceHeader, filterRow, serviceScroll);

        pane.getChildren().addAll(roomBox, serviceBox);
        VBox.setVgrow(serviceBox, Priority.ALWAYS);
        return pane;
    }

    private VBox buildRightPane() {
        VBox pane = new VBox(0);
        pane.setStyle("-fx-background-color: " + C_CARD + ";" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;");
        pane.setEffect(new DropShadow(10, 0, 4, Color.web("#00000008")));

        VBox billHeader = new VBox(4);
        billHeader.setPadding(new Insets(20, 25, 20, 25));
        billHeader.setStyle("-fx-background-color: " + C_SIDEBAR + "; -fx-background-radius: 12 12 0 0;");

        lblRoomTitle = new Label("Hóa đơn: ---");
        lblRoomTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblRoomTitle.setTextFill(Color.WHITE);
        billHeader.getChildren().setAll(lblRoomTitle);

        billContainer = new VBox(0);
        billContainer.setStyle("-fx-background-color: white;");
        ScrollPane billScroll = new ScrollPane(billContainer);
        billScroll.setFitToWidth(true);
        billScroll.setPrefHeight(300);
        billScroll.setStyle("-fx-background-color: transparent; -fx-background: white; -fx-border-color: transparent;");
        VBox.setVgrow(billScroll, Priority.ALWAYS);

        VBox footer = new VBox(15);
        footer.setPadding(new Insets(16, 20, 20, 20));
        footer.setStyle(
                "-fx-border-color: " + C_BORDER + " transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        lblTotal = new Label("Tổng: 0 đồng");
        lblTotal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTotal.setTextFill(Color.web(C_SIDEBAR));

        HBox actionPanel = new HBox(12);
        actionPanel.setAlignment(Pos.CENTER);

        Button btnClear = new Button("Hủy");
        btnClear.setPrefWidth(110);
        btnClear.setPrefHeight(45);
        btnClear.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnClear.setStyle("-fx-background-color: white; -fx-text-fill: " + C_SIDEBAR +
                "; -fx-border-color: #d1d5db; -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");
        btnClear.setOnAction(e -> {
            cart.clear();
            updateBillUI();
            refreshServices();
        });

        Button btnConfirm = new Button("Xác Nhận");
        btnConfirm.setPrefWidth(200);
        btnConfirm.setPrefHeight(45);
        btnConfirm.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnConfirm.setStyle("-fx-background-color: " + C_ACTIVE + "; -fx-text-fill: white;" +
                "-fx-background-radius: 15; -fx-cursor: hand;");
        HBox.setHgrow(btnConfirm, Priority.ALWAYS);
        btnConfirm.setOnAction(e -> handleConfirm());

        actionPanel.getChildren().addAll(btnClear, btnConfirm);
        footer.getChildren().addAll(lblTotal, actionPanel);
        pane.getChildren().addAll(billHeader, billScroll, footer);
        return pane;
    }

    private TextField createSearchField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMinWidth(300);
        field.setPrefWidth(400);
        field.setPrefHeight(34);
        field.setStyle("-fx-background-color: #f8fafc;" +
                "-fx-border-color: " + C_BORDER + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;" +
                "-fx-padding: 7 12 7 12; -fx-font-size: 12;");
        return field;
    }

    private void refreshRooms() {
        filterRooms();
    }

    private void filterRooms() {
        List<Phong> allRooms = phongDAO.getPhongByTrangThai(TrangThaiPhong.DACOKHACH);
        String keyword = normalize(txtRoomSearch == null ? "" : txtRoomSearch.getText());

        allRooms.sort(Comparator.comparing(Phong::getMaPhong, String.CASE_INSENSITIVE_ORDER));
        displayedRooms.setAll(allRooms.stream().filter(room -> {
            if (keyword.isEmpty()) {
                return true;
            }
            String guest = normalize(datPhongDAO.getTenKhachHienTai(room.getMaPhong()));
            return normalize(room.getMaPhong()).contains(keyword) || guest.contains(keyword);
        }).collect(Collectors.toList()));

        lblRoomCount.setText(displayedRooms.size() + " Phòng");
        renderRooms();
    }

    private void renderRooms() {
        roomTilePane.getChildren().clear();
        for (Phong room : displayedRooms) {
            roomTilePane.getChildren().add(buildRoomCard(room));
        }
    }

    private VBox buildRoomCard(Phong room) {
        boolean isSelected = selectedMaPhong.equals(room.getMaPhong());
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(100, 72);
        card.setMinSize(100, 72);
        card.setMaxSize(100, 72);
        card.setCursor(Cursor.HAND);
        card.setPadding(new Insets(6));
        card.setStyle("-fx-background-color: " + (isSelected ? C_SOFT_BLUE : C_CARD) + ";" +
                "-fx-border-color: " + (isSelected ? C_SIDEBAR : C_BORDER) + ";" +
                "-fx-border-width: " + (isSelected ? "2" : "1") + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;");

        Label lblMa = new Label(room.getMaPhong());
        lblMa.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblMa.setTextFill(Color.web(isSelected ? C_SIDEBAR : C_TEXT_DARK));

        String guest = datPhongDAO.getTenKhachHienTai(room.getMaPhong());
        Label lblGuest = new Label(guest != null ? guest : "...");
        lblGuest.setFont(Font.font("Segoe UI", 10));
        lblGuest.setTextFill(Color.web(C_TEXT_GRAY));
        lblGuest.setWrapText(true);
        lblGuest.setMaxWidth(86);
        lblGuest.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lblMa, lblGuest);
        card.setOnMouseClicked(e -> {
            if (!selectedMaPhong.equals(room.getMaPhong())) {
                selectedMaPhong = room.getMaPhong();
                usedServicesExpanded = false;
                lblRoomTitle.setText("Hóa đơn: phòng - " + selectedMaPhong);
                renderRooms();
                updateBillUI();
            }
        });
        return card;
    }

    private void refreshServices() {
        displayedServices.clear();
        activePrices.clear();
        activePrices.putAll(bangGiaDAO.getActivePriceMap());

        String keyword = normalize(txtServiceSearch == null ? "" : txtServiceSearch.getText());
        List<DichVu> services;
        if (currentCategory == null) {
            services = dichVuDAO.getAllActive();
        } else {
            services = dichVuDAO.getActiveByType(currentCategory.getMaLoaiDV());
        }

        for (DichVu dv : services) {
            Double price = activePrices.get(dv.getMaDV());
            if (price != null) {
                dv.setGia(price);
                if (keyword.isEmpty() || normalize(dv.getTenDV()).contains(keyword)) {
                    displayedServices.add(dv);
                }
            }
        }

        lblServiceCount.setText(displayedServices.size() + " dịch vụ");
        renderServices();
    }

    private void refreshCategoryOptions() {
        cboCategory.getItems().clear();
        cboCategory.getItems().add("Tất cả");
        for (LoaiDichVu category : categories) {
            cboCategory.getItems().add(category.getTenLoaiDV());
        }
        cboCategory.setValue("Tất cả");
    }

    private void updateBillUI() {
        billContainer.getChildren().clear();
        double total = 0;

        if (selectedMaPhong != null && !selectedMaPhong.isEmpty()) {
            String maCTDP = datPhongDAO.getMaCTDPDangSuDungByMaPhong(selectedMaPhong);
            if (maCTDP != null) {
                List<DichVuSuDung> usedList = dvsdDAO.findByMaCTDP(maCTDP);
                if (!usedList.isEmpty()) {
                    double subTotalUsed = 0;
                    for (DichVuSuDung sd : usedList) {
                        subTotalUsed += sd.getGiaDV() * sd.getSoLuong();
                    }
                    total += subTotalUsed;

                    HBox usedHeaderRow = new HBox(6);
                    usedHeaderRow.setAlignment(Pos.CENTER_LEFT);
                    usedHeaderRow.setPadding(new Insets(14, 20, 10, 20));
                    usedHeaderRow.setCursor(Cursor.HAND);
                    usedHeaderRow.setStyle("-fx-background-color: #f0f4ff;" +
                            "-fx-border-color: transparent transparent " + C_BORDER + " transparent;" +
                            "-fx-border-width: 0 0 1 0;");

                    Label lblArrow = new Label(usedServicesExpanded ? "v" : ">");
                    lblArrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
                    lblArrow.setTextFill(Color.web(C_SIDEBAR));

                    Label lblUsedTitle = new Label("Dịch vụ đã sử dụng (" + usedList.size() + " món)");
                    lblUsedTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                    lblUsedTitle.setTextFill(Color.web(C_SIDEBAR));
                    HBox.setHgrow(lblUsedTitle, Priority.ALWAYS);

                    Label lblUsedSum = new Label(String.format("%,.0f đồng", subTotalUsed));
                    lblUsedSum.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                    lblUsedSum.setTextFill(Color.web(C_TEXT_GRAY));

                    usedHeaderRow.getChildren().addAll(lblArrow, lblUsedTitle, lblUsedSum);
                    usedHeaderRow.setOnMouseClicked(e -> {
                        usedServicesExpanded = !usedServicesExpanded;
                        updateBillUI();
                    });

                    billContainer.getChildren().add(usedHeaderRow);

                    if (usedServicesExpanded) {
                        billContainer.getChildren().add(buildBillHeader(false));
                        for (DichVuSuDung sd : usedList) {
                            double sub = sd.getGiaDV() * sd.getSoLuong();
                            billContainer.getChildren()
                                    .add(buildBillRow(sd.getDichVu().getTenDV(), sd.getSoLuong(), sub, false, null));
                        }
                    }
                }
            }
        }

        if (!cart.isEmpty()) {
            HBox newHeaderRow = new HBox(6);
            newHeaderRow.setAlignment(Pos.CENTER_LEFT);
            newHeaderRow.setPadding(new Insets(14, 20, 10, 20));
            newHeaderRow.setStyle("-fx-background-color: #f0fdf4;" +
                    "-fx-border-color: transparent transparent " + C_BORDER + " transparent;" +
                    "-fx-border-width: 0 0 1 0;");

            Label lblNewTitle = new Label("Dịch vụ thêm mới (" + cart.size() + " món)");
            lblNewTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            lblNewTitle.setTextFill(Color.web("#166534"));
            HBox.setHgrow(lblNewTitle, Priority.ALWAYS);
            newHeaderRow.getChildren().add(lblNewTitle);
            billContainer.getChildren().add(newHeaderRow);

            billContainer.getChildren().add(buildBillHeader(true));

            double subTotalNew = 0;
            for (Map.Entry<DichVu, Integer> entry : cart.entrySet()) {
                DichVu dv = entry.getKey();
                int qty = entry.getValue();
                double price = dv.getGia() != null ? dv.getGia() : 0;
                double sub = price * qty;
                subTotalNew += sub;
                billContainer.getChildren().add(buildBillRow(dv.getTenDV(), qty, sub, true, dv));
            }
            total += subTotalNew;
        }

        if (billContainer.getChildren().isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(36, 20, 36, 20));

            Label title = new Label("Chưa có dữ liệu để hiển thị");
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
            title.setTextFill(Color.web(C_TEXT_DARK));

            Label note = new Label("Chọn phòng bên trái rồi thêm dịch vụ để xem phiếu.");
            note.setWrapText(true);
            note.setTextFill(Color.web(C_TEXT_GRAY));
            note.setFont(Font.font("Segoe UI", 12));

            emptyState.getChildren().addAll(title, note);
            billContainer.getChildren().add(emptyState);
        }

        lblTotal.setText(String.format("Tổng cộng: %,.0f đồng", total));
        lblTotal.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
    }

    private HBox buildBillHeader(boolean showDelete) {
        HBox header = new HBox(0);
        header.setPadding(new Insets(5, 10, 5, 15));
        header.setStyle("-fx-border-color: transparent transparent #f3f4f6 transparent; -fx-border-width: 0 0 1 0;");

        Label lblName = new Label("Dịch vụ");
        lblName.setPrefWidth(140);

        Label lblQty = new Label("SL");
        lblQty.setPrefWidth(86);
        lblQty.setAlignment(Pos.CENTER);

        Label lblPrice = new Label("Thành tiền");
        lblPrice.setPrefWidth(80);
        lblPrice.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(lblName, lblQty, lblPrice);

        if (showDelete) {
            Label lblDel = new Label("Xóa");
            lblDel.setPrefWidth(40);
            lblDel.setPadding(new Insets(0, 0, 0, 15));
            lblDel.setAlignment(Pos.CENTER_LEFT);
            header.getChildren().add(lblDel);
        } else {
            Label placeholder = new Label("");
            placeholder.setPrefWidth(40);
            header.getChildren().add(placeholder);
        }

        for (javafx.scene.Node n : header.getChildren()) {
            if (n instanceof Label) {
                Label l = (Label) n;
                l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                l.setTextFill(Color.web(C_TEXT_GRAY));
            }
        }
        return header;
    }

    private HBox buildBillRow(String name, int qty, double sub, boolean showDelete, DichVu dv) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 10, 10, 15));
        row.setStyle("-fx-border-color: transparent transparent #f9fafb transparent; -fx-border-width: 0 0 1 0;");

        Label lblName = new Label(name);
        lblName.setPrefWidth(140);
        lblName.setFont(Font.font("Segoe UI", 13));
        lblName.setTextFill(Color.web(C_TEXT_DARK));

        row.getChildren().add(lblName);

        if (!showDelete) { // Dịch vụ đã dùng (readonly)
            Label lblQty = new Label(String.valueOf(qty));
            lblQty.setPrefWidth(86);
            lblQty.setAlignment(Pos.CENTER);
            lblQty.setFont(Font.font("Segoe UI", 13));
            row.getChildren().add(lblQty);
        } else { // Dịch vụ thêm mới (editable)
            HBox qtyBox = new HBox(3);
            qtyBox.setPrefWidth(86);
            qtyBox.setAlignment(Pos.CENTER);

            Button btnMinus = new Button("-");
            styleQtyBtn(btnMinus, "white", C_TEXT_GRAY);

            TextField txtQty = new TextField(String.valueOf(qty));
            txtQty.setPrefWidth(26);
            txtQty.setMinWidth(26);
            txtQty.setMaxWidth(26);
            txtQty.setPrefHeight(24);
            txtQty.setMinHeight(24);
            txtQty.setMaxHeight(24);
            txtQty.setAlignment(Pos.CENTER);
            txtQty.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11;" +
                    "-fx-text-fill: " + C_SIDEBAR + "; -fx-background-color: #f0f4ff;" +
                    "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 4; -fx-background-radius: 4;" +
                    "-fx-alignment: center; -fx-padding: 0;");

            Button btnPlus = new Button("+");
            styleQtyBtn(btnPlus, C_ACTIVE, "white");

            Runnable commitQty = () -> {
                String text = txtQty.getText().trim();
                try {
                    int val = text.isEmpty() ? 0 : Integer.parseInt(text);
                    if (val <= 0) {
                        cart.remove(dv);
                    } else {
                        cart.put(dv, val);
                    }
                    updateBillUI();
                } catch (NumberFormatException ex) {
                    txtQty.setText(String.valueOf(cart.getOrDefault(dv, 0)));
                }
            };

            txtQty.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    txtQty.setText(newVal.replaceAll("[^\\d]", ""));
                }
            });

            txtQty.focusedProperty().addListener((obs, oldVal, focused) -> {
                if (!focused) {
                    commitQty.run();
                }
            });
            txtQty.setOnAction(e -> commitQty.run());

            btnMinus.setOnAction(e -> {
                int q = cart.getOrDefault(dv, 0);
                if (q > 1) {
                    cart.put(dv, q - 1);
                } else {
                    cart.remove(dv);
                }
                updateBillUI();
            });

            btnPlus.setOnAction(e -> {
                cart.put(dv, cart.getOrDefault(dv, 0) + 1);
                updateBillUI();
            });

            qtyBox.getChildren().addAll(btnMinus, txtQty, btnPlus);
            row.getChildren().add(qtyBox);
        }

        Label lblSub = new Label(String.format("%,.0f", sub));
        lblSub.setPrefWidth(80);
        lblSub.setAlignment(Pos.CENTER_RIGHT);
        lblSub.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblSub.setTextFill(Color.web(C_SIDEBAR));

        row.getChildren().add(lblSub);

        if (showDelete) {
            StackPane delPane = new StackPane();
            delPane.setPrefWidth(40);
            delPane.setPadding(new Insets(0, 0, 0, 15));
            delPane.setAlignment(Pos.CENTER_LEFT);

            Button btn = new Button("x");
            btn.setPrefSize(22, 22);
            btn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white;" +
                    "-fx-background-radius: 6; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                cart.remove(dv);
                updateBillUI();
            });
            delPane.getChildren().add(btn);
            row.getChildren().add(delPane);
        } else {
            StackPane placeholder = new StackPane();
            placeholder.setPrefWidth(40);
            row.getChildren().add(placeholder);
        }

        return row;
    }

    private void handleConfirm() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn dịch vụ", "Vui lòng thêm ít nhất một dịch vụ vào đơn!");
            return;
        }
        if (selectedMaPhong.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn phòng",
                    "Bạn đã chọn " + cart.size() + " dịch vụ. Vui lòng chọn phòng ở bên trái để xác nhận!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xác nhận thêm " + cart.size() + " dịch vụ vào phòng " + selectedMaPhong + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (datPhongDAO.saveServiceOrder(selectedMaPhong, cart)) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công!",
                            "Đã lưu dịch vụ vào phòng " + selectedMaPhong);
                    cart.clear();
                    updateBillUI();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi lưu dữ liệu",
                            "Vui lòng kiểm tra kết nối CSDL!");
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String header, String msg) {
        Alert a = new Alert(type);
        a.setTitle(type == Alert.AlertType.ERROR ? "Lỗi" : "Thông báo");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private void renderServices() {
        if (serviceTilePane == null) {
            return;
        }
        serviceTilePane.getChildren().clear();
        for (DichVu dv : displayedServices) {
            serviceTilePane.getChildren().add(buildServiceCard(dv));
        }
    }

    private HBox buildServiceCard(DichVu item) {
        HBox card = new HBox(12);
        card.setPrefSize(230, 85);
        card.setMaxWidth(230);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + C_BORDER +
                "; -fx-border-radius: 12; -fx-background-radius: 12;");
        card.setEffect(new DropShadow(6, 0, 2, Color.web("#00000005")));

        // Hover effect for card
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: " + C_SOFT_BLUE + "; -fx-border-color: " + C_ACTIVE +
                "; -fx-border-radius: 12; -fx-background-radius: 12;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-border-color: " + C_BORDER +
                "; -fx-border-radius: 12; -fx-background-radius: 12;"));

        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(item.getTenDV());
        lblName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblName.setTextFill(Color.web(C_TEXT_DARK));

        Label lblPrice = new Label(item.getGia() != null ? String.format("%,.0f đồng", item.getGia()) : "---");
        lblPrice.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblPrice.setTextFill(Color.web(C_ACTIVE));
        info.getChildren().addAll(lblName, lblPrice);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblAdd = new Label("＋");
        lblAdd.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblAdd.setTextFill(Color.web(C_ACTIVE));

        card.getChildren().addAll(info, lblAdd);
        
        card.setOnMouseClicked(e -> {
            cart.put(item, cart.getOrDefault(item, 0) + 1);
            updateBillUI();
        });
        
        return card;
    }

    private void styleQtyBtn(Button b, String bg, String fg) {
        b.setPrefSize(24, 24);
        b.setMinSize(24, 24);
        b.setMaxSize(24, 24);
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-border-color: #f0f0f0; -fx-border-radius: 5; -fx-background-radius: 5;" +
                " -fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 0;");
    }
}
