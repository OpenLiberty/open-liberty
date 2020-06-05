/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.enterprise.concurrent.ManagedTask;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;
import com.ibm.wsspi.threadcontext.jca.JCAContextProvider;

/**
 * Transaction context service provider.
 */
public class TransactionContextProviderImpl implements JCAContextProvider, ThreadContextProvider {
    // Constant for ManagedTask.TRANSACTION in whichever of Jakarta vs Java EE is NOT enabled
    private static final String OTHER_SPEC_TRANSACTION_CONSTANT;
    static {
        int dot = ManagedTask.TRANSACTION.indexOf('.');
        OTHER_SPEC_TRANSACTION_CONSTANT = new StringBuilder(46 - dot) //
                        .append(dot == 5 ? "jakarta" : "javax") //
                        .append(".enterprise.concurrent.TRANSACTION") //
                        .toString();
    }

    /**
     * Reference to the transaction inflow manager.
     */
    final AtomicServiceReference<Object> transactionInflowManagerRef = new AtomicServiceReference<Object>("transactionInflowManager");

    private EmbeddableWebSphereTransactionManager transactionManager;

    /**
     * Called during service activation.
     *
     * @param context
     */
    protected void activate(ComponentContext context) {
        transactionInflowManagerRef.activate(context);
    }

    /**
     * Called during service deactivation.
     *
     * @param context
     */
    protected void deactivate(ComponentContext context) {
        transactionInflowManagerRef.deactivate(context);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        // Determine the value of the ManagedTask.TRANSACTION execution property, if present
        String key, value;
        if (execProps == null) {
            key = null;
            value = null;
        } else { // prefer the enabled spec
            value = execProps.get(key = ManagedTask.TRANSACTION);
            if (value == null)
                value = execProps.get(key = OTHER_SPEC_TRANSACTION_CONSTANT);
        }
        if (value == null || ManagedTask.SUSPEND.equals(value))
            return new TransactionContextImpl(true);
        else if (ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(value))
            return new TransactionContextImpl(false);
        else if ("PROPAGATE".equals(value)) {
            UOWCurrent uowCurrent = (UOWCurrent) transactionManager;
            if (uowCurrent.getUOWType() == UOWCurrent.UOW_GLOBAL) {
                // Per spec, IllegalStateException could be raised here to reject all propagation of transactions
                // However, we allow propagation as long as the transaction isn't used in parallel.
                return new SerialTransactionContextImpl();
            } else
                return new TransactionContextImpl(true);
        } else
            throw new IllegalArgumentException(key + '=' + value);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        return captureThreadContext(execProps, null);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        TransactionContextImpl context;
        try {
            context = (TransactionContextImpl) in.readObject();

            // Determine the value of the ManagedTask.TRANSACTION execution property, if present
            String key, value;
            if (info == null) {
                key = null;
                value = null;
            } else { // prefer the enabled spec
                value = info.getExecutionProperty(key = ManagedTask.TRANSACTION);
                if (value == null)
                    value = info.getExecutionProperty(key = OTHER_SPEC_TRANSACTION_CONSTANT);
            }

            if (value == null || ManagedTask.SUSPEND.equals(value))
                context.suspendTranOfExecutionThread = true;
            else if (ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(value))
                context.suspendTranOfExecutionThread = false;
            else
                throw new IllegalArgumentException(key + '=' + value);
        } finally {
            in.close();
        }
        return context;
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext getInflowContext(Object workContext, Map<String, String> execProps) {
        // Construct TransactionInflowContext reflectively because it requires packages that are only be available when JCA is used
        try {
            return (ThreadContext) getClass().getClassLoader() //
                            .loadClass("com.ibm.ws.transaction.context.internal.TransactionInflowContext") //
                            .getConstructor(Object.class, Object.class, String.class) //
                            .newInstance(transactionInflowManagerRef.getServiceWithException(), workContext, execProps.get(WSContextService.TASK_OWNER));
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            if (x instanceof RuntimeException)
                throw (RuntimeException) x;
            if (x instanceof Error)
                throw (Error) x;
            else
                throw new RuntimeException(x);
        }
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    /**
     * Declarative Services method for setting the TransactionInflowManager service
     *
     * @param ref reference to the service
     */
    protected void setTransactionInflowManager(ServiceReference<Object> ref) {
        transactionInflowManagerRef.setReference(ref);
    }

    /**
     * Declarative Services method to set the transaction manager.
     */
    protected void setTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        transactionManager = tm;
    }

    /**
     * Declarative Services method for unsetting the TransactionInflowManager service
     *
     * @param ref reference to the service
     */
    protected void unsetTransactionInflowManager(ServiceReference<Object> ref) {
        transactionInflowManagerRef.unsetReference(ref);
    }

    /**
     * Declarative Services method to unset the transaction manager.
     */
    protected void unsetTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        transactionManager = null;
    }
}
