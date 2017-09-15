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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializer;

/**
 * Wraps an object with context.
 * The context is applied before invoking methods on the object and removed afterwards.
 * 
 * @param <T> type of object to wrap with context
 */
public abstract class ContextualObject<T> {
    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required.
     */
    static final String
                    CONTEXT = "C",
                    EXEC_PROPS = "E",
                    INTERNAL_PROP_NAMES = "I",
                    OBJECT = "O";

    /**
     * Fields to serialize
     */
    static final ObjectStreamField[] serialFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(CONTEXT, ThreadContextDescriptorImpl.class),
                                             new ObjectStreamField(EXEC_PROPS, Map.class),
                                             new ObjectStreamField(INTERNAL_PROP_NAMES, Set.class),
                                             new ObjectStreamField(OBJECT, byte[].class)
                    };

    /**
     * Privileged action that gets the thread context class loader.
     */
    private static final GetThreadContextClassLoader getThreadContextClassLoader = new GetThreadContextClassLoader();

    private static final class GetThreadContextClassLoader implements PrivilegedAction<ClassLoader> {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    }

    /**
     * Instance to wrap with context.
     */
    transient T object;

    /**
     * Names of execution properties that were added internally.
     * Null if no execution properties are specified by the user.
     */
    private transient Set<String> internalPropNames;

    /**
     * List of thread context to propagate before invoking the task.
     * Thread context is removed in the reverse order after the operation completes.
     */
    transient ThreadContextDescriptorImpl threadContextDescriptor;

    /**
     * Default constructor - for deserialization only
     */
    ContextualObject() {}

    /**
     * Wrap any Java object with captured thread context.
     * 
     * @param threadContextDescriptor thread context descriptor
     * @param object instance to wrap with context.
     * @param internalPropNames names of internally added execution properties. Null if execution properties were not specified.
     */
    @Trivial
    ContextualObject(ThreadContextDescriptorImpl threadContextDescriptor, T object, Set<String> internalPropNames) {
        this.object = object;
        this.threadContextDescriptor = threadContextDescriptor;
        this.internalPropNames = internalPropNames;
    }

    /**
     * Returns a copy of execution properties.
     * 
     * @return a copy of execution properties. Null if execution properties were not specified.
     */
    @Trivial
    public final Map<String, String> getExecutionProperties() {
        TreeMap<String, String> copy = null;
        if (internalPropNames != null) {
            copy = new TreeMap<String, String>(threadContextDescriptor.getExecutionProperties());
            for (String name : internalPropNames)
                copy.remove(name);
        }
        return copy;
    }

    @Trivial
    public final ThreadContextDescriptorImpl getThreadContextDescriptor() {
        return threadContextDescriptor;
    }

    /**
     * Deserialize.
     * 
     * @param in stream from which to deserialize
     * @throws ClassNotFoundException if a class for a field cannot be found
     * @throws IOException if there is an error reading from the stream
     */
    @SuppressWarnings("unchecked")
    void read(ObjectInputStream in) throws ClassNotFoundException, IOException {
        GetField fields = in.readFields();

        internalPropNames = (Set<String>) fields.get(INTERNAL_PROP_NAMES, null);

        byte[] bytes = (byte[]) fields.get(OBJECT, null);

        // TODO: for internal usage, find a way to supply the class loader of the bundle that creates the contextual object?

        DeserializationObjectInputStream doin = new DeserializationObjectInputStream(
                        new ByteArrayInputStream(bytes),
                        AccessController.doPrivileged(getThreadContextClassLoader));
        object = (T) doin.readObject();
        doin.close();

        Map<String, String> execProps = (Map<String, String>) fields.get(EXEC_PROPS, null);

        bytes = (byte[]) fields.get(CONTEXT, null);
        threadContextDescriptor = (ThreadContextDescriptorImpl) ThreadContextDeserializer.deserialize(bytes, execProps);
    }

    /**
     * Serialize.
     * 
     * @param out stream to which to serialize
     * @throws IOException if there is an error writing to the stream
     */
    void write(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();

        fields.put(CONTEXT, threadContextDescriptor.serialize());

        fields.put(EXEC_PROPS, threadContextDescriptor.getExecutionProperties());

        fields.put(INTERNAL_PROP_NAMES, internalPropNames);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(object);
        oout.flush();
        fields.put(OBJECT, bout.toByteArray());
        oout.close();

        out.writeFields();
    }
}