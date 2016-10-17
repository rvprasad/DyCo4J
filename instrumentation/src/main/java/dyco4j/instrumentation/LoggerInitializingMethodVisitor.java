/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation;

import org.objectweb.asm.MethodVisitor;

final class LoggerInitializingMethodVisitor extends MethodVisitor {
    private final String name;
    private final LoggerInitializingClassVisitor cv;

    LoggerInitializingMethodVisitor(final int api, final MethodVisitor mv,
                                    final LoggerInitializingClassVisitor cv, final String name) {
        super(api, mv);
        this.cv = cv;
        this.name = name;
    }

    @Override
    public final void visitCode() {
        this.mv.visitCode();

        if (this.name.equals("<clinit>")) {
            this.cv.clinitVisited();
            LoggingHelper.emitInsnToLoadAndInitializeLogger(mv);
        }
    }
}
