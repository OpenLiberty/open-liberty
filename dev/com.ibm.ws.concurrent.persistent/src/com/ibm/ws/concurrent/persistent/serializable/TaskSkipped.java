/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.serializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.internal.PersistentExecutorImpl;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Special result type that indicates a task was skipped, possibly due to an unchecked exception on trigger.skipRun.
 * As an optimization, this class is typically not used when trigger.skipRun returns normally.
 */
public class TaskSkipped implements Serializable {
    private static final long serialVersionUID = -5563145431924561136L;
    private static final TraceComponent tc = Tr.register(TaskSkipped.class);

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required in the task store.
     */
    private static final String
                    FAILURE = "F",
                    PREVIOUS_RESULT = "R";

    /**
     * Failure that occurred on trigger.skipRun (if any).
     */
    private transient Throwable failure;

    /**
     * Result of previous task execution (if any).
     */
    private transient byte[] previousResult;

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(FAILURE, Throwable.class),
                                             new ObjectStreamField(PREVIOUS_RESULT, byte[].class)
                    };

    /**
     * Construct a skipped entry for a task.
     * 
     * @param previousResult the previous task result.
     * @param failure the failure, if any, that occurred on trigger.skipRun
     * @param loader class loader from which the task and its result should be deserialized
     * @param persistentExecutor the persistent executor instance.
     */
    public TaskSkipped(byte[] previousResult, Throwable failure, ClassLoader loader, PersistentExecutorImpl persistentExecutor) {
        this.previousResult = previousResult;

        // Only save the error if it will deserialize properly.
        if (failure != null)
            try {
                persistentExecutor.deserialize(persistentExecutor.serialize(failure), loader);
            } catch (Throwable x) {
                FFDCFilter.processException(x, getClass().getName(), "72", this, new Object[] { failure, loader });
                // If it won't serialize/deserialize, replace everything in the stack with RuntimeException
                for (Throwable cause = failure, current, previous = null; cause != null; cause = cause.getCause(), previous = current) {
                    current = new RuntimeException(cause.getMessage());
                    current.setStackTrace(cause.getStackTrace());
                    if (previous == null)
                        failure = current;
                    else
                        previous.initCause(current);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "serialize of failure failed", x);
                    Tr.debug(this, tc, "replace with", failure);
                }
            }
        this.failure = failure;
    }

    /**
     * Returns the failure, if any, if deserializable from the thread context class loader that was present on the thread
     * when the task was submitted, and the class loader has ClassLoaderIdentity.
     * 
     * @return the failure, if any, and if deserializable. Otherwise null.
     */
    public final Throwable getCause() {
        return failure;
    }

    /**
     * Returns the result of previous task execution (if any).
     * 
     * @return the result of previous task execution (if any).
     */
    public final byte[] getPreviousResult() {
        return previousResult;
    }

    /**
     * Deserialize information about the skipped execution.
     * 
     * @param in The stream from which this object is read.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Trivial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        failure = (Throwable) fields.get(FAILURE, null);
        previousResult = (byte[]) fields.get(PREVIOUS_RESULT, null);
    }

    /**
     * Serialize information about the skipped execution.
     * 
     * @param out The stream to which this object is serialized.
     * @throws IOException
     */
    @Trivial
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put(FAILURE, failure);
        fields.put(PREVIOUS_RESULT, previousResult);
        out.writeFields();
    }
}
