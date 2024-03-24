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
        if (cv.cmdLineOptions.traceFieldAccess().isEmpty()) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        final CLI.AccessOption _fieldAccessOption = cv.cmdLineOptions.traceFieldAccess().get();
        final Type _fieldType = Type.getType(desc);
        final String _fieldId = cv.getFieldId(name, owner, desc);
        final boolean _isFieldStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
        switch (opcode) {
            case Opcodes.GETSTATIC, Opcodes.GETFIELD:
                switch (_fieldAccessOption) {
                    case CLI.AccessOption.with_values:
                        if (_isFieldStatic)
                            super.visitInsn(Opcodes.ACONST_NULL);
                        else if (thisInitialized)
                            super.visitInsn(Opcodes.DUP);
                        else {
                            super.visitLdcInsn(LoggingHelper.UNINITIALIZED_THIS);
                            super.visitInsn(Opcodes.SWAP);
                        }
                        super.visitFieldInsn(opcode, owner, name, desc);
                        LoggingHelper.emitLogFieldWithValues(mv, _fieldId, _fieldType, Logger.FieldAction.GETF);
                        break;

                    case CLI.AccessOption.without_values:
                        super.visitFieldInsn(opcode, owner, name, desc);
                        LoggingHelper.emitLogFieldWithoutValues(mv, _fieldId, Logger.FieldAction.GETF);
                }
                break;
            case Opcodes.PUTSTATIC, Opcodes.PUTFIELD:
                switch (_fieldAccessOption) {
                    case CLI.AccessOption.with_values:
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
                        LoggingHelper.emitLogFieldWithValues(mv, _fieldId, _fieldType, Logger.FieldAction.PUTF);
                        break;

                    case CLI.AccessOption.without_values:
                        LoggingHelper.emitLogFieldWithoutValues(mv, _fieldId, Logger.FieldAction.PUTF);
                }
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

        LoggingHelper.emitConvertToString(mv, getArrayElementType(opcode));
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

        LoggingHelper.emitConvertToString(mv, getArrayElementType(opcode));
        LoggingHelper.emitLogArray(mv, Logger.ArrayAction.GETA);
    }

    private static Type getArrayElementType(int opcode) {
        return switch (opcode) {
            case Opcodes.AALOAD, Opcodes.AASTORE -> Type.getObjectType("java/lang/Object");
            case Opcodes.BALOAD, Opcodes.BASTORE -> Type.BYTE_TYPE;
            case Opcodes.CALOAD, Opcodes.CASTORE -> Type.CHAR_TYPE;
            case Opcodes.FALOAD, Opcodes.FASTORE -> Type.FLOAT_TYPE;
            case Opcodes.IALOAD, Opcodes.IASTORE -> Type.INT_TYPE;
            case Opcodes.SALOAD, Opcodes.SASTORE -> Type.SHORT_TYPE;
            case Opcodes.DALOAD, Opcodes.DASTORE -> Type.DOUBLE_TYPE;
            case Opcodes.LALOAD, Opcodes.LASTORE -> Type.LONG_TYPE;
            default -> throw new IllegalStateException("Opcode not related to arrays: " + opcode);
        };
    }
}
