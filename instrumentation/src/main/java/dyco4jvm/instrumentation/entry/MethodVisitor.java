/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4jvm.instrumentation.entry;

import org.objectweb.asm.AnnotationVisitor;
import dyco4jvm.LoggingHelper;

final class MethodVisitor extends org.objectweb.asm.MethodVisitor {
    private final String name;
    private final String desc;
    private final ClassVisitor cv;
    private boolean instrumentMethod;

    MethodVisitor(final org.objectweb.asm.MethodVisitor mv, final String name, final String descriptor,
                  final ClassVisitor cv) {
        super(CLI.ASM_VERSION, mv);
        this.name = name;
        this.desc = descriptor;
        this.cv = cv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        this.instrumentMethod = desc.matches("Lorg/junit/Test;") ||
                                desc.matches("Lorg/junit/After;") || desc.matches("Lorg/junit/Before;") ||
                                desc.matches("Lorg/junit/AfterClass;") || desc.matches("Lorg/junit/BeforeClass;") ||
                                desc.matches("Lorg/testng/annotations/Test") ||
                                desc.matches("Lorg/testng/annotations/AfterTest") ||
                                desc.matches("Lorg/testng/annotations/BeforeTest") ||
                                desc.matches("Lorg/testng/annotations/AfterClass") ||
                                desc.matches("Lorg/testng/annotations/BeforeClass") ||
                                desc.matches("Lorg/testng/annotations/AfterMethod") ||
                                desc.matches("Lorg/testng/annotations/BeforeMethod");
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitCode() {
        if (this.name.equals("<clinit>")) {
            this.cv.clinitVisited();
            LoggingHelper.emitInsnToLoadAndInitializeLogger(mv);
        }

        mv.visitCode();

        if (this.name.matches(this.cv.getMethodNameRegex()) || this.instrumentMethod) {
            final String _msg = "marker:" + this.cv.getClassName() + "/" + name + desc;
            LoggingHelper.emitLogString(mv, _msg);
        }
    }
}
