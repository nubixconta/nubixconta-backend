package com.nubixconta.common.util;

public class AccountingCodeFormatter {

    /**
     * Formatea un código de cuenta contable numérico a un formato con puntos,
     * siguiendo una estructura jerárquica: 1, 11, 11.01, 11.01.01, etc.
     *
     * @param rawCode El código sin formato (ej. "110101"). Debe ser numérico.
     * @return El código formateado o el código original si es nulo o no numérico.
     */
    public static String format(String rawCode) {
        if (rawCode == null || rawCode.isBlank() || !rawCode.matches("\\d+")) {
            return rawCode;
        }

        try {
            int len = rawCode.length();

            // Caso Nivel 1 (ej. "1", "2")
            if (len == 1) {
                return rawCode;
            }

            // Caso Nivel 2 (ej. "11", "12")
            if (len == 2) {
                return rawCode; // Se muestra como "11", no "1.1"
            }

            // Casos Nivel 3 en adelante (se empieza a segmentar)
            // Tomamos los dos primeros dígitos como el primer segmento.
            StringBuilder formatted = new StringBuilder(rawCode.substring(0, 2));
            int currentIndex = 2;

            // Segmentos restantes: Grupos de dos dígitos
            while (currentIndex < len) {
                int nextIndex = Math.min(currentIndex + 2, len);
                formatted.append(".").append(rawCode.substring(currentIndex, nextIndex));
                currentIndex = nextIndex;
            }

            return formatted.toString();

        } catch (Exception e) {
            return rawCode; // Falla segura
        }
    }
}