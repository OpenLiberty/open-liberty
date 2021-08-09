/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.hibernate;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Invocation handler for the Hibernate interface,
 * org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform.
 * A dynamic proxy instance with this invocation handler is supplied to
 * Hibernate as the persistence property hibernate.transaction.jta.platform,
 * allowing Hibernate to interface with the transaction manager.
 */
@Trivial
public class LibertyJtaPlatform implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 8662020839696752408L;

    private static final TraceComponent tc = Tr.register(LibertyJtaPlatform.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    /**
     * Invokes our implementation for methods of org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, methodName, args);
        Object r = null;
        try {
            if (args == null || args.length == 0) {
                if ("canRegisterSynchronization".equals(methodName))
                    r = canRegisterSynchronization();
                else if ("getCurrentStatus".equals(methodName))
                    r = getCurrentStatus();
                else if ("hashCode".equals(methodName))
                    r = System.identityHashCode(this);
                else if ("retrieveTransactionManager".equals(methodName))
                    r = retrieveTransactionManager();
                else if ("retrieveUserTransaction".equals(methodName))
                    r = retrieveUserTransaction();
                else if ("toString".equals(methodName))
                    r = new StringBuilder(getClass().getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).toString();
            } else {
                if ("equals".equals(methodName))
                    r = proxy == args[0]; // assumes one proxy per invocation handler
                else if ("getTransactionIdentifier".equals(methodName))
                    r = getTransactionIdentifier((Transaction) args[0]);
                else if ("registerSynchronization".equals(methodName))
                    registerSynchronization((Synchronization) args[0]);
            }
        } catch (Throwable x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, methodName, x);
            throw x;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, methodName, r);
        return r;
    }

    private boolean canRegisterSynchronization() {
        try {
            return TransactionManagerFactory.getTransactionManager().getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException x) {
            throw new RuntimeException(x);
        }
    }

    private int getCurrentStatus() throws SystemException {
        return TransactionManagerFactory.getTransactionManager().getStatus();
    }

    private Object getTransactionIdentifier(Transaction transaction) {
        return transaction;
    }

    private void registerSynchronization(Synchronization synchronization) {
        try {
            TransactionManagerFactory.getTransactionManager().getTransaction().registerSynchronization(synchronization);
        } catch (IllegalStateException x) {
            throw new RuntimeException(x);
        } catch (RollbackException x) {
            throw new RuntimeException(x);
        } catch (SystemException x) {
            throw new RuntimeException(x);
        }
    }

    private TransactionManager retrieveTransactionManager() {
        return TransactionManagerFactory.getTransactionManager();
    }

    private UserTransaction retrieveUserTransaction() {
        try {
            return InitialContext.doLookup("java:comp/UserTransaction");
        } catch (NamingException x) {
            throw new RuntimeException(x);
        }
    }
}