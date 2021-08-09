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
package com.ibm.ws.ras.instrument.internal.bci;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Interface to be implemented by class adpaters that wish to plug into
 * the trace instrumentation framework.
 */
public interface RasClassAdapter {

    /**
     * Create a method adapter to inject trace code into the methods of a class.
     * 
     * @param delegate
     *            the <code>MethodVisitor</code> that the new method
     *            adapter must forward ASM calls to
     * @param access
     *            the method access flags
     * @param methodName
     *            the name of the method we're processing
     * @param descriptor
     *            the method descriptor containing the parameter and return types
     * @param signature
     *            the method's signature (may be null if generic types are not used)
     * @param exceptions
     *            the internal names of the exception types declared to be thrown
     * 
     * @return the method adapter
     */
    public RasMethodAdapter createRasMethodAdapter(MethodVisitor delegate, int access, String name, String descriptor, String signature, String[] exceptions);

    /**
     * Determine whether or not the field that holds the trace object must
     * be declared.
     */
    public boolean isTraceObjectFieldDefinitionRequired();

    /**
     * Determine whether or not the field that holds the trace object must
     * be initialized out of the static initializer.
     */
    public boolean isTraceObjectFieldInitializationRequired();

    /**
     * Get the name of the field that holds the trace object.
     * 
     * @return the name of the field that holds the trace object
     */
    public String getTraceObjectFieldName();

    /**
     * Get the <code>Type</code> of the field that holds the trace object.
     * 
     * @return the declared <code>Type</code> of the field that holds the
     *         trace object.
     */
    public Type getTraceObjectFieldType();
}
