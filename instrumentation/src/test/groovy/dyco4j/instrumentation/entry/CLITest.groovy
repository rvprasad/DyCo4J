/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry

import dyco4j.instrumentation.AbstractCLITest
import org.junit.Test

class CLITest extends AbstractCLITest {
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
    void withInFolderAndOutFolderOptions() {
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
    void withMethodNameRegexAndSkipAnnotatedTestsOptions() {
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
    void withOnlyAnnotatedTestsOption() {
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
