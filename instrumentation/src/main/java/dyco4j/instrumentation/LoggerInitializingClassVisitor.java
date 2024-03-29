/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class LoggerInitializingClassVisitor extends ClassVisitor {
    private boolean isClinitVisited;

    public LoggerInitializingClassVisitor(final int api, final ClassVisitor cv) {
        super(api, cv);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        final MethodVisitor _mv = super.visitMethod(access, name, desc, signature, exceptions);
        return _mv == null ? null : new LoggerInitializingMethodVisitor(api, _mv, this, name);
    }

    public void visitEnd() {
        if (!isClinitVisited) {
            final org.objectweb.asm.MethodVisitor _mv =
                    super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            if (_mv != null) {
                LoggingHelper.emitInsnToLoadAndInitializeLogger(_mv);
                _mv.visitInsn(Opcodes.RETURN);
            }
        }
    }

    void clinitVisited() {
        isClinitVisited = true;
    }
}
