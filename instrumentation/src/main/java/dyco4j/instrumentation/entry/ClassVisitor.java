/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.entry;

import org.objectweb.asm.Opcodes;
import dyco4j.LoggingHelper;

final class ClassVisitor extends org.objectweb.asm.ClassVisitor {
    private final String methodNameRegex;
    private final boolean onlyAnnotatedTests;
    private boolean isClinitVisited;
    private String className;

    ClassVisitor(final org.objectweb.asm.ClassVisitor cv, final String methodNameRegex, boolean onlyAnnotatedTests) {
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

    void clinitVisited() {
        this.isClinitVisited = true;
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
    public org.objectweb.asm.MethodVisitor visitMethod(final int access, final String name, final String desc,
                                                       final String signature, final String[] exceptions) {
        final org.objectweb.asm.MethodVisitor _mv = cv.visitMethod(access, name, desc, signature, exceptions);
        return _mv == null ? null : new MethodVisitor(_mv, name, desc, this);
    }

    @Override
    public void visitEnd() {
        if (!this.isClinitVisited) {
            final org.objectweb.asm.MethodVisitor _mv =
                    cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            if (_mv != null) {
                LoggingHelper.emitInsnToLoadAndInitializeLogger(_mv);
                _mv.visitInsn(Opcodes.RETURN);
            }
        }
    }
}
