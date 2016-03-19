/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package sindu.jvm.instrumentation.logging;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Date;
import java.util.Objects;


public final class Logger {
    private static Logger Logger;
    private final PrintWriter logWriter;
    private volatile String prevMsg = null;
    private volatile boolean clean = false;
    private volatile int msgFreq = 1;

    private Logger(final PrintWriter pw) throws IOException {
        logWriter = pw;
        writeLog((new Date()).toString());
    }

    public static void log(final String msg) {
        final String _sb = String.valueOf(Thread.currentThread().getId()) +
                "," + msg;
        Logger.writeLog(_sb);
    }

    public static void log(final String... args) {
        log(String.join(",", args));
    }

    public static void logArgument(final byte index, final String val) {
        log("arg", Byte.toString(index), val);
    }

    public static void logArray(final Object array, final int index,
                                final String value, final Action action) {
        log(action.toString().concat("A"), Integer.toString(index),
                toString(array), value);
    }

    public static void logException(final Throwable exception) {
        log("exception", toString(exception), exception.getClass().getName());
    }

    public static void logField(final Object receiver, final String fieldValue,
                                final String fieldName, final Action action) {
        log(action.toString().concat("F"), fieldName,
                (receiver == null) ? "" : toString(receiver), fieldValue);
    }

    public static void logMethodEntry(final String methodId) {
        log("entry", methodId);
    }

    public static void logMethodExit(final String methodId, final String exitId) {
        log("exit", methodId, exitId);
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

    static void initialize(final PrintWriter logWriter)
            throws IOException {
        Logger = new Logger(logWriter);

        java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Logger.cleanup();
                } catch (final Throwable _e) {
                    throw new RuntimeException(_e);
                }
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    synchronized void cleanup() {
        if (!this.clean) {
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
                logWriter.println(String.format("%d,%s", msgFreq, prevMsg));
            }
            logWriter.println(String.format("1,%s", msg));
            prevMsg = msg;
            msgFreq = 1;
        }
    }

    public enum Action {
        GET,
        PUT
    }
}
