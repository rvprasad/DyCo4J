/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package sindu.jvm.instrumentation.entry;

import sindu.jvm.instrumentation.LoggingHelper;

class MethodVisitor extends org.objectweb.asm.MethodVisitor {
    private final String name;
    private final String desc;
    private final ClassVisitor cv;

    public MethodVisitor(final org.objectweb.asm.MethodVisitor mv, final String name, final String descriptor,
                         final ClassVisitor cv) {
        super(CLI.ASM_VERSION, mv);
        this.name = name;
        this.desc = descriptor;
        this.cv = cv;
    }

    @Override
    public void visitCode() {
        mv.visitCode();

        if (this.name.equals("<clinit>")) {
            this.cv.isClinitVisited = true;
            LoggingHelper.emitInsnToLoadAndInitializeLogger(mv);
        }

        // TODO: Add check to only add log to test methods
        final String _msg = "entry point:" + name + desc;
        LoggingHelper.emitLogString(mv, _msg);
    }
}
