/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */

package dyco4j.logging;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("unused")
public final class LoggerInitializer {
    private static File traceFile;
    private static volatile boolean initialized;

    public static synchronized void initialize() throws IOException {
        if (!initialized) {
            final Properties _properties = getProperties();
            final File _folder = new File(_properties.getProperty("traceFolder", "."));
            if (!_folder.exists() && !_folder.mkdir())
                throw new IOException(MessageFormat.format("Cannot create {0}", _folder.toString()));

            final String _prefix = "trace_" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0] + "_";
            LoggerInitializer.traceFile = File.createTempFile(_prefix, ".gz", _folder);
            final OutputStream _stream = new FileOutputStream(LoggerInitializer.traceFile, true);
            final int _bufferLength = Integer.parseInt(_properties.getProperty("bufferLength", "10000000"));
            final PrintWriter _logWriter =
                    new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(_stream), _bufferLength));
            Logger.initialize(_logWriter);
            LoggerInitializer.initialized = true;
        }
    }

    // This method is intended for testing purpose only.
    static File getTraceFile() {
        return traceFile;
    }

    // This method is intended for testing purpose only.
    static void reenableInitialize() {
        LoggerInitializer.initialized = false;
    }

    private static Properties getProperties() throws IOException {
        final Properties _tmp = new Properties();
        try (final InputStream _in1 = LoggerInitializer.class.getResourceAsStream("logging.properties")) {
            if (_in1 != null)
                _tmp.load(_in1);
        }
        return _tmp;
    }
}
