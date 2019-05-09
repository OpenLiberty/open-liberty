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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

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
public class WebSphereTrTracingClassAdapter extends AbstractTracingRasClassAdapter {

    /**
     * Type representing {@type com.ibm.ejs.ras.Tr}.
     */
    public final static Type TR_TYPE = Type.getObjectType("com/ibm/ejs/ras/Tr");

    /**
     * Type representing {@code com.ibm.ejs.ras.TraceComponent}.
     */
    public final static Type TRACE_COMPONENT_TYPE = Type.getObjectType("com/ibm/ejs/ras/TraceComponent");

    private final static String DEFAULT_TRACE_COMPONENT_FIELD_NAME = "$$$tc$$$";

    private FieldInfo declaredLoggerField;
    private FieldInfo traceComponentField;
    private boolean traceComponentAlreadyDefined;

    public WebSphereTrTracingClassAdapter(ClassVisitor visitor, ClassInfo classInfo) {
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

    @Override
    public RasMethodAdapter createRasMethodAdapter(MethodVisitor mv, int access, String name, String descriptor, String signature, String[] exceptions) {
        return new WebSphereTrTracingMethodAdapter(this, mv, access, name, descriptor, signature, exceptions);
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
    public Type getTraceObjectFieldType() {
        return TRACE_COMPONENT_TYPE;
    }

    @Override
    public boolean isTraceObjectFieldDefinitionRequired() {
        return !traceComponentAlreadyDefined;
    }

    @Override
    public boolean isTraceObjectFieldInitializationRequired() {
        return isTraceObjectFieldDefinitionRequired();
    }
}
