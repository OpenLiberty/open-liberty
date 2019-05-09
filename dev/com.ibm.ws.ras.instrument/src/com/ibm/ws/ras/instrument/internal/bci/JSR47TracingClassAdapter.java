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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.model.ClassInfo;
import com.ibm.ws.ras.instrument.internal.model.FieldInfo;

/**
 * A <code>RasClassAdapter</code> implementation that generates calls to the
 * JSR47 Java logging framework.
 * 
 * @see java.util.logging.Logger
 * @see com.ibm.ws.logging.LoggerHelper
 * @see com.ibm.ws.ras.annotation.Logger
 */
public class JSR47TracingClassAdapter extends AbstractTracingRasClassAdapter implements Opcodes {

    public final static Type LOGGER_TYPE = Type.getType(java.util.logging.Logger.class);

    private final static String DEFAULT_LOGGER_FIELD_NAME = "$$$logger$$$";

    private FieldInfo declaredLoggerField;
    private FieldInfo loggerField;
    private boolean loggerAlreadyDefined;

    public JSR47TracingClassAdapter(ClassVisitor visitor, ClassInfo classInfo) {
        super(visitor, classInfo);

        // Look for an annotated field with the correct type
        if (classInfo != null) {
            declaredLoggerField = classInfo.getDeclaredLoggerField();
            if (declaredLoggerField != null) {
                if (declaredLoggerField.getFieldDescriptor().equals("")) {
                    loggerField = declaredLoggerField;
                    loggerAlreadyDefined = true;
                }
            }
        }

        // Build one with the defaults
        if (loggerField == null) {
            loggerField = new FieldInfo(DEFAULT_LOGGER_FIELD_NAME, LOGGER_TYPE.getDescriptor());
        }
    }

    @Override
    public RasMethodAdapter createRasMethodAdapter(MethodVisitor delegate, int access, String name, String descriptor, String signature, String[] exceptions) {
        return new JSR47TracingMethodAdapter(this, delegate, access, name, descriptor, signature, exceptions);
    }

    @Override
    public String getTraceObjectFieldName() {
        return loggerField.getFieldName();
    }

    @Override
    public Type getTraceObjectFieldType() {
        return LOGGER_TYPE;
    }

    @Override
    public boolean isTraceObjectFieldDefinitionRequired() {
        System.out.println("!!MJS: JSR47 tracing class adatper isTraceObjectFieldDefinitionRequired called");
        System.out.println("!!MJS loggerAlreadyDefined = " + loggerAlreadyDefined);
        return !loggerAlreadyDefined;
    }

    @Override
    public boolean isTraceObjectFieldInitializationRequired() {
        return isTraceObjectFieldDefinitionRequired();
    }
}
