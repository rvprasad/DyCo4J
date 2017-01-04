/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import dyco4j.instrumentation.AbstractCLITest
import dyco4j.logging.Logger
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Paths

import static dyco4j.instrumentation.internals.CLITest.*

class CLIInitializerInstrumentationTest extends AbstractCLITest {
    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _files1 = [Paths.get('dyco4j', 'instrumentation', 'internals',
                'CLIInitializerInstrumentationTestSubject.class'),
                         Paths.get('dyco4j', 'instrumentation', 'internals',
                                 'CLIInitializerInstrumentationTestSubject$X.class'),
                         Paths.get('dyco4j', 'instrumentation', 'internals',
                                 'CLIInitializerInstrumentationTestSubject$Y.class'),
                         Paths.get('dyco4j', 'instrumentation', 'internals',
                                 'CLIInitializerInstrumentationTestSubject$1.class')]
        copyClassesToBeInstrumentedIntoInFolder(_files1)
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLIInitializerInstrumentationTestSubject)
    }

    @Test
    void superConstructorCallDoesNotCauseVerifyError() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER]) == [4L, 0L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 11)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(_traceLines, 5)
    }

    @Test
    void callLoggingInConstructorDoesNotCauseVerifyError() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_CALL_OPTION]) == [4L, 0L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 17)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfCallLogs: 6, _traceLines, 5)
    }

    @Test
    void argumentLoggingInConstructorDoesNotCauseVerifyError() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_METHOD_ARGUMENTS_OPTION]) == [4L, 0L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 21)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfArgLogs: 10, _traceLines, 5)
    }

    @Test
    void puttingFieldOnUninitializedThisDoesNotCauseVerifyError() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               TRACE_FIELD_ACCESS_OPTION]) == [4L, 0L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 12)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(numOfPutFieldLogs: 1, _traceLines, 5)
    }
}

