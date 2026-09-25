package com.ura.dest.core;

public final class BreakContext {
    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    public static void setProcessing(boolean flag) {
        PROCESSING.set(flag);
    }

    public static boolean isProcessing() {
        return PROCESSING.get();
    }

    public static void clear() {
        PROCESSING.remove();
    }
}