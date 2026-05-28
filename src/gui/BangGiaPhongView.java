package gui;

import dao.LoaiPhongDAO;
import model.entities.LoaiPhong;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.util.List;

public class BangGiaPhongView extends BorderPane {

    private static final String C_BG = "#f8f9fa";
    private static final String C_CARD_BG = "white";
    private static final String C_BORDER = "#e9ecef";
    private static final String C_TEXT_DARK = "#111827";
    private static final String C_TEXT_GRAY = "#6b7280";
    private static final String C_BLUE = "#1d4ed8";
    private static final String C_BLUE_HOVER = "#1e40af";
    private static final String C_SUCCESS = "#059669";
    private static final String C_ERROR = "#b91c1c";

    private final boolean isAdmin;
    private final LoaiPhongDAO dao = new LoaiPhongDAO();
    private final TableView<LoaiPhong> table = new TableView<>();
    private final Label lblMessage = new Label();
    private ObservableList<LoaiPhong> data = FXCollections.observableArrayList();

    public BangGiaPhongView(boolean isAdmin) {
        this.isAdmin = isAdmin;
        setStyle("-fx-background-color: " + C_BG + ";");
        setPadding(new Insets(28));

        setTop(buildHeader());
        setCenter(buildTableCard());
        loadData();
    }

    private VBox buildHeader() {
        VBox header = new VBox(12);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Bảng giá phòng theo loại");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));

        Label lblSubtitle = new Label("Chỉnh sửa giá phòng cho từng loại. Chỉ admin mới có thể cập nhật.");
        lblSubtitle.setFont(Font.font("Segoe UI", 14));
        lblSubtitle.setTextFill(Color.web(C_TEXT_GRAY));
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        titleRow.getChildren().add(titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        if (isAdmin) {
            Button btnAdd = new Button("＋ Thêm loại phòng");
            btnAdd.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            btnAdd.setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8;");
            btnAdd.setOnMouseEntered(e -> btnAdd.setStyle(
                    "-fx-background-color: " + C_BLUE_HOVER + "; -fx-text-fill: white; -fx-background-radius: 8;"));
            btnAdd.setOnMouseExited(e -> btnAdd.setStyle(
                    "-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8;"));
            btnAdd.setCursor(javafx.scene.Cursor.HAND);
            btnAdd.setOnAction(e -> openAddLoaiPhongDialog());

            Button btnRefresh = new Button("⟳ Tải lại");
            btnRefresh.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
            btnRefresh
                    .setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8;");
            btnRefresh.setOnMouseEntered(e -> btnRefresh.setStyle(
                    "-fx-background-color: " + C_BLUE_HOVER + "; -fx-text-fill: white; -fx-background-radius: 8;"));
            btnRefresh.setOnMouseExited(e -> btnRefresh.setStyle(
                    "-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8;"));
            btnRefresh.setCursor(javafx.scene.Cursor.HAND);
            btnRefresh.setOnAction(e -> loadData());
            titleRow.getChildren().addAll(btnAdd, btnRefresh);
        }

        lblMessage.setFont(Font.font("Segoe UI", 13));
        lblMessage.setWrapText(true);

        header.getChildren().addAll(titleRow, lblMessage);
        return header;
    }

    private VBox buildTableCard() {
        VBox card = new VBox();
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;");
        card.setEffect(new DropShadow(8, 0, 2, Color.web("#00000010")));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Không có loại phòng"));
        table.setEditable(isAdmin);

        TableColumn<LoaiPhong, String> colMa = new TableColumn<>("Mã loại");
        colMa.setMinWidth(120);
        colMa.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getMaLoaiPhong()));
        colMa.setStyle("-fx-alignment: CENTER;");
        colMa.setReorderable(false);

