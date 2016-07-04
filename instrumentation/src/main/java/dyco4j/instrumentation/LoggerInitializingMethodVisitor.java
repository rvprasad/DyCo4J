/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation;

import dyco4j.LoggingHelper;
import org.objectweb.asm.MethodVisitor;

public abstract class LoggerInitializingMethodVisitor<T extends LoggerInitializingClassVisitor> extends MethodVisitor {
    protected final String name;
    protected final T cv;

    protected LoggerInitializingMethodVisitor(final int api, final MethodVisitor mv, final String name,
                                              final T cv) {
        super(api, mv);
        this.name = name;
        this.cv = cv;
    }

    @Override
    public final void visitCode() {
        if (this.name.equals("<clinit>")) {
            this.cv.clinitVisited();
            LoggingHelper.emitInsnToLoadAndInitializeLogger(mv);
        }

        visitCodeAfterLoggerInitialization();
    }

    protected abstract void visitCodeAfterLoggerInitialization();
}
