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

import java.nio.file.Paths

class CLITest extends AbstractCLITest {
    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file1 = Paths.get("dyco4j", "instrumentation", "entry", "CLITestSubject.class")
        copyClassToBeInstrumentedIntoInFolder(_file1)
        final _file2 = Paths.get("dyco4j", "instrumentation", "entry", RESOURCE_FILE_NAME)
        copyResourceIntoInFolder(_file2)
    }

    private static final instrumentCode(args) {
        instrumentCode(CLI, args)
    }

    private static final executeInstrumentedCode() {
        executeInstrumentedCode(CLITestSubject)
    }

    @Test
    void withNoOptions() {
        assert instrumentCode([]) == [0L, 0L]
    }

    @Test
    void withOnlyInFolderOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER]) == [0L, 0L]
    }

    @Test
    void withOnlyOutFolderOption() {
        assert instrumentCode(["--out-folder", OUT_FOLDER]) == [0L, 0L]
    }

    @Test
    void withOnlyInFolderAndOutFolderOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER]) == [1L, 1L]

        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 5

        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        assert _traceLines[1] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/test1\(\)V/
        assert _traceLines[2] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/test2\(\)V/
        assert _traceLines[3] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix1\(\)V/
        assert _traceLines[4] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    @Test
    void withMethodNameRegexOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--method-name-regex",
                               '.*Suffix.$']) == [1L, 1L]

        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 3

        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        assert _traceLines[1] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix1\(\)V/
        assert _traceLines[2] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    @Test
    void withMethodNameRegexAndOnlyAnnotatedTestsOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--method-name-regex",
                               '.*Suffix.$', '--only-annotated-tests']) == [1L, 1L]

        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 2

        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        assert _traceLines[1] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }

    @Test
    void withAnnotatedTestsOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               '--only-annotated-tests']) == [1L, 1L]

        final _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines
        assert _traceLines.length == 3

        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        assert _traceLines[1] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/test2\(\)V/
        assert _traceLines[2] ==~ /\d+,marker:dyco4j\/instrumentation\/entry\/CLITestSubject\/testSuffix2\(\)V/
    }
}
