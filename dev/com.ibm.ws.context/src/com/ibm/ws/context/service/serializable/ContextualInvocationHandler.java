/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.context.service.serializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Wraps an interface or set of interfaces with captured thread context.
 * The context is applied before invoking the specified interface methods and removed afterwards.
 */
public class ContextualInvocationHandler extends ContextualObject<Object> implements InvocationHandler, Serializable {
    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = serialFields;

    /**
     * UID for serialization
     */
    private static final long serialVersionUID = -6017024294962299060L;

    /**
     * Default constructor - for deserialization only
     */
    ContextualInvocationHandler() {
        super();
    }

    /**
     * Invocation handler that allows for wrapping any interface with thread context.
     * 
     * @param threadContextDescriptor thread context descriptor
     * @param instance instance to wrap with context
     * @param internalPropNames names of internally added execution properties. Null if execution properties were not specified.
     */
    public ContextualInvocationHandler(ThreadContextDescriptor threadContextDescriptor, Object instance, Set<String> internalPropNames) {
        super((ThreadContextDescriptorImpl) threadContextDescriptor, instance, internalPropNames);
    }

    /**
     * Apply context, invoke the method, then remove the context from the thread.
     * 
     * @param method the method
     * @param args method parameters
     * @return result of the method
     * @throws IllegalAccessException if unable to access the method
     * @throws InvocationTargetException if the method fails
     */
    private Object invoke(Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
        ArrayList<ThreadContext> contextAppliedToThread = threadContextDescriptor.taskStarting();
        try {
            return method.invoke(object, args);
        } finally {
            threadContextDescriptor.taskStopping(contextAppliedToThread);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // A list of contextual method names can be specified to limit the methods to
        // which context is applied.  Otherwise, context is applied to all methods except
        // for those declared on java.lang.Object (like .equals, .hashCode, .toString)
        String contextualMethods = threadContextDescriptor.getExecutionProperties().get(WSContextService.CONTEXTUAL_METHODS);
        boolean applyContext = contextualMethods == null ?
                        !Object.class.equals(method.getDeclaringClass())
                        : Arrays.asList(contextualMethods.split(",")).contains(method.getName());

        try {
            if (applyContext)
                return invoke(method, args);
            else if (args != null && args.length == 1 && "equals".equals(method.getName())) // special case: Object.equals
                return proxy == args[0];
            else
                return method.invoke(object, args);
        } catch (IllegalAccessException x) {
            throw new RejectedExecutionException(x);
        } catch (InvocationTargetException x) {
            throw x.getTargetException();
        }
    }

    /**
     * Deserialize.
     * 
     * @param in stream from which to deserialize
     * @throws ClassNotFoundException if a class for a field cannot be found
     * @throws IOException if there is an error reading from the stream
     */
    @Trivial
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        read(in);
    }

    /**
     * Serialize.
     * 
     * @param out stream to which to serialize
     * @throws IOException if there is an error writing to the stream
     */
    @Trivial
    private void writeObject(ObjectOutputStream out) throws IOException {
        write(out);
    }
}