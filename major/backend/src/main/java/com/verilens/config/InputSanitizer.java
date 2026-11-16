package com.verilens.config;

/**
 * Input sanitization utility — Phase 14 hardening.
 * Provides common sanitization methods for user input.
 */
public final class InputSanitizer {

    private InputSanitizer() {} // utility class

    /**
     * Sanitize a string for safe logging — prevent log injection.
     * Replaces newlines, tabs, and control characters.
     */
    public static String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n\\t]", "_")
                    .replaceAll("[\\x00-\\x1f]", "");
    }

    /**
     * Validate pagination parameters — enforce bounds.
     */
    public static int clampPage(int page) {
        return Math.max(0, page);
    }

    /**
     * Validate page size — enforce bounds (1–100).
     */
    public static int clampSize(int size) {
        return Math.max(1, Math.min(100, size));
    }

    /**
     * Validate a sort field against an allowed set.
     * Returns the default if the provided sort is invalid.
     */
    public static String validateSortField(String sort, String defaultSort, String... allowed) {
        if (sort == null || sort.isBlank()) return defaultSort;
        for (String s : allowed) {
            if (s.equalsIgnoreCase(sort.trim())) return s;
        }
        return defaultSort;
    }
}

