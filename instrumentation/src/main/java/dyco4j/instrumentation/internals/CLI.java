/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import dyco4j.instrumentation.Helper;
import dyco4j.instrumentation.LoggerInitializingClassVisitor;
import dyco4j.utility.ProgramData;
import org.apache.commons.cli.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ASM5;

public final class CLI {
    public static final String PROGRAM_DATA_FILE_NAME = "program_data.json";
    static final int ASM_VERSION = ASM5;
    static final String IN_FOLDER_OPTION = "in-folder";
    static final String OUT_FOLDER_OPTION = "out-folder";
    static final String CLASSPATH_CONFIG_OPTION = "classpath-config";
    static final String METHOD_NAME_REGEX_OPTION = "method-name-regex";
    static final String TRACE_ARRAY_ACCESS_OPTION = "trace-array-access";
    static final String TRACE_FIELD_ACCESS_OPTION = "trace-field-access";
    static final String TRACE_METHOD_ARGUMENTS_OPTION = "trace-method-arguments";
    static final String TRACE_METHOD_CALL_OPTION = "trace-method-call";
    static final String TRACE_METHOD_RETURN_VALUE_OPTION = "trace-method-return-value";
    private static final String METHOD_NAME_REGEX = ".*";

    public static void main(final String[] args) throws IOException {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt(IN_FOLDER_OPTION).required().hasArg(true)
                .desc("Folders containing the classes to be instrumented.").build());
        _options.addOption(Option.builder().longOpt(OUT_FOLDER_OPTION).required().hasArg(true)
                .desc("Folder containing the classes (as descendants) with instrumentation.").build());
        _options.addOption(Option.builder().longOpt(CLASSPATH_CONFIG_OPTION).hasArg(true)
                .desc("File containing class path (1 entry per line) used by classes to be instrumented.")
                .build());
        final String _msg = MessageFormat.format("Regex identifying the methods to be instrumented. Default: {0}.",
                METHOD_NAME_REGEX);
        _options.addOption(Option.builder().longOpt(METHOD_NAME_REGEX_OPTION).hasArg(true).desc(_msg).build());
        _options.addOption(Option.builder().longOpt(TRACE_ARRAY_ACCESS_OPTION).hasArg(false)
                .desc("Instrument to trace array access.").build());
        _options.addOption(Option.builder().longOpt(TRACE_FIELD_ACCESS_OPTION).hasArg(false)
                .desc("Instrument to trace field access.").build());
        _options.addOption(Option.builder().longOpt(TRACE_METHOD_ARGUMENTS_OPTION).hasArg(false)
                .desc("Instrument to trace method arguments.").build());
        _options.addOption(Option.builder().longOpt(TRACE_METHOD_CALL_OPTION).hasArg(false)
                .desc("Instrument to trace method calls (compile-time signatures).").build());
        _options.addOption(Option.builder().longOpt(TRACE_METHOD_RETURN_VALUE_OPTION).hasArg(false)
                .desc("Instrument to trace method return values.").build());

