/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation

import dyco4j.instrumentation.entry.CLI
import dyco4j.instrumentation.entry.CLITestSubject
import groovy.io.FileType
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

abstract class AbstractCLITest {
    protected static final String LOGGING_LIBRARY = Paths.get("libs", "dyco4j-logging-0.5.1.jar").toString()
    protected static final Path TEST_CLASS_FOLDER = Paths.get("build", "classes", "test")
    private static final Path PROPERTY_FILE = Paths.get(TEST_CLASS_FOLDER.toString(), "dyco4j", "instrumentation",
            "logging", "logging.properties")
    private static final Path ROOT_FOLDER = Paths.get("build", "tmp")
    protected static final Path TRACE_FOLDER = ROOT_FOLDER.resolve("traces")
    protected static final Path OUT_FOLDER = ROOT_FOLDER.resolve("out_classes")
    protected static final Path IN_FOLDER = ROOT_FOLDER.resolve("in_classes")

    protected static void deleteFiles(final Path folder, final pattern) {
        Files.walk(folder).filter { it.fileName ==~ pattern }.each { Files.delete(it) }
    }

    @BeforeClass
    static void setUpOnceOnlyFixture() {
        final _propertyFolder = PROPERTY_FILE.getParent()
        assert Files.createDirectories(_propertyFolder) != null: "Could not create property folder $_propertyFolder"
        final _propertyFile = Files.createFile(PROPERTY_FILE)
        assert _propertyFile != null: "Could not create property file $PROPERTY_FILE"
        _propertyFile.withWriter {
            it.println("traceFolder=" + TRACE_FOLDER.toString())
        }

        assert Files.createDirectories(ROOT_FOLDER) != null: "Could not create root folder $ROOT_FOLDER"
        assert Files.createDirectories(TRACE_FOLDER) != null: "Could not create trace folder $TRACE_FOLDER"
        assert Files.createDirectories(OUT_FOLDER) != null: "Could not create out folder $OUT_FOLDER"
        assert Files.createDirectories(IN_FOLDER) != null: "Could not create in folder $IN_FOLDER"

        // copy to-be-instrumented test classes into IN_FOLDER
        final _testClassFolder = Paths.get("build", "classes", "test")
        Files.walk(_testClassFolder).filter { it.fileName ==~ "CLITestSubject.class" }.each { src ->
            final _trg = IN_FOLDER.resolve(_testClassFolder.relativize(src))
            Files.createDirectories(_trg.parent)
            Files.copy(src, _trg)
        }
    }

    @AfterClass
    static void tearDownOnceOnlyFixture() {
        Files.delete(PROPERTY_FILE)
        deleteFiles(IN_FOLDER, /.*class$/)
    }

    protected static def instrumentCode(final args) {
        CLI.main((String[]) args)
        return Files.walk(OUT_FOLDER).filter { it.fileName ==~ /.*class$/ }.count()
    }
    /**
     * execute instrumented code in a different process
     * @return a quadruple of
     */
    protected static def executeInstrumentedCode() {
        final _path = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        final _cp = [OUT_FOLDER, LOGGING_LIBRARY, TEST_CLASS_FOLDER].join(":")
        final _proc = [_path, "-cp", _cp, CLITestSubject.class.name].execute()
        final _ret = new ExecutionResult(
                _proc.waitFor(),
                _proc.inputStream.readLines(),
                _proc.errorStream.readLines(),
                getTraceLines())
        deleteFiles(TRACE_FOLDER, /^trace.*gz/)
        return _ret
    }

    private static def getTraceLines() {
        def _ret = []
        TRACE_FOLDER.toFile().eachFileMatch(FileType.FILES, ~/^trace.*gz/) {
            _ret << new GZIPInputStream(it.newInputStream()).readLines()
        }
        return _ret.flatten()
    }

    @Before
    void setUpFixture() {
        deleteFiles(OUT_FOLDER, /.*class$/)
    }

    protected static final class ExecutionResult {
        protected final exitCode
        protected final stdoutLines // list of string
        protected final stderrLines // list of string
        protected final traceLines // list of string

        ExecutionResult(exitCode, stdoutLines, stderrLines, traceLines) {
            this.exitCode = exitCode
            this.stdoutLines = stdoutLines
            this.stderrLines = stderrLines
            this.traceLines = traceLines
        }

        def String toString() {
            return [exitCode, stdoutLines, stderrLines, traceLines]
        }
    }
}
