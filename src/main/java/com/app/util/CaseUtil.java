package com.app.util;

import java.util.Locale;

/** String case conversions used when translating Java names to API payload names. */
public final class CaseUtil {

    private CaseUtil() {
        // Private constructor for utility class
    }

    /**
     * Converts a camelCase identifier to snake_case so that validation errors
     * reference the field names clients actually sent (e.g. {@code full_name}).
     */
    public static String toSnakeCase(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        StringBuilder result = new StringBuilder(value.length() + 4);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isUpperCase(character)) {
                if (index > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(character));
            } else {
                result.append(character);
            }
        }
        return result.toString().toLowerCase(Locale.ROOT);
    }
}
