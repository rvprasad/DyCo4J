/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry

import dyco4j.instrumentation.AbstractCLITest
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

class CLITest extends AbstractCLITest {
    @BeforeClass
    static void copyClassesToBeInstrumented() {
        // copy to-be-instrumented test classes into IN_FOLDER
        final _file = Paths.get("dyco4j", "instrumentation", "entry", "CLITestSubject.class")
        final _trg = IN_FOLDER.resolve(_file)
        Files.createDirectories(_trg.parent)
        final _testClassFolder = Paths.get("build", "classes", "test")
        final _src = _testClassFolder.resolve(_file)
        Files.copy(_src, _trg)
    }

    @Test
    void withNoOptions() {
        assert instrumentCode([]) == 0: "No class should have been instrumented"
    }

    @Test
    void withOnlyInFolderOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER]) == 0: "No class should have been instrumented"
    }

    @Test
    void withOnlyOutFolderOption() {
        assert instrumentCode(["--out-folder", OUT_FOLDER]) == 0: "No class should have been instrumented"
    }

    @Test
    void withOnlyInFolderAndOutFolderOptions() {
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
    void withMethodNameRegexOption() {
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
    void withMethodNameRegexAndOnlyAnnotatedTestsOptions() {
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
    void withAnnotatedTestsOption() {
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
}
