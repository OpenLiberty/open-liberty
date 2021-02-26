/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.bci;

import static com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.LOGGER_TYPE;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.introspect.TraceObjectFieldAnnotationVisitor;
import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.ClassTraceInfo;

public class LibertyTracePreprocessClassAdapter extends AbstractTracingRasClassAdapter {

    private TraceObjectFieldAnnotationVisitor traceObjectFieldVisitor;
    private final boolean initializeTraceObjectField;

    public LibertyTracePreprocessClassAdapter(ClassVisitor visitor, boolean initializeTraceObjectField) {
        super(visitor, null);
        this.initializeTraceObjectField = initializeTraceObjectField;
    }

    public LibertyTracePreprocessClassAdapter(ClassVisitor visitor, boolean initializeTraceObjectField, ClassTraceInfo info) {
    	super(visitor, null);
    	this.initializeTraceObjectField = initializeTraceObjectField;
    	traceInfo = info;
	}

	@Override
    public AnnotationVisitor visitAnnotation(String name, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(name, visible);
        if (name.equals(TRACE_OBJECT_FIELD_TYPE.getDescriptor())) {
            traceObjectFieldVisitor = new TraceObjectFieldAnnotationVisitor(av);
            av = traceObjectFieldVisitor;
        }
        return av;
    }

    @Override
    public RasMethodAdapter createRasMethodAdapter(MethodVisitor delegate, int access, String name, String descriptor, String signature, String[] exceptions) {
        if (LOGGER_TYPE.equals(getTraceObjectFieldType())) {
            return new JSR47TracingMethodAdapter(this, delegate, access, name, descriptor, signature, exceptions);
        } else if (WebSphereTrTracingClassAdapter.TRACE_COMPONENT_TYPE.equals(getTraceObjectFieldType())) {
            return new WebSphereTrTracingMethodAdapter(this, delegate, access, name, descriptor, signature, exceptions);
        } else if (LibertyTracingClassAdapter.TRACE_COMPONENT_TYPE.equals(getTraceObjectFieldType()) && "<clinit>".equals(name)) {
            return new LibertyTracingMethodAdapter(this, false, delegate, access, name, descriptor, signature, exceptions);
        }
        return null;
    }

    @Override
    public String getTraceObjectFieldName() {
        return traceObjectFieldVisitor.getFieldName();
    }

    @Override
    public Type getTraceObjectFieldType() {
        return Type.getType(traceObjectFieldVisitor.getFieldDescriptor());
    }

    @Override
    public boolean isTraceObjectFieldDefinitionRequired() {
        return false;
    }

    @Override
    public boolean isTraceObjectFieldInitializationRequired() {
        return initializeTraceObjectField;
    }

	@Override
	public boolean isTrivial() {
		
		return false;
	}

}
