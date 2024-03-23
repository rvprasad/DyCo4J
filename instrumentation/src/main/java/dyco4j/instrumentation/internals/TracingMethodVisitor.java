/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import dyco4j.instrumentation.LoggingHelper;
import dyco4j.logging.Logger;
import dyco4j.utility.ClassNameHelper;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

final class TracingMethodVisitor extends MethodVisitor {
    private final String methodId;
    private final Method method;
    private final boolean isStatic;
    private final TracingClassVisitor cv;
    private final Map<Label, Label> beginLabel2endLabel;
    private int callsiteId;
    private boolean thisInitialized;
    private Label outermostExceptionHandlerBeginLabel;

    TracingMethodVisitor(final int access, final String name, final String desc, final MethodVisitor mv,
                         final TracingClassVisitor owner, final boolean thisInitialized) {
        super(CLI.ASM_VERSION, mv);
        this.method = new Method(name, desc);
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodId = owner.getMethodId(name, desc);
        this.cv = owner;
        this.thisInitialized = thisInitialized;
        this.beginLabel2endLabel = new HashMap<>();
    }

    @Override
    public void visitCode() {
        beginOutermostExceptionHandler();
        emitLogMethodEntry();
        emitLogMethodArguments();
    }

    @Override
    public void visitInsn(final int opcode) {
        switch (opcode) {
            case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN,
                    Opcodes.RETURN: {
                if (cv.cmdLineOptions.traceMethodRetValue())
                    LoggingHelper.emitLogReturn(mv, method.getReturnType());
                LoggingHelper.emitLogMethodExit(mv, methodId, LoggingHelper.ExitKind.NORMAL);
                super.visitInsn(opcode);
                break;
            }
            case Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.DASTORE, Opcodes.FASTORE,
                    Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.SASTORE: {
                if (cv.cmdLineOptions.traceArrayAccess())
                    visitArrayStoreInsn(opcode);
                else
                    super.visitInsn(opcode);
                break;
            }
            case Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.DALOAD, Opcodes.FALOAD,
                    Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.SALOAD: {
                if (cv.cmdLineOptions.traceArrayAccess())
                    visitArrayLoadInsn(opcode);
                else
                    super.visitInsn(opcode);
                break;
            }
            default:
                super.visitInsn(opcode);
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        if (!cv.cmdLineOptions.traceFieldAccess()) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        final Type _fieldType = Type.getType(desc);
        final String _fieldId = cv.getFieldId(name, owner, desc);
        final boolean _isFieldStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
        if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
            if (_isFieldStatic)
                super.visitInsn(Opcodes.ACONST_NULL);
            else if (thisInitialized)
                super.visitInsn(Opcodes.DUP);
            else {
                super.visitLdcInsn(LoggingHelper.UNINITIALIZED_THIS);
                super.visitInsn(Opcodes.SWAP);
            }

            super.visitFieldInsn(opcode, owner, name, desc);
            LoggingHelper.emitLogField(mv, _fieldId, _fieldType, Logger.FieldAction.GETF);
        } else if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
            if (_isFieldStatic) {
                super.visitInsn(Opcodes.ACONST_NULL);
            } else if (thisInitialized) {
                LoggingHelper.emitSwapTwoWordsAndOneWord(mv, _fieldType);
                final int _fieldSort = _fieldType.getSort();
                if (_fieldSort == Type.LONG || _fieldSort == Type.DOUBLE)
                    super.visitInsn(Opcodes.DUP_X2);
                else
                    super.visitInsn(Opcodes.DUP_X1);
            } else {
                super.visitLdcInsn(LoggingHelper.UNINITIALIZED_THIS);
            }

            LoggingHelper.emitSwapOneWordAndTwoWords(mv, _fieldType);
            LoggingHelper.emitLogField(mv, _fieldId, _fieldType, Logger.FieldAction.PUTF);
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        endOutermostExceptionHandler();
        for (final Map.Entry<Label, Label> _e : beginLabel2endLabel.entrySet()) {
            final Label _handlerLabel = new Label();
            super.visitLabel(_handlerLabel);
            super.visitTryCatchBlock(_e.getKey(), _e.getValue(), _handlerLabel, "java/lang/Throwable");
            LoggingHelper.emitLogException(this.mv);
            LoggingHelper.emitLogMethodExit(this.mv, this.methodId, LoggingHelper.ExitKind.EXCEPTIONAL);
            super.visitInsn(Opcodes.ATHROW);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                final boolean itf) {
        if (cv.cmdLineOptions.traceMethodCall())
            LoggingHelper.emitLogMethodCall(mv, cv.getMethodId(name, owner, desc), callsiteId++);
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,
                                       final Object... bsmArgs) {
        if (cv.cmdLineOptions.traceMethodCall())
            LoggingHelper.emitLogMethodCall(mv,
                    cv.getMethodId(name, ClassNameHelper.DYNAMIC_METHOD_OWNER, desc), callsiteId++);
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    void setThisInitialized() {
        thisInitialized = true;
    }

    void beginOutermostExceptionHandler() {
        outermostExceptionHandlerBeginLabel = new Label();
        super.visitLabel(outermostExceptionHandlerBeginLabel);
    }

    void endOutermostExceptionHandler() {
        assert outermostExceptionHandlerBeginLabel != null;
        final Label _l = new Label();
        super.visitLabel(_l);
        beginLabel2endLabel.put(outermostExceptionHandlerBeginLabel, _l);
        outermostExceptionHandlerBeginLabel = null;
    }

    private void emitLogMethodEntry() {
        super.visitCode();
        LoggingHelper.emitLogMethodEntry(mv, methodId);
    }

    private void emitLogMethodArguments() {
        if (!cv.cmdLineOptions.traceMethodArgs())
            return;

        // emit code to trace each arg
        int _position = 0;
        int _localVarIndex = 0;
        if (!isStatic) {
            final OptionalInt _tmp1 = thisInitialized ? OptionalInt.of(_localVarIndex) : OptionalInt.empty();
            _localVarIndex += LoggingHelper.emitLogArgument(mv, _position, _tmp1,
                    Type.getType(Object.class));
            _position++;
        }

        for (final Type _argType : method.getArgumentTypes()) {
            _localVarIndex += LoggingHelper.emitLogArgument(mv, _position, OptionalInt.of(_localVarIndex),
                    _argType);
            _position++;
        }
    }

    private void visitArrayStoreInsn(final int opcode) {
        if (opcode == Opcodes.LASTORE || opcode == Opcodes.DASTORE) {
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.DUP2_X2);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP2_X2);
        } else {
            super.visitInsn(Opcodes.DUP_X2);
            super.visitInsn(Opcodes.POP);
            super.visitInsn(Opcodes.DUP2_X1);
            super.visitInsn(Opcodes.DUP2_X1);
            super.visitInsn(Opcodes.POP2);
            super.visitInsn(Opcodes.DUP_X2);
        }

        switch (opcode) {
            case Opcodes.AASTORE:
                LoggingHelper.emitConvertToString(mv, Type.getObjectType("java/lang/Object"));
                break;
            case Opcodes.BASTORE:
                LoggingHelper.emitConvertToString(mv, Type.BYTE_TYPE);
                break;
            case Opcodes.CASTORE:
                LoggingHelper.emitConvertToString(mv, Type.CHAR_TYPE);
                break;
            case Opcodes.FASTORE:
                LoggingHelper.emitConvertToString(mv, Type.FLOAT_TYPE);
                break;
            case Opcodes.IASTORE:
                LoggingHelper.emitConvertToString(mv, Type.INT_TYPE);
                break;
            case Opcodes.SASTORE:
                LoggingHelper.emitConvertToString(mv, Type.SHORT_TYPE);
                break;
            case Opcodes.DASTORE:
                LoggingHelper.emitConvertToString(mv, Type.DOUBLE_TYPE);
                break;
            case Opcodes.LASTORE:
                LoggingHelper.emitConvertToString(mv, Type.LONG_TYPE);
                break;
        }

        LoggingHelper.emitLogArray(mv, Logger.ArrayAction.PUTA);

        super.visitInsn(opcode);
    }

