/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import com.google.gson.Gson
import dyco4j.instrumentation.AbstractCLITest
import dyco4j.utility.ProgramData
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Paths

import static dyco4j.logging.Logger.*

class CLITest extends AbstractCLITest {

    static final String IN_FOLDER_OPTION = "--$CLI.IN_FOLDER_OPTION"
    static final String OUT_FOLDER_OPTION = "--$CLI.OUT_FOLDER_OPTION"
    static final String METHOD_NAME_REGEX_OPTION = "--$CLI.METHOD_NAME_REGEX_OPTION"
    static final String TRACE_FIELD_ACCESS_OPTION = "--$CLI.TRACE_FIELD_ACCESS_OPTION"
    static final String TRACE_ARRAY_ACCESS_OPTION = "--$CLI.TRACE_ARRAY_ACCESS_OPTION"
    static final String TRACE_METHOD_ARGUMENTS_OPTION = "--$CLI.TRACE_METHOD_ARGUMENTS_OPTION"
    static final String TRACE_METHOD_RETURN_VALUE_OPTION = "--$CLI.TRACE_METHOD_RETURN_VALUE_OPTION"

    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file = Paths.get("dyco4j", "instrumentation", "internals", "CLITestSubject.class")
        copyClassesToBeInstrumentedIntoInFolder([_file])
        final _file2 = Paths.get("dyco4j", "instrumentation", "internals", RESOURCE_FILE_NAME)
        copyResourcesIntoInFolder([_file2])
    }

    static final instrumentCode(args) {
        instrumentCode(CLI, args)
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLITestSubject)
    }

    private static assertAllAndOnlyMatchingMethodsAreTraced(traceLines, methodNameRegex) {
        def _methodIds = traceLines.findAll { it ==~ /^$METHOD_ENTRY_TAG,.*/ }.collect { it.split(',')[1] }
        _methodIds += traceLines.findAll { it ==~ /^$METHOD_EXIT_TAG,.*/ }.collect { it.split(',')[1] }

        new File(CLI.PROGRAM_DATA_FILE_NAME).withReader { rdr ->
            final _prog_data = new Gson().fromJson(rdr, ProgramData.class)

            _methodIds.each {
                assert _prog_data.methodId2Name[it] ==~ methodNameRegex
            }

            _prog_data.methodId2Name.each { methodId, name ->
                assert !(name ==~ methodNameRegex) || methodId in _methodIds
            }
        }
    }

    @Test
    void withNoOptions() {
        assert instrumentCode([]) == [0L, 0L]
    }

    @Test
    void withOnlyInFolderOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER]) == [0L, 0L]
    }

    @Test
    void withOnlyOutFolderOption() {
        assert instrumentCode([OUT_FOLDER_OPTION, OUT_FOLDER]) == [0L, 0L]
    }

    @Test
    void withOnlyInFolderAndOutFolderOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 55)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 0, 4)

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
        final _methodNameRegex = ".*exercise.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 2)

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/
        final _tmp2 = _traceLines[3].split(',')[1]
        assert _traceLines[3] ==~ /^$METHOD_ENTRY_TAG,$_tmp2$/
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp2,N$/

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceArrayAccessOptions() {
        final _methodNameRegex = ".*exerciseStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex, TRACE_ARRAY_ACCESS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 1, 0, 0, 0, 1, 1)

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[3] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[2].split(',')[2] == _traceLines[3].split(',')[2]
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceFieldAccessOptions() {
        final _methodNameRegex = ".*exerciseStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_FIELD_ACCESS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 1, 0, 0, 0, 0, 0, 1, 1)

        final _tmp1 = _traceLines[1].split(',')[1]
        assert _traceLines[1] ==~ /^$METHOD_ENTRY_TAG,$_tmp1$/
        assert _traceLines[2] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[3] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[4] ==~ /^$METHOD_EXIT_TAG,$_tmp1,N$/

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceMethodArgsOptions() {
        final _methodNameRegex = ".*publishedStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 30)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 10, 7, 0, 2)

        assert _traceLines[2] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[3] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[5] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[6] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[10] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[13] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[16] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[19] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[22] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[25] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[28] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceMethodReturnValueOptions() {
        final _methodNameRegex = ".*publishedInstance.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 30)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 10, 0, 7, 2)

        assert _traceLines[2] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[3] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[5] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[6] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[8] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[11] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[14] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[17] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[20] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[25] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withTraceArrayAccessOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 59)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 0, 4, 2, 2)

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
    }

    @Test
    void withTraceArrayAccessAndTraceFieldAccessOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER, TRACE_ARRAY_ACCESS_OPTION,
                               TRACE_FIELD_ACCESS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 65)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 0, 4, 2, 2, 4, 2)

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

        def _line40 = _traceLines[40].split(',')
        def _line42 = _traceLines[42].split(',')
        assert (1..3).every { _line40[it] == _line42[it] }

        assert _traceLines[58] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[59] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28].split(',')[3] == _traceLines[59].split(',')[3]
        assert _traceLines[60] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]
    }

    @Test
    void withTraceArrayAccessAndTraceMethodArgsOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_OPTION, TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 91)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 32, 0, 4, 2, 2)

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

        assert _traceLines[33] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[34] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[33].split(',')[2] == _traceLines[34].split(',')[2]

        assert _traceLines[37] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[37].split(",")[2]
        assert _traceLines[38] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[41] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[43] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[44] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[45] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[47] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[48] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[49] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[51] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[54] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[55] ==~ /^$METHOD_ARG_TAG,1,${INT_TYPE_TAG}9$/
        assert _traceLines[58] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[59] ==~ /^$METHOD_ARG_TAG,1,${CHAR_TYPE_TAG}101$/
        assert _traceLines[62] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[63] ==~ /^$METHOD_ARG_TAG,1,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[66] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[67] ==~ /^$METHOD_ARG_TAG,1,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[70] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[71] ==~ /^$METHOD_ARG_TAG,1,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[74] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[75] ==~ /^$METHOD_ARG_TAG,1,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[78] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[79] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[81] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[82] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[81].split(',')[2] == _traceLines[82].split(',')[2]

        assert _traceLines[85] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[86] ==~ /^$METHOD_ARG_TAG,1,${LONG_TYPE_TAG}0$/
        assert _traceLines[87] ==~ /^$METHOD_ARG_TAG,2,${BYTE_TYPE_TAG}1$/
        assert _traceLines[88] ==~ /^$METHOD_ARG_TAG,3,${SHORT_TYPE_TAG}2$/
    }

    @Test
    void withTraceArrayAccessAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 73)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 14, 4, 2, 2)

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
    }

    @Test
    void withTraceFieldAccessOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 61)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 0, 4, 0, 0, 4, 2)

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
        def _line38 = _traceLines[38].split(',')
        def _line40 = _traceLines[40].split(',')
        assert (1..3).every { _line38[it] == _line40[it] }

        assert _traceLines[56] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[27].split(',')[3] == _traceLines[56].split(',')[3]
    }

    @Test
    void withTraceFieldAccessAndTraceMethodArgsOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER, TRACE_FIELD_ACCESS_OPTION,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 93)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 32, 0, 4, 0, 0, 4, 2)

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/
        assert _traceLines[5] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[6] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[8] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[9] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[10] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[12] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[10].split(',')[1] == _traceLines[12].split(',')[1]

        assert _traceLines[15] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[18] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[21] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[24] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[27] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[30] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[33] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[35] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/

        assert _traceLines[38] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[38].split(",")[2]
        assert _traceLines[39] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[42] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[44] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[45] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[46] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[48] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[49] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[50] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[51] ==~ /^$PUT_FIELD,f\d,$_objId,$STRING_TYPE_TAG\d+$/
        assert _traceLines[53] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[54] ==~ /^$GET_FIELD,f\d,$_objId,$STRING_TYPE_TAG\d+$/
        final _line51 = _traceLines[51].split(',')
        final _line54 = _traceLines[54].split(',')
        assert (1..3).every { _line51[it] == _line54[it] }

        assert _traceLines[57] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[58] ==~ /^$METHOD_ARG_TAG,1,${INT_TYPE_TAG}9$/
        assert _traceLines[61] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[62] ==~ /^$METHOD_ARG_TAG,1,${CHAR_TYPE_TAG}101$/
        assert _traceLines[65] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[66] ==~ /^$METHOD_ARG_TAG,1,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[69] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[70] ==~ /^$METHOD_ARG_TAG,1,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[73] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[74] ==~ /^$METHOD_ARG_TAG,1,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[77] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[78] ==~ /^$METHOD_ARG_TAG,1,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[81] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[82] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/
        assert _traceLines[87] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[88] ==~ /^$METHOD_ARG_TAG,1,${LONG_TYPE_TAG}0$/
        assert _traceLines[89] ==~ /^$METHOD_ARG_TAG,2,${BYTE_TYPE_TAG}1$/
        assert _traceLines[90] ==~ /^$METHOD_ARG_TAG,3,${SHORT_TYPE_TAG}2$/

        assert _traceLines[84] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[35].split(',')[3] == _traceLines[84].split(',')[3]
    }

    @Test
    void withTraceFieldAccessAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 75)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 14, 4, 0, 0, 4, 2)

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
        final _line45 = _traceLines[45].split(',')
        final _line47 = _traceLines[47].split(',')
        assert (1..3).every { _line45[it] == _line47[it] }

        assert _traceLines[48] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[51] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[54] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[57] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[60] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[65] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[68] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[70] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[34].split(',')[3] == _traceLines[70].split(',')[3]
    }

    @Test
    void withTraceMethodArgsOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 87)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 32, 0, 4)

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
        final _objId = _traceLines[35].split(",")[2]
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
        assert _traceLines[73] ==~ /^$METHOD_ARG_TAG,1,$STRING_TYPE_TAG\d+$/
        assert _traceLines[76] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[77] ==~ /^$METHOD_ARG_TAG,1,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[81] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[82] ==~ /^$METHOD_ARG_TAG,1,${LONG_TYPE_TAG}0$/
        assert _traceLines[83] ==~ /^$METHOD_ARG_TAG,2,${BYTE_TYPE_TAG}1$/
        assert _traceLines[84] ==~ /^$METHOD_ARG_TAG,3,${SHORT_TYPE_TAG}2$/
    }

    @Test
    void withTraceMethodArgsAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_ARGUMENTS_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 101)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 32, 14, 4)

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/
        assert _traceLines[5] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[6] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[8] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[9] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[11] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[14] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[15] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[18] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[19] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[22] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[23] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[26] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[27] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[30] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[33] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[34] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[37] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        assert _traceLines[38] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[42] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[42].split(",")[2]
        assert _traceLines[43] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[46] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[48] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[49] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.io.IOException$/
        assert _traceLines[50] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/
        assert _traceLines[52] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[53] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,java.lang.IllegalStateException$/
        assert _traceLines[54] ==~ /^$METHOD_EXIT_TAG,m\d+,E$/

        assert _traceLines[56] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[57] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[60] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[61] ==~ /^$METHOD_ARG_TAG,1,${INT_TYPE_TAG}9$/
        assert _traceLines[62] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[65] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[66] ==~ /^$METHOD_ARG_TAG,1,${CHAR_TYPE_TAG}101$/
        assert _traceLines[67] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[70] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[71] ==~ /^$METHOD_ARG_TAG,1,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[72] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[75] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[76] ==~ /^$METHOD_ARG_TAG,1,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[77] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[80] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[81] ==~ /^$METHOD_ARG_TAG,1,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[84] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[85] ==~ /^$METHOD_ARG_TAG,1,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[86] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[89] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[90] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/
        assert _traceLines[91] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/
        assert _traceLines[95] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[96] ==~ /^$METHOD_ARG_TAG,1,${LONG_TYPE_TAG}0$/
        assert _traceLines[97] ==~ /^$METHOD_ARG_TAG,2,${BYTE_TYPE_TAG}1$/
        assert _traceLines[98] ==~ /^$METHOD_ARG_TAG,3,${SHORT_TYPE_TAG}2$/
    }

    @Test
    void withTraceMethodReturnValueOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]
                "Class was not instrumented"

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 69)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 25, 0, 14, 4)

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
    }
}
