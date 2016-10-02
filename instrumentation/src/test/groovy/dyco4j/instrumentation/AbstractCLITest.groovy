/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation

import groovy.io.FileType
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

abstract class AbstractCLITest {
    private static final Path TEST_CLASS_FOLDER = Paths.get("build", "classes", "test")
    private static final Path TEST_RESOURCE_FOLDER = Paths.get("build", "resources", "test")
    private static final Path LOGGING_PROPERTY_FILE = Paths.get(TEST_CLASS_FOLDER.toString(), "dyco4j",
            "instrumentation", "logging", "logging.properties")
    private static final Path ROOT_FOLDER = Paths.get("build", "tmp")
    private static final String LOGGING_LIBRARY = Paths.get("libs", "dyco4j-logging-0.5.1.jar").toString()
    private static final CLASS_FILE_REGEX = /.*class$/
    private static final TRACE_FILE_REGEX = /^trace.*gz/
    private static final Path TRACE_FOLDER = ROOT_FOLDER.resolve("traces")
    protected static final Path OUT_FOLDER = ROOT_FOLDER.resolve("out_classes")
    protected static final Path IN_FOLDER = ROOT_FOLDER.resolve("in_classes")
    protected static final RESOURCE_FILE_NAME = 'resource.txt'
    protected static final RESOURCE_FILE_REGEX = /^${RESOURCE_FILE_NAME}$/

    protected static void deleteFiles(final Path folder, final pattern) {
        Files.walk(folder).filter { it.fileName ==~ pattern }.each { Files.delete(it) }
    }

    @BeforeClass
    static void createFoldersAndCopyPropertyFile() {
        final _propertyFolder = LOGGING_PROPERTY_FILE.getParent()
        assert Files.createDirectories(_propertyFolder) != null: "Could not create property folder $_propertyFolder"
        final _propertyFile = Files.createFile(LOGGING_PROPERTY_FILE)
        assert _propertyFile != null: "Could not create property file $LOGGING_PROPERTY_FILE"
        _propertyFile.withWriter { it.println("traceFolder=" + TRACE_FOLDER.toString()) }

        assert Files.createDirectories(ROOT_FOLDER) != null: "Could not create root folder $ROOT_FOLDER"
        assert Files.createDirectories(TRACE_FOLDER) != null: "Could not create trace folder $TRACE_FOLDER"
        assert Files.createDirectories(OUT_FOLDER) != null: "Could not create out folder $OUT_FOLDER"
        assert Files.createDirectories(IN_FOLDER) != null: "Could not create in folder $IN_FOLDER"
    }

    @AfterClass
    static void deletePropertyAndClassFiles() {
        Files.delete(LOGGING_PROPERTY_FILE)
        deleteFiles(IN_FOLDER, CLASS_FILE_REGEX)
        deleteFiles(IN_FOLDER, RESOURCE_FILE_REGEX)
    }

    protected static def copyClassToBeInstrumentedIntoInFolder(final Path pathToClass) {
        copyFileIntoInFolder(pathToClass, TEST_CLASS_FOLDER)
    }

    protected static def copyResourceIntoInFolder(final Path pathToClass) {
        copyFileIntoInFolder(pathToClass, TEST_RESOURCE_FOLDER)
    }

    protected static def instrumentCode(final clazz, final args) {
        clazz.main((String[]) args)
        def _numOfClassFiles = Files.walk(OUT_FOLDER).filter { it.fileName ==~ CLASS_FILE_REGEX }.count()
        def _numOfResourceFiles = Files.walk(OUT_FOLDER).filter { it.fileName ==~ RESOURCE_FILE_REGEX }.count()
        [_numOfClassFiles, _numOfResourceFiles]
    }

    /**
     * execute instrumented code in a different process
     * @return a quadruple of
     */
    protected static def executeInstrumentedCode(final Class clazz) {
        final _path = Paths.get(System.getProperty("java.home"), "bin", "java").toString()
        final _cp = [OUT_FOLDER, LOGGING_LIBRARY, TEST_CLASS_FOLDER].join(":")
        final _proc = [_path, "-cp", _cp, clazz.name].execute()
        final _ret = new ExecutionResult(
                _proc.waitFor(),
                _proc.inputStream.readLines(),
                _proc.errorStream.readLines(),
                getTraceLines())
        deleteFiles(TRACE_FOLDER, TRACE_FILE_REGEX)
        _ret
    }

    private static def copyFileIntoInFolder(final Path pathToFile, final Path srcFolder) {
        final _trg = IN_FOLDER.resolve(pathToFile)
        Files.createDirectories(_trg.parent)
        final _src = srcFolder.resolve(pathToFile)
        Files.copy(_src, _trg)
    }

    private static def getTraceLines() {
        def _ret = []
        TRACE_FOLDER.toFile().eachFileMatch(FileType.FILES, ~TRACE_FILE_REGEX) {
            _ret << new GZIPInputStream(it.newInputStream()).readLines()
        }
        _ret.flatten()
    }

    @Before
    void setUpFixture() {
        deleteFiles(OUT_FOLDER, CLASS_FILE_REGEX)
        deleteFiles(OUT_FOLDER, RESOURCE_FILE_REGEX)
    }

    protected static final class ExecutionResult {
        final int exitCode
        final stdoutLines // list of string
        final stderrLines // list of string
        final traceLines // list of string

        ExecutionResult(exitCode, stdoutLines, stderrLines, traceLines) {
            this.exitCode = exitCode
            this.stdoutLines = stdoutLines
            this.stderrLines = stderrLines
            this.traceLines = traceLines
        }

        def String toString() {
            [exitCode, stdoutLines, stderrLines, traceLines]
        }
    }
}
