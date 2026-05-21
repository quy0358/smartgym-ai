package ntu.quy65132908.smartgym_ai.util;

import android.util.Patterns;

public final class InputValidator {
    private static final int MAX_NAME = 50;
    private static final int MAX_CONTENT = 500;

    private InputValidator() {}

    public static String sanitizeName(String input) {
        if (input == null) return "";
        String s = input.trim().replaceAll("<[^>]*>", "");
        return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
    }

    public static String sanitizeContent(String input) {
        if (input == null) return "";
        String s = input.trim();
        return s.length() > MAX_CONTENT ? s.substring(0, MAX_CONTENT) : s;
    }

    public static boolean isValidName(String input) {
        return input != null && !input.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
