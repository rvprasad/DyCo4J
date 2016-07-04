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
                assert _tmp1.split(',')[1] == l.split(',')[1]
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
        final _numOfLines = 53
        assert _traceLines.length == _numOfLines

        commonCheck(_traceLines, _numOfLines)

        assert _traceLines[4] ==~ /^exception,r_t:\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^exit,m\d+,E/
        assert _traceLines[7] ==~ /^exception,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^exit,m\d+,E/
        assert _traceLines[30] ==~ /^exception,r_t:\d+,java.io.IOException$/
        assert _traceLines[31] ==~ /^exit,m\d+,E/
        assert _traceLines[33] ==~ /^exception,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[34] ==~ /^exit,m\d+,E/
    }

    @Test
    void withMethodNameRegexOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*exercise.*"]) == 1: "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines.collect { it.replaceAll(/\d+,\d+,/, "") }
        final _numOfLines = 5
        assert _traceLines.length == _numOfLines

        // should not raise exception
        Date.parseToStringDate(_traceLines[0].split(',')[1])

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^entry,$_tmp1/
        assert _traceLines[2] ==~ /^exit,$_tmp1,N/
        final _tmp2 = _traceLines[3].split(',')[1]
        assert _traceLines[3] ==~ /^entry,$_tmp2/
        assert _traceLines[4] ==~ /^exit,$_tmp2,N/
    }

    @Test
    void withMethodNameRegexAndTraceArrayAccessOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*exerciseStatic.*", "--traceArrayAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines.collect { it.replaceAll(/\d+,\d+,/, "") }
        final _numOfLines = 5
        assert _traceLines.length == _numOfLines

        // should not raise exception
        Date.parseToStringDate(_traceLines[0].split(',')[1])

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^entry,$_tmp1/
        assert _traceLines[2] ==~ /^PUTA,1,r_a:\d+,p_i:28$/
        assert _traceLines[3] ==~ /^GETA,2,r_a:\d+,p_i:0$/
        assert _traceLines[2].split(',')[2] == _traceLines[3].split(',')[2]
        assert _traceLines[4] ==~ /^exit,$_tmp1,N/
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
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--traceArrayAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        final String[] _traceLines = _executionResult.traceLines.collect { it.replaceAll(/\d+,\d+,/, "") }
        final _numOfLines = 62
        assert _traceLines.length == _numOfLines

        commonCheck(_traceLines, _numOfLines)

        assert _traceLines[4] ==~ /^exception,r_t:\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^exit,m\d+,E/
        assert _traceLines[7] ==~ /^exception,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^exit,m\d+,E/

        assert _traceLines[25] ==~ /^PUTA,1,r_a:\d+,p_i:28$/
        assert _traceLines[26] ==~ /^GETA,2,r_a:\d+,p_i:0$/
        assert _traceLines[25].split(',')[2] == _traceLines[26].split(',')[2]

        assert _traceLines[32] ==~ /^exception,r_t:\d+,java.io.IOException$/
        assert _traceLines[33] ==~ /^exit,m\d+,E/
        assert _traceLines[35] ==~ /^exception,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[36] ==~ /^exit,m\d+,E/

        assert _traceLines[53] ==~ /^PUTA,0,r_a:\d+,p_i:29$/
        assert _traceLines[54] ==~ /^GETA,1,r_a:\d+,p_i:0$/
        assert _traceLines[53].split(',')[2] == _traceLines[54].split(',')[2]
    }

    private void commonCheck(String[] traceLines, int numOfLines) {
        // should not raise exception
        Date.parseToStringDate(traceLines[0].split(',')[1])

        assertNestingOfCallsIsValid(traceLines[1..numOfLines - 1])

        for (i in 1..23) {
            final _tmp1 = "m$i"
            assert traceLines[1..numOfLines - 1].count { it ==~ /entry,$_tmp1/ || it ==~ /exit,$_tmp1,[NE]/ } == 2
        }
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
    void withTraceMethodReturnValueOption() {
        assert false
    }
}
