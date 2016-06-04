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

final class MemberDataCollectingClassVisitor extends ClassVisitor {
    private final Map<String, String> shortFieldName2Id;
    private final Map<String, String> fieldId2name;
    private final Map<String, String> methodId2name;
    private final Map<String, String> shortMethodName2Id;
    private final Map<String, String> class2superClass;
    private String name;

    MemberDataCollectingClassVisitor(final Map<String, String> fieldId2name,
                                     final Map<String, String> shortFieldName2Id,
                                     final Map<String, String> methodId2name,
                                     final Map<String, String> shortMethodName2Id,
                                     final Map<String, String> class2superClass, final boolean collectFieldInfo) {
        super(CLI.ASM_VERSION);
        this.fieldId2name = fieldId2name;
        this.shortFieldName2Id = shortFieldName2Id;
        this.methodId2name = methodId2name;
        this.shortMethodName2Id = shortMethodName2Id;
        this.class2superClass = class2superClass;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        this.name = name;
        this.class2superClass.put(name, superName);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        final Map<String, String> _id2Name = this.methodId2name;
        final Map<String, String> _shortName2Id = this.shortMethodName2Id;
        collectMemberInfo(access, name, desc, _id2Name, _shortName2Id, "m");
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
                                   final Object value) {
        final Map<String, String> _id2Name = this.fieldId2name;
        final Map<String, String> _shortName2Id = this.shortFieldName2Id;
        collectMemberInfo(access, name, desc, _id2Name, _shortName2Id, "f");
        return super.visitField(access, name, desc, signature, value);
    }

    private void collectMemberInfo(final int access, final String name, final String desc,
                                   final Map<String, String> id2Name, final Map<String, String> shortName2Id,
                                   final String prefix) {
        final String _shortName = Helper.createShortNameDesc(name, this.name, desc);
        final String _name = Helper.createNameDesc(name, this.name, desc, (access & Opcodes.ACC_STATIC) != 0,
                                                   (access & Opcodes.ACC_PRIVATE) == 0);
        if (!shortName2Id.containsKey(_shortName)) {
            final String _tmp = prefix + String.valueOf(shortName2Id.size());
            shortName2Id.put(_shortName, _tmp);
            id2Name.put(_tmp, _name);
        }
    }
}
