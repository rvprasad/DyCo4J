/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation;

import dyco4j.LoggingHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public abstract class LoggerInitializingClassVisitor extends ClassVisitor {
    private boolean isClinitVisited;

    public LoggerInitializingClassVisitor(int api, ClassVisitor cv) {
        super(api, cv);
    }

    public void clinitVisited() {
        this.isClinitVisited = true;
    }

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
