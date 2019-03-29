package edu.cmu.jacoco.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to normalize {@link String} instances in a way that if
 * <code>equals()</code> is <code>true</code> for two strings they will be
 * represented the same instance. While this is exactly what
 * {@link String#intern()} does, this implementation avoids VM specific side
 * effects and is supposed to be faster, as neither native code is called nor
 * synchronization is required for concurrent lookup.
 */
public final class StringPool {

    private static final String[] EMPTY_ARRAY = new String[0];

    private final Map<String, String> pool = new ConcurrentHashMap<>(1024);

    /**
     * Returns a normalized instance that is equal to the given {@link String} .
     *
     * @param s
     *            any string or <code>null</code>
     * @return normalized instance or <code>null</code>
     */
    public String get(final String s) {
        if (s == null) {
            return null;
        }
        final String norm = pool.get(s);
        if (norm == null) {
            pool.put(s, s);
            return s;
        }
        return norm;
    }

    /**
     * Returns a modified version of the array with all string slots normalized.
     * It is up to the implementation to replace strings in the array instance
     * or return a new array instance.
     *
     * @param arr
     *            String array or <code>null</code>
     * @return normalized instance or <code>null</code>
     */
    public synchronized String[] get(final String[] arr) {
        if (arr == null) {
            return null;
        }
        if (arr.length == 0) {
            return EMPTY_ARRAY;
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = get(arr[i]);
        }
        return arr;
    }

}

