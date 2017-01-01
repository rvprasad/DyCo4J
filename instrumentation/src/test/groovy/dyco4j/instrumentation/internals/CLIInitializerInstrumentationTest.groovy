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
                                 'CLIInitializerInstrumentationTestSubject$Y.class')]
        copyClassesToBeInstrumentedIntoInFolder(_files1)
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLIInitializerInstrumentationTestSubject)
    }

    @Test
    void superCallDoesNotCauseVerifyError() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER]) == [3L, 0L]

        final ExecutionResult _executionResult = executeInstrumentedCode()
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 9)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(_traceLines, 4)
    }
}

