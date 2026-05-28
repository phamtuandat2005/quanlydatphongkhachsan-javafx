package model.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Tiện ích tạo thẻ trạng thái (Badge) đồng nhất cho toàn hệ thống.
 */
public class BadgeUtils {

    /**
     * Tạo một thẻ trạng thái cơ bản có màu nền, màu chữ và tùy chọn mũi tên.
     *
     * @param text      Nội dung hiển thị
     * @param bgColor   Mã màu nền (HEX)
     * @param textColor Mã màu chữ (HEX)
     * @param hasArrow  Có hiện mũi tên sổ xuống hay không
     * @return HBox chứa thẻ trạng thái
     */
    public static HBox createStatusBadge(String text, String bgColor, String textColor, boolean hasArrow) {
        HBox badge = new HBox(4);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(2, 8, 2, 8));
        badge.setMaxWidth(Region.USE_PREF_SIZE);
        badge.setMaxHeight(Region.USE_PREF_SIZE);

        Label lblText = new Label(text);
        lblText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblText.setStyle("-fx-text-fill: " + textColor + ";");

        badge.getChildren().add(lblText);

        if (hasArrow) {
            badge.setCursor(Cursor.HAND);
            Label lblArrow = new Label("▾");
            lblArrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblArrow.setStyle("-fx-text-fill: " + textColor + ";");
            badge.getChildren().add(lblArrow);
        }

        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");
        
        return badge;
    }
}