    private void visitArrayLoadInsn(final int opcode) {
        super.visitInsn(Opcodes.DUP2);

        super.visitInsn(opcode);

        if (opcode == Opcodes.LALOAD || opcode == Opcodes.DALOAD)
            super.visitInsn(Opcodes.DUP2_X2);
        else
            super.visitInsn(Opcodes.DUP_X2);

        switch (opcode) {
            case Opcodes.AALOAD:
                LoggingHelper.emitConvertToString(mv, Type.getObjectType("java/lang/Object"));
                break;
            case Opcodes.BALOAD:
                LoggingHelper.emitConvertToString(mv, Type.BYTE_TYPE);
                break;
            case Opcodes.CALOAD:
                LoggingHelper.emitConvertToString(mv, Type.CHAR_TYPE);
                break;
            case Opcodes.FALOAD:
                LoggingHelper.emitConvertToString(mv, Type.FLOAT_TYPE);
                break;
            case Opcodes.IALOAD:
                LoggingHelper.emitConvertToString(mv, Type.INT_TYPE);
                break;
            case Opcodes.SALOAD:
                LoggingHelper.emitConvertToString(mv, Type.SHORT_TYPE);
                break;
            case Opcodes.DALOAD:
                LoggingHelper.emitConvertToString(mv, Type.DOUBLE_TYPE);
                break;
            case Opcodes.LALOAD:
                LoggingHelper.emitConvertToString(mv, Type.LONG_TYPE);
                break;
        }

        LoggingHelper.emitLogArray(mv, Logger.ArrayAction.GETA);
    }

}
