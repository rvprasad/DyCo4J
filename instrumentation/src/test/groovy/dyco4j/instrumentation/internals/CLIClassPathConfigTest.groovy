/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals

import dyco4j.instrumentation.AbstractCLITest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.nio.file.Path

import static dyco4j.instrumentation.internals.CLITest.instrumentCode
import static dyco4j.instrumentation.internals.CLITest.IN_FOLDER_OPTION
import static dyco4j.instrumentation.internals.CLITest.OUT_FOLDER_OPTION
import static groovy.test.GroovyAssert.shouldFail

import java.nio.file.Files
import java.nio.file.Paths

class CLIClassPathConfigTest extends AbstractCLITest {

    private static final String CLASSPATH_CONFIG_OPTION = "--$CLI.CLASSPATH_CONFIG_OPTION"

    private static Path classpathConfigFile
    private static Path sourceFile
    private static Path targetFile

    @BeforeAll
    static void copyClassesToBeInstrumentedIntoInFolder() {
        final _file1 = Paths.get('dyco4j', 'instrumentation', 'internals', 'CLIClassPathConfigTestSubject.class')
        copyClassesToBeInstrumentedIntoInFolder([_file1])

        final _extra_class_folder = resolveUnderRootFolder("extra_classes")
        assert Files.createDirectories(_extra_class_folder) != null: "Could not create in folder $_extra_class_folder"

        final _file2 = Paths.get('dyco4j', 'instrumentation', 'internals',
                'CLIClassPathConfigTestSubject$SomeClass.class')
        targetFile = _extra_class_folder.resolve(_file2)
        Files.createDirectories(targetFile.parent)
        sourceFile = resolveUnderTestClassFolder(_file2)
        Files.move(sourceFile, targetFile)

        classpathConfigFile = resolveUnderRootFolder("classpath-config.txt")
        classpathConfigFile.toFile().withWriter { it.println(_extra_class_folder.toString()) }
    }

    @AfterAll
    static void removeExtraClasses() {
        Files.move(targetFile, sourceFile)
        Files.delete(classpathConfigFile)
    }

    private static executeInstrumentedCode() {
        executeInstrumentedCode(CLIClassPathConfigTestSubject)
    }

    @Test
    void withOutClassPathConfig() {
        final _e = shouldFail TypeNotPresentException, {
            instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER])
        }
        assert _e.message == 'Type dyco4j/instrumentation/internals/CLIClassPathConfigTestSubject$SomeClass not present'
    }

    @Test
    void withClassPathConfig() {
        assert instrumentCode([IN_FOLDER_OPTION, IN_FOLDER, OUT_FOLDER_OPTION, OUT_FOLDER,
                               CLASSPATH_CONFIG_OPTION, classpathConfigFile.toString()]) == [1L, 0L]

        Files.move(targetFile, sourceFile) // move SomeClass class back into build/classes/test
        final ExecutionResult _executionResult = executeInstrumentedCode()
        Files.move(sourceFile, targetFile) // move SomeClass class back into build/tmp/extra_classes
        assert _executionResult.exitCode == 0

        assertTraceLengthIs(_executionResult, 3)

        final String[] _traceLines = removeThreadIdFromLog(_executionResult.traceLines)
        assertFreqOfLogs(_traceLines, 1)

        assertPropertiesAboutExit(_traceLines)
    }
}
