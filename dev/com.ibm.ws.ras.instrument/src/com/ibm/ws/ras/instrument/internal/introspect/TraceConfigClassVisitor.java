/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.introspect;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.FieldInfo;
import com.ibm.ws.ras.instrument.internal.model.MethodInfo;

/**
 * A read-only class visitor that is intended to forward to a
 * <code>ClassWriter</code>. This visitor will build a runtime model of the
 * ras configuration for the class that's being visited.
 * <p>
 * The intent of the forward calls is to allow for a value-copy of the class
 * bytes as we process the data. The resulting byte stream from the writer
 * can then be used by another class adapter to modify the class.
 * </p>
 */
public class TraceConfigClassVisitor extends ClassVisitor {

    protected final static Type SENSITIVE_TYPE = Type.getType(com.ibm.websphere.ras.annotation.Sensitive.class);
    protected final static Type TRACE_OPTIONS_TYPE = Type.getType(com.ibm.websphere.ras.annotation.TraceOptions.class);
    protected final static Type TRIVIAL_TYPE = Type.getType(com.ibm.websphere.ras.annotation.Trivial.class);;
    protected final static Type FFDC_IGNORE_TYPE = Type.getType(com.ibm.ws.ffdc.annotation.FFDCIgnore.class);
    protected final static Type TRACE_OBJECT_FIELD_TYPE = Type.getType(com.ibm.websphere.ras.annotation.TraceObjectField.class);

    protected ClassInfo classInfo;
    protected TraceOptionsAnnotationVisitor traceOptionsAnnotationVisitor;
    protected TraceObjectFieldAnnotationVisitor traceObjectFieldAnnotationVisitor;

    public TraceConfigClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM8, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        classInfo = new ClassInfo(name);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (TRACE_OPTIONS_TYPE.getDescriptor().equals(desc)) {
            traceOptionsAnnotationVisitor = new TraceOptionsAnnotationVisitor(av);
            av = traceOptionsAnnotationVisitor;
        } else if (TRIVIAL_TYPE.getDescriptor().equals(desc)) {
            classInfo.setTrivial(true);
        } else if (SENSITIVE_TYPE.getDescriptor().equals(desc)) {
            classInfo.setSensitive(true);
        } else if (TRACE_OBJECT_FIELD_TYPE.getDescriptor().equals(desc)) {
            traceObjectFieldAnnotationVisitor = new TraceObjectFieldAnnotationVisitor(av);
            av = traceObjectFieldAnnotationVisitor;
        }
        return av;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        if ((access & Opcodes.ACC_STATIC) != 0) {
            FieldInfo fieldInfo = new FieldInfo(name, desc);
            classInfo.addFieldInfo(fieldInfo);
        }
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        MethodInfo methodInfo = new MethodInfo(name, desc);
        classInfo.addMethodInfo(methodInfo);
        return new MethodInfoMethodVisitor(mv, methodInfo);
    }

    private final static class MethodInfoMethodVisitor extends MethodVisitor {
        private final MethodInfo methodInfo;

        private MethodInfoMethodVisitor(MethodVisitor mv, MethodInfo methodInfo) {
            super(Opcodes.ASM8, mv);
            this.methodInfo = methodInfo;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(desc, visible);
            if (FFDC_IGNORE_TYPE.getDescriptor().equals(desc)) {
                av = new FFDCIgnoreAnnotationVisitor(av, methodInfo);
            } else if (SENSITIVE_TYPE.getDescriptor().equals(desc)) {
                methodInfo.setResultSensitive(true);
            } else if (TRIVIAL_TYPE.getDescriptor().equals(desc)) {
                methodInfo.setTrivial(true);
            }
            return av;
        }

        private static final class FFDCIgnoreAnnotationVisitor extends AnnotationVisitor {
            private final MethodInfo methodInfo;

            private FFDCIgnoreAnnotationVisitor(AnnotationVisitor av, MethodInfo methodInfo) {
                super(Opcodes.ASM8, av);
                this.methodInfo = methodInfo;
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor av = super.visitArray(name);
                if (name.equals("value")) {
                    av = new FFDCIgnoreValueArrayVisitor(av, methodInfo);
                }
                return av;
            }
        }

        private final static class FFDCIgnoreValueArrayVisitor extends AnnotationVisitor {
            private final MethodInfo methodInfo;

            private FFDCIgnoreValueArrayVisitor(AnnotationVisitor av, MethodInfo methodInfo) {
                super(Opcodes.ASM8, av);
                this.methodInfo = methodInfo;
            }

            @Override
            public void visit(String name, Object value) {
                if (value instanceof Type) {
                    methodInfo.addFFDCIgnoreException(Type.class.cast(value));
                }
                super.visit(name, value);
            }
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
            if (SENSITIVE_TYPE.getDescriptor().equals(desc)) {
                methodInfo.setArgIsSensitive(parameter, true);
            }
            return super.visitParameterAnnotation(parameter, desc, visible);
        }
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        if (traceOptionsAnnotationVisitor != null) {
            classInfo.setTraceOptionsData(traceOptionsAnnotationVisitor.getTraceOptionsData());
        }
        if (traceObjectFieldAnnotationVisitor != null) {
            FieldInfo fi = classInfo.getDeclaredLoggerField();
            if (fi == null) {
                fi = classInfo.getDeclaredFieldByName(traceObjectFieldAnnotationVisitor.getFieldName());
            }
            if (fi != null) {
                fi.setLoggerField(true);
            }
        }
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
