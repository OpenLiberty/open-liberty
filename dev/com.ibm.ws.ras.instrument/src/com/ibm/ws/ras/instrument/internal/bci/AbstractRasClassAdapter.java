/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.bci;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.ibm.ws.ras.instrument.internal.introspect.TraceObjectFieldAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.introspect.TraceOptionsAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.ClassTraceInfo;
import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

/**
 * ASM <code>ClassAdapter</code> implementation that will help inject trace
 * into a class.
 * 
 * @see com.ibm.websphere.ras.annotation.TraceOptions
 * @see com.ibm.websphere.ras.annotation.Sensitive
 * @see com.ibm.websphere.ras.annotation.Trivial
 */
public abstract class AbstractRasClassAdapter extends CheckInstrumentableClassAdapter implements RasClassAdapter {

    /**
     * The {@code Type} representing the {@code @Sensitive} annotation.
     */
    public final static Type SENSITIVE_TYPE = Type.getType(com.ibm.websphere.ras.annotation.Sensitive.class);

    /**
     * The {@code Type} representing the {@code TraceObjectField} annotation.
     */
    public final static Type TRACE_OBJECT_FIELD_TYPE = Type.getType(com.ibm.websphere.ras.annotation.TraceObjectField.class);

    /**
     * The {@code Type} representing the {@code @TraceOptions} annotation.
     */
    public final static Type TRACE_OPTIONS_TYPE = Type.getType(com.ibm.websphere.ras.annotation.TraceOptions.class);

    /**
     * The {@code Type} representing the {@code @Trivial} annotation.
     */
    public final static Type TRIVIAL_TYPE = Type.getType(com.ibm.websphere.ras.annotation.Trivial.class);

    /**
     * The {@code Type} representing the class that's being processed.
     */
    protected Type classType;

    /**
     * Indication of whether or not this class represents an interface.
     */
    protected boolean isInterface;

    /**
     * Indication of whether or not this class was generated and not
     * present in source.
     */
    protected boolean isSynthetic;

    /**
     * Result of applying an "inner class detection" heuristic.
     */
    protected boolean isInnerClass;

    /**
     * The version number from the class we're processing.
     */
    protected int classVersion;

    /**
     * The trace configuration and introspection information at the class level.
     */
    protected final ClassInfo classInfo;

    protected final ClassTraceInfo traceInfo;
    
    /**
     * The set of methods that have been visited.
     */
    private final Set<Method> visitedMethods = new HashSet<Method>();

    /**
     * The types of annotations that were observed on this class.
     */
    private final Set<Type> observedAnnotations = new HashSet<Type>();

    /**
     * The annotation visitor used to read the {@code TraceOptions} annotation.
     */
    private TraceOptionsAnnotationVisitor optionsAnnotationVisitor;

    /**
     * The annotation visitor used to read the {@code TraceObjectField} annotation.
     */
    private TraceObjectFieldAnnotationVisitor traceObjectFieldAnnotationVisitor;

    /**
     * Flag that indicates a class static initializer has been created.
     */
    private boolean staticInitializerGenerated;

    private final boolean throwComputeFrames;

    public boolean getThrowComputeFrames() {
    	return throwComputeFrames;
    }
    
    //

    /**
     * Control parameter: Throw a {@link ComputeRequiredException} if a variable
     * store instruction is seen in an initializer before the superclass initializer
     * is invoked.
     */
    public static final boolean THROW_COMPUTE_FRAMES = true;

    public AbstractRasClassAdapter(ClassVisitor visitor,
    		ClassInfo classInfo, ClassTraceInfo traceInfo,
    		boolean throwComputeFrames) {

        super(visitor);

        this.classInfo = classInfo;
        this.traceInfo = traceInfo;
        
        this.throwComputeFrames = throwComputeFrames;
    }

