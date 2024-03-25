/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation

import com.google.gson.Gson
import dyco4j.instrumentation.internals.CLI
import dyco4j.logging.Logger
import dyco4j.utility.ProgramData
import groovy.io.FileType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.BeforeAll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream

abstract class AbstractCLITest {
    private static final String LOGGING_LIBRARY = System.getProperty('logging.jar').toString()
    private static final Path TEST_CLASS_FOLDER = Paths.get("build", "classes", "java", "test")
    private static final Path TEST_RESOURCE_FOLDER = Paths.get("build", "resources", "test")
    private static final Path LOGGING_PROPERTY_FILE = Paths.get(TEST_CLASS_FOLDER.toString(), "dyco4j",
            "logging", "logging.properties")
    private static final TRACE_FILE_REGEX = /^trace.*gz/
    private static final CLASS_FILE_REGEX = /.*class$/
    private static final Path ROOT_FOLDER = Paths.get("build", "tmp")
    private static final Path TRACE_FOLDER = resolveUnderRootFolder("traces")
    protected static final Path OUT_FOLDER = resolveUnderRootFolder("out_classes")
    protected static final Path IN_FOLDER = resolveUnderRootFolder("in_classes")
    protected static final RESOURCE_FILE_NAME = 'resource.txt'
    protected static final RESOURCE_FILE_REGEX = /^${RESOURCE_FILE_NAME}$/
    protected static final GET_ARRAY = Logger.ArrayAction.GETA.toString()
    protected static final PUT_ARRAY = Logger.ArrayAction.PUTA.toString()
    protected static final GET_FIELD = Logger.FieldAction.GETF.toString()
    protected static final PUT_FIELD = Logger.FieldAction.PUTF.toString()

    private static fixupPath(final pathStr) {
        pathStr.replaceAll('\\\\', '/')
    }

    @BeforeAll
    static void createFoldersAndCopyPropertyFile() {
        final _propertyFolder = LOGGING_PROPERTY_FILE.getParent()
        assert Files.createDirectories(_propertyFolder) != null: "Could not create property folder $_propertyFolder"
        final _propertyFile = Files.createFile(LOGGING_PROPERTY_FILE)
        assert _propertyFile != null: "Could not create property file $LOGGING_PROPERTY_FILE"
        _propertyFile.withWriter { it.println("traceFolder=" + fixupPath(TRACE_FOLDER.toString())) }

        assert Files.createDirectories(ROOT_FOLDER) != null: "Could not create root folder $ROOT_FOLDER"
        assert Files.createDirectories(TRACE_FOLDER) != null: "Could not create trace folder $TRACE_FOLDER"
        assert Files.createDirectories(OUT_FOLDER) != null: "Could not create out folder $OUT_FOLDER"
        assert Files.createDirectories(IN_FOLDER) != null: "Could not create in folder $IN_FOLDER"
    }

    @AfterAll
    static void deletePropertyAndClassFiles() {
        Files.delete(LOGGING_PROPERTY_FILE)
        deleteFiles(IN_FOLDER, CLASS_FILE_REGEX)
        deleteFiles(IN_FOLDER, RESOURCE_FILE_REGEX)
    }

    protected static resolveUnderRootFolder(String path) {
        ROOT_FOLDER.resolve(path)
    }

    protected static resolveUnderTestClassFolder(Path path) {
        TEST_CLASS_FOLDER.resolve(path)
    }

    protected static assertNestingOfCallsIsValid(List<String> traceLines, int numOfMethodEntries) {
        def (_stack, _cnt) = traceLines.inject([[], 0]) { result, line ->
            def (_stack, _cnt) = result
            if (line =~ /$Logger.METHOD_ENTRY_TAG/) {
                _stack.push(line)
            } else if (line =~ /$Logger.METHOD_EXIT_TAG/) {
                final String _tmp1 = _stack.pop()
                assert _tmp1.split(',')[1] == line.split(',')[1]
                _cnt++
            }
            [_stack, _cnt]
        }
        assert !_stack
        assert _cnt == numOfMethodEntries: "${traceLines}"
    }

    protected static assertTraceLengthIs(_executionResult, _numOfLines) {
        assert _executionResult.traceLines.size() == _numOfLines
    }

