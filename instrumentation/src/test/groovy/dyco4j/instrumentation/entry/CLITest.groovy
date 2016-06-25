/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry

import org.junit.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CLITest {
    private static final Path PROPERTY_FILE = Paths.get(".", "build", "classes", "test", "dyco4j", "instrumentation",
            "logging", "logging.properties");
    private static final Path ROOT_FOLDER = Paths.get(".", "build", "tmp")
    private static final Path TRACE_FOLDER = ROOT_FOLDER.resolve("traces")
    private static final Path OUT_FOLDER = ROOT_FOLDER.resolve("out_classes")
    private static final Path IN_FOLDER = ROOT_FOLDER.resolve("in_classes")

    private static void deleteFiles(folder, pattern) {
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
        Files.walk(_testClassFolder).filter { it.fileName ==~ "TestSubject.class" }.each { src ->
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
        deleteFiles(TRACE_FOLDER, /^trace.*gz/)
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
        // def (stdoutLines, _traceLines) = executeInstrumentedCode()
        // TODO: assert expected std output
        // TODO: assert expected trace output
    }

    @Test
    void testMethodNameRegexOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--method-name-regex",
                               '^check.*']) == 1: "Class was not instrumented"
        // def (stdoutLines, _traceLines) = executeInstrumentedCode()
        // TODO: assert expected std output
        // TODO: assert expected trace output
    }

    @Test
    void testMethodNameRegexAndSkipAnnotatedTestsOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--method-name-regex",
                               '^check.*', '--skip-annotated-tests']) == 1: "Class was not instrumented"
        // def (stdoutLines, _traceLines) = executeInstrumentedCode()
        // TODO: assert expected std output
        // TODO: assert expected trace output
    }

    @Test
    void testSkipAnnotatedTestsOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               '--skip-annotated-tests']) == 1: "Class was not instrumented"
        // def (stdoutLines, _traceLines) = executeInstrumentedCode()
        // TODO: assert expected std output
        // TODO: assert expected trace output
    }

    private def instrumentCode(args) {
        CLI.main((String[]) args)
        return Files.walk(OUT_FOLDER).filter { it.fileName ==~ /.*class$/ }.count()
    }

    private def executeInstrumentedCode() {
        // TODO: execute instrumented code in a different process
        //       and return a pair of
        //        - the output on std out and std error as a list and
        //        - the lines from the trace file as a list
    }
}


class TestSubject {
    void test1() {
        assert true
    }

    void check1() {
        assert true
    }

    @Test
    void check2() {
        assert true
    }

    @Test
    void test2() {
        assert true
    }
}