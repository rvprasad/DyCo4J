/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

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
}