        TableColumn<LoaiPhong, String> colLoai = new TableColumn<>("Tên loại");
        colLoai.setMinWidth(200);
        colLoai.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().toString()));
        colLoai.setStyle("-fx-alignment: CENTER-LEFT;");
        colLoai.setReorderable(false);

        TableColumn<LoaiPhong, Double> colGia = new TableColumn<>("Giá / đêm");
        colGia.setMinWidth(180);
        colGia.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getGia()));
        colGia.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colGia.setReorderable(false);
        colGia.setOnEditCommit(event -> {
            if (!isAdmin) {
                showMessage("Bạn không có quyền cập nhật giá.", true);
                table.refresh();
                return;
            }
            LoaiPhong lp = event.getRowValue();
            Double newGia = event.getNewValue();
            if (newGia == null || newGia <= 0) {
                showMessage("Giá phòng phải lớn hơn 0.", true);
                table.refresh();
                return;
            }
            if (dao.updateGiaByMaLoai(lp.getMaLoaiPhong(), newGia)) {
                lp.setGia(newGia);
                showMessage("Cập nhật giá thành công cho loại phòng " + lp + ".", false);
            } else {
                showMessage("Không thể lưu giá, vui lòng thử lại.", true);
            }
            table.refresh();
        });
        colGia.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(colMa, colLoai, colGia);

        StackPane content = new StackPane(table);
        content.setPadding(new Insets(4, 0, 0, 0));
        card.getChildren().add(content);
        return card;
    }

    private void loadData() {
        List<LoaiPhong> list = dao.getAll();
        data = FXCollections.observableArrayList(list);
        table.setItems(data);
        showMessage("Đã tải " + list.size() + " loại phòng.", false);
    }

    private void openAddLoaiPhongDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Thêm loại phòng");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white; -fx-border-color: " + C_BORDER
                + "; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label lblTitle = new Label("Thêm loại phòng mới");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web(C_TEXT_DARK));

        TextField txtMaLoai = new TextField();
        txtMaLoai.setPromptText("Mã loại (ví dụ: SINGLE, DOUBLE)");
        txtMaLoai.setPrefWidth(320);

        TextField txtGia = new TextField();
        txtGia.setPromptText("Giá / đêm");

        TextField txtSucChua = new TextField();
        txtSucChua.setPromptText("Sức chứa");

        Label lblError = new Label();
        lblError.setTextFill(Color.web(C_ERROR));
        lblError.setWrapText(true);

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnSave = new Button("Lưu");
        btnSave.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btnSave.setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8;");
        btnSave.setOnMouseEntered(e -> btnSave.setStyle(
                "-fx-background-color: " + C_BLUE_HOVER + "; -fx-text-fill: white; -fx-background-radius: 8;"));
        btnSave.setOnMouseExited(e -> btnSave
                .setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8;"));
        btnSave.setCursor(javafx.scene.Cursor.HAND);

        Button btnCancel = new Button("Hủy");
        btnCancel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btnCancel.setStyle(
                "-fx-background-color: #e5e7eb; -fx-text-fill: " + C_TEXT_DARK + "; -fx-background-radius: 8;");
        btnCancel.setCursor(javafx.scene.Cursor.HAND);
        btnCancel.setOnAction(e -> dialog.close());

        btnSave.setOnAction(e -> {
            String maLoai = txtMaLoai.getText().trim().toUpperCase();
            String giaText = txtGia.getText().trim();
            String sucChuaText = txtSucChua.getText().trim();

            if (maLoai.isEmpty() || giaText.isEmpty() || sucChuaText.isEmpty()) {
                lblError.setText("Vui lòng nhập đủ thông tin mã loại, giá và sức chứa.");
                return;
            }
            double gia;
            int sucChua;
            try {
                gia = Double.parseDouble(giaText);
                sucChua = Integer.parseInt(sucChuaText);
            } catch (NumberFormatException ex) {
                lblError.setText("Giá và sức chứa phải là số hợp lệ.");
                return;
            }
            if (gia <= 0 || sucChua <= 0) {
                lblError.setText("Giá và sức chứa phải lớn hơn 0.");
                return;
            }
            if (dao.findByID(maLoai) != null) {
                lblError.setText("Mã loại phòng đã tồn tại.");
                return;
            }
            LoaiPhong lp = new LoaiPhong(maLoai, gia, sucChua);
            if (dao.insertLoaiPhong(lp)) {
                loadData();
                showMessage("Đã thêm loại phòng mới: " + maLoai + ".", false);
                dialog.close();
            } else {
                lblError.setText("Không thể thêm loại phòng. Vui lòng thử lại.");
            }
        });

        buttonRow.getChildren().addAll(btnCancel, btnSave);
        root.getChildren().addAll(lblTitle, txtMaLoai, txtGia, txtSucChua, lblError, buttonRow);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showMessage(String text, boolean isError) {
        lblMessage.setText(text);
        lblMessage.setTextFill(Color.web(isError ? C_ERROR : C_SUCCESS));
    }
}
