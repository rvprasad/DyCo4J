/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package sindu.jvm.instrumentation.internals;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import sindu.jvm.instrumentation.LoggingHelper;
import sindu.jvm.instrumentation.logging.Logger.Action;

final class TracingMethodVisitor extends MethodVisitor {
    private final TracingClassVisitor cv;
    private final String methodId;
    private final Method method;
    private final boolean isStatic;
    private Label beginOutermostHandler;

    TracingMethodVisitor(final int access, final String name, final String desc, final String signature,
                         final String[] exceptions, final MethodVisitor mv, final TracingClassVisitor owner) {
        super(CLI.ASM_VERSION, mv);
        this.method = new Method(name, desc);
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.beginOutermostHandler = new Label();
        this.methodId = owner.getMethodId(name, desc);
        this.cv = owner;
    }

    @Override
    public void visitCode() {
        this.mv.visitCode();
        LoggingHelper.emitLogMethodEntry(this.mv, this.methodId);

        if (this.cv.cmdLineOptions.traceMethodArgs) {
            // emit code to trace each arg
            int _position = 0;
            if (!this.isStatic)
                _position += LoggingHelper.emitLogArgument(this.mv, _position, Type.getType(Object.class));

            for (final Type _argType : this.method.getArgumentTypes()) {
                _position += LoggingHelper.emitLogArgument(this.mv, _position, _argType);
            }
        }

        this.mv.visitLabel(this.beginOutermostHandler);
    }

    @Override
    public void visitInsn(final int opcode) {
        if (this.cv.cmdLineOptions.traceMethodRetValue) {
            if (opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN || opcode == Opcodes .FRETURN ||
            opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN ) {
                LoggingHelper.emitLogReturn(this.mv, method.getReturnType());
                LoggingHelper.emitLogMethodExit(this.mv, this.methodId, "N");
                this.mv.visitInsn(opcode);
            }
        } else if (this.cv.cmdLineOptions.traceArrayAccess) {
            if (opcode == Opcodes.AASTORE || opcode == Opcodes.BASTORE || opcode == Opcodes.CASTORE ||
                opcode == Opcodes.DASTORE || opcode == Opcodes.FASTORE || opcode == Opcodes.IASTORE ||
                opcode == Opcodes.LASTORE || opcode == Opcodes.SASTORE) {
                visitArrayStoreInsn(opcode);
            } else if (opcode == Opcodes.AALOAD || opcode == Opcodes.BALOAD || opcode == Opcodes.CALOAD ||
                       opcode == Opcodes.DALOAD || opcode == Opcodes.FALOAD || opcode == Opcodes.IALOAD ||
                       opcode == Opcodes.LALOAD || opcode == Opcodes.SALOAD) {
                visitArrayLoadInsn(opcode);
            }
        } else {
            this.mv.visitInsn(opcode);
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        if (this.cv.cmdLineOptions.traceFieldAccess) {
            final Type _fieldType = Type.getType(desc);
            final String _fieldId = this.cv.getFieldId(name, owner, desc);
            final boolean _isFieldStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
            if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
                if (_isFieldStatic)
                    this.mv.visitInsn(Opcodes.ACONST_NULL);
                else
                    this.mv.visitInsn(Opcodes.DUP);

                this.mv.visitFieldInsn(opcode, owner, name, desc);
                LoggingHelper.emitLogField(this.mv, _fieldId, _fieldType, Action.GET);
            } else if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
                if (_isFieldStatic) {
                    this.mv.visitInsn(Opcodes.ACONST_NULL);
                } else {
                    LoggingHelper.emitSwapTwoWordsAndOneWord(this.mv, _fieldType);
                    final int _fieldSort = _fieldType.getSort();
                    if (_fieldSort == Type.LONG || _fieldSort == Type.DOUBLE)
                        this.mv.visitInsn(Opcodes.DUP_X2);
                    else
                        this.mv.visitInsn(Opcodes.DUP_X1);
                }

                LoggingHelper.emitSwapOneWordAndTwoWords(this.mv, _fieldType);
                LoggingHelper.emitLogField(this.mv, _fieldId, _fieldType, Action.PUT);
                this.mv.visitFieldInsn(opcode, owner, name, desc);
            }
        } else {
            this.mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        final Label _endLabel = new Label();
        this.mv.visitLabel(_endLabel);
        this.mv.visitTryCatchBlock(this.beginOutermostHandler, _endLabel, _endLabel, "java/lang/Throwable");
        LoggingHelper.emitLogException(this.mv);
        LoggingHelper.emitLogMethodExit(this.mv, this.methodId, "E");
        this.mv.visitInsn(Opcodes.ATHROW);

        this.mv.visitMaxs(maxStack, maxLocals);
    }

    private void visitArrayStoreInsn(final int opcode) {
        if (opcode == Opcodes.LASTORE || opcode == Opcodes.DASTORE) {
            this.mv.visitInsn(Opcodes.DUP2_X2);
            this.mv.visitInsn(Opcodes.POP2);
            this.mv.visitInsn(Opcodes.DUP2_X2);
            this.mv.visitInsn(Opcodes.DUP2_X2);
            this.mv.visitInsn(Opcodes.POP2);
            this.mv.visitInsn(Opcodes.DUP2_X2);
        } else {
            this.mv.visitInsn(Opcodes.DUP_X2);
            this.mv.visitInsn(Opcodes.POP);
            this.mv.visitInsn(Opcodes.DUP2_X1);
            this.mv.visitInsn(Opcodes.DUP2_X1);
            this.mv.visitInsn(Opcodes.POP2);
            this.mv.visitInsn(Opcodes.DUP_X2);
        }

        switch (opcode) {
            case Opcodes.AASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.getObjectType("java/lang/Object"));
                break;
            case Opcodes.BASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.BYTE_TYPE);
                break;
            case Opcodes.CASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.CHAR_TYPE);
                break;
            case Opcodes.FASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.FLOAT_TYPE);
                break;
            case Opcodes.IASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.INT_TYPE);
                break;
            case Opcodes.SASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.SHORT_TYPE);
                break;
            case Opcodes.DASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.DOUBLE_TYPE);
                break;
            case Opcodes.LASTORE:
                LoggingHelper.emitConvertToString(this.mv, Type.LONG_TYPE);
                break;
        }

        LoggingHelper.emitLogArray(this.mv, Action.PUT);

        this.mv.visitInsn(opcode);
    }

    private void visitArrayLoadInsn(final int opcode) {
        this.mv.visitInsn(Opcodes.DUP2);

        this.mv.visitInsn(opcode);

        if (opcode == Opcodes.LALOAD || opcode == Opcodes.DALOAD)
            this.mv.visitInsn(Opcodes.DUP2_X2);
        else
            this.mv.visitInsn(Opcodes.DUP_X2);

        switch (opcode) {
            case Opcodes.AALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.getObjectType("java/lang/Object"));
                break;
            case Opcodes.BALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.BYTE_TYPE);
                break;
            case Opcodes.CALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.CHAR_TYPE);
                break;
            case Opcodes.FALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.FLOAT_TYPE);
                break;
            case Opcodes.IALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.INT_TYPE);
                break;
            case Opcodes.SALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.SHORT_TYPE);
                break;
            case Opcodes.DALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.DOUBLE_TYPE);
                break;
            case Opcodes.LALOAD:
                LoggingHelper.emitConvertToString(this.mv, Type.LONG_TYPE);
                break;
        }

        LoggingHelper.emitLogArray(this.mv, Action.GET);
    }
}
