/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package dyco4j;

import dyco4j.instrumentation.logging.Logger;
import dyco4j.instrumentation.logging.LoggerInitializer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class LoggingHelper {
    private static final String LOGGER;
    private static final String LOGGER_INITIALIZER;
    private static final Method LOGGER_INITIALIZER_INITIALIZE;
    private static final Method LOG_ARGUMENT;
    private static final Method LOG_ARRAY;
    private static final Method LOG_EXCEPTION;
    private static final Method LOG_FIELD;
    private static final Method LOG_METHOD_ENTRY;
    private static final Method LOG_METHOD_EXIT;
    private static final Method LOG_RETURN;
    private static final Method LOG_STRING;

    static {
        try {
            LOGGER = Logger.class.getName().replace(".", "/");
            LOG_STRING = Method.getMethod(Logger.class.getMethod("log", String.class));
            LOG_METHOD_ENTRY = Method.getMethod(Logger.class.getMethod("logMethodEntry", String.class));
            LOG_METHOD_EXIT = Method.getMethod(Logger.class.getMethod("logMethodExit", String.class, String.class));
            LOG_ARGUMENT = Method.getMethod(Logger.class.getMethod("logArgument", Byte.TYPE, String.class));
            LOG_RETURN = Method.getMethod(Logger.class.getMethod("logReturn", String.class));
            LOG_FIELD = Method.getMethod(Logger.class.getMethod("logField", Object.class, String.class, String.class,
                    Logger.FieldAction.class));
            LOG_ARRAY = Method.getMethod(Logger.class.getMethod("logArray", Object.class, Integer.TYPE, String.class,
                    Logger.ArrayAction.class));
            LOG_EXCEPTION = Method.getMethod(Logger.class.getMethod("logException", Throwable.class));
            LOGGER_INITIALIZER = LoggerInitializer.class.getName().replace("" + ".", "/");
            LOGGER_INITIALIZER_INITIALIZE = Method.getMethod(LoggerInitializer.class.getMethod("initialize"));
        } catch (final NoSuchMethodException | SecurityException _ex) {
            throw new RuntimeException(_ex);
        }
    }

    private LoggingHelper() {
    }

    public static void emitConvertToString(final MethodVisitor mv, final Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString",
                        "(Ljava/lang/Object;)Ljava/lang/String;" + "", false);
                break;
            case Type.BOOLEAN:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(Z)Ljava/lang/String;", false);
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(B)Ljava/lang/String;", false);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(C)Ljava/lang/String;", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(D)Ljava/lang/String;", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(F)Ljava/lang/String;", false);
                break;
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(I)Ljava/lang/String;", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(J)Ljava/lang/String;", false);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString", "(S)Ljava/lang/String;", false);
                break;
            case Type.OBJECT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, "toString",
                        "(Ljava/lang/Object;)Ljava/lang/String;" + "", false);
                break;
            default:
                throw new RuntimeException("Unknown type" + type.getInternalName());
        }
    }

    public static void emitInsnToLoadAndInitializeLogger(final MethodVisitor mv) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER_INITIALIZER, LOGGER_INITIALIZER_INITIALIZE.getName(),
                LOGGER_INITIALIZER_INITIALIZE.getDescriptor(), false);
    }

    public static int emitLogArgument(final MethodVisitor mv, final int position, final int index,
                                      final Type argType) {
        mv.visitLdcInsn(position);

        int _typeLength = 1;
        switch (argType.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.INT:
            case Type.SHORT:
                mv.visitVarInsn(Opcodes.ILOAD, index);
                break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LLOAD, index);
                _typeLength++;
                break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FLOAD, index);
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DLOAD, index);
                _typeLength++;
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitVarInsn(Opcodes.ALOAD, index);
                break;
        }

        emitConvertToString(mv, argType);
        emitInvokeLog(mv, LOG_ARGUMENT);

        return _typeLength;
    }

    public static void emitLogArray(final MethodVisitor mv, final Logger.ArrayAction action) {
        final Type _a = Type.getType(Logger.ArrayAction.class);
        final String _name = action.toString();
        mv.visitFieldInsn(Opcodes.GETSTATIC, _a.getInternalName(), _name, _a.getDescriptor());
        emitInvokeLog(mv, LOG_ARRAY);
    }

    public static void emitLogException(final MethodVisitor mv) {
        mv.visitInsn(Opcodes.DUP);
        LoggingHelper.emitInvokeLog(mv, LoggingHelper.LOG_EXCEPTION);
    }

    public static void emitLogField(final MethodVisitor mv, final String fieldName, final Type fieldType,
                                    final Logger.FieldAction action) {
        final int _fieldSort = fieldType.getSort();
        if (_fieldSort == Type.LONG || _fieldSort == Type.DOUBLE) {
            mv.visitInsn(Opcodes.DUP2_X1);
        } else {
            mv.visitInsn(Opcodes.DUP_X1);
        }

        emitConvertToString(mv, fieldType);
        mv.visitLdcInsn(fieldName);

        final Type _a = Type.getType(Logger.FieldAction.class);
        final String _name = action.toString();
        mv.visitFieldInsn(Opcodes.GETSTATIC, _a.getInternalName(), _name, _a.getDescriptor());
        emitInvokeLog(mv, LOG_FIELD);
    }

    public static void emitLogMethodEntry(final MethodVisitor mv, final String methodId) {
        mv.visitLdcInsn(methodId);
        emitInvokeLog(mv, LOG_METHOD_ENTRY);
    }

    public static void emitLogMethodExit(final MethodVisitor mv, final String methodId, final ExitKind exitKind) {
        mv.visitLdcInsn(methodId);
        mv.visitLdcInsn(exitKind.getAbbreviatedName());
        emitInvokeLog(mv, LOG_METHOD_EXIT);
    }

    public static void emitLogReturn(final MethodVisitor mv, final Type returnType) {
        final int _retSort = returnType.getSort();
        if (_retSort != Type.VOID) {
            if (_retSort == Type.LONG || _retSort == Type.DOUBLE) {
                mv.visitInsn(Opcodes.DUP2);
            } else {
                mv.visitInsn(Opcodes.DUP);
            }
            emitConvertToString(mv, returnType);
            emitInvokeLog(mv, LOG_RETURN);
        }
    }

    public static void emitLogString(final MethodVisitor mv, final String s) {
        mv.visitLdcInsn(s);
        emitInvokeLog(mv, LOG_STRING);
    }

    public static void emitSwapOneWordAndTwoWords(final MethodVisitor mv, final Type tos) {
        if (tos.getSort() == Type.LONG || tos.getSort() == Type.DOUBLE) {
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public static void emitSwapTwoWordsAndOneWord(final MethodVisitor mv, final Type tos) {
        if (tos.getSort() == Type.LONG || tos.getSort() == Type.DOUBLE) {
            mv.visitInsn(Opcodes.DUP2_X1);
            mv.visitInsn(Opcodes.POP2);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    private static void emitInvokeLog(final MethodVisitor mv, final Method method) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, LOGGER, method.getName(), method.getDescriptor(), false);
    }

    public enum ExitKind {
        NORMAL,
        EXCEPTIONAL;

        public String getAbbreviatedName() {
            return this.name().substring(0, 1);
        }
    }
}
