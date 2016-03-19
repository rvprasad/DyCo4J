package sindu.jvm.instrumentation.logging;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public final class LoggerHelper {
    static {
        try {
            final File _profileFile = File.createTempFile("trace_", ".gz",
                    new File("."));
            final OutputStream _stream = new FileOutputStream(_profileFile, true);
            final PrintWriter _logWriter = new PrintWriter(new BufferedOutputStream(
                    new GZIPOutputStream(_stream), 10000000));
            Logger.initialize(_logWriter);
        } catch (final IOException _e) {
            throw new RuntimeException(_e);
        }
    }
}
