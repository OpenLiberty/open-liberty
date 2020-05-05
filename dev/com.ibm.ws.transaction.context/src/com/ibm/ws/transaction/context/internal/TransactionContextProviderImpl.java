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

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.javaee.version.JavaEEVersion;
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
    /**
     * Jakarta EE versiom if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    int eeVersion;

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
        } else if (eeVersion < 9) { // prefer javax
            value = execProps.get(key = "javax.enterprise.concurrent.TRANSACTION");
            if (value == null)
                value = execProps.get(key = "jakarta.enterprise.concurrent.TRANSACTION");
        } else { // prefer jakarta
            value = execProps.get(key = "jakarta.enterprise.concurrent.TRANSACTION");
            if (value == null)
                value = execProps.get(key = "javax.enterprise.concurrent.TRANSACTION");
        }
        if (value == null || "SUSPEND".equals(value)) // ManagedTask.SUSPEND
            return new TransactionContextImpl(true);
        else if ("USE_TRANSACTION_OF_EXECUTION_THREAD".equals(value)) // ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD
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
            } else if (eeVersion < 9) { // prefer javax
                value = info.getExecutionProperty(key = "javax.enterprise.concurrent.TRANSACTION");
                if (value == null)
                    value = info.getExecutionProperty(key = "jakarta.enterprise.concurrent.TRANSACTION");
            } else { // prefer jakarta
                value = info.getExecutionProperty(key = "jakarta.enterprise.concurrent.TRANSACTION");
                if (value == null)
                    value = info.getExecutionProperty(key = "javax.enterprise.concurrent.TRANSACTION");
            }

            if (value == null || "SUSPEND".equals(value)) // ManagedTask.SUSPEND
                context.suspendTranOfExecutionThread = true;
            else if ("USE_TRANSACTION_OF_EXECUTION_THREAD".equals(value)) // ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD
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
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
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
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        eeVersion = 0;
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
