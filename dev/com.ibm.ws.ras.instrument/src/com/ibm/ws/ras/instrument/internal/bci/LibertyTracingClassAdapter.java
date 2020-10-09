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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.ClassTraceInfo;
import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.FieldInfo;

/**
 * A <code>RasClassAdapter</code> implementation that generates calls to
 * the WebSphere Tr tracing and logging framework.
 * 
 * @see com.ibm.ejs.ras.Tr
 * @see com.ibm.ejs.ras.TraceComponent
 * @see com.ibm.ws.ras.annotation.Logger
 */
public class LibertyTracingClassAdapter extends AbstractTracingRasClassAdapter {

    /**
     * Type representing {@code com.ibm.websphere.ras.Tr}.
     */
    public final static Type TR_TYPE = Type.getObjectType("com/ibm/websphere/ras/Tr");

    /**
     * Type representing {@code com.ibm.websphere.ras.TraceComponent}.
     */
    public final static Type TRACE_COMPONENT_TYPE = Type.getObjectType("com/ibm/websphere/ras/TraceComponent");

    private final static String DEFAULT_TRACE_COMPONENT_FIELD_NAME = "$$$tc$$$";

    private FieldInfo declaredLoggerField;
    private FieldInfo traceComponentField;
    private boolean traceComponentAlreadyDefined;
    private boolean onlyInstrumentPreprocessed;

    /**
     * List of method adapters that have been created. This list will be used to
     * determine if the class bytecode has been modified.
     */
    List<LibertyTracingMethodAdapter> createdMethodAdapters = new ArrayList<LibertyTracingMethodAdapter>();

    public LibertyTracingClassAdapter(ClassVisitor visitor, ClassInfo classInfo) {
        super(visitor, classInfo);

        // Look for an annotated field with the correct type
        if (classInfo != null) {
            declaredLoggerField = classInfo.getDeclaredLoggerField();
            if (declaredLoggerField != null) {
                if (declaredLoggerField.getFieldDescriptor().equals(TRACE_COMPONENT_TYPE.getDescriptor())) {
                    traceComponentField = declaredLoggerField;
                    traceComponentAlreadyDefined = true;
                }
            }
        }

        // Build one with the defaults
        if (traceComponentField == null) {
            traceComponentField = new FieldInfo(DEFAULT_TRACE_COMPONENT_FIELD_NAME, TRACE_COMPONENT_TYPE.getDescriptor());
        }
    }

	public LibertyTracingClassAdapter(ClassVisitor visitor, boolean onlyInstrumentPreprocessed) {
        this(visitor, null);
        this.onlyInstrumentPreprocessed = onlyInstrumentPreprocessed;
    }

    public LibertyTracingClassAdapter(ClassVisitor visitor, ClassTraceInfo info, boolean onlyInstrumentPreprocessed) {
    	this(visitor, null);
    	this.onlyInstrumentPreprocessed = onlyInstrumentPreprocessed;
    	traceInfo = info;
	}

	@Override
    public RasMethodAdapter createRasMethodAdapter(MethodVisitor mv, int access, String name, String descriptor, String signature, String[] exceptions) {
        // Use the super class's observation of the TraceObjectField annotation
        // to detect a pre-processed class. This is a little bit of a kludge but
        // avoids throwing on yet another annotation.
        if (onlyInstrumentPreprocessed && super.getTraceObjectAnnotationFieldName() == null) {
            return null;
        }
        // If the trace object field is not an Liberty TraceComponent, don't
        // attempt to instrument the class. The WsLogger object will inflate a
        // TraceComponent with a class literal from the call stack at the time
        // the logger is obtained. That means when the trace spec changes, a
        // class redefine will be attempted.
        if (onlyInstrumentPreprocessed && !super.getTraceObjectAnnotationFieldType().equals(TRACE_COMPONENT_TYPE)) {
            return null;
        }
        LibertyTracingMethodAdapter methodAdapter = new LibertyTracingMethodAdapter(this, mv, access, name, descriptor, signature, exceptions);
        createdMethodAdapters.add(methodAdapter);
        return methodAdapter;
    }

    @Override
    public String getTraceObjectFieldName() {
        String fieldName = super.getTraceObjectAnnotationFieldName();
        if (fieldName != null && super.getTraceObjectAnnotationFieldType().equals(TRACE_COMPONENT_TYPE)) {
            traceComponentAlreadyDefined = true;
            return fieldName;
        }
        return traceComponentField.getFieldName();
    }

    @Override
    protected void ensureTraceObjectFieldAnnotated() {
        if (!onlyInstrumentPreprocessed) {
            super.ensureTraceObjectFieldAnnotated();
        }
    }

    @Override
    public Type getTraceObjectFieldType() {
        return TRACE_COMPONENT_TYPE;
    }

    @Override
    public boolean isTraceObjectFieldDefinitionRequired() {
        if (onlyInstrumentPreprocessed) {
            return false;
        }
        return !traceComponentAlreadyDefined;
    }

    @Override
    public boolean isTraceObjectFieldInitializationRequired() {
        return isTraceObjectFieldDefinitionRequired();
    }

    @Override
    public boolean isStaticInitializerRequired() {
        return !onlyInstrumentPreprocessed;
    }

    public boolean isClassModified() {
        if (isStaticInitializerRequired()) {
            return true;
        }

        if (isTraceObjectFieldDefinitionRequired()) {
            return true;
        }

        if (isTraceObjectFieldInitializationRequired()) {
            return true;
        }

        for (LibertyTracingMethodAdapter methodAdapter : createdMethodAdapters) {
            if (methodAdapter.isModifiedMethod()) {
                return true;
            }
        }

        return false;
    }

	public boolean isTraceComponentAlreadyDefined() {
		return traceComponentAlreadyDefined;
	}



}
