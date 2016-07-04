/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import dyco4j.instrumentation.AbstractCLITest
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

class CLITest extends AbstractCLITest {
    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file = Paths.get("dyco4j", "instrumentation", "internals", "CLITestSubject.class")
        copyClassToBeInstrumentedIntoInFolder(_file)
    }

    @After
    void deleteAuxiliaryFiles() {
        final _tmp1 = Paths.get("auxiliary_data.json")
        if (Files.exists(_tmp1))
            Files.delete(_tmp1)
        final _tmp2 = Paths.get("auxiliary_data.json.bak")
        if (Files.exists(_tmp2))
            Files.delete(_tmp2)
    }

    private static final instrumentCode(args) {
        instrumentCode(CLI, args)
    }

    private static final executeInstrumentedCode() {
        executeInstrumentedCode(CLITestSubject)
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

    private static assertNestingOfCallsIsValid(lines) {
        final _stack = []
        for (String l in lines) {
            if (l =~ /entry/) {
                _stack << l
            } else if (l =~ /exit/) {
                final String _tmp1 = _stack.pop()
                assert _tmp1.split(",")[1] == l.split(",")[1]
            }
        }
        assert !_stack
    }

    @Test
    void withOnlyInFolderAndOutFolderOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines.collect { it.replaceAll(/\d+,\d+,/, "") }
        final num_of_lines = 53
        assert _traceLines.length == num_of_lines: "${_traceLines.join('/')}"

        // should not raise exception
        Date.parseToStringDate(_traceLines[0].split(',')[1])

        assertNestingOfCallsIsValid(_traceLines[1..num_of_lines - 1])

        for (i in 1..23) {
            final _tmp1 = "m$i"
            assert _traceLines[1..num_of_lines - 1].count { it ==~ /entry,$_tmp1/ || it ==~ /exit,$_tmp1,[NE]/ } == 2
        }

        assert _traceLines[20] ==~ /^exception,r_t:\d+,java.io.IOException$/
        assert _traceLines[21] ==~ /^exit,m\d+,E/
        assert _traceLines[23] ==~ /^exception,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[24] ==~ /^exit,m\d+,E/
        assert _traceLines[46] ==~ /^exception,r_t:\d+,java.io.IOException$/
        assert _traceLines[47] ==~ /^exit,m\d+,E/
        assert _traceLines[49] ==~ /^exception,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[50] ==~ /^exit,m\d+,E/
    }

    @Test
    void withMethodNameRegexOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*exercise.*"]) == 1: "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines.collect { it.replaceAll(/\d+,\d+,/, "") }
        final num_of_lines = 5
        assert _traceLines.length == num_of_lines: "${_traceLines.join('/')}"

        // should not raise exception
        Date.parseToStringDate(_traceLines[0].split(',')[1])

        final _tmp1 = _traceLines[1].split(",")[1]
        assert _traceLines[1] ==~ /^entry,$_tmp1/
        assert _traceLines[2] ==~ /^exit,$_tmp1,N/
        final _tmp2 = _traceLines[3].split(",")[1]
        assert _traceLines[3] ==~ /^entry,$_tmp2/
        assert _traceLines[4] ==~ /^exit,$_tmp2,N/
    }

    @Test
    void withMethodNameRegexAndTraceArrayAccessOptions() {
        assert false
    }

    @Test
    void withMethodNameRegexAndTraceFieldAccessOptions() {
        assert false
    }

    @Test
    void withMethodNameRegexAndTraceMethodArgsOptions() {
        assert false
    }

    @Test
    void withMethodNameRegexAndTraceMethodReturnValueOptions() {
        assert false
    }

    @Test
    void withTraceArrayAccessOption() {
        assert false
    }

    @Test
    void withTraceArrayAccessAndTraceFieldAccessOptions() {
        assert false
    }

    @Test
    void withTraceArrayAccessAndTraceMethodArgsOptions() {
        assert false
    }

    @Test
    void withTraceArrayAccessAndTraceMethodReturnValueOptions() {
        assert false
    }

    @Test
    void withTraceFieldAccessOption() {
        assert false
    }

    @Test
    void withTraceFieldAccessAndTraceMethodArgsOptions() {
        assert false
    }

    @Test
    void withTraceFieldAccessAndTraceMethodReturnValueOptions() {
        assert false
    }

    @Test
    void withTraceMethodArgsOption() {
        assert false
    }

    @Test
    void withTraceMethodArgsAndTraceMethodReturnValueOptions() {
        assert false
    }

    @Test
    void withTraceMethodReturValueOption() {
        assert false
    }
}
