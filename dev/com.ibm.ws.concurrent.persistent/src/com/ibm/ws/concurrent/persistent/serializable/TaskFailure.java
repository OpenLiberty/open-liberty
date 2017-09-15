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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Special result type that indicates failure of a task itself
 * or a failure related to the task, such as a failure to serialize the task result.
 */
public class TaskFailure implements Serializable {
    private static final long serialVersionUID = -3056770769390754606L;
    private static final TraceComponent tc = Tr.register(TaskFailure.class);

    /**
     * Reason code for task failure indicating the task was aborted because the failure limit has been reached.
     */
    public static final short FAILURE_LIMIT_REACHED = 1;

    /**
     * Reason code for task failure indicating the result is not serializable.
     */
    public static final short NONSER_RESULT = 2;

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required in the task store.
     */
    private static final String
                    FAILURE = "F",
                    PARAMS = "P",
                    REASON = "R";

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(FAILURE, Throwable.class),
                                             new ObjectStreamField(PARAMS, String[].class),
                                             new ObjectStreamField(REASON, short.class)
                    };

    /**
     * Error that occurred when attempting to serialize the task.
     * Could be a copy of the error if the error itself will not deserialize.
     */
    private transient Throwable failure;

    /**
     * Parameters for the task failure. May vary depending on the reason code.
     */
    private transient String[] params;

    /**
     * Reason code for the failure.
     */
    private transient short reason;

    /**
     * Construct a failure entry for a task.
     * 
     * @param failure the error.
     * @param loader class loader from which the task and its result should be deserialized.
     * @param persistentExecutor the persistent executor instance.
     * @param reason reason code for the task failure.
     * @param optional parameters for the task failure.
     */
    @FFDCIgnore(Throwable.class)
    public TaskFailure(Throwable failure, ClassLoader loader, PersistentExecutorImpl persistentExecutor, short reason, String... params) {
        this.reason = reason;
        this.params = params;

        // Only save the error if it will deserialize properly.
        if (failure != null)
            try {
                persistentExecutor.deserialize(persistentExecutor.serialize(failure), loader);
            } catch (Throwable x) {
                if (params.length == 0)
                    this.params = new String[] { failure.getClass().getName() };
                // If it won't serialize/deserialize, replace everything in the stack with RuntimeException
                for (Throwable cause = failure, current, previous = null; cause != null; cause = cause.getCause(), previous = current) {
                    current = new RuntimeException(cause.getClass().getName() + ": " + cause.getMessage());
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
     * Returns the failure, if deserializable from the thread context class loader that was present on the thread
     * when the task was submitted, and the class loader has ClassLoaderIdentity.
     * 
     * @return the failure, if deserializable. Otherwise null.
     */
    public final Throwable getCause() {
        return failure;
    }

    /**
     * Returns the specified parameter, if any.
     * 
     * @return the specified parameter, if any.
     */
    public final String getParameter(int index) {
        return params.length > 0 ? params[index] : null;
    }

    /**
     * Returns the reason code for the task failure.
     * 
     * @return the reason code for the task failure.
     */
    @Trivial
    public final short getReason() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getReason", toString(reason));
        return reason;
    }

    /**
     * Deserialize task information.
     * 
     * @param in The stream from which this object is read.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Trivial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        failure = (Throwable) fields.get(FAILURE, null);
        params = (String[]) fields.get(PARAMS, new String[] {});
        reason = fields.get(REASON, (short) 0);
    }

    /**
     * Utility method to format a reason code in readable form for trace.
     * 
     * @param reason reason code
     * @return text suitable for trace.
     */
    @Trivial
    private static final String toString(short reason) {
        switch (reason) {
            case FAILURE_LIMIT_REACHED:
                return "FAILURE_LIMIT_REACHED";
            case NONSER_RESULT:
                return "NONSER_RESULT";
            default:
                return null;
        }
    }

    /**
     * Serialize task information.
     * 
     * @param out The stream to which this object is serialized.
     * @throws IOException
     */
    @Trivial
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put(FAILURE, failure);
        fields.put(PARAMS, params);
        fields.put(REASON, reason);
        out.writeFields();
    }
}
