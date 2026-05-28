package gui;

import dao.BangGiaDichVuDAO;
import dao.DichVuDAO;
import model.entities.BangGiaDichVu;
import model.entities.BangGiaDichVu_ChiTiet;
import model.entities.DichVu;
import model.utils.DimOverlay;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.util.StringConverter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

/**
 * ThemSuaBangGiaDialog – JavaFX
 * Thay thế ThemBangGiaDialog và SuaBangGiaDialog (Swing).
 * 
 * FIX:
 * - Thêm CheckBox trạng thái (bật/tắt áp dụng)
 * - Sinh mã bảng giá tuần tự (BG001, BG002...)
 * - Kiểm tra xung đột thời gian trước khi lưu
 * - Validate ngày áp dụng (không cho chọn quá khứ khi tạo mới)
 */
public class ThemSuaBangGiaDialog {

    private static final String C_BG = "#f8f9fa";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_NAVY = "#1e3a8a";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Window owner;
    private final BangGiaDichVu bangGia; // null = thêm mới
    private final List<BangGiaDichVu_ChiTiet> dsChiTietGoc;
    private final Runnable onSuccess;
    private final boolean isEdit;
    private Button btnSubmit;

    // --- Dirty Tracking ---
    private String initTen;
    private java.time.LocalDate initAD, initHH;
    private final Map<String, String> initPrices = new HashMap<>();

    // Controls
    private TextField txtMaBG, txtTenBG;
    private model.utils.DatePicker dpNgayAD, dpNgayHH;
    private TableView<Object[]> table; // [maDV, tenDV, loai, giaGoc, giaApDung]
    private ObservableList<Object[]> tableData;
    private FilteredList<Object[]> filteredData;
    private TextField txtSearch;

    public ThemSuaBangGiaDialog(Window owner, BangGiaDichVu bangGia,
            List<BangGiaDichVu_ChiTiet> dsChiTiet, Runnable onSuccess) {
        this.owner = owner;
        this.bangGia = bangGia;
        this.dsChiTietGoc = dsChiTiet;
        this.onSuccess = onSuccess;
        this.isEdit = (bangGia != null);
    }

    public void show() {
        Region overlay = DimOverlay.show(owner);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Chỉnh sửa bảng giá" : "Tạo bảng giá mới");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null)
            dialog.initOwner(owner);
        dialog.setResizable(true);

        // FIX: Tăng PrefWidth lên 1100 để các cột không bị hiện "..."
        dialog.getDialogPane().setContent(buildContent());
        dialog.getDialogPane().setStyle("-fx-padding: 0;");
        dialog.getDialogPane().setPrefWidth(1000);
        dialog.getDialogPane().setMinWidth(1000);
        dialog.getDialogPane().setPrefHeight(700);

        ButtonType btSubmit = new ButtonType(isEdit ? "💾 Cập nhật" : "💾 Thêm mới", ButtonBar.ButtonData.OK_DONE);
        ButtonType btCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btSubmit, btCancel);
        styleDialogButtons(dialog, btSubmit, btCancel);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(btSubmit);
        this.btnSubmit = okBtn;

