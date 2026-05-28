package model.utils;

import model.entities.HoaDon;

/**
 * Utility class to centralize revenue calculation logic.
 * Unifies the formula used across Dashboard and Invoice views.
 */
public class RevenueCalculator {

    /**
     * Calculates the actual revenue for a given invoice.
     * Formula: Revenue = Total Remaining Payment + Deposit.
     * 
     * Special Condition: Cancelled or Refunded bookings (DA_MAT_COC, DA_HOAN_COC,
     * DA_HUY)
     * are excluded from the total revenue calculation.
     * 
     * @param hd The invoice to calculate
     * @return The calculated revenue amount
     */
    public static double calculateActualRevenue(HoaDon hd) {
        if (hd == null)
            return 0;

        String status = hd.getTrangThaiThanhToan();

        if ("DA_THANH_TOAN".equals(status)) {
            return new dao.HoaDonDAO().tinhDoanhThu(hd);
        } else if ("DA_MAT_COC".equals(status)) {
            return hd.getTienCoc();
        }

        return 0;
    }
}
