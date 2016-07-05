/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */

package dyco4j.instrumentation.logging;

import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class LoggerInitializerTest {
    @Test
    public void testInitializeWithPropertiesFile() throws Exception {
        LoggerInitializer.initialize();
        final String _msg = "test initialize with properties file";
        Logger.log(_msg);
        Logger.cleanupForTest();

        final Properties _tmp = new Properties();
        try (final InputStream _in1 = LoggerInitializer.class.getResourceAsStream("logging.properties")) {
            if (_in1 != null)
                _tmp.load(_in1);
        }
        assert _tmp.getProperty("traceFolder") != null : "traceFolder property not found";

        checkTraceFilesForLogs(_msg);
    }

    @Test
    public void testInitializeWithoutPropertiesFile() throws Exception {
        // change the name of build/resources/test/dyco4j/instrumentation/logging/logging.properties
        final Path _srcPropFilePath =
                Paths.get("build", "resources", "test", "dyco4j", "instrumentation", "logging", "logging.properties");
        final File _srcPropFile = _srcPropFilePath.toFile();
        final Path _destPropFilePath = _srcPropFilePath.resolveSibling(Paths.get("logging.properties1"));
        final File _destPropFile = _destPropFilePath.toFile();
        assert _srcPropFile.renameTo(_destPropFile) : "property file could not be renamed";

        LoggerInitializer.initialize();
        final String _msg = "test initialize without properties file";
        Logger.log(_msg);
        Logger.cleanupForTest();

        // undo the name change of build/resources/test/dyco4j/instrumentation/logging/logging.properties
        assert _destPropFile.renameTo(_srcPropFile) : "property file renaming could not be undone.";

        checkTraceFilesForLogs(_msg);
    }

    private void checkTraceFilesForLogs(final String expectedMessage) throws IOException {
        final File _traceFile = LoggerInitializer.getTraceFile();
        try (final BufferedReader _traceReader =
                new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(_traceFile))))) {
            _traceReader.readLine();
            final String _line = _traceReader.readLine();
            final String _regex = MessageFormat.format("^\\d+,{0}$", expectedMessage);
            assert _line.matches(_regex) : "expected log statement not found";
        }
        assert _traceFile.delete() : "Could not delete trace file";
    }
}