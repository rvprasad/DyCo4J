/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Helper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Helper.class);
    private Helper() {
    }

    public static void processFiles(final Path srcRoot, final Path trgRoot, final Predicate<Path> pathSelector,
                                    final BiConsumer<Path, Path> transformer) throws IOException {
        final Stream<Path> _srcPaths = Files.walk(srcRoot).filter(pathSelector);
        _srcPaths.parallel().forEach(_srcPath -> {
            try {
                final Path _relativeSrcPath = srcRoot.relativize(_srcPath);
                final Path _trgPath = trgRoot.resolve(_relativeSrcPath);
                final Path _parent = _trgPath.getParent();
                if (!Files.exists(_parent))
                    Files.createDirectories(_parent);

                if (Files.exists(_trgPath))
                    LOGGER.info(MessageFormat.format("Overwriting {0}", _trgPath));
                else
                    LOGGER.info(MessageFormat.format("Writing {0}", _trgPath));

                transformer.accept(_srcPath, _trgPath);
            } catch (final IOException _ex) {
                throw new RuntimeException(_ex);
            }
        });
    }
}
