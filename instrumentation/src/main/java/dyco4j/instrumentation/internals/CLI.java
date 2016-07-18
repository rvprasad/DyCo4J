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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ASM5;

public final class CLI {
    final static int ASM_VERSION = ASM5;
    private static final Path AUXILIARY_DATA_FILE_NAME = Paths.get("auxiliary_data.json");
    private static final String METHOD_NAME_REGEX = ".*";

    public static void main(final String[] args) throws IOException {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt("in-folder").required().hasArg(true)
                                 .desc("Folder containing the classes (as descendants) to be instrumented.").build());
        _options.addOption(Option.builder().longOpt("out-folder").required().hasArg(true)
                                 .desc("Folder containing the classes (as descendants) with instrumentation.").build());
        final String _msg = MessageFormat
                .format("Regex identifying the methods to be instrumented. Default: {0}.", METHOD_NAME_REGEX);
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
            processCommandLine(new DefaultParser().parse(_options, args));
        } catch (final ParseException _ex1) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        }
    }

    private static void processCommandLine(CommandLine cmdLine) throws IOException {
        final CommandLineOptions _cmdLineOptions =
                new CommandLineOptions(cmdLine.hasOption("traceArrayAccess"), cmdLine.hasOption("traceFieldAccess"),
                                       cmdLine.hasOption("traceMethodArgs"),
                                       cmdLine.hasOption("traceMethodReturnValue"));
        final Path _srcRoot = Paths.get(cmdLine.getOptionValue("in-folder"));
        final Set<Path> _filenames = getFilenames(_srcRoot);
        final AuxiliaryData _auxiliaryData = AuxiliaryData.loadData(AUXILIARY_DATA_FILE_NAME);
        getMemberId2NameMapping(_filenames, _auxiliaryData, _cmdLineOptions.traceFieldAccess);

        final String _methodNameRegex = cmdLine.getOptionValue("methodNameRegex", METHOD_NAME_REGEX);
        final Path _trgRoot = Paths.get(cmdLine.getOptionValue("out-folder"));
        _filenames.parallelStream().forEach(_srcPath -> {
            try {
                final Path _relativeSrcPath = _srcRoot.relativize(_srcPath);
                final Path _trgPath = _trgRoot.resolve(_relativeSrcPath);
                final Path _parent = _trgPath.getParent();
                if (!Files.exists(_parent))
                    Files.createDirectories(_parent);

                if (Files.exists(_trgPath))
                    System.out.println(MessageFormat.format("Overwriting {0}", _trgPath));
                else
                    System.out.println(MessageFormat.format("Writing {0}", _trgPath));

                final ClassReader _cr = new ClassReader(Files.readAllBytes(_srcPath));
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
                _cr.accept(_cv2, 0);

                Files.write(_trgPath, _cw.toByteArray());
            } catch (final IOException _ex) {
                throw new RuntimeException(_ex);
            }
        });
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
