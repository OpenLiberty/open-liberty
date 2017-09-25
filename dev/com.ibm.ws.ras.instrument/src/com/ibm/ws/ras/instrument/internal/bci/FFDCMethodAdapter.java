/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.bci;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.introspect.FFDCIgnoreAnnotationVisitor;

public class FFDCMethodAdapter extends AbstractRasMethodAdapter<FFDCClassAdapter> {

    public final static Type FFDC_FILTER_TYPE = Type.getObjectType("com/ibm/ws/ffdc/FFDCFilter");

    private final String descriptor;

    private Set<Type> ignoredExceptionTypes = null;

    private Set<Type> visitedIgnoredExceptionTypes = null;

    private String unresolvedCompilationErrors;

    FFDCIgnoreAnnotationVisitor ignoreAnnotationVisitor;

    public FFDCMethodAdapter(FFDCClassAdapter classAdapter, MethodVisitor visitor, int access, String methodName, String descriptor, String signature, String[] exceptions) {
        super(classAdapter, false, visitor, access, methodName, descriptor, signature, exceptions);
        this.descriptor = descriptor;
        if (getMethodInfo() != null) {
            ignoredExceptionTypes = getMethodInfo().getFFDCIgnoreExceptions();
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (FFDCIgnoreAnnotationVisitor.FFDC_IGNORE_TYPE.getDescriptor().equals(desc)) {
            ignoreAnnotationVisitor = new FFDCIgnoreAnnotationVisitor(av);
            av = ignoreAnnotationVisitor;
        }
        return av;
    }

    @Override
    protected boolean isMethodInstrumentedByThisAdapter() {
        return super.isMethodInstrumentedByThisAdapter() ||
               getClassAdapter().isClassInstrumentedByThisAdapter();
    }

    @Override
    public void initializeTraceObjectField() {
        // No trace object field to initialize
    }

    @Override
    public boolean onMethodEntry() {
        // Nothing to do for method entry
        return false;
    }

    @Override
    public boolean onMethodReturn() {
        // Nothing to do for method exit
        return false;
    }

    @Override
    public void visitLdcInsn(Object constant) {
        if (constant instanceof String) {
            String string = (String) constant;
            if (string.startsWith("Unresolved compilation problem")) {
                unresolvedCompilationErrors = string;
            }
        }
        super.visitLdcInsn(constant);
    }

    private Set<Type> getIgnoredExceptionTypes() {
        if (ignoredExceptionTypes != null) {
            return ignoredExceptionTypes;
        }
        if (ignoreAnnotationVisitor != null) {
            return ignoreAnnotationVisitor.getIgnoredExceptionTypes();
        }
        return Collections.emptySet();
    }

    private boolean isExceptionTypeIgnored(Type exceptionType) {
        return getIgnoredExceptionTypes().contains(exceptionType);
    }

    @Override
    public boolean onExceptionHandlerEntry(Type exceptionType, int var) {
        if (isExceptionTypeIgnored(exceptionType)) {
            if (visitedIgnoredExceptionTypes == null) {
                visitedIgnoredExceptionTypes = new LinkedHashSet<Type>();
            }
            visitedIgnoredExceptionTypes.add(exceptionType);
        } else if (!isMethodInstrumentedByThisAdapter()) {
            insertFFDC(var);
            return true;
        }
        return false;
    }

    @Override
    public boolean onThrowInstruction() {
        if (getClassAdapter().isCallFFDCOnThrow()) {
            insertFFDC(-1);
            return true;
        }
        return false;
    }

    private void insertFFDC(int var) {
        if (var == -1) {
            // Duplicate the top of stack so we have an instance to consume
            visitInsn(DUP);
        } else {
            visitVarInsn(ALOAD, var);
        }

        // Setup the parameter list for the call to processException
        visitLoadClassName();
        visitLdcInsn(Integer.toString(getLineNumber()));
        if (!isStatic()) {
            visitVarInsn(ALOAD, 0);
        } else {
            visitInsn(ACONST_NULL);
        }
        createTraceArrayForParameters();
        visitMethodInsn(INVOKESTATIC,
                        FFDC_FILTER_TYPE.getInternalName(),
                        "processException",
                        Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(Throwable.class),
                                                                             Type.getType(String.class),
                                                                             Type.getType(String.class),
                                                                             Type.getType(Object.class),
                                                                             Type.getType(Object[].class) }), false);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        // Ensure that a catch clause exists for each FFDCIgnore'd type.
        Set<Type> expected = getIgnoredExceptionTypes();
        if (expected != null) {
            Set<Type> actual = this.visitedIgnoredExceptionTypes;
            if (actual == null) {
                actual = Collections.emptySet();
            }

            Set<Type> unnecessary = new LinkedHashSet<Type>(expected);
            unnecessary.removeAll(actual);

            if (!unnecessary.isEmpty()) {
                // When a compilation problem occurs, Eclipse still writes a
                // .class file, but the method contains:
                //    throw new Error("Unresolved compilation problem...")
                // It is confusing to give an error about unnecessary
                // @FFDCIgnore in this case, so use Eclipse's compilation error
                // message instead.
                if (unresolvedCompilationErrors != null) {
                    throw new InstrumentationException("[Eclipse] " + unresolvedCompilationErrors +
                                                       "If you haven't changed this class, consider cleaning and rebuilding.\n" +
                                                       "If that doesn't help, investigate why Eclipse fails to compile the class.");
                }

                throw new InstrumentationException("unnecessary @FFDCIgnore specified on " + getMethodName() + descriptor + " for " + unnecessary);
            }
        }
    }
}
