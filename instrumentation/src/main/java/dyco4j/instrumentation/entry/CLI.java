/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package dyco4j.instrumentation.entry;

import dyco4j.instrumentation.LoggerInitializingClassVisitor;
import org.apache.commons.cli.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static dyco4j.instrumentation.Helper.processFiles;
import static org.objectweb.asm.Opcodes.ASM5;

public final class CLI {
    static final int ASM_VERSION = ASM5;
    static final String IN_FOLDER_OPTION = "in-folder";
    static final String OUT_FOLDER_OPTION = "out-folder";
    static final String METHOD_NAME_REGEX_OPTION = "method-name-regex";
    static final String ONLY_ANNOTATED_TESTS_OPTION = "only-annotated-tests";
    private final static String METHOD_NAME_REGEX = "^test.*";

    public static void main(final String[] args) throws IOException {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt(IN_FOLDER_OPTION).required().hasArg()
                                 .desc("Folder containing the classes (as descendants) to be instrumented.").build());
        _options.addOption(Option.builder().longOpt(OUT_FOLDER_OPTION).required().hasArg()
                                 .desc("Folder containing the classes (as descendants) with instrumentation.").build());
        final String _msg = MessageFormat
                .format("Regex identifying the methods to be instrumented. Default: {0}.", METHOD_NAME_REGEX);
        _options.addOption(Option.builder().longOpt(METHOD_NAME_REGEX_OPTION).hasArg(true).desc(_msg).build());
        _options.addOption(Option.builder().longOpt(ONLY_ANNOTATED_TESTS_OPTION).hasArg(false)
                                 .desc("Instrument only tests identified by annotations.").build());

        try {
            processCommandLine(new DefaultParser().parse(_options, args));
        } catch (final ParseException _ex) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        }
    }

    private static void processCommandLine(final CommandLine cmdLine) throws IOException {
        final Path _srcRoot = Paths.get(cmdLine.getOptionValue(IN_FOLDER_OPTION));
        final Path _trgRoot = Paths.get(cmdLine.getOptionValue(OUT_FOLDER_OPTION));

        final Predicate<Path> _nonClassFileSelector = p -> !p.toString().endsWith(".class") && Files.isRegularFile(p);
        final BiConsumer<Path, Path> _fileCopier = (srcPath, trgPath) -> {
            try {
                Files.copy(srcPath, trgPath);
            } catch (final IOException _ex) {
                throw new RuntimeException(_ex);
            }
        };
        processFiles(_srcRoot, _trgRoot, _nonClassFileSelector, _fileCopier);

        final Predicate<Path> _classFileSelector = p -> p.toString().endsWith(".class");
        final String _methodNameRegex = cmdLine.getOptionValue(METHOD_NAME_REGEX_OPTION, METHOD_NAME_REGEX);
        final Boolean _onlyAnnotatedTests = cmdLine.hasOption(ONLY_ANNOTATED_TESTS_OPTION);
        final BiConsumer<Path, Path> _classInstrumenter = (srcPath, trgPath) -> {
            try {
                final byte[] _bytecode = Files.readAllBytes(srcPath);
                final ClassReader _cr = new ClassReader(_bytecode);
                final ClassWriter _cw = new ClassWriter(_cr, ClassWriter.COMPUTE_MAXS);
                final ClassVisitor _cv1 = new LoggerInitializingClassVisitor(CLI.ASM_VERSION, _cw);
                final ClassVisitor _cv2 = new TracingClassVisitor(_cv1, _methodNameRegex, _onlyAnnotatedTests);
                _cr.accept(_cv2, 0);
                final byte[] _out = _cw.toByteArray();
                Files.write(trgPath, _out);
            } catch (final IOException _ex) {
                throw new RuntimeException(_ex);
            }
        };
        processFiles(_srcRoot, _trgRoot, _classFileSelector, _classInstrumenter);
    }
}
