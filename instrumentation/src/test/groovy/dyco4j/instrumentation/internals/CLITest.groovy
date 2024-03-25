/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import dyco4j.instrumentation.AbstractCLITest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.nio.file.Paths

import static dyco4j.logging.Logger.*

class CLITest extends AbstractCLITest {
    static final String IN_FOLDER_OPTION = "--$CLI.IN_FOLDER_OPTION"
    static final String OUT_FOLDER_OPTION = "--$CLI.OUT_FOLDER_OPTION"
    static final String METHOD_NAME_REGEX_OPTION = "--$CLI.METHOD_NAME_REGEX_OPTION"
    static final String TRACE_FIELD_ACCESS_WITH_VALUES_OPTION = "--$CLI.TRACE_FIELD_ACCESS_OPTION=$CLI.AccessOption.with_values"
    static final String TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION = "--$CLI.TRACE_FIELD_ACCESS_OPTION=$CLI.AccessOption.without_values"
    static final String TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION = "--$CLI.TRACE_ARRAY_ACCESS_OPTION=$CLI.AccessOption.with_values"
    static final String TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION = "--$CLI.TRACE_ARRAY_ACCESS_OPTION=$CLI.AccessOption.without_values"
    static final String TRACE_METHOD_ARGUMENTS_OPTION = "--$CLI.TRACE_METHOD_ARGUMENTS_OPTION"
    static final String TRACE_METHOD_RETURN_VALUE_OPTION = "--$CLI.TRACE_METHOD_RETURN_VALUE_OPTION"
    static final String TRACE_METHOD_CALL_OPTION = "--$CLI.TRACE_METHOD_CALL_OPTION"

