package run.slicer.vf.impl;

import java.util.Map;
import java.util.TreeMap;

/**
 * Serializes decompiled output plus Vineflower's offset table as JSON, in the same shape as the CFR port's
 * {@code decompileMapped}, so one consumer handles both. The JS side only needs one JSON.parse.
 *
 * <pre>{
 *   "com/example/Foo": {
 *     "source": "...",
 *     "methods": [
 *       { "owner": "com/example/Foo$Inner", "name": "run", "descriptor": "()V",
 *         "lines": [[0, 12], [7, 13]], "original": null }
 *     ]
 *   }
 * }</pre>
 * {@code lines} are [offset, 1-based line] pairs sorted by offset. Vineflower lists every byte of a mapped
 * instruction, operand bytes included. {@code original} is always null (CFR fills it from the LineNumberTable).
 */
public final class MappingJson {
    private MappingJson() {
    }

    public static String write(Map<String, String> output, Map<String, Map<String, Map<String, Map<Integer, Integer>>>> offsets) {
        final var sb = new StringBuilder(64 * 1024);
        sb.append('{');
        boolean firstClass = true;
        for (final var entry : output.entrySet()) {
            if (!firstClass) sb.append(',');
            firstClass = false;

            string(sb, entry.getKey());
            sb.append(":{\"source\":");
            string(sb, entry.getValue());
            sb.append(",\"methods\":[");

            boolean firstMethod = true;
            for (final var owner : offsets.getOrDefault(entry.getKey(), Map.of()).entrySet()) {
                for (final var method : owner.getValue().entrySet()) {
                    // key is "name descriptor"
                    final String key = method.getKey();
                    final int space = key.indexOf(' ');
                    if (space < 0) continue;

                    if (!firstMethod) sb.append(',');
                    firstMethod = false;
                    sb.append("{\"owner\":");
                    string(sb, owner.getKey());
                    sb.append(",\"name\":");
                    string(sb, key.substring(0, space));
                    sb.append(",\"descriptor\":");
                    string(sb, key.substring(space + 1));
                    sb.append(",\"lines\":[");
                    boolean first = true;
                    for (final var e : new TreeMap<>(method.getValue()).entrySet()) {
                        if (!first) sb.append(',');
                        first = false;
                        sb.append('[').append(e.getKey().intValue()).append(',').append(e.getValue().intValue() + 1).append(']');
                    }
                    sb.append("],\"original\":null}");
                }
            }
            sb.append("]}");
        }
        sb.append('}');
        return sb.toString();
    }

    private static void string(StringBuilder sb, String s) {
        if (s == null) {
            sb.append("null");
            return;
        }
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                        sb.append("\\u");
                        final String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
