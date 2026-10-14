package run.slicer.vf;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSExport;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Uint8Array;
import run.slicer.vf.impl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Main {
    @JSExport
    public static JSPromise<JSMapLike<JSString>> decompile(String[] names, Options options) {
        return decompile0(names, options == null || JSObjects.isUndefined(options) ? JSObjects.create() : options);
    }

    /**
     * Like {@link #decompile}, plus Vineflower's bytecode offset -> output line table for every method it printed.
     * Resolves to a JSON string (see {@link MappingJson}); the vf.js wrapper parses it.
     * <p>
     * Forces {@code bytecode-source-mapping} on. The Kotlin plugin's offsets don't line up with the class file
     * (offsets under abstract methods, past the method end, mid-instruction), so {@code kt-enable} defaults to
     * {@code 0} here unless the caller sets it.
     */
    @JSExport
    public static JSPromise<JSString> decompileMapped(String[] names, Options options) {
        final Options opts = options == null || JSObjects.isUndefined(options) ? JSObjects.create() : options;
        return JSPromise.callAsync(() -> {
            final var outputSink = run(names, opts, true);
            return JSString.valueOf(MappingJson.write(outputSink.output(), outputSink.offsets()));
        });
    }

    private static JSPromise<JSMapLike<JSString>> decompile0(String[] names, Options options) {
        return JSPromise.callAsync(() -> {
            final var outputSink = run(names, options, false);

            final JSMapLike<JSString> output = JSObjects.create();
            for (final var entry : outputSink.output().entrySet()) {
                output.set(entry.getKey(), JSString.valueOf(entry.getValue()));
            }

            return output;
        });
    }

    private static OutputSinkImpl run(String[] names, Options options, boolean mapped) {
        final Map<String, Object> options0 = new HashMap<>(IFernflowerPreferences.DEFAULTS);
        if (mapped) {
            options0.put("kt-enable", "0"); // see decompileMapped
        }
        options0.putAll(options.rawOptions());
        if (mapped) {
            options0.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
        }
        // report errors to us, not Vineflower
        options0.put(IFernflowerPreferences.ERROR_MESSAGE, "Please report this to the vf issue tracker at https://github.com/katana-project/vf/issues with a copy of the class file (if you have the rights to distribute it!)");

        final var outputSink = new OutputSinkImpl();
        final var logger = options.logger() != null ? new OptionLogger(Objects.requireNonNull(options.logger())) : DefaultLogger.INSTANCE;
        final var fernflower = new Fernflower(ResultSaverImpl.INSTANCE, options0, logger);

        if (options.tokenCollector() != null) {
            TextTokenVisitor.addVisitor(next -> new TextTokenCollector(next, options.tokenCollector()));
        }

        final var source = ClassSource.create(names, options.resources(), name0 -> source0(options, name0), outputSink);
        fernflower.addSource(source);
        fernflower.addLibrary(source.librarySource());

        fernflower.decompileContext();
        fernflower.clearContext();
        Runtime.getRuntime().gc(); // check MethodDelegates#java_lang_Runtime_gc, to clean up TThreadLocal

        return outputSink;
    }

    private static byte[] source0(Options options, String name) {
        final Uint8Array b = options.source(name).await();
        return b == null || JSObjects.isUndefined(b) ? null : unwrapByteArray(b);
    }

    @JSBody(params = {"data"}, script = "return data;")
    private static native @JSByRef(optional = true) byte[] unwrapByteArray(Uint8Array data);
}
