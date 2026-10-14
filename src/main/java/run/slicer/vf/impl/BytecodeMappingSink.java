package run.slicer.vf.impl;

import java.util.Map;

/**
 * An output sink that also wants Vineflower's bytecode offset -> output line table, which
 * {@code ContextUnitPatch} hands over after each class (only with {@code bytecode-source-mapping} on).
 */
public interface BytecodeMappingSink {
    /**
     * @param qualifiedName the class the output belongs to, as passed to {@code acceptClass}
     * @param offsets       declaring class -> "name descriptor" -> bytecode offset -> 0-based output line
     */
    void acceptOffsetMapping(String qualifiedName, Map<String, Map<String, Map<Integer, Integer>>> offsets);
}
