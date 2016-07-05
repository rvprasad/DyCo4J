/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import dyco4j.instrumentation.AbstractCLITest
import dyco4j.instrumentation.logging.Logger
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

import static Logger.METHOD_ENTRY_TAG
import static Logger.METHOD_EXIT_TAG
import static dyco4j.instrumentation.logging.Logger.METHOD_EXCEPTION_TAG

class CLITest extends AbstractCLITest {

    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file = Paths.get("dyco4j", "instrumentation", "internals", "CLITestSubject.class")
        copyClassToBeInstrumentedIntoInFolder(_file)
    }

    private static assertNestingOfCallsIsValid(lines) {
        final _stack = []
        for (String l in lines) {
            if (l =~ /$METHOD_ENTRY_TAG/) {
                _stack << l
            } else if (l =~ /$METHOD_EXIT_TAG/) {
                final String _tmp1 = _stack.pop()
                assert _tmp1.split(',')[1] == l.split(',')[1]
            }
        }
        assert !_stack
    }

    private static assertTraceLengthIs(_executionResult, _numOfLines) {
        assert _executionResult.traceLines.size == _numOfLines
    }

    private static commonCheck(String[] traceLines) {
        // should not raise exception
        Date.parseToStringDate(traceLines[0])

        final _numOfLines = traceLines.length - 1
        assertNestingOfCallsIsValid(traceLines[1.._numOfLines])

        for (i in 1..23) {
            final _tmp1 = "m$i"
            assert traceLines[1.._numOfLines].count {
                it ==~ /$METHOD_ENTRY_TAG,$_tmp1/ ||
                        it ==~ /$METHOD_EXIT_TAG,$_tmp1,[NE]/
            } == 2
        }
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLITestSubject)
    }

    private static instrumentCode(args) {
        instrumentCode(CLI, args)
    }

    private static removeThreadIdFromLog(final traceLines) {
        traceLines.collect { it.replaceAll(/^\d+,/, "") }
    }

    @After
    final void deleteAuxiliaryFiles() {
        final _tmp1 = Paths.get("auxiliary_data.json")
        if (Files.exists(_tmp1))
            Files.delete(_tmp1)
        final _tmp2 = Paths.get("auxiliary_data.json.bak")
        if (Files.exists(_tmp2))
            Files.delete(_tmp2)
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

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 53)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[30] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[31] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[33] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[34] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
    }

    @Test
    void withMethodNameRegexOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*exercise.*"]) == 1: "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)

        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/
        final _tmp2 = _traceLines[3].split(',')[1]
        assert _traceLines[3] ==~ /^$METHOD_ENTRY_TAG,$_tmp2$/
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp2,N$/
    }

    @Test
    void withMethodNameRegexAndTraceArrayAccessOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*exerciseStatic.*", "--traceArrayAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^PUTA,1,r_a:\d+,r_o:\d+$/
        assert _traceLines[3] ==~ /^GETA,2,r_a:\d+,r_o:null$/
        assert _traceLines[2].split(',')[2] == _traceLines[3].split(',')[2]
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/
    }

    @Test
    void withMethodNameRegexAndTraceFieldAccessOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*exerciseStatic.*", "--traceFieldAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        // should not raise exception
        Date.parseToStringDate(_traceLines[0])

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^PUTF,f\d,,p_i:4$/
        assert _traceLines[3] ==~ /^GETF,f\d,,r_o:\d+$/
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/
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

        assertTraceLengthIs(_executionResult, 57)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[25] ==~ /^PUTA,1,r_a:\d+,r_o:\d+$/
        assert _traceLines[26] ==~ /^GETA,2,r_a:\d+,r_o:null$/
        assert _traceLines[25].split(',')[2] == _traceLines[26].split(',')[2]

        assert _traceLines[32] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[33] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[35] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[36] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[53] ==~ /^PUTA,0,r_a:\d+,p_i:29$/
        assert _traceLines[54] ==~ /^GETA,1,r_a:\d+,p_i:0$/
        assert _traceLines[53].split(',')[2] == _traceLines[54].split(',')[2]
    }

    @Test
    void withTraceArrayAccessAndTraceFieldAccessOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--traceArrayAccess",
                               "--traceFieldAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 63)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[9] ==~ /^PUTF,f\d,,p_i:4$/
        assert _traceLines[11] ==~ /^GETF,f\d,,p_i:4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^PUTA,1,r_a:\d+,r_o:\d+$/
        assert _traceLines[28] ==~ /^GETF,f\d,,r_o:\d+$/
        assert _traceLines[29] ==~ /^GETA,2,r_a:\d+,r_o:null$/
        assert _traceLines[27].split(',')[2] == _traceLines[29].split(',')[2]

        assert _traceLines[35] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[36] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[38] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[39] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[40] ==~ /^PUTF,f\d,r_o:\d+,r_s:\d+$/
        assert _traceLines[42] ==~ /^GETF,f\d,r_o:\d+,r_s:\d+$/
        assert _traceLines[40].split(',')[1] == _traceLines[42].split(',')[1]
        assert _traceLines[40].split(',')[2] == _traceLines[42].split(',')[2]
        assert _traceLines[40].split(',')[3] == _traceLines[42].split(',')[3]

        assert _traceLines[58] ==~ /^PUTA,0,r_a:\d+,p_i:29$/
        assert _traceLines[59] ==~ /^GETF,f\d,,r_o:\d+$/
        assert _traceLines[28].split(',')[3] == _traceLines[59].split(',')[3]
        assert _traceLines[60] ==~ /^GETA,1,r_a:\d+,p_i:0$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]
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
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--traceFieldAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 59)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[9] ==~ /^PUTF,f\d,,p_i:4$/
        assert _traceLines[11] ==~ /^GETF,f\d,,p_i:4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^GETF,f\d,,r_o:\d+$/

        assert _traceLines[33] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.io.IOException$/
        assert _traceLines[34] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[36] ==~ /^$METHOD_EXCEPTION_TAG,r_t:\d+,java.lang.IllegalStateException$/
        assert _traceLines[37] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[38] ==~ /^PUTF,f\d,r_o:\d+,r_s:\d+$/
        assert _traceLines[40] ==~ /^GETF,f\d,r_o:\d+,r_s:\d+$/
        assert _traceLines[38].split(',')[1] == _traceLines[40].split(',')[1]
        assert _traceLines[38].split(',')[2] == _traceLines[40].split(',')[2]
        assert _traceLines[38].split(',')[3] == _traceLines[40].split(',')[3]

        assert _traceLines[56] ==~ /^GETF,f\d,,r_o:\d+$/
        assert _traceLines[27].split(',')[3] == _traceLines[56].split(',')[3]
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
