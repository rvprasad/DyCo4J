/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry;

import dyco4j.LoggingHelper;
import dyco4j.instrumentation.LoggerInitializingMethodVisitor;
import org.objectweb.asm.AnnotationVisitor;

public final class TracingMethodVisitor extends LoggerInitializingMethodVisitor<TracingClassVisitor> {
    private final String desc;
    private boolean isAnnotatedAsTest;

    TracingMethodVisitor(final org.objectweb.asm.MethodVisitor mv, final String name, final String descriptor,
                         final TracingClassVisitor owner) {
        super(CLI.ASM_VERSION, mv, name, owner);
        this.desc = descriptor;
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

    private boolean shouldInstrument() {
        return this.name.matches(this.cv.getMethodNameRegex()) &&
               (!this.cv.instrumentOnlyAnnotatedTests() || this.isAnnotatedAsTest);
    }

    @Override
    protected void visitCodeAfterLoggerInitialization() {
        mv.visitCode();

        if (this.shouldInstrument()) {
            final String _msg = "marker:" + this.cv.getClassName() + "/" + name + desc;
            LoggingHelper.emitLogString(mv, _msg);
        }
    }
}
