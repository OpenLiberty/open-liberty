/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Wraps a Runnable instance with context.
 * The context is applied before invoking run and removed afterwards.
 */
public class ContextualRunnable extends ContextualObject<Runnable> implements Runnable, Serializable {
    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = serialFields;

    /**
     * UID for serialization
     */
    private static final long serialVersionUID = -6407285680151763966L;

    /**
     * Default constructor - for deserialization only
     */
    ContextualRunnable() {
        super();
    }

    /**
     * Wrap a Runnable with context.
     * 
     * @param threadContextDescriptor thread context descriptor
     * @param runnable instance to wrap with context
     * @param internalPropNames names of internally added execution properties. Null if execution properties were not specified.
     */
    public ContextualRunnable(ThreadContextDescriptor threadContextDescriptor, Runnable runnable, Set<String> internalPropNames) {
        super((ThreadContextDescriptorImpl) threadContextDescriptor, runnable, internalPropNames);
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

    /** {@inheritDoc} */
    @Override
    public void run() {
        String contextualMethods = threadContextDescriptor.getExecutionProperties().get(WSContextService.CONTEXTUAL_METHODS);
        boolean applyContext = contextualMethods == null || Arrays.asList(contextualMethods.split(",")).contains("run");
        ArrayList<ThreadContext> contextAppliedToThread = applyContext ? threadContextDescriptor.taskStarting() : null;
        try {
            object.run();
        } finally {
            if (applyContext)
                threadContextDescriptor.taskStopping(contextAppliedToThread);
        }
    };

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