/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package dyco4j.instrumentation.logging;


import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;


@SuppressWarnings("WeakerAccess")
public final class Logger {
    private static Logger logger;
    private final PrintWriter logWriter;
    private volatile String prevMsg = null;
    private volatile boolean clean = false;
    private volatile int msgFreq = 1;

    private Logger(final PrintWriter pw) {
        logWriter = pw;
        writeLog((new Date()).toString());
    }

    public static void log(final String msg) {
        final String _sb = String.valueOf(Thread.currentThread().getId()) + "," + msg;
        logger.writeLog(_sb);
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public static void log(final String... args) {
        log(String.join(",", args));
    }

    public static void logArgument(final byte index, final String val) {
        log("arg", Byte.toString(index), val);
    }

    public static void logArray(final Object array, final int index, final String value, final Action action) {
        log(action.toString().concat("A"), Integer.toString(index), toString(array), value);
    }

    public static void logException(final Throwable exception) {
        log("exception", toString(exception), exception.getClass().getName());
    }

    public static void logField(final Object receiver, final String fieldValue, final String fieldName,
                                final Action action) {
        log(action.toString().concat("F"), fieldName, (receiver == null) ? "" : toString(receiver), fieldValue);
    }

    public static void logMethodEntry(final String methodId) {
        log("entry", methodId);
    }

    public static void logMethodExit(final String methodId, final String returnKind) {
        log("exit", methodId, returnKind);
    }

    public static void logReturn(final String val) {
        if (val != null) {
            log("return", val);
        } else {
            log("return");
        }
    }

    public static String toString(final boolean v) {
        return v ? "p_b:1" : "p_b:0";
    }

    public static String toString(final byte v) {
        return "p_y:" + v;
    }

    public static String toString(final char v) {
        return "p_c:" + Character.hashCode(v);
    }

    public static String toString(final short v) {
        return "p_s:" + v;
    }

    public static String toString(final int v) {
        return "p_i:" + v;
    }

    public static String toString(final long v) {
        return "p_l:" + v;
    }

    public static String toString(final float v) {
        return "p_f:" + v;
    }

    public static String toString(final double v) {
        return "p_d:" + v;
    }

    public static String toString(final Object o) {
        if (o == null) {
            return "r_o:null";
        } else {
            final String _tmp;

            if (o instanceof String) {
                _tmp = "r_s:";
            } else if (o instanceof Throwable) {
                _tmp = "r_t:";
            } else if (o.getClass().isArray()) {
                _tmp = "r_a:";
            } else {
                _tmp = "r_o:";
            }

            return _tmp + System.identityHashCode(o);
        }
    }

    static void initialize(final PrintWriter logWriter) {
        logger = new Logger(logWriter);

        java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    logger.cleanup();
                } catch (final Throwable _e) {
                    throw new RuntimeException(_e);
                }
            }
        });
    }

    /**
     * This method is intended for testing purpose only.
     */
    static void cleanupForTest() {
        logger.cleanup();
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    private synchronized void cleanup() {
        if (!this.clean) {
            if (msgFreq > 1) {
                logWriter.println(String.format("%d,%s", msgFreq - 1, prevMsg));
            }
            logWriter.flush();
            logWriter.close();
            clean = true;
        }
    }

    private synchronized void writeLog(final String msg) {
        if (Objects.equals(prevMsg, msg)) {
            msgFreq++;
        } else {
            if (msgFreq > 1) {
                logWriter.println(String.format("%d,%s", msgFreq - 1, prevMsg));
            }

            logWriter.println(String.format("%s", msg));
            prevMsg = msg;
            msgFreq = 1;
        }
    }

    public enum Action {
        GET,
        PUT
    }
}
