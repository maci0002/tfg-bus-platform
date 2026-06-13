package com.tfg.busplatform.transport.ctan;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Normaliza los textos crudos que devuelve la API de CTAN (nombres de línea y
 * de parada) para presentarlos de forma homogénea y legible.
 *
 * <p>La API de CTAN entrega los nombres con capitalización irregular
 * ({@code "Jaén - Sotogordo Por Vados De Torralba"}), sufijos de año
 * ({@code "(2026)"}), notas operativas embebidas en el nombre de parada
 * ({@code "Hospital Neurotrau. Solo Bajada (8 a 10 ...)"}) y acentos perdidos
 * en algunos municipios ({@code "Mengibar"}). Esta clase aplica:</p>
 * <ul>
 *   <li>Title-case con minúsculas en las preposiciones/artículos españoles.</li>
 *   <li>Eliminación de sufijos de año y notas operativas.</li>
 *   <li>Normalización del espaciado alrededor de los guiones separadores.</li>
 *   <li>Corrección de acentos en municipios conocidos del entorno de Jaén.</li>
 * </ul>
 */
public final class CtanTextNormalizer {

    private static final Locale ES = Locale.forLanguageTag("es");

    /** Palabras que se mantienen en minúscula salvo al inicio de un segmento. */
    private static final Set<String> CONNECTORS = Set.of(
            "de", "del", "la", "las", "los", "el", "lo", "y", "e", "a", "al",
            "con", "en", "por", "para", "o", "u", "sin", "sobre", "tras"
    );

    /** Correcciones de acento para municipios/núcleos del entorno de Jaén. */
    private static final Map<String, String> ACCENTS = Map.ofEntries(
            Map.entry("Jaen", "Jaén"),
            Map.entry("Mengibar", "Mengíbar"),
            Map.entry("Andujar", "Andújar"),
            Map.entry("Ubeda", "Úbeda"),
            Map.entry("Begijar", "Begíjar"),
            Map.entry("Bailen", "Bailén"),
            Map.entry("Alcala", "Alcalá"),
            Map.entry("Jodar", "Jódar"),
            Map.entry("Magina", "Mágina"),
            Map.entry("Geolit", "Geolit")
    );

    private CtanTextNormalizer() {
    }

    /** Normaliza el nombre comercial de una línea. */
    public static String lineName(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String s = raw.trim();
        s = stripYear(s);
        // Espaciado uniforme alrededor de los guiones que separan tramos.
        s = s.replaceAll("\\s*[-–]\\s*", " - ");
        s = collapse(s);
        s = titleCase(s);
        s = fixAccents(s);
        return s.isBlank() ? raw.trim() : s;
    }

    /** Normaliza el nombre de una parada. */
    public static String stopName(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String s = raw.trim();
        s = stripYear(s);
        // Notas operativas embebidas: "... Solo Bajada (8 a 10 y 15 a 16)".
        s = s.replaceAll("(?i)\\s*\\b(solo bajada|solo subida)\\b.*$", "");
        // Sufijo de sentido de una sola letra: "Campus Universitario-V".
        s = s.replaceAll("\\s*-\\s*[A-Za-z]$", "");
        s = collapse(s);
        s = titleCase(s);
        s = fixAccents(s);
        return s.isBlank() ? raw.trim() : s;
    }

    // ── Internos ────────────────────────────────────────────────────────────

    private static String stripYear(String s) {
        s = s.replaceAll("\\s*\\(\\s*20\\d{2}\\s*\\)", "");
        s = s.replaceAll("\\s+20\\d{2}$", "");
        s = s.replaceAll("\\(\\s*\\)", "");
        return s;
    }

    private static String collapse(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    private static String titleCase(String s) {
        String[] tokens = s.split(" ");
        StringBuilder sb = new StringBuilder();
        boolean segmentStart = true;
        for (String tok : tokens) {
            if (tok.isEmpty()) {
                continue;
            }
            String out;
            if (tok.equals("-")) {
                out = "-";
                segmentStart = true;
            } else {
                String lower = tok.toLowerCase(ES);
                if (!segmentStart && CONNECTORS.contains(lower)) {
                    out = lower;
                } else {
                    out = capitalizeWord(tok);
                }
                segmentStart = false;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(out);
        }
        return sb.toString();
    }

    private static String capitalizeWord(String w) {
        boolean hasLetter = w.chars().anyMatch(Character::isLetter);
        if (!hasLetter) {
            return w;
        }
        // Palabras en mayúsculas completas se normalizan a tipo título.
        boolean allUpper = w.chars().filter(Character::isLetter).allMatch(Character::isUpperCase);
        if (allUpper) {
            w = w.toLowerCase(ES);
        }
        int i = 0;
        while (i < w.length() && !Character.isLetter(w.charAt(i))) {
            i++;
        }
        if (i < w.length()) {
            w = w.substring(0, i) + Character.toUpperCase(w.charAt(i)) + w.substring(i + 1);
        }
        return w;
    }

    private static String fixAccents(String s) {
        for (Map.Entry<String, String> e : ACCENTS.entrySet()) {
            s = s.replaceAll("\\b" + e.getKey() + "\\b", e.getValue());
        }
        return s;
    }
}