    @BeforeAll
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file = Paths.get("dyco4j", "instrumentation", "internals", "CLITestSubject.class")
        copyClassesToBeInstrumentedIntoInFolder([_file])
        final _file2 = Paths.get("dyco4j", "instrumentation", "internals", RESOURCE_FILE_NAME)
        copyResourcesIntoInFolder([_file2])
    }

    static final instrumentCode(args) {
        instrumentCode(CLI, args)
    }

    private static assertCallEntryCoupling(traceLines) {
        final _ = traceLines.tail().inject(null) { String prev, String line ->
            if (line ==~ /^$METHOD_ENTRY_TAG,.*/ && prev) {
                final _tmp1 = prev.split(',')
                final _tmp2 = line.split(',')
                assert _tmp1[1] == _tmp2[1] && _tmp1[0] == METHOD_CALL_TAG
            }
            line
        }
    }

    private static assertExceptionLogs(traceLines, indices) {
        indices.each {
            assert traceLines[it.key] ==~ /^$METHOD_EXCEPTION_TAG,$THROWABLE_TYPE_TAG\d+,${it.value}$/
        }
    }

    private static assertCallSitesOccurOnlyOnce(traceLines) {
        final _ = traceLines.inject([[], []]) { result, String line ->
            def (_seen, _stack) = result
            if (line ==~ /^$METHOD_ENTRY_TAG,.*/) {
                _stack.push(_seen)
                _seen.clear()
            } else if (line ==~ /^$METHOD_EXIT_TAG,.*/) {
                _seen = _stack.pop()
            } else if (line ==~ /^$METHOD_CALL_TAG,.*/) {
                final _tmp = line.split(',')[2]
                assert !(_tmp in _seen)
                _seen.push(_tmp)
            }
            [_seen, _stack]
        }
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLITestSubject)
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
        assertFreqOfLogs(numOfExceptionLogs: 4, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          30: 'java.io.IOException',
                                          33: 'java.lang.IllegalStateException',])
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
        assertFreqOfLogs(_traceLines, 2)

        assertPropertiesAboutExit(_traceLines)

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceArrayAccessWithValueOptions() {
        final _methodNameRegex = ".*exerciseStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs([numOfGetArrayLogs: 1, numOfPutArrayLogs: 1], _traceLines, 1)

        assertCallEntryCoupling(_traceLines)

        assertPropertiesAboutExit(_traceLines)

        assert _traceLines[2] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[3] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[2].split(',')[2] == _traceLines[3].split(',')[2]

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceArrayAccessWithoutValueOptions() {
        final _methodNameRegex = ".*exerciseStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfGetArrayLogs: 1, numOfPutArrayLogs: 1, _traceLines, 1)

        assertCallEntryCoupling(_traceLines)

        assertPropertiesAboutExit(_traceLines)

        assert _traceLines[2] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[3] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[2].split(',')[2] == _traceLines[3].split(',')[2]

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceFieldAccessWithValuesOptions() {
        final _methodNameRegex = ".*exerciseStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfGetFieldLogs: 1, numOfPutFieldLogs: 1, _traceLines, 1)

        assertCallEntryCoupling(_traceLines)

        assertPropertiesAboutExit(_traceLines)

        assert _traceLines[2] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[3] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)
    }

    @Test
    void withMethodNameRegexAndTraceFieldAccessWithoutValuesOptions() {
        final _methodNameRegex = ".*exerciseStatic.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 5)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfGetFieldLogs: 1, numOfPutFieldLogs: 1, _traceLines, 1)

        assertCallEntryCoupling(_traceLines)

        assertPropertiesAboutExit(_traceLines)

        assert _traceLines[2] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[3] ==~ /^$GET_FIELD,f\d,\*,\*$/

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
        assertFreqOfLogs(numOfArgLogs: 7, numOfExceptionLogs: 2, _traceLines, 10)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [2: 'java.io.IOException',
                                          5: 'java.lang.IllegalStateException'])

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
        assertFreqOfLogs(numOfReturnLogs: 7, numOfExceptionLogs: 2, _traceLines, 10)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [2: 'java.io.IOException',
                                          5: 'java.lang.IllegalStateException'])

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
    void withMethodNameRegexAndTraceMethodCallOptions() {
        final _methodNameRegex = ".*exercise.*"
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               METHOD_NAME_REGEX_OPTION, _methodNameRegex,
                               TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 30)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfCallLogs: 25, _traceLines, 2)

        assertPropertiesAboutExit(_traceLines)

        assertAllAndOnlyMatchingMethodsAreTraced(_traceLines, _methodNameRegex)

        assert _traceLines.count { it ==~ /^$METHOD_CALL_TAG,.*/ } == 25

        def _prev = null
        for (final l in _traceLines.tail()) {
            if (l ==~ /^$METHOD_ENTRY_TAG,.*/ && _prev) {
                assert _prev.split(',')[0] != METHOD_CALL_TAG
            }
            _prev = l
        }

        assertCallSitesOccurOnlyOnce(_traceLines)
    }

    @Test
    void withTraceArrayAccessWithValuesOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 59)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          32: 'java.io.IOException',
                                          35: 'java.lang.IllegalStateException'])

        assert _traceLines[25] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[26] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[25].split(',')[2] == _traceLines[26].split(',')[2]

        assert _traceLines[53] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[54] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[53].split(',')[2] == _traceLines[54].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithValuesAndTraceFieldAccessWithValuesOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 65)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, numOfGetFieldLogs: 4,
                numOfPutFieldLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          35: 'java.io.IOException',
                                          38: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[29] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[27].split(',')[2] == _traceLines[29].split(',')[2]

        assert _traceLines[40] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[42] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/

        final _line40 = _traceLines[40].split(',')
        final _line42 = _traceLines[42].split(',')
        assert _line40[1..3] == _line42[1..3]

        assert _traceLines[58] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[59] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28].split(',')[3] == _traceLines[59].split(',')[3]
        assert _traceLines[60] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithValuesAndTraceFieldAccessWithoutValuesOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 65)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, numOfGetFieldLogs: 4,
                numOfPutFieldLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          35: 'java.io.IOException',
                                          38: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[29] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[27].split(',')[2] == _traceLines[29].split(',')[2]

        assert _traceLines[40] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[42] ==~ /^$GET_FIELD,f\d,\*,\*$/

        final _line40 = _traceLines[40].split(',')
        final _line42 = _traceLines[42].split(',')
        assert _line40[1] == _line42[1]

        assert _traceLines[58] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[59] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[60] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithValuesAndTraceMethodArgsOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION, TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 91)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 32, numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [5 : 'java.io.IOException',
                                          8 : 'java.lang.IllegalStateException',
                                          44: 'java.io.IOException',
                                          48: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

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

        assert _traceLines[37] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[38] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[41] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[41].split(",")[2]
        assert _traceLines[43] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[47] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

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
    void withTraceArrayAccessWithValuesAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 73)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfReturnLogs: 14, numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          39: 'java.io.IOException',
                                          42: 'java.lang.IllegalStateException'])

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
    void withTraceArrayAccessWithValuesAndTraceMethodCallOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITH_VALUES_OPTION, TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 101)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, numOfCallLogs: 42,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [7 : 'java.io.IOException',
                                          12: 'java.lang.IllegalStateException',
                                          56: 'java.io.IOException',
                                          61: 'java.lang.IllegalStateException'])

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/

        assert _traceLines[42] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[43] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,$NULL_VALUE$/
        assert _traceLines[42].split(',')[2] == _traceLines[43].split(',')[2]

        assert _traceLines[91] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}29$/
        assert _traceLines[92] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,${INT_TYPE_TAG}0$/
        assert _traceLines[91].split(',')[2] == _traceLines[92].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithoutValuesOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 59)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          32: 'java.io.IOException',
                                          35: 'java.lang.IllegalStateException'])

        assert _traceLines[25] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[26] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[25].split(',')[2] == _traceLines[26].split(',')[2]

        assert _traceLines[53] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[54] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[53].split(',')[2] == _traceLines[54].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithoutValuesAndTraceFieldAccessWithValuesOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 65)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, numOfGetFieldLogs: 4,
                numOfPutFieldLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          35: 'java.io.IOException',
                                          38: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[28] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[29] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[27].split(',')[2] == _traceLines[29].split(',')[2]

        assert _traceLines[40] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[42] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/

        final _line40 = _traceLines[40].split(',')
        final _line42 = _traceLines[42].split(',')
        assert _line40[1..3] == _line42[1..3]

        assert _traceLines[58] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[59] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[28].split(',')[3] == _traceLines[59].split(',')[3]
        assert _traceLines[60] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithoutValuesAndTraceFieldAccessWithoutValuesOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 65)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, numOfGetFieldLogs: 4,
                numOfPutFieldLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          35: 'java.io.IOException',
                                          38: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[28] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[29] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[27].split(',')[2] == _traceLines[29].split(',')[2]

        assert _traceLines[40] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[42] ==~ /^$GET_FIELD,f\d,\*,\*$/

        final _line40 = _traceLines[40].split(',')
        final _line42 = _traceLines[42].split(',')
        assert _line40[1] == _line42[1]

        assert _traceLines[58] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[59] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[60] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[58].split(',')[2] == _traceLines[60].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithoutValuesAndTraceMethodArgsOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION, TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 91)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 32, numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [5 : 'java.io.IOException',
                                          8 : 'java.lang.IllegalStateException',
                                          44: 'java.io.IOException',
                                          48: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

        assert _traceLines[13] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[16] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[19] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[22] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[25] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[28] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[31] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[33] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[34] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[33].split(',')[2] == _traceLines[34].split(',')[2]

        assert _traceLines[37] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[38] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[41] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[41].split(",")[2]
        assert _traceLines[43] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[47] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

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

        assert _traceLines[81] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[82] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[81].split(',')[2] == _traceLines[82].split(',')[2]

        assert _traceLines[85] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[86] ==~ /^$METHOD_ARG_TAG,1,${LONG_TYPE_TAG}0$/
        assert _traceLines[87] ==~ /^$METHOD_ARG_TAG,2,${BYTE_TYPE_TAG}1$/
        assert _traceLines[88] ==~ /^$METHOD_ARG_TAG,3,${SHORT_TYPE_TAG}2$/
    }

    @Test
    void withTraceArrayAccessWithoutValuesAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 73)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfReturnLogs: 14, numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          39: 'java.io.IOException',
                                          42: 'java.lang.IllegalStateException'])

        assert _traceLines[10] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[13] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[16] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[19] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[22] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[27] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[30] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[32] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[33] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[32].split(',')[2] == _traceLines[33].split(',')[2]

        assert _traceLines[45] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[48] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[51] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[54] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[57] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[62] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[65] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[67] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[68] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[67].split(',')[2] == _traceLines[68].split(',')[2]
    }

    @Test
    void withTraceArrayAccessWithoutValuesAndTraceMethodCallOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_ARRAY_ACCESS_WITHOUT_VALUES_OPTION, TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 101)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetArrayLogs: 2, numOfPutArrayLogs: 2, numOfCallLogs: 42,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [7 : 'java.io.IOException',
                                          12: 'java.lang.IllegalStateException',
                                          56: 'java.io.IOException',
                                          61: 'java.lang.IllegalStateException'])

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/

        assert _traceLines[42] ==~ /^$PUT_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[43] ==~ /^$GET_ARRAY,2,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[42].split(',')[2] == _traceLines[43].split(',')[2]

        assert _traceLines[91] ==~ /^$PUT_ARRAY,0,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[92] ==~ /^$GET_ARRAY,1,$ARRAY_TYPE_TAG\d+,\*$/
        assert _traceLines[91].split(',')[2] == _traceLines[92].split(',')[2]
    }

    @Test
    void withTraceFieldAccessWithValuesOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 61)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          33: 'java.io.IOException',
                                          36: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/

        assert _traceLines[38] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[40] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        final _line38 = _traceLines[38].split(',')
        final _line40 = _traceLines[40].split(',')
        assert _line38[1..3] == _line40[1..3]

        assert _traceLines[56] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[27].split(',')[3] == _traceLines[56].split(',')[3]
    }

    @Test
    void withTraceFieldAccessWithValuesAndTraceMethodArgsOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 93)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 32, numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [5 : 'java.io.IOException',
                                          8 : 'java.lang.IllegalStateException',
                                          45: 'java.io.IOException',
                                          49: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

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

        assert _traceLines[38] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[39] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[42] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[42].split(",")[2]
        assert _traceLines[44] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[48] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

        assert _traceLines[51] ==~ /^$PUT_FIELD,f\d,$_objId,$STRING_TYPE_TAG\d+$/
        assert _traceLines[53] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[54] ==~ /^$GET_FIELD,f\d,$_objId,$STRING_TYPE_TAG\d+$/
        final _line51 = _traceLines[51].split(',')
        final _line54 = _traceLines[54].split(',')
        assert _line51[1..3] == _line54[1..3]

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
    void withTraceFieldAccessWithValuesAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 75)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfReturnLogs: 14, numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          40: 'java.io.IOException',
                                          43: 'java.lang.IllegalStateException'])

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

        assert _traceLines[45] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[47] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        final _line45 = _traceLines[45].split(',')
        final _line47 = _traceLines[47].split(',')
        assert _line45[1..3] == _line47[1..3]

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
    void withTraceFieldAccessWithValuesAndTraceMethodCallOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITH_VALUES_OPTION,
                               TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 103)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2, numOfCallLogs: 42,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [7 : 'java.io.IOException',
                                          12: 'java.lang.IllegalStateException',
                                          57: 'java.io.IOException',
                                          62: 'java.lang.IllegalStateException'])

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/

        assert _traceLines[14] ==~ /^$PUT_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[17] ==~ /^$GET_FIELD,f\d,,${INT_TYPE_TAG}4$/
        assert _traceLines[14].split(',')[1] == _traceLines[14].split(',')[1]

        assert _traceLines[44] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/

        assert _traceLines[64] ==~ /^$PUT_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        assert _traceLines[67] ==~ /^$GET_FIELD,f\d,$OBJECT_TYPE_TAG\d+,$STRING_TYPE_TAG\d+$/
        final _line64 = _traceLines[64].split(',')
        final _line67 = _traceLines[67].split(',')
        assert _line64[1..3] == _line67[1..3]

        assert _traceLines[94] ==~ /^$GET_FIELD,f\d,,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[44].split(',')[3] == _traceLines[94].split(',')[3]
    }

    @Test
    void withTraceFieldAccessWithoutValuesOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 61)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          33: 'java.io.IOException',
                                          36: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[27] ==~ /^$GET_FIELD,f\d,\*,\*$/

        assert _traceLines[38] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[40] ==~ /^$GET_FIELD,f\d,\*,\*$/
        final _line38 = _traceLines[38].split(',')
        final _line40 = _traceLines[40].split(',')
        assert _line38[1] == _line40[1]

        assert _traceLines[56] ==~ /^$GET_FIELD,f\d,\*,\*$/
    }

    @Test
    void withTraceFieldAccessWithoutValuesAndTraceMethodArgsOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 93)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 32, numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [5 : 'java.io.IOException',
                                          8 : 'java.lang.IllegalStateException',
                                          45: 'java.io.IOException',
                                          49: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

        assert _traceLines[10] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[12] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[10].split(',')[1] == _traceLines[12].split(',')[1]

        assert _traceLines[15] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[18] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[21] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[24] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[27] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[30] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[33] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[35] ==~ /^$GET_FIELD,f\d,\*,\*$/

        assert _traceLines[38] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[39] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[42] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[42].split(",")[2]
        assert _traceLines[44] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[48] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

        assert _traceLines[51] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[53] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[54] ==~ /^$GET_FIELD,f\d,\*,\*$/
        final _line51 = _traceLines[51].split(',')
        final _line54 = _traceLines[54].split(',')
        assert _line51[1] == _line54[1]

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

        assert _traceLines[84] ==~ /^$GET_FIELD,f\d,\*,\*$/
    }

    @Test
    void withTraceFieldAccessWithoutValuesAndTraceMethodReturnValueOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION, TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 75)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfReturnLogs: 14, numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          40: 'java.io.IOException',
                                          43: 'java.lang.IllegalStateException'])

        assert _traceLines[9] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[11] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[9].split(',')[1] == _traceLines[11].split(',')[1]

        assert _traceLines[12] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[15] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[18] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[21] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[24] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[29] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[32] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[34] ==~ /^$GET_FIELD,f\d,\*,\*$/

        assert _traceLines[45] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[47] ==~ /^$GET_FIELD,f\d,\*,\*$/
        final _line45 = _traceLines[45].split(',')
        final _line47 = _traceLines[47].split(',')
        assert _line45[1] == _line47[1]

        assert _traceLines[48] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[51] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[54] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[57] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[60] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[65] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[68] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[70] ==~ /^$GET_FIELD,f\d,\*,\*$/
    }

    @Test
    void withTraceFieldAccessWithoutValuesAndTraceMethodCallOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_WITHOUT_VALUES_OPTION,
                               TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 103)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfGetFieldLogs: 4, numOfPutFieldLogs: 2, numOfCallLogs: 42,
                _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [7 : 'java.io.IOException',
                                          12: 'java.lang.IllegalStateException',
                                          57: 'java.io.IOException',
                                          62: 'java.lang.IllegalStateException'])

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/

        assert _traceLines[14] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[17] ==~ /^$GET_FIELD,f\d,\*,\*$/
        assert _traceLines[14].split(',')[1] == _traceLines[14].split(',')[1]

        assert _traceLines[44] ==~ /^$GET_FIELD,f\d,\*,\*$/

        assert _traceLines[64] ==~ /^$PUT_FIELD,f\d,\*,\*$/
        assert _traceLines[67] ==~ /^$GET_FIELD,f\d,\*,\*$/
        final _line64 = _traceLines[64].split(',')
        final _line67 = _traceLines[67].split(',')
        assert _line64[1] == _line67[1]

        assert _traceLines[94] ==~ /^$GET_FIELD,f\d,\*,\*$/
    }

    @Test
    void withTraceMethodArgsOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 87)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 32, numOfExceptionLogs: 4, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [5 : 'java.io.IOException',
                                          8 : 'java.lang.IllegalStateException',
                                          42: 'java.io.IOException',
                                          46: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

        assert _traceLines[13] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[16] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[19] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[22] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[25] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[28] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[31] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[35] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[36] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[39] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[39].split(",")[2]
        assert _traceLines[41] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[45] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

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
        assertFreqOfLogs(numOfArgLogs: 32, numOfReturnLogs: 14, numOfExceptionLogs: 4, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [5 : 'java.io.IOException',
                                          8 : 'java.lang.IllegalStateException',
                                          49: 'java.io.IOException',
                                          53: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

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

        assert _traceLines[42] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[43] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[46] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[46].split(",")[2]
        assert _traceLines[48] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[52] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

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
    void withTraceMethodArgsAndTraceMethodCallOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_ARGUMENTS_OPTION, TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 129)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 32, numOfExceptionLogs: 4, numOfCallLogs: 42, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [8 : 'java.io.IOException',
                                          13: 'java.lang.IllegalStateException',
                                          66: 'java.io.IOException',
                                          72: 'java.lang.IllegalStateException'])

        assert _traceLines[2] ==~ /^$METHOD_ARG_TAG,0,$ARRAY_TYPE_TAG\d+$/

        assert _traceLines[20] ==~ /^$METHOD_ARG_TAG,0,${INT_TYPE_TAG}9$/
        assert _traceLines[24] ==~ /^$METHOD_ARG_TAG,0,${CHAR_TYPE_TAG}101$/
        assert _traceLines[28] ==~ /^$METHOD_ARG_TAG,0,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[32] ==~ /^$METHOD_ARG_TAG,0,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[36] ==~ /^$METHOD_ARG_TAG,0,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[40] ==~ /^$METHOD_ARG_TAG,0,${STRING_TYPE_TAG}\d+$/
        assert _traceLines[46] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[55] ==~ /^$METHOD_ARG_TAG,0,$UNINITIALIZED_THIS_REP$/
        assert _traceLines[56] ==~ /^$METHOD_ARG_TAG,1,${OBJECT_TYPE_TAG}\d+$/

        assert _traceLines[61] ==~ /^$METHOD_ARG_TAG,0,${OBJECT_TYPE_TAG}\d+$/
        final _objId = _traceLines[61].split(",")[2]
        assert _traceLines[64] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[70] ==~ /^$METHOD_ARG_TAG,0,$_objId$/

        assert _traceLines[76] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[81] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[82] ==~ /^$METHOD_ARG_TAG,1,${INT_TYPE_TAG}9$/
        assert _traceLines[86] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[87] ==~ /^$METHOD_ARG_TAG,1,${CHAR_TYPE_TAG}101$/
        assert _traceLines[91] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[92] ==~ /^$METHOD_ARG_TAG,1,${FLOAT_TYPE_TAG}323.3$/
        assert _traceLines[96] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[97] ==~ /^$METHOD_ARG_TAG,1,${DOUBLE_TYPE_TAG}898.98$/
        assert _traceLines[101] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[102] ==~ /^$METHOD_ARG_TAG,1,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[106] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[107] ==~ /^$METHOD_ARG_TAG,1,$STRING_TYPE_TAG\d+$/
        assert _traceLines[113] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[114] ==~ /^$METHOD_ARG_TAG,1,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[121] ==~ /^$METHOD_ARG_TAG,0,$_objId$/
        assert _traceLines[122] ==~ /^$METHOD_ARG_TAG,1,${LONG_TYPE_TAG}0$/
        assert _traceLines[123] ==~ /^$METHOD_ARG_TAG,2,${BYTE_TYPE_TAG}1$/
        assert _traceLines[124] ==~ /^$METHOD_ARG_TAG,3,${SHORT_TYPE_TAG}2$/

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/
    }

    @Test
    void withTraceMethodReturnValueOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_RETURN_VALUE_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 69)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfReturnLogs: 14, numOfExceptionLogs: 4, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [4 : 'java.io.IOException',
                                          7 : 'java.lang.IllegalStateException',
                                          37: 'java.io.IOException',
                                          40: 'java.lang.IllegalStateException'])

        assert _traceLines[10] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[13] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[16] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[19] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[22] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[27] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[30] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[43] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[46] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[49] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[52] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[55] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[60] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[63] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/
    }

    @Test
    void withTraceMethodReturnValueAndTraceMethodCallOptions() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_RETURN_VALUE_OPTION, TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 111)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfReturnLogs: 14, numOfExceptionLogs: 4, numOfCallLogs: 42, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [7 : 'java.io.IOException',
                                          12: 'java.lang.IllegalStateException',
                                          61: 'java.io.IOException',
                                          66: 'java.lang.IllegalStateException'])

        assert _traceLines[16] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[20] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[24] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[28] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[32] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[40] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[46] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assert _traceLines[71] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}t$/
        assert _traceLines[75] ==~ /^$METHOD_RETURN_TAG,${FLOAT_TYPE_TAG}27.0$/
        assert _traceLines[79] ==~ /^$METHOD_RETURN_TAG,${BOOLEAN_TYPE_TAG}f$/
        assert _traceLines[83] ==~ /^$METHOD_RETURN_TAG,${CHAR_TYPE_TAG}323$/
        assert _traceLines[87] ==~ /^$METHOD_RETURN_TAG,${INT_TYPE_TAG}2694$/
        assert _traceLines[95] ==~ /^$METHOD_RETURN_TAG,$OBJECT_TYPE_TAG\d+$/
        assert _traceLines[101] ==~ /^$METHOD_RETURN_TAG,$STRING_TYPE_TAG\d+$/

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/
    }

    @Test
    void withTraceMethodCallOption() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_CALL_OPTION]) == [1L, 1L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 97)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfExceptionLogs: 4, numOfCallLogs: 42, _traceLines, 25)

        assertPropertiesAboutExit(_traceLines)

        assertExceptionLogs(_traceLines, [7 : 'java.io.IOException',
                                          12: 'java.lang.IllegalStateException',
                                          54: 'java.io.IOException',
                                          59: 'java.lang.IllegalStateException'])

        assertCallEntryCoupling(_traceLines)

        assertCallSitesOccurOnlyOnce(_traceLines[0..-5])

        assert _traceLines[-4] ==~ /^$METHOD_CALL_TAG,.*,0$/
        assert _traceLines[-3] ==~ /^$METHOD_CALL_TAG,.*,0,9$/
    }
}
