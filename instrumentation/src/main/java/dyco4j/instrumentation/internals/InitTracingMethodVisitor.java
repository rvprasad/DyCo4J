/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This class is an adaptation of <code></code>org.objectweb.asm.commons.AdviceAdapter</code> class in
 * <a href="http://asm.ow2.org/">ASM</a>.
 */
final class InitTracingMethodVisitor extends MethodVisitor {
    private static final Object THIS = new Object();
    private static final Object OTHER = new Object();
    private final Map<Label, Stack<Object>> branchTarget2frame = new HashMap<>();
    private boolean thisIsInitialized;
    private Stack<Object> stackFrame = new Stack<>();

    InitTracingMethodVisitor(final String name, final TracingMethodVisitor mv) {
        super(CLI.ASM_VERSION, mv);
        assert name.equals("<init>");

        thisIsInitialized = false;
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        final Stack<Object> _frame = branchTarget2frame.get(label);
        if (_frame != null) {
            stackFrame = _frame;
            branchTarget2frame.remove(label);
        }
    }

    @Override
    public void visitInsn(final int opcode) {
        super.visitInsn(opcode);
        switch (opcode) {
            case Opcodes.IRETURN: // 1 before n/a after
            case Opcodes.FRETURN: // 1 before n/a after
            case Opcodes.ARETURN: // 1 before n/a after
            case Opcodes.ATHROW: // 1 before n/a after
                stackFrame.pop();
                break;
            case Opcodes.LRETURN: // 2 before n/a after
            case Opcodes.DRETURN: // 2 before n/a after
                stackFrame.pop();
                stackFrame.pop();
                break;
            case Opcodes.NOP:
            case Opcodes.LALOAD: // remove 2 add 2
            case Opcodes.DALOAD: // remove 2 add 2
            case Opcodes.LNEG:
            case Opcodes.DNEG:
            case Opcodes.FNEG:
            case Opcodes.INEG:
            case Opcodes.L2D:
            case Opcodes.D2L:
            case Opcodes.F2I:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
            case Opcodes.I2F:
            case Opcodes.ARRAYLENGTH:
                break;
            case Opcodes.ACONST_NULL:
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
            case Opcodes.F2L: // 1 before 2 after
            case Opcodes.F2D:
            case Opcodes.I2L:
            case Opcodes.I2D:
                stackFrame.push(OTHER);
                break;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                stackFrame.push(OTHER);
                stackFrame.push(OTHER);
                break;
            case Opcodes.IALOAD: // remove 2 add 1
            case Opcodes.FALOAD: // remove 2 add 1
            case Opcodes.AALOAD: // remove 2 add 1
            case Opcodes.BALOAD: // remove 2 add 1
            case Opcodes.CALOAD: // remove 2 add 1
            case Opcodes.SALOAD: // remove 2 add 1
            case Opcodes.POP:
            case Opcodes.IADD:
            case Opcodes.FADD:
            case Opcodes.ISUB:
            case Opcodes.LSHL: // 3 before 2 after
            case Opcodes.LSHR: // 3 before 2 after
            case Opcodes.LUSHR: // 3 before 2 after
            case Opcodes.L2I: // 2 before 1 after
            case Opcodes.L2F: // 2 before 1 after
            case Opcodes.D2I: // 2 before 1 after
            case Opcodes.D2F: // 2 before 1 after
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:
            case Opcodes.FCMPL: // 2 before 1 after
            case Opcodes.FCMPG: // 2 before 1 after
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                stackFrame.pop();
                break;
            case Opcodes.POP2:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LADD:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.DADD:
            case Opcodes.DMUL:
            case Opcodes.DSUB:
            case Opcodes.DDIV:
            case Opcodes.DREM:
                stackFrame.pop();
                stackFrame.pop();
                break;
            case Opcodes.IASTORE:
            case Opcodes.FASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.LCMP: // 4 before 1 after
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                stackFrame.pop();
                stackFrame.pop();
                stackFrame.pop();
                break;
            case Opcodes.LASTORE:
            case Opcodes.DASTORE:
                stackFrame.pop();
                stackFrame.pop();
                stackFrame.pop();
                stackFrame.pop();
                break;
            case Opcodes.DUP:
                stackFrame.push(stackFrame.peek());
                break;
            case Opcodes.DUP_X1: {
                final int _s = stackFrame.size();
                stackFrame.add(_s - 2, stackFrame.get(_s - 1));
                break;
            }
            case Opcodes.DUP_X2: {
                final int _s = stackFrame.size();
                stackFrame.add(_s - 3, stackFrame.get(_s - 1));
                break;
            }
            case Opcodes.DUP2: {
                final int _s = stackFrame.size();
                stackFrame.add(_s - 2, stackFrame.get(_s - 1));
                stackFrame.add(_s - 2, stackFrame.get(_s - 1));
                break;
            }
            case Opcodes.DUP2_X1: {
                final int _s = stackFrame.size();
                stackFrame.add(_s - 3, stackFrame.get(_s - 1));
                stackFrame.add(_s - 3, stackFrame.get(_s - 1));
                break;
            }
            case Opcodes.DUP2_X2: {
                final int _s = stackFrame.size();
                stackFrame.add(_s - 4, stackFrame.get(_s - 1));
                stackFrame.add(_s - 4, stackFrame.get(_s - 1));
                break;
            }
            case Opcodes.SWAP: {
                final int _s = stackFrame.size();
                stackFrame.add(_s - 2, stackFrame.get(_s - 1));
                stackFrame.remove(_s);
                break;
            }
        }
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode, var);
        switch (opcode) {
            case Opcodes.ILOAD, Opcodes.FLOAD:
                stackFrame.push(OTHER);
                break;
            case Opcodes.LLOAD, Opcodes.DLOAD:
                stackFrame.push(OTHER);
                stackFrame.push(OTHER);
                break;
            case Opcodes.ALOAD:
                stackFrame.push(var == 0 ? THIS : OTHER);
                break;
            case Opcodes.ASTORE, Opcodes.ISTORE, Opcodes.FSTORE:
                stackFrame.pop();
                break;
            case Opcodes.LSTORE, Opcodes.DSTORE:
                stackFrame.pop();
                stackFrame.pop();
                break;
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        mv.visitFieldInsn(opcode, owner, name, desc);
        final char _c = desc.charAt(0);
        final boolean _longOrDouble = _c == 'J' || _c == 'D';
        switch (opcode) {
            case Opcodes.GETSTATIC: // add 1 or 2
                stackFrame.push(OTHER);
                if (_longOrDouble)
                    stackFrame.push(OTHER);
                break;
            case Opcodes.PUTSTATIC: // remove 1 or 2
                stackFrame.pop();
                if (_longOrDouble)
                    stackFrame.pop();
                break;
            case Opcodes.PUTFIELD: // remove 2 or 3
                stackFrame.pop();
                stackFrame.pop();
                if (_longOrDouble)
                    stackFrame.pop();
                break;
            case Opcodes.GETFIELD: // remove 1 add 1 or 2
                if (_longOrDouble)
                    stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        mv.visitIntInsn(opcode, operand);
        if (opcode != Opcodes.NEWARRAY)
            stackFrame.push(OTHER);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        mv.visitLdcInsn(cst);
        stackFrame.push(OTHER);
        if (cst instanceof Double || cst instanceof Long)
            stackFrame.push(OTHER);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        mv.visitMultiANewArrayInsn(desc, dims);
        for (int _i = 0; _i < dims; _i++)
            stackFrame.pop();
        stackFrame.push(OTHER);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        stackFrame.pop();
        addBranch(dflt);
        for (final Label l : labels)
            addBranch(l);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        mv.visitTypeInsn(opcode, type);
        // ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack
        if (opcode == Opcodes.NEW) {
            stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                final boolean itf) {
        for (final Type _type : Type.getArgumentTypes(desc)) {
            stackFrame.pop();
            if (_type.getSize() == 2)
                stackFrame.pop();
        }

        boolean _flag = false;
        switch (opcode) {
            case Opcodes.INVOKESTATIC:
                break;
            case Opcodes.INVOKEINTERFACE, Opcodes.INVOKEVIRTUAL:
                stackFrame.pop(); // objectref
                break;
            case Opcodes.INVOKESPECIAL:
                _flag = stackFrame.pop() == THIS && !thisIsInitialized;  // objectref
                break;
        }

        if (_flag) {
            thisIsInitialized = true;
            ((TracingMethodVisitor) mv).setThisInitialized();
            ((TracingMethodVisitor) mv).endOutermostExceptionHandler();
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);

        if (_flag) {
            ((TracingMethodVisitor) mv).beginOutermostExceptionHandler();
        }

        final Type _returnType = Type.getReturnType(desc);
        if (_returnType != Type.VOID_TYPE) {
            stackFrame.push(OTHER);
            if (_returnType.getSize() == 2)
                stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        mv.visitJumpInsn(opcode, label);
        switch (opcode) {
            case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE, Opcodes.IFNULL,
                    Opcodes.IFNONNULL:
                stackFrame.pop();
                break;
            case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT,
                    Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE:
                stackFrame.pop();
                stackFrame.pop();
                break;
            case Opcodes.JSR:
                stackFrame.push(OTHER);
                break;
        }
        addBranch(label);
    }

    @Override
    public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,
                                       final Object... bsmArgs) {
        final Type[] _types = Type.getArgumentTypes(desc);
        for (final Type _type : _types) {
            stackFrame.pop();
            if (_type.getSize() == 2)
                stackFrame.pop();
        }

        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);

        final Type _returnType = Type.getReturnType(desc);
        if (_returnType != Type.VOID_TYPE) {
            stackFrame.push(OTHER);
            if (_returnType.getSize() == 2)
                stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        if (!branchTarget2frame.containsKey(handler)) {
            final Stack<Object> _frame = new Stack<>();
            _frame.push(OTHER);
            branchTarget2frame.put(handler, _frame);
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        stackFrame.pop();
        addBranch(dflt);
        for (final Label l : labels)
            addBranch(l);
    }

    private void addBranch(final Label label) {
        if (!branchTarget2frame.containsKey(label)) {
            final Stack<Object> _frame = new Stack<>();
            _frame.addAll(stackFrame);
            branchTarget2frame.put(label, _frame);
        }
    }
}
