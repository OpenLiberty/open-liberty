/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.handlelist.context.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;

import com.ibm.ejs.j2c.HandleList;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.cm.handle.HandleListInterface;
import com.ibm.ws.threadContext.ConnectionHandleAccessorImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This pseudo-context ensures that each contextual task runs with its own
 * initially empty HandleList.
 *
 * The thread that executes the task calls taskStarting() and taskStopping()
 * on this object, which this class uses to establish a new HandleList and
 * remove it afterward, restoring the previous HandleList, if any.
 *
 * This context type is serializable, which means it can be flattened and
 * re-inflated, possibly on another machine.
 */
public class EmptyHandleListContext implements ThreadContext {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(EmptyHandleListContext.class);

    /**
     * Fields to serialize
     *
     * The field names are used by the readObject/writeObject operations as the key to lookup the serializable
     * data in the stream. If you change field names, they must also be changed in readObject/writeObject.
     *
     * CAUTION! Deleting a field or changing the type of a field is an incompatible change, and should be avoided.
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {};

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            EmptyHandleListContext copy = (EmptyHandleListContext) super.clone();
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Deserialize.
     *
     * @param in The stream from which this object is read.
     *
     * @throws IOException            if there are I/O errors while reading from the underlying InputStream
     * @throws ClassNotFoundException if the class of a serialized object could not be found.
     */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        // Currently, there is no serializable state
        // GetField fields = in.readFields();
    }

    /**
     * Push a new HandleList onto the thread.
     */
    @Override
    public void taskStarting() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "taskStarting");

        com.ibm.ws.threadContext.ThreadContext<HandleListInterface> threadContext = //
                        ConnectionHandleAccessorImpl.getConnectionHandleAccessor().getThreadContext();

        HandleListInterface prevHandleList = threadContext.beginContext(new HandleList());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "taskStarting", prevHandleList + " --> " + threadContext.getContext());
    }

    /**
     * Restore the HandleList (if any) that was previously on the thread.
     */
    @Override
    public void taskStopping() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "taskStopping");

        com.ibm.ws.threadContext.ThreadContext<HandleListInterface> threadContext = //
                        ConnectionHandleAccessorImpl.getConnectionHandleAccessor().getThreadContext();

        HandleList removedHandleList = (HandleList) threadContext.endContext();
        if (removedHandleList != null)
            removedHandleList.close();

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "taskStopping", threadContext.getContext() + " <-- " + removedHandleList);
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(31).append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

    /**
     * Serialize.
     *
     * @param out The stream to which this object is serialized.
     *
     * @throws IOException if an error occurs.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        // Currently, there is no serializable state
        out.writeFields();
    }
}