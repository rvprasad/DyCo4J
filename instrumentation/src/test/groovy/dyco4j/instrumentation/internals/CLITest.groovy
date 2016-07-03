/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import dyco4j.instrumentation.AbstractCLITest
import org.junit.BeforeClass
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

class CLITest extends AbstractCLITest {
    @BeforeClass
    static void copyClassesToBeInstrumented() {
        // copy to-be-instrumented test classes into IN_FOLDER
        final _file = Paths.get("dyco4j", "instrumentation", "internals", "CLITestSubject.class")
        final _trg = IN_FOLDER.resolve(_file)
        Files.createDirectories(_trg.parent)
        final _testClassFolder = Paths.get("build", "classes", "test")
        final _src = _testClassFolder.resolve(_file)
        Files.copy(_src, _trg)
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
        assert false
    }

    @Test
    void withAllOptions() {
        assert false
    }

    @Test
    void withMethodNameRegexOption() {
        assert false
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
