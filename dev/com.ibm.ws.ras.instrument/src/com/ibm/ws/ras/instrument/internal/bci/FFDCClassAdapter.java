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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.annotation.InjectedFFDC;
import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation.ClassTraceInfo;
import com.ibm.ws.ras.instrument.internal.model.ClassInfo;

/**
 * A <code>RasClassAdapter</code> that generates calls to FFDC when an exception
 * handler is entered. Depending on the configuration, this adapter may also
 * call FFDC when exceptions are explicitly thrown.
 * <p>
 * If FFDC is not required in certain exception handlers, the FFDCIgnore annotation may be
 * used to omit FFDC from catch blocks by ignoring the exception type declared to be caught.
 * </p>
 * 
 * @see com.ibm.ws.ras.annotation.FFDCIgnore
 */
public class FFDCClassAdapter extends AbstractRasClassAdapter {

    public final static Type INJECTED_FFDC_TYPE = Type.getType(InjectedFFDC.class);

    /**
     * True if @InjectedFFDC has already been added.
     */
    private boolean injectedFFDCAnnotation;

    /**
     * True if the class is already instrumented.
     */
    private boolean instrumented;
    
    public FFDCClassAdapter(ClassVisitor visitor, ClassInfo classInfo) {
        super(visitor, classInfo);
    }

    public FFDCClassAdapter(ClassVisitor visitor, ClassInfo classInfo, ClassTraceInfo ignored) {
    	super(visitor, classInfo);
	}

	@Override
    public RasMethodAdapter createRasMethodAdapter(MethodVisitor mv, int access, String name, String descriptor, String signature, String[] exceptions) {
        return new FFDCMethodAdapter(this, mv, access, name, descriptor, signature, exceptions);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(desc, visible);
        if (INJECTED_FFDC_TYPE.getDescriptor().equals(desc)) {
            injectedFFDCAnnotation = true;
            instrumented = true;
        }
        return av;
    }

    @Override
    protected void ensureAnnotated() {
        super.ensureAnnotated();

        if (!injectedFFDCAnnotation) {
            // Use super.visitAnnotation to avoid instrumented = true.
            super.visitAnnotation(INJECTED_FFDC_TYPE.getDescriptor(), true).visitEnd();
            injectedFFDCAnnotation = true;
        }
    }

    protected boolean isClassInstrumentedByThisAdapter() {
        return instrumented;
    }

    @Override
    public String getTraceObjectFieldName() {
        return null;
    }

    @Override
    public Type getTraceObjectFieldType() {
        return null;
    }

    @Override
    public boolean isTraceObjectFieldDefinitionRequired() {
        return false;
    }

    @Override
    public boolean isTraceObjectFieldInitializationRequired() {
        return false;
    }

    // TODO: [sykesm] Get this information from the ClassInfo object
    public boolean isCallFFDCOnThrow() {
        return false;
    }

    @Override
    public boolean isTrivial() {
        return false;
    }

    @Override
    public boolean isStaticInitializerRequired() {
        return false;
    }

    @Override
    public boolean isInjectedTraceAnnotationRequired() {
        // FFDC is added statically at build time in a single pass, so we do not
        // need to worry about this adapter running multiple times.
        return false;
    }
}
