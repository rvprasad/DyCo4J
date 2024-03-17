/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import dyco4j.utility.ClassNameHelper;
import dyco4j.utility.ProgramData;
import org.objectweb.asm.*;

import java.util.Optional;

final class ProgramDataCollectingClassVisitor extends ClassVisitor {
    private final ProgramData programData;
    private String name;

    ProgramDataCollectingClassVisitor(final ProgramData programData) {
        super(CLI.ASM_VERSION);
        this.programData = programData;
    }

    private static void collectMemberInfo(final Optional<Integer> access, final String name, final String desc,
                                          final Optional<String> owner, final String prefix,
                                          final NameAdder adder) {
        final String _shortName = ClassNameHelper.createShortNameDesc(name, owner, desc);
        final Optional<Boolean> _isStatic = access.map(v -> (v & Opcodes.ACC_STATIC) != 0);
        final Optional<Boolean> _isPublished = access.map(v -> (v & Opcodes.ACC_PRIVATE) == 0);
        final String _name = ClassNameHelper.createNameDesc(name, owner, desc, _isStatic, _isPublished);
        adder.add(_shortName, _name, prefix);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        this.name = name;
        this.programData.addClass2SuperClassMapping(name, superName);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        collectMemberInfo(Optional.of(access), name, desc, Optional.of(this.name), "m",
                programData::addNewMethod);
        return new ProgramDataCollectionMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
                                   final Object value) {
        collectMemberInfo(Optional.of(access), name, desc, Optional.of(this.name), "f", programData::addNewField);
        return super.visitField(access, name, desc, signature, value);
    }

    @FunctionalInterface
    private interface NameAdder {
        Optional<String> add(String shortName, String name, String prefix);
    }

    private class ProgramDataCollectionMethodVisitor extends MethodVisitor {

        ProgramDataCollectionMethodVisitor(final MethodVisitor mv) {
            super(CLI.ASM_VERSION, mv);
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
            /*
             * ASSUMPTION
             *
             * All fields of a class will be visited before visiting the methods of the class.  This guarantees
             * private fields be correctly identified as "unpublished" since their declarations are processed before
             * the instructions that use them.
             */
            final int _access = (opcode & (Opcodes.GETSTATIC | Opcodes.PUTSTATIC)) > 0 ? Opcodes.ACC_STATIC : 0;
            collectMemberInfo(Optional.of(_access), name, desc, Optional.of(owner), "f",
                    ProgramDataCollectingClassVisitor.this.programData::addNewField);
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                    final boolean itf) {
            final int _access = (opcode & (Opcodes.GETSTATIC | Opcodes.PUTSTATIC)) > 0 ? Opcodes.ACC_STATIC : 0;
            collectMemberInfo(Optional.of(_access), name, desc, Optional.of(owner), "m",
                    ProgramDataCollectingClassVisitor.this.programData::addNewMethod);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        @Override
        public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm,
                                           final Object... bsmArgs) {
            collectMemberInfo(Optional.empty(), name, desc, Optional.empty(), "m",
                    ProgramDataCollectingClassVisitor.this.programData::addNewMethod);
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
    }
}