        if (isEdit) {
            captureInitialState();
            setupDirtyTracking();
            btnSubmit.setDisable(true); // Ban đầu chưa có thay đổi
        }

        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateAndSubmit())
                event.consume();
        });

        dialog.showAndWait();
        DimOverlay.hide(owner, overlay);
    }

    private void captureInitialState() {
        if (!isEdit)
            return;
        initTen = txtTenBG.getText().trim();
        initAD = dpNgayAD.getValue();
        initHH = dpNgayHH.getValue();

        initPrices.clear();
        for (Object[] row : tableData) {
            String ma = str(row[0]);
            String price = str(row[3]).replaceAll("[^\\d]", "");
            initPrices.put(ma, price);
        }
    }

    private void setupDirtyTracking() {
        txtTenBG.textProperty().addListener((o, ov, nv) -> checkChanges());
        dpNgayAD.valueProperty().addListener((o, ov, nv) -> checkChanges());
        dpNgayHH.valueProperty().addListener((o, ov, nv) -> checkChanges());
        // Lắng nghe sự thay đổi của danh sách (thêm/xóa dòng)
        tableData.addListener((javafx.collections.ListChangeListener<Object[]>) c -> checkChanges());
    }

    private void checkChanges() {
        if (!isEdit || btnSubmit == null)
            return;

        boolean changed = false;

        // 1. Kiểm tra thông tin chung
        if (!txtTenBG.getText().trim().equals(initTen))
            changed = true;
        if (!Objects.equals(dpNgayAD.getValue(), initAD))
            changed = true;
        if (!Objects.equals(dpNgayHH.getValue(), initHH))
            changed = true;

        if (!changed) {
            // 2. Kiểm tra bảng giá
            if (tableData.size() != initPrices.size()) {
                changed = true;
            } else {
                for (Object[] row : tableData) {
                    String ma = str(row[0]);
                    String currentPrice = str(row[3]).replaceAll("[^\\d]", "");
                    String initialPrice = initPrices.get(ma);
                    if (initialPrice == null || !currentPrice.equals(initialPrice)) {
                        changed = true;
                        break;
                    }
                }
            }
        }

        btnSubmit.setDisable(!changed);
    }

    @SuppressWarnings("unchecked")
    private VBox buildContent() {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: white;");
        content.setMaxWidth(Double.MAX_VALUE);

        // ── 1. Header ───────────────────────────────────────────────
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(22, 28, 16, 28));
        headerBox.setStyle("-fx-background-color: " + C_NAVY + ";");
        Label dlgTitle = new Label(isEdit ? "Chỉnh sửa bảng giá dịch vụ" : "Tạo bảng giá dịch vụ mới");
        dlgTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        dlgTitle.setTextFill(Color.WHITE);
        Label dlgSub = new Label(
                isEdit ? "Mã bảng giá: " + bangGia.getMaBangGia() : "Thiết lập đợt giá mới cho các dịch vụ");
        dlgSub.setFont(Font.font("Segoe UI", 14));
        dlgSub.setTextFill(Color.web("#93c5fd"));
        Region sep = new Region();
        sep.setPrefHeight(2);
        sep.setStyle("-fx-background-color: #3b82f6;");
        headerBox.getChildren().addAll(dlgTitle, dlgSub, sep);

        // ── 2. Form thông tin chung ─────────────────────────────────
        BangGiaDichVuDAO bgDAO = new BangGiaDichVuDAO();
        String autoMa = isEdit ? bangGia.getMaBangGia() : bgDAO.generateNextMaBangGia();

        GridPane infoGrid = new GridPane();
        infoGrid.setPadding(new Insets(20, 28, 10, 28));
        infoGrid.setHgap(30);
        infoGrid.setVgap(10);
        infoGrid.setStyle("-fx-background-color: " + C_BG + ";");

        txtMaBG = new TextField(autoMa);
        txtMaBG.setEditable(false);
        txtMaBG.setStyle(
                fieldStyle() + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; -fx-font-weight: bold;");

        txtTenBG = new TextField(isEdit ? nvl(bangGia.getTenBangGia()) : "");
        txtTenBG.setPromptText("Nhập tên bảng giá...");
        txtTenBG.setStyle(fieldStyle());
        txtTenBG.setMaxWidth(Double.MAX_VALUE);

        int currentYear = LocalDate.now().getYear();
        dpNgayAD = new model.utils.DatePicker(currentYear - 5, currentYear + 15);
        dpNgayAD.setPromptText("dd/MM/yyyy");
        dpNgayAD.setMaxWidth(Double.MAX_VALUE);
        dpNgayAD.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 40;");

        dpNgayHH = new model.utils.DatePicker(currentYear - 5, currentYear + 15);
        dpNgayHH.setPromptText("dd/MM/yyyy");
        dpNgayHH.setMaxWidth(Double.MAX_VALUE);
        dpNgayHH.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 40;");

        if (isEdit && bangGia.getNgayApDung() != null)
            dpNgayAD.setValue(bangGia.getNgayApDung().toLocalDate());
        else
            dpNgayAD.setValue(LocalDate.now());

        if (isEdit && bangGia.getNgayHetHieuLuc() != null)
            dpNgayHH.setValue(bangGia.getNgayHetHieuLuc().toLocalDate());
        else
            dpNgayHH.setValue(LocalDate.now());

        // Lock chỉ khi ngày áp dụng ở quá khứ
        if (isEdit && bangGia.getNgayApDung() != null
                && bangGia.getNgayApDung().toLocalDate().isBefore(LocalDate.now())) {
            dpNgayAD.setDisable(true);
            dpNgayAD.setStyle(dpNgayAD.getStyle() + "-fx-opacity: 0.7;");
        }

        // Khống chế ngày áp dụng >= hôm nay
        dpNgayAD.setMinDate(LocalDate.now());

        dpNgayAD.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && dpNgayHH.getValue() != null && dpNgayHH.getValue().isBefore(newV)) {
                dpNgayHH.setValue(newV.plusDays(1));
            }
            refreshDateHHConstraint();
        });
        refreshDateHHConstraint(); // Khởi tạo ban đầu

        infoGrid.add(makeLbl("Mã bảng giá"), 0, 0);
        infoGrid.add(txtMaBG, 1, 0);
        infoGrid.add(makeLbl("Tên bảng giá *"), 2, 0);
        infoGrid.add(txtTenBG, 3, 0);
        infoGrid.add(makeLbl("Ngày áp dụng *"), 0, 1);
        infoGrid.add(dpNgayAD, 1, 1);
        infoGrid.add(makeLbl("Ngày hết hạn *"), 2, 1);
        infoGrid.add(dpNgayHH, 3, 1);

        ColumnConstraints ccLabel = new ColumnConstraints(120);
        ColumnConstraints ccField = new ColumnConstraints();
        ccField.setHgrow(Priority.ALWAYS);
        infoGrid.getColumnConstraints().addAll(ccLabel, ccField, ccLabel, ccField);

        // ── 3. Bảng chi tiết giá (TĂNG CHIỀU CAO SEARCH, GIẢM FOOTER) ──────
        VBox tableSection = new VBox(10);
        // GIẢM PADDING DƯỚI (chỉ để 5 thay vì 20)
        tableSection.setPadding(new Insets(10, 28, 3, 28));
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        HBox tableToolbar = new HBox(15);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblTableTitle = new Label("DANH SÁCH DỊCH VỤ");
        lblTableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblTableTitle.setTextFill(Color.web(C_NAVY));

        txtSearch = new TextField();
        txtSearch.setPromptText("🔍 Tìm nhanh theo mã, tên, loại dịch vụ...");
        // TĂNG CHIỀU CAO LÊN 50, FONT SIZE 16px
        txtSearch.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 16px; -fx-pref-height: 60; " +
                "-fx-background-radius: 25; -fx-border-radius: 25; -fx-border-color: " + C_BORDER + "; " +
                "-fx-padding: 0 25; -fx-background-color: white;");
        txtSearch.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter(newV));

        Button btnAddService = new Button("＋ Chọn dịch vụ");
        btnAddService.setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand;");
        btnAddService
                .setOnMouseEntered(e -> btnAddService.setStyle(btnAddService.getStyle().replace(C_BLUE, C_BLUE_HOVER)));
        btnAddService
                .setOnMouseExited(e -> btnAddService.setStyle(btnAddService.getStyle().replace(C_BLUE_HOVER, C_BLUE)));
        btnAddService.setOnAction(e -> showServicePicker());

        tableToolbar.getChildren().addAll(lblTableTitle, txtSearch, btnAddService);

        tableData = FXCollections.observableArrayList();
        loadAllServices();
        filteredData = new FilteredList<>(tableData, p -> true);
        table = new TableView<>(filteredData);
        table.setPlaceholder(new Label("Không có dữ liệu"));
        table.setStyle("-fx-background-color: white; -fx-border-color: " + C_BORDER + "; -fx-border-radius: 8;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Định nghĩa cột (Bỏ cột Giá gốc, chỉ giữ Giá áp dụng)
        TableColumn<Object[], String> cMa = col("Mã DV", 0, false, "-fx-alignment: CENTER; -fx-font-weight: bold;");
        TableColumn<Object[], String> cTen = col("Tên dịch vụ", 1, false, null);
        TableColumn<Object[], String> cLoai = col("Loại", 2, false, "-fx-alignment: CENTER;");
        TableColumn<Object[], String> cGiaAD = new TableColumn<>("Giá áp dụng (đ)");
        cGiaAD.setReorderable(false);
        cGiaAD.setSortable(false);

        cMa.setMaxWidth(1f * Integer.MAX_VALUE * 10);
        cTen.setMaxWidth(1f * Integer.MAX_VALUE * 45);
        cLoai.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        cGiaAD.setMaxWidth(1f * Integer.MAX_VALUE * 30);

        // Logic CellFactory giữ nguyên
        cGiaAD.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: " + C_BLUE + ";");
        cGiaAD.setCellValueFactory(
                p -> new SimpleStringProperty(p.getValue()[3] != null ? p.getValue()[3].toString() : ""));
        cGiaAD.setCellFactory(tc -> new TableCell<>() {
            private final TextField tf = new TextField();
            private boolean updating = false;
            {
                tf.setStyle("-fx-background-color: transparent; -fx-alignment: CENTER-RIGHT;");

                // Chỉ cho phép nhập số và dấu phẩy
                tf.setTextFormatter(new TextFormatter<>(change -> {
                    if (change.getControlNewText().matches("[\\d,]*")) {
                        return change;
                    }
                    return null;
                }));
            }

            @Override
            public void startEdit() {
                super.startEdit();
                setGraphic(tf);
                setText(null);
                String raw = getItem() != null ? getItem().replaceAll("[^\\d]", "") : "";
                tf.setText(!raw.isEmpty() ? String.format("%,d", Long.parseLong(raw)) : "");
                tf.requestFocus();
                tf.selectAll();
                tf.setOnAction(e -> commitEdit(tf.getText()));
                tf.focusedProperty().addListener((o, ov, nv) -> {
                    if (!nv)
                        commitEdit(tf.getText());
                });
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem());
            }

            @Override
            public void commitEdit(String newVal) {
                String digits = newVal != null ? newVal.replaceAll("[^\\d]", "") : "";
                String display = digits.isEmpty() ? "" : String.format("%,d", Long.parseLong(digits));
                super.commitEdit(display);
                getTableRow().getItem()[3] = display;
                setGraphic(null);
                setText(display);
                checkChanges(); // Cập nhật trạng thái nút
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEditing()) {
                    setGraphic(null);
                    setText(empty ? null : item);
                }
            }
        });

        table.getColumns().setAll(cMa, cTen, cLoai, cGiaAD);
        table.setEditable(true);

        // Thiết lập ContextMenu xóa dịch vụ khi ở chế độ Sửa
        table.setRowFactory(tv -> {
            TableRow<Object[]> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Xóa dịch vụ khỏi bảng giá này");
            deleteItem.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            deleteItem.setOnAction(event -> {
                Object[] rowData = row.getItem();
                if (rowData != null) {
                    handleDeleteServiceFromList(rowData);
                }
            });
            contextMenu.getItems().add(deleteItem);

            if (isEdit) {
                row.contextMenuProperty().bind(
                        Bindings.when(row.emptyProperty())
                                .then((ContextMenu) null)
                                .otherwise(contextMenu));
            }
            return row;
        });

        Label lblHint = new Label("💡 Gợi ý: Click đúp vào ô ở cột \"Giá áp dụng\" để thay đổi giá.");
        lblHint.setFont(Font.font("Segoe UI", 12));
        lblHint.setTextFill(Color.web(C_TEXT_GRAY));

        tableSection.getChildren().addAll(tableToolbar, table, lblHint);
        content.getChildren().addAll(headerBox, infoGrid, tableSection);

        return content;
    }

    private void applyFilter(String kw) {
        if (kw == null || kw.trim().isEmpty()) {
            filteredData.setPredicate(p -> true);
        } else {
            String lower = kw.trim().toLowerCase();
            filteredData.setPredicate(row -> {
                String ma = str(row[0]).toLowerCase();
                String ten = str(row[1]).toLowerCase();
                String loai = str(row[2]).toLowerCase();
                return ma.contains(lower) || ten.contains(lower) || loai.contains(lower);
            });
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private void refreshDateHHConstraint() {
        LocalDate minFromAD = dpNgayAD.getValue() != null ? dpNgayAD.getValue() : LocalDate.now();
        // Ngày hết hạn phải >= max(ngày áp dụng, hôm nay)
        LocalDate minDate = minFromAD.isBefore(LocalDate.now()) ? LocalDate.now() : minFromAD;
        dpNgayHH.setValue(minDate.plusDays(1));
    }

    /**
     * Load tất cả dịch vụ vào bảng.
     * - Nếu sửa: giữ lại giá áp dụng cũ
     * - Nếu thêm mới: lấy giá từ bảng giá đang Active, nếu không có thì lấy giá gốc
     */
    private void loadAllServices() {
        tableData.clear();
        DichVuDAO dvDAO = new DichVuDAO();
        List<DichVu> allDV = dvDAO.getAllActive();
        Map<String, DichVu> dvMap = toDichVuMap(allDV);

        Map<String, String> giaApDungMap = new HashMap<>();

        if (isEdit && dsChiTietGoc != null) {
            for (BangGiaDichVu_ChiTiet ct : dsChiTietGoc) {
                giaApDungMap.put(ct.getMaDichVu().getMaDV(),
                        String.valueOf(ct.getGiaDichVu() != null ? ct.getGiaDichVu().longValue() : 0L));
            }
        } else {
            // Thêm mới: lấy giá từ bảng giá đang Active
            BangGiaDichVuDAO bgDAO = new BangGiaDichVuDAO();
            Map<String, Double> activePrices = bgDAO.getActivePriceMap();
            for (Map.Entry<String, Double> entry : activePrices.entrySet()) {
                giaApDungMap.put(entry.getKey(), String.valueOf((long) entry.getValue().doubleValue()));
            }
        }

        tableData.clear();
        if (isEdit && dsChiTietGoc != null) {
            // Chỉ nạp những dịch vụ đã có trong chi tiết của bảng giá này
            for (BangGiaDichVu_ChiTiet ct : dsChiTietGoc) {
                DichVu dv = ct.getMaDichVu();
                DichVu fullDv = dvMap.getOrDefault(dv.getMaDV(), dv);

                String rawGia = String.valueOf(ct.getGiaDichVu() != null ? ct.getGiaDichVu().longValue() : 0L);
                String formatted;
                try {
                    long num = Long.parseLong(rawGia.replaceAll("[^\\d]", ""));
                    formatted = String.format("%,d", num);
                } catch (NumberFormatException e) {
                    formatted = rawGia;
                }

                tableData.add(new Object[] {
                        fullDv.getMaDV(),
                        fullDv.getTenDV(),
                        getDisplayLoaiDV(fullDv),
                        formatted
                });
            }
        }
        // Nếu không phải Edit (Thêm mới), tableData sẽ để trống để User tự thêm.
    }

    private boolean validateAndSubmit() {
        // Validate tên bảng giá
        if (txtTenBG.getText().trim().isEmpty()) {
            showError("Lỗi", "Tên bảng giá không được để trống!");
            return false;
        }
        // Validate ngày
        if (dpNgayAD.getValue() == null || dpNgayHH.getValue() == null) {
            showError("Lỗi", "Vui lòng chọn đủ ngày áp dụng và hết hạn!");
            return false;
        }
        if (dpNgayHH.getValue().isBefore(dpNgayAD.getValue())) {
            showError("Lỗi", "Ngày hết hạn phải sau ngày áp dụng!");
            return false;
        }

        // Parse chi tiết giá từ cột giá áp dụng (index [3])
        List<BangGiaDichVu_ChiTiet> dsGia = new ArrayList<>();
        for (Object[] row : tableData) {
            String rawGia = row[3] != null ? row[3].toString().trim() : "";
            if (!rawGia.isEmpty()) {
                try {
                    String digitsGia = rawGia.replaceAll("[^\\d]", "");
                    if (digitsGia.isEmpty())
                        continue;
                    double gia = Double.parseDouble(digitsGia);
                    if (gia > 0) {
                        BangGiaDichVu_ChiTiet ct = new BangGiaDichVu_ChiTiet();
                        ct.setMaDichVu(new DichVu(row[0].toString()));
                        ct.setGiaDichVu(gia);
                        ct.setDonViTinh("Cái");
                        dsGia.add(ct);
                    }
                } catch (NumberFormatException e) {
                    showError("Lỗi giá", "Giá tại dịch vụ \"" + row[1] + "\" không hợp lệ!");
                    return false;
                }
            }
        }
        if (dsGia.isEmpty()) {
            showError("Thiếu thông tin", "Phải nhập giá cho ít nhất 1 dịch vụ!");
            return false;
        }

        // Build entity
        BangGiaDichVu bg = new BangGiaDichVu();
        String currentMa = txtMaBG.getText().trim();

        BangGiaDichVuDAO bgDAO = new BangGiaDichVuDAO();

        // Kiểm tra tránh trùng mã (vòng lặp để đảm bảo mã duy nhất tuyệt đối)
        int safety = 0;
        while (!isEdit && bgDAO.exists(currentMa) && safety < 10) {
            currentMa = bgDAO.generateNextMaBangGia();
            safety++;
        }
        txtMaBG.setText(currentMa); // Cập nhật UI để người dùng thấy mã thực tế được lưu

        bg.setMaBangGia(currentMa);
        bg.setTenBangGia(txtTenBG.getText().trim());
        bg.setNgayApDung(Date.valueOf(dpNgayAD.getValue()));
        bg.setNgayHetHieuLuc(Date.valueOf(dpNgayHH.getValue()));

        // Lưu/Cập nhật
        boolean ok;
        if (isEdit) {
            ok = bgDAO.updateFullBangGia(bg, dsGia);
            if (ok)
                showInfo("Cập nhật thành công!", "Bảng giá " + bg.getMaBangGia() + " đã được cập nhật.");
            else {
                showError("Thất bại", "Không thể cập nhật. Kiểm tra kết nối CSDL.");
                return false;
            }
        } else {
            ok = bgDAO.insertFullBangGia(bg, dsGia);
            if (ok)
                showInfo("Thêm thành công!", "Bảng giá " + bg.getMaBangGia() + " đã được tạo.");
            else {
                showError("Thất bại", "Lỗi hệ thống khi lưu bảng giá!");
                return false;
            }
        }

        // Đồng bộ giá active vào bảng DV
        bgDAO.syncActivePricesToDB();

        if (onSuccess != null)
            onSuccess.run();
        return true;
    }

    // Utilities
    private TableColumn<Object[], String> col(String header, int idx, boolean editable, String style) {
        TableColumn<Object[], String> c = new TableColumn<>(header);
        c.setCellValueFactory(p -> {
            Object v = p.getValue()[idx];
            return new SimpleStringProperty(v != null ? v.toString() : "");
        });
        c.setEditable(editable);
        if (style != null)
            c.setStyle(style);
        c.setReorderable(false);
        c.setSortable(false);
        return c;
    }

    private Label makeLbl(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(C_TEXT_DARK));
        return l;
    }

    private String fieldStyle() {
        return "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px; -fx-pref-height: 40;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + C_BORDER + ";";
    }

    private void styleDialogButtons(Dialog<?> dialog, ButtonType submit, ButtonType cancel) {
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(submit);
        okBtn.setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-font-weight: bold;" +
                "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(okBtn.getStyle().replace(C_BLUE, C_BLUE_HOVER)));
        okBtn.setOnMouseExited(e -> okBtn.setStyle(okBtn.getStyle().replace(C_BLUE_HOVER, C_BLUE)));
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancel);
        cancelBtn.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: " + C_TEXT_DARK + ";" +
                "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
    }

    private void showInfo(String h, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Thông báo");
        a.setHeaderText(h);
        a.setContentText(m);
        a.showAndWait();
    }

    private void showError(String h, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Lỗi");
        a.setHeaderText(h);
        a.setContentText(m);
        a.showAndWait();
    }

    // ── 4. Phương thức chọn dịch vụ hoàn chỉnh ──────────────────────
    private void showServicePicker() {
        Dialog<List<DichVu>> picker = new Dialog<>();
        picker.setTitle("Chọn dịch vụ thêm vào bảng giá");
        picker.initOwner(owner);
        picker.initModality(Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setPrefWidth(600);

        TextField search = new TextField();
        search.setPromptText("Tìm dịch vụ...");
        search.setStyle("-fx-background-radius: 15; -fx-padding: 8 15;");

        DichVuDAO dvDAO = new DichVuDAO();
        List<DichVu> all = dvDAO.getAllActive();

        // Lọc bỏ những dịch vụ đã có sẵn trong bảng chính
        Set<String> existingIds = new LinkedHashSet<>();
        for (Object[] row : tableData) {
            existingIds.add(row[0].toString());
        }

        List<DichVu> available = new ArrayList<>();
        for (DichVu d : all) {
            if (!existingIds.contains(d.getMaDV())) {
                available.add(d);
            }
        }

        ObservableList<DichVuSelection> pickerData = FXCollections.observableArrayList();
        for (DichVu d : available) {
            pickerData.add(new DichVuSelection(d));
        }

        FilteredList<DichVuSelection> filteredPicker = new FilteredList<>(pickerData, p -> true);
        search.textProperty().addListener((obs, oldV, newV) -> {
            filteredPicker.setPredicate(item -> {
                if (newV == null || newV.trim().isEmpty())
                    return true;
                String lower = newV.toLowerCase();
                return item.dv.getMaDV().toLowerCase().contains(lower) ||
                        item.dv.getTenDV().toLowerCase().contains(lower);
            });
        });

        TableView<DichVuSelection> pickerTable = new TableView<>(filteredPicker);
        pickerTable.setPlaceholder(new Label("Không có dịch vụ khả dụng"));
        pickerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pickerTable.setPrefHeight(400);
        pickerTable.setEditable(true);

        TableColumn<DichVuSelection, Boolean> cCheck = new TableColumn<>();
        CheckBox cbAll = new CheckBox("Chọn");
        cbAll.setStyle("-fx-font-weight: bold;");
        cbAll.setOnAction(ev -> {
            boolean sel = cbAll.isSelected();
            for (DichVuSelection item : filteredPicker) { // Chỉ chọn trên danh sách đang hiển thị
                item.selected.set(sel);
            }
        });

        cCheck.setGraphic(cbAll);
        cCheck.setCellValueFactory(p -> p.getValue().selected);
        cCheck.setCellFactory(tc -> new javafx.scene.control.cell.CheckBoxTableCell<>());
        cCheck.setEditable(true);
        cCheck.setMaxWidth(80);
        cCheck.setMinWidth(80);
        cCheck.setStyle("-fx-alignment: CENTER;");

        TableColumn<DichVuSelection, String> cId = new TableColumn<>("Mã");
        cId.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().dv.getMaDV()));

        TableColumn<DichVuSelection, String> cName = new TableColumn<>("Tên dịch vụ");
        cName.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().dv.getTenDV()));

        TableColumn<DichVuSelection, String> cType = new TableColumn<>("Loại");
        cType.setCellValueFactory(p -> new SimpleStringProperty(getDisplayLoaiDV(p.getValue().dv)));

        for (TableColumn<DichVuSelection, ?> c : List.of(cCheck, cId, cName, cType)) {
            c.setReorderable(false);
            c.setSortable(false);
        }

        pickerTable.getColumns().setAll(List.of(cCheck, cId, cName, cType));
        root.getChildren().addAll(search, pickerTable);
        picker.getDialogPane().setContent(root);

        ButtonType btnOk = new ButtonType("Thêm vào danh sách", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Bỏ qua", ButtonBar.ButtonData.CANCEL_CLOSE);
        picker.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);

        picker.setResultConverter(dialogButton -> {
            if (dialogButton == btnOk) {
                List<DichVu> selectedServices = new ArrayList<>();
                for (DichVuSelection item : pickerData) {
                    if (item.selected.get()) {
                        selectedServices.add(item.dv);
                    }
                }
                return selectedServices;
            }
            return null;
        });

        picker.showAndWait().ifPresent(selectedServices -> {
            if (!selectedServices.isEmpty()) {
                // Lấy giá mặc định từ bảng giá active hiện tại (nếu có)
                BangGiaDichVuDAO bgDAO = new BangGiaDichVuDAO();
                Map<String, Double> activePrices = bgDAO.getActivePriceMap();

                for (DichVu dv : selectedServices) {
                    Long currentPrice = 0L;
                    if (activePrices.containsKey(dv.getMaDV())) {
                        currentPrice = (long) activePrices.get(dv.getMaDV()).doubleValue();
                    }

                    String formattedPrice = currentPrice > 0 ? String.format("%,d", currentPrice) : "0";

                    tableData.add(new Object[] {
                            dv.getMaDV(),
                            dv.getTenDV(),
                            getDisplayLoaiDV(dv),
                            formattedPrice
                    });
                }
                checkChanges(); // Cập nhật trạng thái nút Submit chính
            }
        });
    }

    private Map<String, DichVu> toDichVuMap(List<DichVu> services) {
        Map<String, DichVu> map = new HashMap<>();
        for (DichVu service : services) {
            map.put(service.getMaDV(), service);
        }
        return map;
    }

    private String getDisplayLoaiDV(DichVu dv) {
        if (dv == null) {
            return "Chưa phân loại";
        }
        String tenLoai = dv.getTenLoaiDV();
        if (tenLoai != null && !tenLoai.trim().isEmpty()) {
            return tenLoai;
        }
        String maLoai = dv.getLoaiDV();
        return (maLoai == null || maLoai.trim().isEmpty()) ? "Chưa phân loại" : maLoai;
    }

    // ── 5. Xử lý xóa phần tử khỏi bảng ──────────────────────────────
    private void handleDeleteServiceFromList(Object[] rowData) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa dịch vụ khỏi bảng giá?");
        confirm.setContentText("Bạn có chắc chắn muốn bỏ dịch vụ " + rowData[1] + " (" + rowData[0]
                + ") ra khỏi đợt áp dụng giá này?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                tableData.remove(rowData);
                checkChanges(); // Kích hoạt dirty tracking
            }
        });
    }

    // ── 6. Helper Class dùng riêng cho Service Picker Dialog ──────────
    private static class DichVuSelection {
        final DichVu dv;
        final javafx.beans.property.BooleanProperty selected = new javafx.beans.property.SimpleBooleanProperty(false);

        DichVuSelection(DichVu dv) {
            this.dv = dv;
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
