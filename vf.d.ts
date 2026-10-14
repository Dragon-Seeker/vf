export type Options = Record<string, string>;

export interface TokenCollector {
    start: (content: string) => void;
    visitClass: (start: number, length: number, declaration: boolean, name: string) => void;
    visitField: (start: number, length: number, declaration: boolean, className: string, name: string, descriptor: string) => void;
    visitMethod: (start: number, length: number, declaration: boolean, className: string, name: string, descriptor: string) => void;
    visitParameter: (start: number, length: number, declaration: boolean, className: string, methodName: string, methodDescriptor: string, index: number, name: string) => void;
    visitLocal: (start: number, length: number, declaration: boolean, className: string, methodName: string, methodDescriptor: string, index: number, name: string) => void;
    end: () => void;
}

export type LogLevel = "trace" | "info" | "warn" | "error";

export interface Logger {
    writeMessage: (level: LogLevel, message: string, error?: unknown) => void;
    startProcessingClass?: (className: string) => void;
    endProcessingClass?: () => void;
    startReadingClass?: (className: string) => void;
    endReadingClass?: () => void;
    startClass?: (className: string) => void;
    endClass?: () => void;
    startMethod?: (methodName: string) => void;
    endMethod?: () => void;
    startWriteClass?: (className: string) => void;
    endWriteClass?: () => void;
}

export interface Config {
    source?: (name: string) => Promise<Uint8Array | null>;
    resources?: string[];
    options?: Options;
    tokenCollector?: TokenCollector;
    logger?: Logger;
}

export declare const decompile: (names: string | string[], config?: Config) => Promise<Record<string, string>>;

/** [bytecode offset, 1-based line] pairs, sorted by offset. */
export type OffsetLinePairs = [number, number][];

export interface MethodMapping {
    /** Internal name of the declaring class; differs from the output key for inner, anonymous and local classes. */
    owner: string;
    name: string;
    descriptor: string;
    /**
     * Offset -> line in `source`, as Vineflower tracks it: every byte of a mapped instruction, operand bytes
     * included. Unmapped offsets are absent, so resolve them via the nearest mapped offset in the same basic block.
     */
    lines: OffsetLinePairs;
    /** Always null here; kept for the same shape as the CFR port, which fills it from the LineNumberTable. */
    original: OffsetLinePairs | null;
}

export interface MappedOutput {
    source: string;
    methods: MethodMapping[];
}

/**
 * Decompiles with `bytecode-source-mapping` forced on, returning output keyed by class name, each with its
 * methods' bytecode offset -> line tables. `kt-enable` defaults to "0", as the Kotlin plugin's offsets don't
 * match the class file; set it in `options` to override.
 */
export declare const decompileMapped: (names: string | string[], config?: Config) => Promise<Record<string, MappedOutput>>;
