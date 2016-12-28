/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry;

import dyco4j.instrumentation.LoggingHelper;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

final class TracingMethodVisitor extends MethodVisitor {
    private final String desc;
    private final TracingClassVisitor cv;
    private final String name;
    private boolean isAnnotatedAsTest;

    TracingMethodVisitor(final String name, final String descriptor, final MethodVisitor mv,
                         final TracingClassVisitor owner) {
        super(CLI.ASM_VERSION, mv);
        this.name = name;
        this.desc = descriptor;
        this.cv = owner;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        this.isAnnotatedAsTest = desc.matches("Lorg/junit/Test;") ||
                                 desc.matches("Lorg/junit/After;") || desc.matches("Lorg/junit/Before;") ||
                                 desc.matches("Lorg/junit/AfterClass;") ||
                                 desc.matches("Lorg/junit/BeforeClass;") ||
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
        this.mv.visitCode();

        if (this.shouldInstrument()) {
            final String _msg = "marker:" + this.cv.getClassName() + "/" + this.name + this.desc;
            LoggingHelper.emitLogString(this.mv, _msg);
        }
    }

    private boolean shouldInstrument() {
        return this.name.matches(this.cv.getMethodNameRegex()) &&
                (!this.cv.instrumentOnlyAnnotatedTests() || this.isAnnotatedAsTest);
    }
}