    public AbstractRasClassAdapter(ClassVisitor visitor,
    		ClassInfo classInfo, ClassTraceInfo traceInfo) {

    	this(visitor, classInfo, traceInfo, !THROW_COMPUTE_FRAMES);
    }

    public AbstractRasClassAdapter(ClassVisitor visitor, ClassInfo classInfo) {
    	this(visitor, classInfo, null, !THROW_COMPUTE_FRAMES);
    }

    public AbstractRasClassAdapter(ClassVisitor visitor) {
        this(visitor, null, null, !THROW_COMPUTE_FRAMES);
    }

    //

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classVersion = version;
        this.classType = Type.getObjectType(name);
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        this.isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * Visit the class annotations looking at the supported RAS annotations.
     * The result of running the visitors are only used when a {@code ClassInfo}
     * model object was not provided during construction.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        observedAnnotations.add(Type.getType(desc));
        TraceOptionsData od;
        if (classInfo == null) {
            if (TRACE_OPTIONS_TYPE.getDescriptor().equals(desc)) {
            	if ((traceInfo != null) && ((od = traceInfo.getTraceOptionsData()) != null))
            		optionsAnnotationVisitor = new TraceOptionsAnnotationVisitor(av,od);
            	else
            		optionsAnnotationVisitor = new TraceOptionsAnnotationVisitor(av);
                av = optionsAnnotationVisitor;
            }
        }
        if (TRACE_OBJECT_FIELD_TYPE.getDescriptor().equals(desc)) {
            traceObjectFieldAnnotationVisitor = new TraceObjectFieldAnnotationVisitor(av);
            av = traceObjectFieldAnnotationVisitor;
        }
        return av;
    }

    /**
     * Visit the information about an inner class. We use this to determine
     * whether or not the we're visiting an inner class. This callback is
     * also used to ensure that appropriate class level annotations exist.
     */
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        // Make sure the class is annotated.
        ensureAnnotated();

        if (name.equals(getClassInternalName())) {
            StringBuilder sb = new StringBuilder();
            sb.append(outerName);
            sb.append("$");
            sb.append(innerName);
            isInnerClass = name.equals(sb.toString());
        }

        super.visitInnerClass(name, outerName, innerName, access);
    }

    /**
     * Visit each field defined in the class. We use this to ensure that
     * appropriate class level annotations exist.
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        // Make sure the class is annotated.
        ensureAnnotated();

        return super.visitField(access, name, desc, signature, value);
    }

    /**
     * Ensure that annotations exist on the instrumented class, and add them if
     * they do not already exist.
     */
    protected void ensureAnnotated() {
        ensureTraceObjectFieldAnnotated();
    }

    /**
     * Verify that the TraceObjectField annotation exists on the instrumented
     * class. If one does not exist, add it.
     */
    protected void ensureTraceObjectFieldAnnotated() {
        if (traceObjectFieldAnnotationVisitor == null && getTraceObjectFieldType() != null) {
            visitAnnotation(TRACE_OBJECT_FIELD_TYPE.getDescriptor(), true);

            // Save and restore the reference to the traceObjectFieldAnnotationVisitor
            // so subclasses don't believe it existed prior to processing
            TraceObjectFieldAnnotationVisitor av = traceObjectFieldAnnotationVisitor;
            traceObjectFieldAnnotationVisitor = null;
            av.visit("fieldName", getTraceObjectFieldName());
            av.visit("fieldDesc", getTraceObjectFieldType().getDescriptor());
            av.visitEnd();
            traceObjectFieldAnnotationVisitor = av;
        }
    }

    /**
     * Visit each method defined in the class. We use this to check for the
     * presence of static initializers and to chain the method adapter that
     * will instrument the methods with entry/exit trace calls.
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // Add to observed methods list
        visitedMethods.add(new Method(name, descriptor));

        // Make sure the class is annotated.
        ensureAnnotated();

        // Chain to the appropriate RAS method adapter
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (isInstrumentableClass() && isInstrumentableMethod(access, name, descriptor)) {
            MethodVisitor rasMethodAdapter = createRasMethodAdapter(mv, access, name, descriptor, signature, exceptions);
            mv = (rasMethodAdapter != null) ? rasMethodAdapter : mv;
        }
        return mv;
    }

    /**
     * Visit the end of the class. All of the methods and fieldInfos defined in
     * the class have been visited at this point. If we didn't encounter a
     * trace state field or static initializer, we'll add them now.
     */
    @Override
    public void visitEnd() {
        if (isInstrumentableClass()) {
            // Add a field to hold the trace state if needed
            if (isTraceObjectFieldDefinitionRequired()) {
                int access = (Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC);
                String name = getTraceObjectFieldName();
                String desc = getTraceObjectFieldType().getDescriptor();
                visitField(access, name, desc, null, null);
            }

            // Add a static initializer to setup trace if needed
            if (isStaticInitializerRequired() && !isStaticInitDefined()) {
                staticInitializerGenerated = true;

                MethodVisitor mv = visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                Label lineNumberLabel = new Label();
                mv.visitCode();
                mv.visitLabel(lineNumberLabel);
                mv.visitLineNumber(65535, lineNumberLabel);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(1, 0);
                mv.visitEnd();
            }
        }
        super.visitEnd();
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    @Override
    public boolean isInstrumentableClass() {
        if (isTrivial()) {
            return false;
        }
        return super.isInstrumentableClass();
    }

    /**
     * Determine whether or not a class static initializer is required for
     * this RAS adapter. If a static initializer is needed, this adapter
     * will force one to be visited if none was observed in the class byte
     * code stream.
     * <p>
     * Extenders should override this behavior if a static initializer is not needed.
     * 
     * @return true
     */
    protected boolean isStaticInitializerRequired() {
        return true;
    }

    protected boolean isInjectedTraceAnnotationRequired() {
        return true;
    }

    public Type getClassType() {
        return classType;
    }

    public String getClassInternalName() {
        return classType.getInternalName();
    }

    public String getClassName() {
        return classType.getClassName();
    }

    public int getClassVersion() {
        return classVersion;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isSynthetic() {
        return isSynthetic;
    }

    public boolean isInnerClass() {
        return isInnerClass;
    }

    public boolean isStaticInitDefined() {
        if (classInfo != null) {
            return classInfo.getDeclaredMethod("<clinit>", "()V") != null;
        }
        return visitedMethods.contains(new Method("<clinit>", "()V"));
    }

    public boolean isSensitveType() {
        if (classInfo != null) {
            return classInfo.isSensitive();
        }
        return observedAnnotations.contains(SENSITIVE_TYPE);
    }

    public boolean isTrivial() {
        if (classInfo != null) {
            return classInfo.isTrivial();
        }
        return observedAnnotations.contains(TRIVIAL_TYPE);
    }

    public TraceOptionsData getTraceOptionsData() {
        if ((classInfo != null) && (classInfo.getTraceOptionsData() != null)) {
            return classInfo.getTraceOptionsData();
        }
        if (optionsAnnotationVisitor != null) {
            return optionsAnnotationVisitor.getTraceOptionsData();
        }
        if (traceInfo != null) 
        	return traceInfo.getTraceOptionsData();
        return null;
    }

    protected boolean isStaticInitializerGenerated() {
        return staticInitializerGenerated;
    }

    // TODO: Workable? Document?
    protected String getTraceObjectAnnotationFieldName() {
        if (traceObjectFieldAnnotationVisitor != null) {
            return traceObjectFieldAnnotationVisitor.getFieldName();
        }
        return null;
    }

    protected Type getTraceObjectAnnotationFieldType() {
        if (traceObjectFieldAnnotationVisitor != null) {
            return Type.getType(traceObjectFieldAnnotationVisitor.getFieldDescriptor());
        }
        return null;
    }
}
