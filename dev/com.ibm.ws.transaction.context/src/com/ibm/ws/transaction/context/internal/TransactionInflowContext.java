/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.context.internal;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.concurrent.RejectedExecutionException;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkContextErrorCodes;
import javax.resource.spi.work.WorkContextLifecycleListener;

import com.ibm.tx.jta.TransactionInflowManager;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Transaction inflow context.
 */
public class TransactionInflowContext implements ThreadContext {
    private static final long serialVersionUID = 5814032739830044264L;

    /**
     * Execution context.
     */
    private final transient ExecutionContext executionContext;

    /**
     * Identifier for the resource adapter that provides the TransactionContext or ExecutionContext.
     */
    private final String resourceAdapterIdentifier;

    /**
     * Transaction inflow manager.
     */
    private final transient TransactionInflowManager transactionInflowManager;

    /**
     * Constructs transaction inflow context.
     * 
     * @param tranInflowManager com.ibm.tx.jta.TransactionInflowManager
     * @param executionContext ExecutionContext or TransactionContext (which inherits from ExecutionContext).
     * @param resourceAdapterIdentifier identifier for the resource adapter.
     */
    public TransactionInflowContext(Object transactionInflowManager, Object executionContext, String resourceAdapterIdentifier) {
        this.executionContext = (ExecutionContext) executionContext;
        this.resourceAdapterIdentifier = resourceAdapterIdentifier;
        this.transactionInflowManager = (TransactionInflowManager) transactionInflowManager;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            return (TransactionInflowContext) super.clone();
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContext#taskStarting()
     */
    @Override
    public void taskStarting() {
        try {
            transactionInflowManager.associate(executionContext, resourceAdapterIdentifier);
            if (executionContext instanceof WorkContextLifecycleListener)
                ((WorkContextLifecycleListener) executionContext).contextSetupComplete();
        } catch (WorkCompletedException x) {
            if (executionContext instanceof WorkContextLifecycleListener)
                ((WorkContextLifecycleListener) executionContext).contextSetupFailed(WorkContextErrorCodes.CONTEXT_SETUP_FAILED);
            throw new RejectedExecutionException(x);
        }
    }

    /***
     * @see com.ibm.wsspi.threadcontext.ThreadContext#taskStopping()
     */
    @Override
    public void taskStopping() {
        transactionInflowManager.dissociate();
    }

    /**
     * Serialization is not supported for inflow context.
     * 
     * @param outStream The stream to write the serialized data.
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream outStream) throws IOException {
        throw new NotSerializableException();
    }
}
