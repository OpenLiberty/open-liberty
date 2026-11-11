/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.persistence.internal.jta;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.persistence.internal.PersistenceServiceConstants;

/**
 * Invocation handler for the Hibernate interface,
 * org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform.
 *
 * A dynamic proxy instance with this invocation handler is supplied to
 * Hibernate as the persistence property hibernate.transaction.jta.platform,
 * allowing Hibernate to interface with Liberty's transaction manager.
 *
 * This is the Jakarta EE 12 version — uses jakarta.transaction.* namespace.
 * The javax.transaction.* equivalent lives in com.ibm.ws.jpa.container.core
 * as LibertyJtaPlatform
 */
@Trivial
public class LibertyJtaPlatformJakarta implements InvocationHandler, Serializable {
    private static final long serialVersionUID = -3952751839838427897L;

    private static final TraceComponent tc = Tr.register(LibertyJtaPlatformJakarta.class,
                                                         PersistenceServiceConstants.TRACE_GROUP);

    /**
     * Invokes our implementation for methods of
     * org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, methodName, args);
        Object r = null;
        try {
            if (args == null || args.length == 0) {
                r = switch (methodName) {
                    case "canRegisterSynchronization" -> canRegisterSynchronization();
                    case "getCurrentStatus" -> getCurrentStatus();
                    case "hashCode" -> System.identityHashCode(this);
                    case "retrieveTransactionManager" -> retrieveTransactionManager();
                    case "retrieveUserTransaction" -> retrieveUserTransaction();
                    case "toString" -> getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this));
                    default -> null;
                };
            } else {
                switch (methodName) {
                    case "equals" -> r = proxy == args[0]; // assumes one proxy per invocation handler
                    case "getTransactionIdentifier" -> r = getTransactionIdentifier((Transaction) args[0]);
                    case "registerSynchronization" -> registerSynchronization((Synchronization) args[0]);
                    default -> {
                    }
                }
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
            return retrieveTransactionManager().getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException x) {
            throw new RuntimeException(x);
        }
    }

    private int getCurrentStatus() {
        try {
            return retrieveTransactionManager().getStatus();
        } catch (SystemException x) {
            throw new RuntimeException(x);
        }
    }

    private Object getTransactionIdentifier(Transaction transaction) {
        return transaction;
    }

    private void registerSynchronization(Synchronization synchronization) {
        try {
            retrieveTransactionManager().getTransaction().registerSynchronization(synchronization);
        } catch (IllegalStateException | RollbackException | SystemException x) {
            throw new RuntimeException(x);
        }
    }

    private TransactionManager retrieveTransactionManager() {
        try {
            return InitialContext.doLookup("java:comp/TransactionManager");
        } catch (NamingException x) {
            throw new RuntimeException(x);
        }
    }

    private UserTransaction retrieveUserTransaction() {
        try {
            return InitialContext.doLookup("java:comp/UserTransaction");
        } catch (NamingException x) {
            throw new RuntimeException(x);
        }
    }
}
