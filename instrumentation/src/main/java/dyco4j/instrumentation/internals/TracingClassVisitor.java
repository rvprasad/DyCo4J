/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */

package dyco4j.instrumentation.internals;

import dyco4j.utility.ClassNameHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

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
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
                                     final String[] exceptions) {
        final MethodVisitor _mv1 = super.visitMethod(access, name, desc, signature, exceptions);
        if (_mv1 != null && shouldInstrumentMethod(name)) {
            final boolean _isInit = name.equals("<init>");
            final TracingMethodVisitor _mv2 = new TracingMethodVisitor(access, name, desc, _mv1, this, !_isInit);
            return _isInit ? new InitTracingMethodVisitor(name, _mv2) : _mv2;
        } else
            return _mv1;
    }

    String getFieldId(final String name, final String owner, final String desc) {
        assert cmdLineOptions.traceFieldAccess().isPresent() :
                "Should be invoked only when traceFieldAccess is true";
        final String _shortName = ClassNameHelper.createShortNameDesc(name, Optional.of(owner), desc);
        final String _id = shortFieldName2Id.get(_shortName);
        if (_id == null) {
            final String _superClass = class2superClass.get(owner);
            if (_superClass == null) {
                final String _msg = MessageFormat.format("Incomplete information: name={0}, owner={1}, desc={2} " +
                        "_shortName={3}", name, owner, desc, _shortName);
                throw new IllegalStateException(_msg);
            } else
                return getFieldId(name, _superClass, desc);
        }
        return _id;
    }

    String getMethodId(final String name, final String desc) {
        assert shouldInstrumentMethod(name) : "Should be invoked only when the method matches methodNameRegex";
        return getMethodId(name, className, desc);
    }

    String getMethodId(final String name, final String owner, final String desc) {
        final String _shortName = ClassNameHelper.createShortNameDesc(name, Optional.of(owner), desc);
        final String _id = shortMethodName2Id.get(_shortName);
        if (_id == null) {
            final String _superClass = class2superClass.get(owner);
            if (_superClass == null)
                throw new IllegalStateException("Could not find methodId for " + _shortName);
            else
                return getMethodId(name, _superClass, desc);
        }
        return _id;
    }

    private boolean shouldInstrumentMethod(final String name) {
        return ClassNameHelper.createJavaName(name, className).matches(methodNameRegex);
    }
}
