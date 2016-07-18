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

import static dyco4j.instrumentation.logging.Logger.*

class CLITest extends AbstractCLITest {
    private static final GET_ARRAY = ArrayAction.GETA.toString()
    private static final PUT_ARRAY = ArrayAction.PUTA.toString()
    private static final GET_FIELD = FieldAction.GETF.toString()
    private static final PUT_FIELD = FieldAction.PUTF.toString()

    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file = Paths.get("dyco4j", "instrumentation", "internals", "CLITestSubject.class")
        copyClassToBeInstrumentedIntoInFolder(_file)
    }

    private static assertNestingOfCallsIsValid(traceLines, numOfCalls) {
        final _stack = []
        def _cnt = 0
        for (String l in traceLines) {
            if (l =~ /$METHOD_ENTRY_TAG/) {
                _stack << l
            } else if (l =~ /$METHOD_EXIT_TAG/) {
                final String _tmp1 = _stack.pop()
                assert _tmp1.split(',')[1] == l.split(',')[1]
                _cnt++
            }
        }
        assert !_stack
        assert _cnt == numOfCalls: "${traceLines}"
    }

    private static assertTraceLengthIs(_executionResult, _numOfLines) {
        assert _executionResult.traceLines.size == _numOfLines
    }

    private static commonCheck(String[] traceLines, numOfCalls) {
        // should not raise exception
        Date.parseToStringDate(traceLines[0])

        final _numOfLines = traceLines.length - 1
        assertNestingOfCallsIsValid(traceLines[1.._numOfLines], numOfCalls)
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
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[30] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[31] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[33] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
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

        commonCheck(_traceLines, 2)

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
        commonCheck(_traceLines, 1)

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[3] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
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
        commonCheck(_traceLines, 1)

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[3] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/
    }

    @Test
    void withMethodNameRegexAndTraceMethodArgsOptions() {
        assert false
    }

    @Test
    void withMethodNameRegexAndTraceMethodReturnValueOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--methodNameRegex", ".*publishedStatic.*", "--traceMethodReturnValue"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 30)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 10)

        assert _traceLines[2] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[3] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[5] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[6] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[8] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[11] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[14] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[17] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[20] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[25] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines.count { it =~ /^$METHOD_RETURN_TAG/ } == 7
    }

    @Test
    void withTraceArrayAccessOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--traceArrayAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 57)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[25] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[26] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[25].split(',')[2] == _traceLines[26].split(',')[2]

        assert _traceLines[32] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[33] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[35] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[36] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[53] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[54] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[53].split(',')[2] == _traceLines[54].split(',')[2]

        assert _traceLines.count { it =~ /^$GET_ARRAY/ } == 2
        assert _traceLines.count { it =~ /^$PUT_ARRAY/ } == 2
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
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[29] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[27].split(',')[2] == _traceLines[29].split(',')[2]

        assert _traceLines[35] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[36] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[38] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[39] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[40] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[42] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[40].split(',')[1] == _traceLines[42].split(',')[1]
        assert _traceLines[40].split(',')[2] == _traceLines[42].split(',')[2]
        assert _traceLines[40].split(',')[3] == _traceLines[42].split(',')[3]

        assert _traceLines[58] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[59] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28].split(',')[3] == _traceLines[59].split(',')[3]
        assert _traceLines[60] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]

        assert _traceLines.count { it =~ /^$GET_ARRAY/ } == 2
        assert _traceLines.count { it =~ /^$PUT_ARRAY/ } == 2
        assert _traceLines.count { it =~ /^$GET_FIELD/ } == 4
        assert _traceLines.count { it =~ /^$PUT_FIELD/ } == 2
    }

    @Test
    void withTraceArrayAccessAndTraceMethodArgsOptions() {
        assert false
    }

    @Test
    void withTraceArrayAccessAndTraceMethodReturnValueOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--traceArrayAccess", "--traceMethodReturnValue"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 71)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[10] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[13] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[16] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[19] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[22] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[27] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[30] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[32] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[33] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[32].split(',')[2] == _traceLines[33].split(',')[2]

        assert _traceLines[39] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[40] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[42] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[43] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[45] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[48] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[51] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[54] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[57] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[62] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[65] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[67] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[68] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[67].split(',')[2] == _traceLines[68].split(',')[2]

        assert _traceLines.count { it =~ /^$GET_ARRAY/ } == 2
        assert _traceLines.count { it =~ /^$PUT_ARRAY/ } == 2
        assert _traceLines.count { it =~ /^$METHOD_RETURN_TAG/ } == 14
    }

    @Test
    void withTraceFieldAccessOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER, "--traceFieldAccess"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 59)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/

        assert _traceLines[33] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[34] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[36] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[37] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[38] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[40] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[38].split(',')[1] == _traceLines[40].split(',')[1]
        assert _traceLines[38].split(',')[2] == _traceLines[40].split(',')[2]
        assert _traceLines[38].split(',')[3] == _traceLines[40].split(',')[3]

        assert _traceLines[56] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[27].split(',')[3] == _traceLines[56].split(',')[3]

        assert _traceLines.count { it =~ /^$GET_FIELD/ } == 4
        assert _traceLines.count { it =~ /^$PUT_FIELD/ } == 2
    }

    @Test
    void withTraceFieldAccessAndTraceMethodArgsOptions() {
        assert false
    }

    @Test
    void withTraceFieldAccessAndTraceMethodReturnValueOptions() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--traceFieldAccess", "--traceMethodReturnValue"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 73)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[12] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[15] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[18] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[21] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[24] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[29] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[32] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[34] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/

        assert _traceLines[40] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[41] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[43] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[44] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[45] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[47] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[45].split(',')[1] == _traceLines[47].split(',')[1]
        assert _traceLines[45].split(',')[2] == _traceLines[47].split(',')[2]
        assert _traceLines[45].split(',')[3] == _traceLines[47].split(',')[3]

        assert _traceLines[48] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[51] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[54] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[57] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[60] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[65] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[68] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[70] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[34].split(',')[3] == _traceLines[70].split(',')[3]

        assert _traceLines.count { it =~ /^$GET_FIELD/ } == 4
        assert _traceLines.count { it =~ /^$PUT_FIELD/ } == 2
        assert _traceLines.count { it =~ /^$METHOD_RETURN_TAG/ } == 14
    }

    @Test
    void withTraceMethodArgsOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--traceMethodArgs"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 81)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 24)

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/
        assert _traceLines[5] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[6] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[8] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[9] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[13] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[16] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[19] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[22] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[25] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[28] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[31] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[35] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final def _objId = _traceLines[35].split(",")[2]
        assert _traceLines[36] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[39] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[41] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[42] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[43] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[45] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[46] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[47] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[49] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[52] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[53] ==~ /^$METHOD_ARG_TAG,1,${INT_TYPE_TAG}9$/
        assert _traceLines[56] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[57] ==~ /^$METHOD_ARG_TAG,1,${CHAR_TYPE_TAG}101$/
        assert _traceLines[60] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[61] ==~ /^$METHOD_ARG_TAG,1,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[64] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[65] ==~ /^$METHOD_ARG_TAG,1,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[68] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[69] ==~ /^$METHOD_ARG_TAG,1,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[72] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[73] ==~ /^$METHOD_ARG_TAG,1,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[76] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[77] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/
    }

    @Test
    void withTraceMethodArgsAndTraceMethodReturnValueOptions() {
        assert false
    }

    @Test
    void withTraceMethodReturnValueOption() {
        assert instrumentCode(["--in-folder", IN_FOLDER, "--out-folder", OUT_FOLDER,
                               "--traceMethodReturnValue"]) == 1:
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 67)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        commonCheck(_traceLines, 24)

        assert _traceLines[4] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[5] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[7] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[8] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[10] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[13] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[16] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[19] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[22] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[27] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[30] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[37] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[38] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[40] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[41] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[43] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[46] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[49] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[52] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[55] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[60] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[63] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines.count { it =~ /^$METHOD_RETURN_TAG/ } == 14
    }
}
