/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

final class AuxiliaryDataCollectingClassVisitor extends ClassVisitor {
    private final AuxiliaryData auxiliaryData;
    private String name;

    AuxiliaryDataCollectingClassVisitor(final AuxiliaryData auxiliaryData, final boolean collectFieldInfo) {
        super(CLI.ASM_VERSION);
        this.auxiliaryData = auxiliaryData;
    }

    private static void collectMemberInfo(final int access, final String name, final String desc, final String owner,
                                          final Map<String, String> id2Name, final Map<String, String> shortName2Id,
                                          final String prefix) {
        final String _shortName = Helper.createShortNameDesc(name, owner, desc);
        if (!shortName2Id.containsKey(_shortName)) {
            final String _tmp = prefix + String.valueOf(shortName2Id.size());
            shortName2Id.put(_shortName, _tmp);
            final String _name = Helper.createNameDesc(name, owner, desc, (access & Opcodes.ACC_STATIC) != 0,
                    (access & Opcodes.ACC_PRIVATE) == 0);
            id2Name.put(_tmp, _name);
        }
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        this.name = name;
        this.auxiliaryData.class2superClass.put(name, superName);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        collectMemberInfo(access, name, desc, this.name, this.auxiliaryData.methodId2Name,
                this.auxiliaryData.shortMethodName2Id, "m");
        return new AuxiliaryDataCollectionMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
                                   final Object value) {
        collectMemberInfo(access, name, desc, this.name, this.auxiliaryData.fieldId2Name,
                this.auxiliaryData.shortFieldName2Id, "f");
        return super.visitField(access, name, desc, signature, value);
    }

    private class AuxiliaryDataCollectionMethodVisitor extends MethodVisitor {

        AuxiliaryDataCollectionMethodVisitor(final MethodVisitor mv) {
            super(CLI.ASM_VERSION, mv);
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
            final int _access = (opcode & (Opcodes.GETSTATIC | Opcodes.PUTSTATIC)) > 0 ? Opcodes.ACC_STATIC : 0;
            final AuxiliaryData _auxiliaryData = AuxiliaryDataCollectingClassVisitor.this.auxiliaryData;
            collectMemberInfo(_access, name, desc, owner, _auxiliaryData.fieldId2Name,
                    _auxiliaryData.shortFieldName2Id, "f");
        }
    }
}
