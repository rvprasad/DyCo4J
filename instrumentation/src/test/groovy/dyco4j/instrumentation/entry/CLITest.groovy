/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry

import groovy.io.FileType
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

class CLITest {
    private static final String LOGGING_LIBRARY = Paths.get("libs", "dyco4j-logging-0.5.1.jar").toString()
    private static final Path TEST_CLASS_FOLDER = Paths.get("build", "classes", "test")
    private static final Path PROPERTY_FILE = Paths.get(TEST_CLASS_FOLDER.toString(), "dyco4j", "instrumentation",
            "logging", "logging.properties");
    private static final Path ROOT_FOLDER = Paths.get("build", "tmp")
    private static final Path TRACE_FOLDER = ROOT_FOLDER.resolve("traces")
    private static final Path OUT_FOLDER = ROOT_FOLDER.resolve("out_classes")
    private static final Path IN_FOLDER = ROOT_FOLDER.resolve("in_classes")

    private static void deleteFiles(final Path folder, final pattern) {
        Files.walk(folder).filter { it.fileName ==~ pattern }.each { Files.delete(it) }
    }

    @BeforeClass
    static void setUpClass() {
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
    static void tearDownClass() {
        Files.delete(PROPERTY_FILE)
        deleteFiles(IN_FOLDER, /.*class$/)
    }

    @Before
    void setUp() {
        deleteFiles(OUT_FOLDER, /.*class$/)
    }

    @Test
    void testNoOptions() {
        assert instrumentCode([]) == 0: "No class should have been instrumented"
    }

    @Test
    void testOnlyInFolderOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER]) == 0: "No class should have been instrumented"
    }

    @Test
    void testOnlyOutFolderOption() {
        assert instrumentCode(["--out-folder", OUT_FOLDER]) == 0: "No class should have been instrumented"
    }

    @Test
    void testInFolderAndOutFolderOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER]) == 1:
                "Class was not instrumented"
        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0
        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 5
        try {
            Date.parseToStringDate(_traceLines[0].split(',')[1])
        } catch (_e) {
            assert False: "Incorrect first line in trace: $_e / ${_traceLines[0]}"
        }
        assert _traceLines[1] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/test1\(\)V/
        assert _traceLines[2] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/test2\(\)V/
        assert _traceLines[3] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix1\(\)V/
        assert _traceLines[4] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    @Test
    void testMethodNameRegexOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--method-name-regex",
                               '.*Suffix.$']) == 1: "Class was not instrumented"
        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0
        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 3
        try {
            Date.parseToStringDate(_traceLines[0].split(',')[1])
        } catch (_e) {
            assert False: "Incorrect first line in trace: $_e / ${_traceLines[0]}"
        }
        assert _traceLines[1] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix1\(\)V/
        assert _traceLines[2] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    @Test
    void testMethodNameRegexAndSkipAnnotatedTestsOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--method-name-regex",
                               '.*Suffix.$', '--only-annotated-tests']) == 1: "Class was not instrumented"
        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0
        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 2
        try {
            Date.parseToStringDate(_traceLines[0].split(',')[1])
        } catch (_e) {
            assert False: "Incorrect first line in trace: $_e / ${_traceLines[0]}"
        }
        assert _traceLines[1] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    @Test
    void testSkipAnnotatedTestsOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               '--only-annotated-tests']) == 1: "Class was not instrumented"
        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0
        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 3
        try {
            Date.parseToStringDate(_traceLines[0].split(',')[1])
        } catch (_e) {
            assert False: "Incorrect first line in trace: $_e / ${_traceLines[0]}"
        }
        assert _traceLines[1] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/test2\(\)V/
        assert _traceLines[2] ==~ /1,\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    private static def instrumentCode(final args) {
        CLI.main((String[]) args)
        return Files.walk(OUT_FOLDER).filter { it.fileName ==~ /.*class$/ }.count()
    }

    /**
     * execute instrumented code in a different process
     * @return a quadruple of
     *  - exit code
     *  - lines on std out as a list
     *  - lines on std error as a list
     *  - lines in trace file as a list
     */
    private def executeInstrumentedCode() {
        final _path = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        final _cp = [OUT_FOLDER, LOGGING_LIBRARY, TEST_CLASS_FOLDER].join(":")
        final _proc = [_path, "-cp", _cp, CLITestSubject.class.name].execute()
        final _ret = new ExecutionResult()
        _ret.exitCode = _proc.waitFor()
        _ret.stdoutLines = _proc.inputStream.readLines()
        _ret.stderrLines = _proc.errorStream.readLines()
        _ret.traceLines = getTraceLines()
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

    private final class ExecutionResult {
        def exitCode
        def stdoutLines
        def stderrLines
        def traceLines

        def String toString() {
            return [exitCode, stdoutLines, stderrLines, traceLines]
        }
    }
}
