/*
 * Copyright (c) 2024, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 *
 */

package dyco4j.logging

import org.junit.jupiter.api.Test

import java.nio.file.Paths
import java.util.zip.GZIPInputStream

final class LoggerInitializerTest {
    @Test
    void testInitializeWithPropertiesFile() {
        LoggerInitializer.initialize()
        final _msg = "test initialize method with properties file"
        Logger.log(_msg)
        Logger.cleanupForTest()

        final _tmp = new Properties()
        try (final _in1 = LoggerInitializer.class.getResourceAsStream("logging.properties")) {
            if (_in1 != null)
                _tmp.load(_in1)
        }
        assert _tmp.getProperty("traceFolder") != null: "traceFolder property not found"

        checkTraceFilesForLogs(_msg)
        LoggerInitializer.initialized = false
    }

    @Test
    void testInitializeWithoutPropertiesFile() {
        LoggerInitializer.initialized = false
        // change the name of build/resources/test/dyco4j/instrumentation/logging/logging.properties
        final _srcPropFilePath =
                Paths.get("build", "resources", "test", "dyco4j", "logging", "logging.properties")
        final _srcPropFile = _srcPropFilePath.toFile()
        final _destPropFilePath = _srcPropFilePath.resolveSibling(Paths.get("logging.properties1"))
        final _destPropFile = _destPropFilePath.toFile()
        assert _srcPropFile.renameTo(_destPropFile): "property file could not be renamed"

        LoggerInitializer.initialize()
        final _msg = "test initialize method without properties file"
        Logger.log(_msg)
        Logger.cleanupForTest()

        // undo the name change of build/resources/test/dyco4j/instrumentation/logging/logging.properties
        assert _destPropFile.renameTo(_srcPropFile): "property file renaming could not be undone."

        checkTraceFilesForLogs(_msg)
        LoggerInitializer.initialized = false
    }

    private static void checkTraceFilesForLogs(final String expectedMessage) throws IOException {
        final _traceFile = LoggerInitializer.traceFile
        try (final _stream = new GZIPInputStream(new FileInputStream(_traceFile))) {
            final _line = _stream.readLines()[1]
            assert _line ==~ /^\d+,$expectedMessage$/: "expected log statement not found"
        }
        assert _traceFile.delete(): "Could not delete trace file"
    }
}
