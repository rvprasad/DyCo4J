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
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import static dyco4j.instrumentation.internals.CLITest.IN_FOLDER_OPTION
import static dyco4j.instrumentation.internals.CLITest.OUT_FOLDER_OPTION
import static groovy.test.GroovyAssert.shouldFail

import java.nio.file.Files
import java.nio.file.Paths

class CLIClassPathConfigTest extends AbstractCLITest {

    private static final String CLASSPATH_CONFIG_OPTION = "--$CLI.CLASSPATH_CONFIG_OPTION"

    private static classpathConfigFile
    private static sourceFile
    private static targetFile

    @BeforeClass
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file1 = Paths.get("dyco4j", "instrumentation", "internals", "CLIClassPathConfigTestSubject.class")
        copyClassToBeInstrumentedIntoInFolder(_file1)

        final _extra_class_folder = ROOT_FOLDER.resolve("extra_classes")
        assert Files.createDirectories(_extra_class_folder) != null: "Could not create in folder $_extra_class_folder"

        final _file2 = Paths.get("dyco4j", "instrumentation", "internals", 'Pair.class')
        targetFile = _extra_class_folder.resolve(_file2)
        Files.createDirectories(targetFile.parent)
        sourceFile = TEST_CLASS_FOLDER.resolve(_file2)
        Files.move(sourceFile, targetFile)

        classpathConfigFile = ROOT_FOLDER.resolve("classpath-config.txt")
        classpathConfigFile.toFile().withWriter { wt ->
            wt.println(_extra_class_folder.toString())
        }
    }

    @AfterClass
    static void removeExtraClasses() {
        Files.move(targetFile, sourceFile)
        Files.delete(classpathConfigFile)
    }

    protected static final instrumentCode(args) {
        instrumentCode(CLI, args)
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLIClassPathConfigTestSubject)
    }

    @Test
    void withOutClassPathConfig() {
        final _e = shouldFail RuntimeException, {
            instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER])
        }
        assert _e.message ==~ /java.lang.ClassNotFoundException.*dyco4j.instrumentation.internals.Pair/
    }

    @Test
    void withClassPathConfig() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               CLASSPATH_CONFIG_OPTION, classpathConfigFile.toString()]) == [1L, 0L]

        Files.move(targetFile, sourceFile) // move Pair class back into build/classes/test
        final ExecutionResult _executionResult = executeInstrumentedCode()
        Files.move(sourceFile, targetFile) // move Pair class back into build/tmp/extra_classes
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 3)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        checkFreqOfLogs(_traceLines, 1)

        assert _traceLines[2] ==~ /^$Logger.METHOD_EXIT_TAG,m1,N$/
    }
}
