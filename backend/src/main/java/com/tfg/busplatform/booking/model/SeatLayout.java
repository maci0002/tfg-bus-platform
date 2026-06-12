package com.tfg.busplatform.booking.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout fijo del autobús para el prototipo:
 *
 *      [ Conductor ]
 *
 *   1A  1B    1C  1D
 *   2A  2B    2C  2D
 *   3A  3B    3C  3D
 *   ...
 *  10A 10B   10C 10D
 *
 * 10 filas × 4 asientos = 40 plazas.
 * Pasillo central entre las columnas B y C.
 *
 * En la siguiente iteración se podrá personalizar por línea/vehículo.
 */
public final class SeatLayout {

    public static final int ROWS = 10;
    public static final char[] COLUMNS = {'A', 'B', 'C', 'D'};
    public static final int TOTAL_SEATS = ROWS * COLUMNS.length;

    private SeatLayout() {}

    /** Genera todos los códigos de asiento en orden ("1A","1B","1C","1D","2A"…). */
    public static List<String> allSeatCodes() {
        List<String> codes = new ArrayList<>(TOTAL_SEATS);
        for (int row = 1; row <= ROWS; row++) {
            for (char col : COLUMNS) {
                codes.add(row + String.valueOf(col));
            }
        }
        return codes;
    }

    /** Comprueba que un código es válido (ej. "5B"). */
    public static boolean isValidSeatCode(String code) {
        if (code == null || code.length() < 2 || code.length() > 3) return false;
        char column = code.charAt(code.length() - 1);
        String rowStr = code.substring(0, code.length() - 1);
        try {
            int row = Integer.parseInt(rowStr);
            if (row < 1 || row > ROWS) return false;
            for (char c : COLUMNS) if (c == column) return true;
            return false;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
