package com.biw.common.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects the before/after values of the fields a service method actually changed.
 * <p>
 * A service calls {@link #change(String, Object, Object)} while building the update; the audit
 * aspect drains the map when the method returns. Kept in a {@code ThreadLocal} because the values
 * are produced inside the service and consumed outside it, and the request thread is the only
 * scope that matters.
 */
public final class AuditTrail {

    private static final ThreadLocal<Map<String, String>> CHANGES = new ThreadLocal<>();

    private AuditTrail() {
    }

    /**
     * Records one changed field. A value that did not change is skipped, so a full-row update that
     * only touches one column produces one entry.
     */
    public static void change(String field, Object before, Object after) {
        if (java.util.Objects.equals(before, after)) {
            return;
        }
        Map<String, String> changes = CHANGES.get();
        if (changes == null) {
            changes = new LinkedHashMap<>();
            CHANGES.set(changes);
        }
        changes.put(field, before + "->" + after);
    }

    /** Returns what was collected and clears it, so the next call cannot inherit it. */
    public static Map<String, String> drain() {
        Map<String, String> changes = CHANGES.get();
        CHANGES.remove();
        return changes == null ? Map.of() : Map.copyOf(changes);
    }

    /** Clears without returning, used when the method failed and no record will be written. */
    public static void clear() {
        CHANGES.remove();
    }
}
