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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Wraps a Callable instance with context.
 * The context is applied before invoking call and removed afterwards.
 * 
 * @param <T> type of result returned by Callable.call
 */
public class ContextualCallable<T> extends ContextualObject<Callable<T>> implements Callable<T>, Serializable {
    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = serialFields;

    /**
     * UID for serialization
     */
    private static final long serialVersionUID = -7816200235335790948L;

    /**
     * Default constructor - for deserialization only
     */
    ContextualCallable() {
        super();
    }

    /**
     * Wrap a Callable with context.
     * 
     * @param threadContextDescriptor thread context descriptor
     * @param callable instance to wrap with context
     * @param internalPropNames names of internally added execution properties. Null if execution properties were not specified.
     */
    public ContextualCallable(ThreadContextDescriptor threadContextDescriptor, Callable<T> callable, Set<String> internalPropNames) {
        super((ThreadContextDescriptorImpl) threadContextDescriptor, callable, internalPropNames);
    }

    /** {@inheritDoc} */
    @Override
    public T call() throws Exception {
        String contextualMethods = threadContextDescriptor.getExecutionProperties().get(WSContextService.CONTEXTUAL_METHODS);
        boolean applyContext = contextualMethods == null || Arrays.asList(contextualMethods.split(",")).contains("call");
        T result;
        ArrayList<ThreadContext> contextAppliedToThread = applyContext ? threadContextDescriptor.taskStarting() : null;
        try {
            result = object.call();
        } finally {
            if (applyContext)
                threadContextDescriptor.taskStopping(contextAppliedToThread);
        }
        return result;
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