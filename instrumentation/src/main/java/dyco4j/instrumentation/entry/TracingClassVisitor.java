/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class TracingClassVisitor extends ClassVisitor {
    private final String methodNameRegex;
    private final boolean onlyAnnotatedTests;
    private String className;

    TracingClassVisitor(final ClassVisitor cv, final String methodNameRegex,
                        final boolean onlyAnnotatedTests) {
        super(CLI.ASM_VERSION, cv);
        this.methodNameRegex = methodNameRegex;
        this.onlyAnnotatedTests = onlyAnnotatedTests;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc,
                                     final String signature, final String[] exceptions) {
        final MethodVisitor _mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
        return _mv == null || (access & Opcodes.ACC_PUBLIC) == 0 ? _mv :
                new TracingMethodVisitor(name, desc, _mv, this);
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
}