        try {
            final CommandLine _cmdLine = new DefaultParser().parse(_options, args);
            processCommandLine(_cmdLine);
        } catch (final ParseException _ex1) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        }
    }

    private static ClassLoader createCustomClassLoader(final CommandLine cmdLine) throws IOException {
        final List<URL> _urls = new ArrayList<>();
        _urls.add(new File(cmdLine.getOptionValue(IN_FOLDER_OPTION)).toURI().toURL());
        final String _classpathConfig = cmdLine.getOptionValue(CLASSPATH_CONFIG_OPTION);
        if (_classpathConfig != null) {
            for (String _s: Files.readAllLines(Paths.get(_classpathConfig))) {
                _urls.add(new File(_s).toURI().toURL());
            }
        }
        return new URLClassLoader(_urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    private static void processCommandLine(final CommandLine cmdLine) throws IOException {
        final Path _srcRoot = Paths.get(cmdLine.getOptionValue(IN_FOLDER_OPTION));
        final Path _trgRoot = Paths.get(cmdLine.getOptionValue(OUT_FOLDER_OPTION));
        Helper.copyFiles(_srcRoot, _trgRoot);

        final CommandLineOptions _cmdLineOptions =
                new CommandLineOptions(cmdLine.hasOption(TRACE_ARRAY_ACCESS_OPTION),
                        cmdLine.hasOption(TRACE_FIELD_ACCESS_OPTION),
                        cmdLine.hasOption(TRACE_METHOD_ARGUMENTS_OPTION),
                        cmdLine.hasOption(TRACE_METHOD_CALL_OPTION),
                        cmdLine.hasOption(TRACE_METHOD_RETURN_VALUE_OPTION));
        final Set<Path> _filenames = getFilenames(_srcRoot);
        final Path _programDataFile = Paths.get(PROGRAM_DATA_FILE_NAME);
        final ProgramData _programData = ProgramData.loadData(_programDataFile);
        getMemberId2NameMapping(_filenames, _programData);

        final Predicate<Path> _classFileSelector = p -> p.toString().endsWith(".class");
        final String _methodNameRegex = cmdLine.getOptionValue(METHOD_NAME_REGEX_OPTION, METHOD_NAME_REGEX);
        final ClassLoader _customClassLoader = createCustomClassLoader(cmdLine);
        final BiConsumer<Path, Path> _classInstrumenter = (srcPath, trgPath) -> {
            try {
                final ClassReader _cr = new ClassReader(Files.readAllBytes(srcPath));
                final ClassWriter _cw = new CustomClassLoadingClassWriter(_cr, ClassWriter.COMPUTE_FRAMES, _customClassLoader);
                final Map<String, String> _shortFieldName2Id = _programData.getImmutableCopyOfShortFieldName2Id();
                final Map<String, String> _shortMethodName2Id = _programData.getImmutableCopyOfShortMethodName2Id();
                final Map<String, String> _class2superClass =  _programData.getImmutableCopyOfClass2SuperClass();
                final ClassVisitor _cv1 = new LoggerInitializingClassVisitor(CLI.ASM_VERSION, _cw);
                final ClassVisitor _cv2 =
                        new TracingClassVisitor(_cv1, _shortFieldName2Id, _shortMethodName2Id, _class2superClass,
                                _methodNameRegex, _cmdLineOptions);
                _cr.accept(_cv2, ClassReader.SKIP_FRAMES);

                Files.write(trgPath, _cw.toByteArray());
            } catch (final IOException _ex) {
                throw new RuntimeException(_ex);
            }
        };
        Helper.processFiles(_srcRoot, _trgRoot, _classFileSelector, _classInstrumenter);

        ProgramData.saveData(_programData, _programDataFile);
    }

    private static Set<Path> getFilenames(final Path folder) throws IOException {
        try (Stream<Path> walker = Files.walk(folder)) {
            return walker.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toSet());
        }
    }

    private static void getMemberId2NameMapping(final Collection<Path> filenames, final ProgramData programData) {
        try {
            for (final Path _arg : filenames) {
                final ClassReader _cr = new ClassReader(Files.readAllBytes(_arg));
                final ClassVisitor _cv = new ProgramDataCollectingClassVisitor(programData);
                _cr.accept(_cv, 0);
            }
        } catch (final Exception _ex) {
            throw new RuntimeException(_ex);
        }
    }

    record CommandLineOptions(boolean traceArrayAccess, boolean traceFieldAccess, boolean traceMethodArgs,
                              boolean traceMethodCall, boolean traceMethodRetValue) {
    }

    private static class CustomClassLoadingClassWriter extends ClassWriter {
        private final ClassLoader customClassLoader;

        CustomClassLoadingClassWriter(final ClassReader reader, final int flags, final ClassLoader customClassLoader ) {
            super(reader, flags);
            this.customClassLoader = customClassLoader;
        }
        @Override
        protected ClassLoader getClassLoader() {
            return customClassLoader;
        }

    }
}
