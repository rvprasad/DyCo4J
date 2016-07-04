/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry;

import dyco4j.instrumentation.LoggerInitializingClassVisitor;
import org.objectweb.asm.MethodVisitor;

final class TracingClassVisitor extends LoggerInitializingClassVisitor {
    private final String methodNameRegex;
    private final boolean onlyAnnotatedTests;
    private String className;

    TracingClassVisitor(final org.objectweb.asm.ClassVisitor cv, final String methodNameRegex,
                        boolean onlyAnnotatedTests) {
        super(CLI.ASM_VERSION, cv);
        this.methodNameRegex = methodNameRegex;
        this.onlyAnnotatedTests = onlyAnnotatedTests;
    }

    String getMethodNameRegex() {
        return this.methodNameRegex;
    }

    boolean instrumentOnlyAnnotatedTests() {
        return this.onlyAnnotatedTests;
    }

    String getClassName() {
        return this.className;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc,
                                     final String signature, final String[] exceptions) {
        final MethodVisitor _mv = cv.visitMethod(access, name, desc, signature, exceptions);
        return _mv == null ? null : new TracingMethodVisitor(_mv, name, desc, this);
    }
}
