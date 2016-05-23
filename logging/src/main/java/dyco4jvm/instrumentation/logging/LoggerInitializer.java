package dyco4jvm.instrumentation.logging;

import java.io.*;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("unused")
public final class LoggerInitializer {

    @SuppressWarnings({"unused"})
    public static void initialize() throws IOException {
        final int _bufferLength = 10000000;
        final File _profileFile = File.createTempFile("trace_", ".gz", new File("."));
        final OutputStream _stream = new FileOutputStream(_profileFile, true);
        final PrintWriter _logWriter =
                new PrintWriter(new BufferedOutputStream(new GZIPOutputStream(_stream), _bufferLength));
        Logger.initialize(_logWriter);
    }
}
