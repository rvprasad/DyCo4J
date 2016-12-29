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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

final class ProgramDataCollectingClassVisitor extends ClassVisitor {
    private final ProgramData programData;
    private String name;

    ProgramDataCollectingClassVisitor(final ProgramData programData, final boolean collectFieldInfo) {
        super(CLI.ASM_VERSION);
        this.programData = programData;
    }

    private static void collectMemberInfo(final int access, final String name, final String desc, final String owner,
                                          final Map<String, String> id2Name, final Map<String, String> shortName2Id,
                                          final String prefix) {
        final String _shortName = ClassNameHelper.createShortNameDesc(name, owner, desc);
        if (!shortName2Id.containsKey(_shortName)) {
            final String _tmp = prefix + String.valueOf(shortName2Id.size());
            shortName2Id.put(_shortName, _tmp);
            final String _name = ClassNameHelper.createNameDesc(name, owner, desc, (access & Opcodes.ACC_STATIC) != 0,
                    (access & Opcodes.ACC_PRIVATE) == 0);
            id2Name.put(_tmp, _name);
        }
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        this.name = name;
        this.programData.class2superClass.put(name, superName);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        collectMemberInfo(access, name, desc, this.name, this.programData.methodId2Name,
                this.programData.shortMethodName2Id, "m");
        return new ProgramDataCollectionMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
                                   final Object value) {
        collectMemberInfo(access, name, desc, this.name, this.programData.fieldId2Name,
                this.programData.shortFieldName2Id, "f");
        return super.visitField(access, name, desc, signature, value);
    }

    private class ProgramDataCollectionMethodVisitor extends MethodVisitor {

        ProgramDataCollectionMethodVisitor(final MethodVisitor mv) {
            super(CLI.ASM_VERSION, mv);
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
            final int _access = (opcode & (Opcodes.GETSTATIC | Opcodes.PUTSTATIC)) > 0 ? Opcodes.ACC_STATIC : 0;
            final ProgramData _programData = ProgramDataCollectingClassVisitor.this.programData;
            collectMemberInfo(_access, name, desc, owner, _programData.fieldId2Name,
                    _programData.shortFieldName2Id, "f");
        }
    }
}
