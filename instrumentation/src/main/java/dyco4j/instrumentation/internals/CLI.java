/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.objectweb.asm.Opcodes.ASM5;

public final class CLI {
    final static int ASM_VERSION = ASM5;

    public static void main(final String[] args) throws IOException {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt("file").required().hasArg(true)
                                 .desc("File listing the fully-qualified paths to classes to be instrumented.")
                                 .build());
        _options.addOption(Option.builder().longOpt("methodNameRegex").hasArg(true)
                                 .desc("Regex identifying the methods to be instrumented. Default: .*.").build());
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
            final CommandLineOptions _cmdLineOptions = new CommandLineOptions(_cmdLine.hasOption("traceArrayAccess"),
                                                                              _cmdLine.hasOption("traceFieldAccess"),
                                                                              _cmdLine.hasOption("traceMethodArgs"),
                                                                              _cmdLine.hasOption(
                                                                                      "traceMethodReturnValue"));
            final Set<String> _filenames = new HashSet<>(FileUtils.readLines(new File(_cmdLine.getOptionValue('f'))));
            final String _methodNameRegex = _cmdLine.getOptionValue("methodNameRegex", ".*");

            final Path _dataFile = Paths.get("auxiliary_data.json");
            final AuxiliaryData _auxiliaryData = AuxiliaryData.loadData(_dataFile);

            getMemberId2NameMapping(_filenames, _auxiliaryData, _cmdLineOptions.traceFieldAccess);

            _filenames.parallelStream().forEach(_arg -> {
                try {
                    final File _src = new File(_arg);
                    final File _trg = new File(_arg + ".orig");

                    final ClassReader _cr = new ClassReader(FileUtils.readFileToByteArray(_src));
                    final ClassWriter _cw = new ClassWriter(_cr, ClassWriter.COMPUTE_MAXS);
                    final Map<String, String> _shortFieldName2Id =
                            Collections.unmodifiableMap(_auxiliaryData.shortFieldName2Id);
                    final Map<String, String> _shortMethodName2Id =
                            Collections.unmodifiableMap(_auxiliaryData.shortMethodName2Id);
                    final Map<String, String> _class2superClass =
                            Collections.unmodifiableMap(_auxiliaryData.class2superClass);
                    final ClassVisitor _cv =
                            new TracingClassVisitor(_cw, _shortFieldName2Id, _shortMethodName2Id, _class2superClass,
                                                    _methodNameRegex, _cmdLineOptions);
                    _cr.accept(_cv, 0);

                    if (!_trg.exists())
                        FileUtils.moveFile(_src, _trg);

                    FileUtils.writeByteArrayToFile(_src, _cw.toByteArray());
                } catch (final Exception _ex) {
                    throw new RuntimeException(_ex);
                }
            });

            AuxiliaryData.saveData(_auxiliaryData, _dataFile);
        } catch (final ParseException _ex) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        } catch (final IOException _ex) {
            Logger.getGlobal().log(Level.SEVERE, null, _ex);
        }
    }

    private static void getMemberId2NameMapping(final Collection<String> filenames, final AuxiliaryData auxiliaryData,
                                                final boolean collectFieldInfo) {
        for (final String _arg : filenames) {
            try {
                final ClassReader _cr = new ClassReader(FileUtils.readFileToByteArray(new File(_arg)));
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
