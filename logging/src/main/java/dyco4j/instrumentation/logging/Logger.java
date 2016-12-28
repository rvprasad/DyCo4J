/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */

package dyco4j.instrumentation.logging;


import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;


@SuppressWarnings("WeakerAccess")
public final class Logger {
    public static final String METHOD_ENTRY_TAG = "en";
    public static final String METHOD_EXIT_TAG = "ex";
    public static final String METHOD_EXCEPTION_TAG = "xp";
    public static final String METHOD_ARG_TAG = "ar";
    public static final String METHOD_RETURN_TAG = "re";

    public static final String ARRAY_TYPE_TAG = "a:";
    public static final String BOOLEAN_TYPE_TAG = "b:";
    public static final String BYTE_TYPE_TAG = "y:";
    public static final String CHAR_TYPE_TAG = "c:";
    public static final String DOUBLE_TYPE_TAG = "d:";
    public static final String FLOAT_TYPE_TAG = "f:";
    public static final String INT_TYPE_TAG = "i:";
    public static final String LONG_TYPE_TAG = "l:";
    public static final String OBJECT_TYPE_TAG = "o:";
    public static final String SHORT_TYPE_TAG = "h:";
    public static final String STRING_TYPE_TAG = "s:";
    public static final String THROWABLE_TYPE_TAG = "t:";

    public static final String FALSE_VALUE = BOOLEAN_TYPE_TAG + "f";
    public static final String TRUE_VALUE = BOOLEAN_TYPE_TAG + "t";
    public static final String NULL_VALUE = "null";

    private static Logger logger;
    private final PrintWriter logWriter;
    private volatile String prevMsg = null;
    private volatile boolean clean = false;
    private volatile int msgFreq = 1;

    private Logger(final PrintWriter pw) {
        this.logWriter = pw;
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
        log(METHOD_ARG_TAG, Byte.toString(index), val);
    }

    public static void logArray(final Object array, final int index, final String value, final ArrayAction action) {
        log(action.toString(), Integer.toString(index), toString(array), value);
    }

    public static void logException(final Throwable exception) {
        log(METHOD_EXCEPTION_TAG, toString(exception), exception.getClass().getName());
    }

    public static void logField(final Object receiver, final String fieldValue, final String fieldName,
                                final FieldAction action) {
        log(action.toString(), fieldName, (receiver == null) ? "" : toString(receiver), fieldValue);
    }

    public static void logMethodEntry(final String methodId) {
        log(METHOD_ENTRY_TAG, methodId);
    }

    public static void logMethodExit(final String methodId, final String returnKind) {
        log(METHOD_EXIT_TAG, methodId, returnKind);
    }

    public static void logReturn(final String val) {
        if (val != null) {
            log(METHOD_RETURN_TAG, val);
        } else {
            log(METHOD_RETURN_TAG);
        }
    }

    public static String toString(final boolean v) {
        return v ? TRUE_VALUE : FALSE_VALUE;
    }

    public static String toString(final byte v) {
        return BYTE_TYPE_TAG + v;
    }

    public static String toString(final char v) {
        return CHAR_TYPE_TAG + Character.hashCode(v);
    }

    public static String toString(final short v) {
        return SHORT_TYPE_TAG + v;
    }

    public static String toString(final int v) {
        return INT_TYPE_TAG + v;
    }

    public static String toString(final long v) {
        return LONG_TYPE_TAG + v;
    }

    public static String toString(final float v) {
        return FLOAT_TYPE_TAG + v;
    }

    public static String toString(final double v) {
        return DOUBLE_TYPE_TAG + v;
    }

    public static String toString(final Object o) {
        if (o == null) {
            return NULL_VALUE;
        } else {
            final String _tmp;

            if (o instanceof String) {
                _tmp = STRING_TYPE_TAG;
            } else if (o instanceof Throwable) {
                _tmp = THROWABLE_TYPE_TAG;
            } else if (o.getClass().isArray()) {
                _tmp = ARRAY_TYPE_TAG;
            } else {
                _tmp = OBJECT_TYPE_TAG;
            }

            return _tmp + System.identityHashCode(o);
        }
    }

    static void initialize(final PrintWriter logWriter) {
        logger = new Logger(logWriter);

        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.cleanup();
            } catch (final Throwable _e) {
                throw new RuntimeException(_e);
            }
        }));
    }

    // This method is intended for testing purpose only.
    static void cleanupForTest() {
        logger.cleanup();
    }

    @Override
    protected void finalize() throws Throwable {
        this.cleanup();
        super.finalize();
    }

    private synchronized void cleanup() {
        if (!this.clean) {
            if (this.msgFreq > 1) {
                this.logWriter.println(String.format("%d,%s", this.msgFreq - 1, this.prevMsg));
            }
            this.logWriter.flush();
            this.logWriter.close();
            this.clean = true;
        }
    }

    private synchronized void writeLog(final String msg) {
        if (Objects.equals(this.prevMsg, msg)) {
            this.msgFreq++;
        } else {
            if (this.msgFreq > 1) {
                this.logWriter.println(String.format("%d,%s", this.msgFreq - 1, this.prevMsg));
            }

            this.logWriter.println(String.format("%s", msg));
            this.prevMsg = msg;
            this.msgFreq = 1;
        }
    }

    public enum ArrayAction {
        GETA,
        PUTA
    }

    public enum FieldAction {
        GETF,
        PUTF
    }
}
