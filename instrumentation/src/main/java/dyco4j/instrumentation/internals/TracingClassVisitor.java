/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Map;

final class TracingClassVisitor extends ClassVisitor {
    final CLI.CommandLineOptions cmdLineOptions;
    private final Map<String, String> shortMethodName2Id;
    private final Map<String, String> shortFieldName2Id;
    private final Map<String, String> class2superClass;
    private final String methodNameRegex;
    private String className;

    TracingClassVisitor(final ClassVisitor cv, final Map<String, String> shortFieldName2Id,
                        final Map<String, String> shortMethodName2Id, final Map<String, String> class2superClass,
                        final String methodNameRegex, final CLI.CommandLineOptions clo) {
        super(CLI.ASM_VERSION, cv);
        this.shortFieldName2Id = shortFieldName2Id;
        this.shortMethodName2Id = shortMethodName2Id;
        this.class2superClass = class2superClass;
        this.methodNameRegex = methodNameRegex;
        this.cmdLineOptions = clo;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        // force the class version to be 52 (Java 8 compliant)
        this.cv.visit(52, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        final MethodVisitor _mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (_mv != null && shouldInstrumentMethod(name))
            return new TracingMethodVisitor(access, name, desc, signature, exceptions, _mv, this);
        else
            return _mv;
    }

    String getFieldId(final String name, final String owner, final String desc) {
        assert this.cmdLineOptions.traceFieldAccess : "Should be invoked only when traceFieldAccess is true";
        final String _shortName = Helper.createShortNameDesc(name, owner, desc);
        final String _id = this.shortFieldName2Id.get(_shortName);
        if (_id == null) {
            final String _superClass = this.class2superClass.get(owner);
            if (_superClass == null)
                throw new IllegalStateException(_shortName);
            else
                return getFieldId(name, _superClass, desc);
        }
        return _id;
    }

    String getMethodId(final String name, final String desc) {
        return getMethodId(name, this.className, desc);
    }

    private String getMethodId(final String name, final String owner, final String desc) {
        assert shouldInstrumentMethod(name) : "Should be invoked only when the method matches methodNameRegex";
        final String _shortName = Helper.createShortNameDesc(name, owner, desc);
        final String _id = this.shortMethodName2Id.get(_shortName);
        if (_id == null) {
            final String _superClass = this.class2superClass.get(owner);
            if (_superClass == null)
                throw new IllegalStateException(_shortName);
            else
                return getMethodId(name, _superClass, desc);
        }
        return _id;
    }

    private boolean shouldInstrumentMethod(final String name) {
        return Helper.createJavaName(name, this.className).matches(this.methodNameRegex);
    }
}
