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
        isAnnotatedAsTest = desc.matches("Lorg/junit/Test;") ||
                desc.matches("Lorg/junit/After;") || desc.matches("Lorg/junit/Before;") ||
                desc.matches("Lorg/junit/AfterClass;") ||
                desc.matches("Lorg/junit/BeforeClass;") ||
                desc.matches("Lorg/junit/jupiter/apiTest;") ||
                desc.matches("Lorg/junit/jupiter/api/AfterEach;") || desc.matches("Lorg/junit/jupiter/api/BeforeEach;") ||
                desc.matches("Lorg/junit/jupiter/api/AfterAll;") ||
                desc.matches("Lorg/junit/jupiter/api/BeforeAll;") ||
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
        super.visitCode();

        if (shouldInstrument()) {
            final String _msg = "marker:" + cv.getClassName() + "/" + name + desc;
            LoggingHelper.emitLogString(mv, _msg);
        }
    }

    private boolean shouldInstrument() {
        return name.matches(cv.getMethodNameRegex()) &&
                (!cv.instrumentOnlyAnnotatedTests() || isAnnotatedAsTest);
    }
}
