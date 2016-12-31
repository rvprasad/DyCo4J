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
    private boolean superInitialized;
    private Stack<Object> stackFrame = new Stack<>();

    InitTracingMethodVisitor(final int access, final String name, final TracingMethodVisitor mv) {
        super(CLI.ASM_VERSION, mv);
        assert name.equals("<init>");

        this.superInitialized = false;
    }

    @Override
    public void visitCode() {
        final TracingMethodVisitor _mv = (TracingMethodVisitor) this.mv;
        _mv.emitLogMethodEntry();
        _mv.beginOutermostHandlerRegion();
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        final Stack<Object> _frame = this.branchTarget2frame.get(label);
        if (_frame != null) {
            stackFrame = _frame;
            this.branchTarget2frame.remove(label);
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
                this.stackFrame.pop();
                break;
            case Opcodes.LRETURN: // 2 before n/a after
            case Opcodes.DRETURN: // 2 before n/a after
                this.stackFrame.pop();
                this.stackFrame.pop();
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
                this.stackFrame.push(OTHER);
                break;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                this.stackFrame.push(OTHER);
                this.stackFrame.push(OTHER);
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
                this.stackFrame.pop();
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
                this.stackFrame.pop();
                this.stackFrame.pop();
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
                this.stackFrame.pop();
                this.stackFrame.pop();
                this.stackFrame.pop();
                break;
            case Opcodes.LASTORE:
            case Opcodes.DASTORE:
                this.stackFrame.pop();
                this.stackFrame.pop();
                this.stackFrame.pop();
                this.stackFrame.pop();
                break;
            case Opcodes.DUP:
                this.stackFrame.push(this.stackFrame.peek());
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
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
                this.stackFrame.push(OTHER);
                break;
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
                this.stackFrame.push(OTHER);
                this.stackFrame.push(OTHER);
                break;
            case Opcodes.ALOAD:
                this.stackFrame.push(var == 0 ? THIS : OTHER);
                break;
            case Opcodes.ASTORE:
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
                this.stackFrame.pop();
                break;
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                this.stackFrame.pop();
                this.stackFrame.pop();
                break;
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
                               final String name, final String desc) {
        this.mv.visitFieldInsn(opcode, owner, name, desc);
        final char _c = desc.charAt(0);
        final boolean _longOrDouble = _c == 'J' || _c == 'D';
        switch (opcode) {
            case Opcodes.GETSTATIC: // add 1 or 2
                this.stackFrame.push(OTHER);
                if (_longOrDouble)
                    this.stackFrame.push(OTHER);
                break;
            case Opcodes.PUTSTATIC: // remove 1 or 2
                this.stackFrame.pop();
                if (_longOrDouble)
                    this.stackFrame.pop();
                break;
            case Opcodes.PUTFIELD: // remove 2 or 3
                this.stackFrame.pop();
                this.stackFrame.pop();
                if (_longOrDouble)
                    this.stackFrame.pop();
                break;
            case Opcodes.GETFIELD: // remove 1 add 1 or 2
                if (_longOrDouble)
                    this.stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        this.mv.visitIntInsn(opcode, operand);
        if (opcode != Opcodes.NEWARRAY)
            this.stackFrame.push(OTHER);
    }

    @Override
    public void visitLdcInsn(final Object cst) {
        this.mv.visitLdcInsn(cst);
        this.stackFrame.push(OTHER);
        if (cst instanceof Double || cst instanceof Long)
            this.stackFrame.push(OTHER);
    }

    @Override
    public void visitMultiANewArrayInsn(final String desc, final int dims) {
        this.mv.visitMultiANewArrayInsn(desc, dims);
        for (int _i = 0; _i < dims; _i++)
            this.stackFrame.pop();
        this.stackFrame.push(OTHER);
    }

    @Override
    public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        this.stackFrame.pop();
        this.addBranch(dflt);
        for (final Label l : labels)
            this.addBranch(l);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        this.mv.visitTypeInsn(opcode, type);
        // ANEWARRAY, CHECKCAST or INSTANCEOF don't change stack
        if (opcode == Opcodes.NEW) {
            this.stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                final boolean itf) {
        processVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        mv.visitJumpInsn(opcode, label);
        switch (opcode) {
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
                this.stackFrame.pop();
                break;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
                this.stackFrame.pop();
                this.stackFrame.pop();
                break;
            case Opcodes.JSR:
                this.stackFrame.push(OTHER);
                break;
        }
        addBranch(label);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
                                       Object... bsmArgs) {
        this.mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        final Type[] _types = Type.getArgumentTypes(desc);
        for (final Type _type : _types) {
            this.stackFrame.pop();
            if (_type.getSize() == 2)
                this.stackFrame.pop();
        }

        final Type _returnType = Type.getReturnType(desc);
        if (_returnType != Type.VOID_TYPE) {
            this.stackFrame.push(OTHER);
            if (_returnType.getSize() == 2)
                this.stackFrame.push(OTHER);
        }
    }

    @Override
    public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        if (!this.branchTarget2frame.containsKey(handler)) {
            final Stack<Object> _frame = new Stack<>();
            _frame.push(OTHER);
            this.branchTarget2frame.put(handler, _frame);
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        this.stackFrame.pop();
        this.addBranch(dflt);
        for (final Label l : labels)
            this.addBranch(l);
    }

    private void processVisitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                        final boolean itf) {
        for (final Type _type : Type.getArgumentTypes(desc)) {
            this.stackFrame.pop();
            if (_type.getSize() == 2)
                this.stackFrame.pop();
        }
        boolean _flag = false;
        switch (opcode) {
            case Opcodes.INVOKESTATIC:
                break;
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKEVIRTUAL:
                this.stackFrame.pop(); // objectref
                break;
            case Opcodes.INVOKESPECIAL:
                _flag = this.stackFrame.pop() == THIS && !superInitialized;  // objectref
                break;
        }

        if (_flag) {
            this.superInitialized = true;
            final TracingMethodVisitor _mv = (TracingMethodVisitor) this.mv;
            _mv.endOutermostHandlerRegion();
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            _mv.beginOutermostHandlerRegion();
            _mv.emitLogMethodArguments();
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }


        final Type _returnType = Type.getReturnType(desc);
        if (_returnType != Type.VOID_TYPE) {
            this.stackFrame.push(OTHER);
            if (_returnType.getSize() == 2)
                this.stackFrame.push(OTHER);
        }
    }

    private void addBranch(final Label label) {
        if (!this.branchTarget2frame.containsKey(label)) {
            final Stack<Object> _frame = new Stack<>();
            _frame.addAll(this.stackFrame);
            this.branchTarget2frame.put(label, _frame);
        }
    }
}
