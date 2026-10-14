package run.slicer.vf.impl;

import org.jetbrains.java.decompiler.main.extern.IContextSource;

import java.util.HashMap;
import java.util.Map;

public final class OutputSinkImpl implements IContextSource.IOutputSink, BytecodeMappingSink {
    private final Map<String, String> output = new HashMap<>();
    private final Map<String, Map<String, Map<String, Map<Integer, Integer>>>> offsets = new HashMap<>();

    @Override
    public void begin() {
    }

    @Override
    public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
        this.output.put(qualifiedName, content);
    }

    @Override
    public void acceptOffsetMapping(String qualifiedName, Map<String, Map<String, Map<Integer, Integer>>> offsets) {
        this.offsets.put(qualifiedName, offsets);
    }

    @Override
    public void acceptDirectory(String directory) {
    }

    @Override
    public void acceptOther(String path) {
    }

    @Override
    public void close() {
    }

    public Map<String, String> output() {
        return this.output;
    }

    /**
     * Output class -> declaring class -> "name descriptor" -> bytecode offset -> 0-based output line.
     */
    public Map<String, Map<String, Map<String, Map<Integer, Integer>>>> offsets() {
        return this.offsets;
    }
}
