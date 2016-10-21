/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation

import dyco4j.instrumentation.logging.Logger
import groovy.io.FileType
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

abstract class AbstractCLITest {
    private static final LOGGER = LoggerFactory.getLogger(AbstractCLITest.class)
    protected static final Path TEST_CLASS_FOLDER = Paths.get("build", "classes", "test")
    private static final Path TEST_RESOURCE_FOLDER = Paths.get("build", "resources", "test")
    private static final Path LOGGING_PROPERTY_FILE = Paths.get(TEST_CLASS_FOLDER.toString(), "dyco4j",
            "instrumentation", "logging", "logging.properties")
    private static final String LOGGING_LIBRARY = System.getProperty('logging.jar').toString()
    protected static final CLASS_FILE_REGEX = /.*class$/
    private static final TRACE_FILE_REGEX = /^trace.*gz/
    protected static final Path ROOT_FOLDER = Paths.get("build", "tmp")
    private static final Path TRACE_FOLDER = ROOT_FOLDER.resolve("traces")
    protected static final Path OUT_FOLDER = ROOT_FOLDER.resolve("out_classes")
    protected static final Path IN_FOLDER = ROOT_FOLDER.resolve("in_classes")
    protected static final RESOURCE_FILE_NAME = 'resource.txt'
    protected static final RESOURCE_FILE_REGEX = /^${RESOURCE_FILE_NAME}$/
    protected static final GET_ARRAY = Logger.ArrayAction.GETA.toString()
    protected static final PUT_ARRAY = Logger.ArrayAction.PUTA.toString()
    protected static final GET_FIELD = Logger.FieldAction.GETF.toString()
    protected static final PUT_FIELD = Logger.FieldAction.PUTF.toString()

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

    protected static assertNestingOfCallsIsValid(traceLines, numOfCalls) {
        final _stack = []
        def _cnt = 0
        for (String l in traceLines) {
            if (l =~ /$Logger.METHOD_ENTRY_TAG/) {
                _stack << l
            } else if (l =~ /$Logger.METHOD_EXIT_TAG/) {
                final String _tmp1 = _stack.pop()
                assert _tmp1.split(',')[1] == l.split(',')[1]
                _cnt++
            }
        }
        assert !_stack
        assert _cnt == numOfCalls: "${traceLines}"
    }

    protected static assertTraceLengthIs(_executionResult, _numOfLines) {
        assert _executionResult.traceLines.size == _numOfLines
    }

    protected static checkFreqOfLogs(String[] traceLines, numOfCalls, numOfArgLogs = 0, numOfReturnLogs = 0,
                                     numOfExceptionLogs = 0, numOfGetArrayLogs = 0, numOfPutArrayLogs = 0,
                                     numOfGetFieldLogs = 0, numOfPutFieldLogs = 0) {
        // should not raise exception
        Date.parseToStringDate(traceLines[0])

        final _numOfLines = traceLines.length - 1
        assertNestingOfCallsIsValid(traceLines[1.._numOfLines], numOfCalls)

        assert traceLines.count { it =~ /^$Logger.METHOD_ARG_TAG/ } == numOfArgLogs
        assert traceLines.count { it =~ /^$Logger.METHOD_RETURN_TAG/ } == numOfReturnLogs
        assert traceLines.count { it =~ /^$Logger.METHOD_EXCEPTION_TAG/ } == numOfExceptionLogs
        assert traceLines.count { it =~ /^$GET_ARRAY/ } == numOfGetArrayLogs
        assert traceLines.count { it =~ /^$PUT_ARRAY/ } == numOfPutArrayLogs
        assert traceLines.count { it =~ /^$GET_FIELD/ } == numOfGetFieldLogs
        assert traceLines.count { it =~ /^$PUT_FIELD/ } == numOfPutFieldLogs
    }

    protected static removeThreadIdFromLog(final traceLines) {
        traceLines.collect { it.replaceAll(/^\d+,/, "") }
    }

    protected static copyClassToBeInstrumentedIntoInFolder(final Path pathToClass) {
        copyFileIntoInFolder(pathToClass, TEST_CLASS_FOLDER)
    }

    protected static copyResourceIntoInFolder(final Path pathToClass) {
        copyFileIntoInFolder(pathToClass, TEST_RESOURCE_FOLDER)
    }

    protected static instrumentCode(final clazz, final args) {
        clazz.main((String[]) args)
        def _numOfClassFiles = Files.walk(OUT_FOLDER).filter { it.fileName ==~ CLASS_FILE_REGEX }.count()
        def _numOfResourceFiles = Files.walk(OUT_FOLDER).filter { it.fileName ==~ RESOURCE_FILE_REGEX }.count()
        [_numOfClassFiles, _numOfResourceFiles]
    }

    /**
     * execute instrumented code in a different process
     * @return a quadruple of process return status, standard output, standard error, and generated trace
     */
    protected static executeInstrumentedCode(final Class clazz) {
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

    protected static copyFileIntoInFolder(final Path pathToFile, final Path srcFolder) {
        final _trg = IN_FOLDER.resolve(pathToFile)
        Files.createDirectories(_trg.parent)
        final _src = srcFolder.resolve(pathToFile)
        Files.copy(_src, _trg)
    }

    protected static deleteFiles(final Path folder, final pattern) {
        Files.walk(folder).filter { it.fileName ==~ pattern }.each { Files.delete(it) }
    }

    private static getTraceLines() {
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

    @After
    void deleteAuxiliaryFiles() {
        final _tmp1 = Paths.get("auxiliary_data.json")
        if (Files.exists(_tmp1))
            Files.delete(_tmp1)
        final _tmp2 = Paths.get("auxiliary_data.json.bak")
        if (Files.exists(_tmp2))
            Files.delete(_tmp2)
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

        String toString() {
            [exitCode, stdoutLines, stderrLines, traceLines]
        }
    }
}
