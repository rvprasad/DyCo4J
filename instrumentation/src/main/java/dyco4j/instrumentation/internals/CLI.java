/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import dyco4j.instrumentation.LoggerInitializingClassVisitor;
import org.apache.commons.cli.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dyco4j.instrumentation.Helper.processFiles;
import static org.objectweb.asm.Opcodes.ASM5;

public final class CLI {
    final static int ASM_VERSION = ASM5;
    private static final Path AUXILIARY_DATA_FILE_NAME = Paths.get("auxiliary_data.json");
    private static final String METHOD_NAME_REGEX = ".*";
    private static final Logger LOGGER = LoggerFactory.getLogger(CLI.class);

    public static void main(final String[] args) throws IOException {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt("in-folder").required().hasArg(true)
                .desc("Folders containing the classes to be instrumented.").build());
        _options.addOption(Option.builder().longOpt("out-folder").required().hasArg(true)
                .desc("Folder containing the classes (as descendants) with instrumentation.").build());
        _options.addOption(Option.builder().longOpt("classpath-config").hasArg(true)
                .desc("File containing class path (1 entry per line) used by classes to be instrumented.")
                .build());
        final String _msg = MessageFormat.format("Regex identifying the methods to be instrumented. Default: {0}.",
                METHOD_NAME_REGEX);
        _options.addOption(Option.builder().longOpt("methodNameRegex").hasArg(true).desc(_msg).build());
        _options.addOption(Option.builder().longOpt("traceFieldAccess").hasArg(false)
                .desc("Instrument classes to trace field access.").build());
        _options.addOption(Option.builder().longOpt("traceArrayAccess").hasArg(false)
                .desc("Instrument classes to trace array access.").build());
        _options.addOption(Option.builder().longOpt("traceMethodArgs").hasArg(false)
                .desc("Instrument classes to trace method arguments.").build());
        _options.addOption(Option.builder().longOpt("traceMethodReturnValue").hasArg(false)
                .desc("Instrument classes to trace method return values.").build());

        try {
            final CommandLine _cmdLine = new DefaultParser().parse(_options, args);
            addToClassPath(_cmdLine.getOptionValue("classpath-config"));
            processCommandLine(_cmdLine);
        } catch (final ParseException _ex1) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        }
    }

    private static void addToClassPath(final String classpathConfig) throws IOException {
        if (classpathConfig != null) {
            try {
                final URLClassLoader _urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
                final Class<URLClassLoader> _urlClass = URLClassLoader.class;
                for (final String _s : Files.readAllLines(Paths.get(classpathConfig))) {
                    final File _f = new File(_s);
                    final URL _u = _f.toURI().toURL();
                    final Method _method = _urlClass.getDeclaredMethod("addURL", URL.class);
                    _method.setAccessible(true);
                    _method.invoke(_urlClassLoader, _u);
                    LOGGER.info(MessageFormat.format("Adding {0} to classpath", _s));
                }
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                    MalformedURLException _e) {
                throw new RuntimeException(_e);
            }
        }
    }

    private static void processCommandLine(final CommandLine cmdLine) throws IOException {
        final Path _srcRoot = Paths.get(cmdLine.getOptionValue("in-folder"));
        final Path _trgRoot = Paths.get(cmdLine.getOptionValue("out-folder"));

        final Predicate<Path> _nonClassFileSelector = p -> !p.toString().endsWith(".class") && Files.isRegularFile(p);
        final BiConsumer<Path, Path> _fileCopier = (srcPath, trgPath) -> {
            try {
                Files.copy(srcPath, trgPath);
            } catch (final IOException _ex) {
                throw new RuntimeException(_ex);
            }
        };
        processFiles(_srcRoot, _trgRoot, _nonClassFileSelector, _fileCopier);

        final CommandLineOptions _cmdLineOptions =
                new CommandLineOptions(cmdLine.hasOption("traceArrayAccess"), cmdLine.hasOption("traceFieldAccess"),
                        cmdLine.hasOption("traceMethodArgs"),
                        cmdLine.hasOption("traceMethodReturnValue"));
        final Set<Path> _filenames = getFilenames(_srcRoot);
        final AuxiliaryData _auxiliaryData = AuxiliaryData.loadData(AUXILIARY_DATA_FILE_NAME);
        getMemberId2NameMapping(_filenames, _auxiliaryData, _cmdLineOptions.traceFieldAccess);

        final Predicate<Path> _classFileSelector = p -> p.toString().endsWith(".class");
        final String _methodNameRegex = cmdLine.getOptionValue("methodNameRegex", METHOD_NAME_REGEX);
        final BiConsumer<Path, Path> _classInstrumenter = (srcPath, trgPath) -> {
            try {
                final ClassReader _cr = new ClassReader(Files.readAllBytes(srcPath));
                final ClassWriter _cw = new ClassWriter(_cr, ClassWriter.COMPUTE_FRAMES);
                final Map<String, String> _shortFieldName2Id =
                        Collections.unmodifiableMap(_auxiliaryData.shortFieldName2Id);
                final Map<String, String> _shortMethodName2Id =
                        Collections.unmodifiableMap(_auxiliaryData.shortMethodName2Id);
                final Map<String, String> _class2superClass =
                        Collections.unmodifiableMap(_auxiliaryData.class2superClass);
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
        processFiles(_srcRoot, _trgRoot, _classFileSelector, _classInstrumenter);

        AuxiliaryData.saveData(_auxiliaryData, AUXILIARY_DATA_FILE_NAME);
    }

    private static Set<Path> getFilenames(final Path folder) throws IOException {
        final Stream<Path> _tmp1 = Files.walk(folder).filter(p -> p.toString().endsWith(".class"));
        return _tmp1.collect(Collectors.toSet());
    }

    private static void getMemberId2NameMapping(final Collection<Path> filenames, final AuxiliaryData auxiliaryData,
                                                final boolean collectFieldInfo) {
        for (final Path _arg : filenames) {
            try {
                final ClassReader _cr = new ClassReader(Files.readAllBytes(_arg));
                final ClassVisitor _cv = new AuxiliaryDataCollectingClassVisitor(auxiliaryData, collectFieldInfo);
                _cr.accept(_cv, 0);
            } catch (final Exception _ex) {
                throw new RuntimeException(_ex);
            }
        }
    }

    static class CommandLineOptions {
        final boolean traceArrayAccess;
        final boolean traceFieldAccess;
        final boolean traceMethodArgs;
        final boolean traceMethodRetValue;

        CommandLineOptions(final boolean traceArrayAccess, final boolean traceFieldAccess,
                           final boolean traceMethodArgs, final boolean traceMethodRetValue) {
            this.traceArrayAccess = traceArrayAccess;
            this.traceFieldAccess = traceFieldAccess;
            this.traceMethodArgs = traceMethodArgs;
            this.traceMethodRetValue = traceMethodRetValue;
        }
    }
}
