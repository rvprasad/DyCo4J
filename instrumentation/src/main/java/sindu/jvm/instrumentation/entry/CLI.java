/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package sindu.jvm.instrumentation.entry;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.objectweb.asm.Opcodes.ASM5;

public class CLI {
    // TODO: Add tests for the package

    final static int ASM_VERSION = ASM5;

    public static void main(final String[] args) {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt("file").hasArg().required()
                                 .desc("File listing the fully-qualified paths to classes to be instrumented.")
                                 .build());
        _options.addOption(Option.builder().longOpt("methodNameRegex").hasArg()
                                 .desc("Regex identifying the methods to be instrumented. Default: test.*.").build());
        try {
            final CommandLine _cmdLine = new DefaultParser().parse(_options, args);
            final Collection<String> _fileNames = FileUtils.readLines(new File(_cmdLine.getOptionValue("file")));
            new HashSet<>(_fileNames).parallelStream().forEach(_arg -> {
                try {
                    final File _src = new File(_arg);
                    final File _trg = new File(_arg + ".orig");
                    final byte[] _in = FileUtils.readFileToByteArray(_src);
                    final byte[] _out = instrumentClass(_in, _cmdLine.getOptionValue("methodNameRegex", "test.*"));

                    if (!_trg.exists())
                        FileUtils.moveFile(_src, _trg);

                    FileUtils.writeByteArrayToFile(_src, _out);
                } catch (final IOException _ex) {
                    Logger.getGlobal().log(Level.SEVERE, null, _ex);
                }
            });
        } catch (final ParseException _ex) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        } catch (final IOException _ex) {
            Logger.getGlobal().log(Level.SEVERE, null, _ex);
        }
    }

    public static byte[] instrumentClass(final byte[] b, final String methodNamePattern) {
        final ClassReader _cr = new ClassReader(b);
        final ClassWriter _cw = new ClassWriter(_cr, ClassWriter.COMPUTE_MAXS);
        final org.objectweb.asm.ClassVisitor _cv = new ClassVisitor(_cw, methodNamePattern);
        _cr.accept(_cv, 0);
        return _cw.toByteArray();
    }
}
