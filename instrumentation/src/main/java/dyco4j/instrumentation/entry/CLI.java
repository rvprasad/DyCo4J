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
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.ASM5;

public final class CLI {
    final static int ASM_VERSION = ASM5;
    private final static String METHOD_NAME_REGEX = "^test.*";

    public static void main(final String[] args) throws IOException {
        final Options _options = new Options();
        _options.addOption(Option.builder().longOpt("in-folder").required().hasArg()
                                 .desc("Folder containing the classes (as descendants) to be instrumented.").build());
        _options.addOption(Option.builder().longOpt("out-folder").required().hasArg()
                                 .desc("Folder containing the classes (as descendants) with instrumentation.").build());
        final String _msg = MessageFormat
                .format("Regex identifying the methods to be instrumented. Default: {0}.", METHOD_NAME_REGEX);
        _options.addOption(Option.builder().longOpt("method-name-regex").hasArg(true).desc(_msg).build());
        _options.addOption(Option.builder().longOpt("only-annotated-tests").hasArg(false)
                                 .desc("Instrument only tests identified by annotations.").build());

        try {
            final CommandLine _cmdLine = new DefaultParser().parse(_options, args);
            final String _methodNameRegex = _cmdLine.getOptionValue("method-name-regex", METHOD_NAME_REGEX);
            final Path _srcRoot = Paths.get(_cmdLine.getOptionValue("in-folder"));
            final Path _trgRoot = Paths.get(_cmdLine.getOptionValue("out-folder"));
            final Stream<Path> _srcPaths = Files.walk(_srcRoot).filter(p -> p.toString().endsWith(".class"));
            _srcPaths.parallel().forEach(_srcPath -> {
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

                    final byte[] _in = Files.readAllBytes(_srcPath);
                    final byte[] _out =
                            instrumentClass(_in, _methodNameRegex, _cmdLine.hasOption("only-annotated-tests"));
                    Files.write(_trgPath, _out);
                } catch (final IOException _ex) {
                    throw new RuntimeException(_ex);
                }
            });
        } catch (final ParseException _ex) {
            new HelpFormatter().printHelp(CLI.class.getName(), _options);
        }
    }

    private static byte[] instrumentClass(final byte[] b, final String methodNameRegex,
                                          final boolean onlyAnnotatedTests) {
        final ClassReader _cr = new ClassReader(b);
        final ClassWriter _cw = new ClassWriter(_cr, ClassWriter.COMPUTE_FRAMES);
        final ClassVisitor _cv1 = new LoggerInitializingClassVisitor(CLI.ASM_VERSION, _cw);
        final ClassVisitor _cv2 = new TracingClassVisitor(_cv1, methodNameRegex, onlyAnnotatedTests);
        _cr.accept(_cv2, 0);
        return _cw.toByteArray();
    }
}