    protected static assertFreqOfLogs(Map freq = [:], String[] traceLines, int numOfMethodEntries) {
        // should not raise exception
        Date.parseToStringDate(traceLines[0])

        final _numOfLines = traceLines.length - 1
        assertNestingOfCallsIsValid(traceLines[1.._numOfLines], numOfMethodEntries)

        assert traceLines.count { it =~ /^$Logger.METHOD_ARG_TAG/ } == (freq['numOfArgLogs'] ?: 0)
        assert traceLines.count { it =~ /^$Logger.METHOD_RETURN_TAG/ } == (freq['numOfReturnLogs'] ?: 0)
        assert traceLines.count { it =~ /^$Logger.METHOD_EXCEPTION_TAG/ } == (freq['numOfExceptionLogs'] ?: 0)
        assert traceLines.count { it =~ /^$GET_ARRAY/ } == (freq['numOfGetArrayLogs'] ?: 0)
        assert traceLines.count { it =~ /^$PUT_ARRAY/ } == (freq['numOfPutArrayLogs'] ?: 0)
        assert traceLines.count { it =~ /^$GET_FIELD/ } == (freq['numOfGetFieldLogs'] ?: 0)
        assert traceLines.count { it =~ /^$PUT_FIELD/ } == (freq['numOfPutFieldLogs'] ?: 0)
        assert traceLines.count { it =~ /^$Logger.METHOD_CALL_TAG/ } == (freq['numOfCallLogs'] ?: 0)
    }

    protected static assertAllAndOnlyMatchingMethodsAreTraced(traceLines, methodNameRegex) {
        def _methodIds = traceLines.findAll { it ==~ /^$Logger.METHOD_ENTRY_TAG,.*/ }.collect { it.split(',')[1] }
        _methodIds += traceLines.findAll { it ==~ /^$Logger.METHOD_EXIT_TAG,.*/ }.collect { it.split(',')[1] }

        new File(CLI.PROGRAM_DATA_FILE_NAME).withReader { rdr ->
            final _prog_data = new Gson().fromJson(rdr, ProgramData.class)

            _methodIds.each {
                assert _prog_data.methodId2Name[it].split(':')[0].split('/')[-1] ==~ methodNameRegex
            }

            _prog_data.methodId2Name.each { methodId, name ->
                assert !(name.split(':')[0].split('/')[-1] ==~ methodNameRegex) || methodId in _methodIds
            }
        }
    }

    protected static assertPropertiesAboutExit(traceLines) {
        traceLines.tail().inject(null) { String prev, line ->
            // Every exceptional exit is preceded by an exception
            if (line ==~ /^$Logger.METHOD_EXIT_TAG,.*,E$/ && prev) {
                final _tmp1 = prev.split(',')
                assert _tmp1[0] == Logger.METHOD_EXCEPTION_TAG
            }
            line
        }

        traceLines.tail().inject(null) { String prev, _l ->
            // Every return should be followed by a normal exit
            if (_l ==~ /^$Logger.METHOD_RETURN_TAG,.*$/) {
                _l
            } else if (prev) {
                assert _l ==~ /^$Logger.METHOD_EXIT_TAG,.*,N$/
                null
            } else {
                prev
            }
        }
    }

    protected static removeThreadIdFromLog(final traceLines) {
        traceLines.collect {
            if (it ==~ /^\d+,[a-zA-Z]+,.*/)
                it.replaceAll(/^\d+,/, "")
            else {
                final _tmp = it =~ /^(\d+),\d+,(.*)/
                if (_tmp) {
                    _tmp.replaceFirst('$1,$2')
                } else
                    it
            }
        }
    }

    protected static copyClassesToBeInstrumentedIntoInFolder(final pathsToClasses) {
        copyFilesIntoInFolder(pathsToClasses, TEST_CLASS_FOLDER)
    }

    protected static copyResourcesIntoInFolder(final pathsToResources) {
        copyFilesIntoInFolder(pathsToResources, TEST_RESOURCE_FOLDER)
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
        final _cp = fixupPath([OUT_FOLDER, LOGGING_LIBRARY, TEST_CLASS_FOLDER].join(File.pathSeparator))
        final _proc = [_path, "-cp", _cp, clazz.name].execute()
        final _ret = new ExecutionResult(
                _proc.waitFor(),
                _proc.inputStream.readLines(),
                _proc.errorStream.readLines(),
                getTraceLines())
        deleteFiles(TRACE_FOLDER, TRACE_FILE_REGEX)
        _ret
    }

    protected static copyFilesIntoInFolder(final pathsToFiles, final Path srcFolder) {
        for (final Path _pathToFile in pathsToFiles) {
            final _trg = IN_FOLDER.resolve(_pathToFile)
            Files.createDirectories(_trg.parent)
            final _src = srcFolder.resolve(_pathToFile)
            Files.copy(_src, _trg)
        }
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

    @BeforeEach
    void setUpFixture() {
        deleteFiles(OUT_FOLDER, CLASS_FILE_REGEX)
        deleteFiles(OUT_FOLDER, RESOURCE_FILE_REGEX)
    }

    @AfterEach
    void deleteAuxiliaryFiles() {
        final _tmp1 = Paths.get(CLI.PROGRAM_DATA_FILE_NAME)
        if (Files.exists(_tmp1))
            Files.delete(_tmp1)
        final _tmp2 = Paths.get(CLI.PROGRAM_DATA_FILE_NAME + ".bak")
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
