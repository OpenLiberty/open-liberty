/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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

package com.ibm.tx.jta.impl;

import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;

public class TransactionSynchronizationRegistryImpl implements TransactionSynchronizationRegistry {
    private static final TraceComponent tc = Tr.register(TransactionSynchronizationRegistryImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public TransactionSynchronizationRegistryImpl() {
    }

    protected TransactionImpl getTransaction() {
        return ((TranManagerSet) TransactionManagerFactory.getTransactionManager()).getTransactionImpl();
    }

    @Override
    public Object getTransactionKey() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionKey", this);

        final Object key;

        final TransactionImpl transaction = getTransaction();

        if (transaction == null) {
            key = null;
        } else {
            long id = transaction.getLocalId();
            key = new Long(id);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionKey", key);
        return key;
    }

    @Override
    public void putResource(Object key, Object resource) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "putResource", new Object[] { key, resource, this });

        final TransactionImpl transaction = getTransaction();

        if (transaction == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "putResource", "IllegalStateException");
            throw new IllegalStateException();
        }

        transaction.putResource(key, resource);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "putResource");
    }

    @Override
    public Object getResource(Object key) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getResource", new Object[] { key, this });

        final TransactionImpl transaction = getTransaction();

        if (transaction == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getResource", "IllegalStateException");
            throw new IllegalStateException();
        }

        final Object resource = transaction.getResource(key);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getResource", resource);
        return resource;
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerInterposedSynchronization", new Object[] { sync, this });

        if (sync == null) {
            final NullPointerException npe = new NullPointerException();

            if (tc.isEntryEnabled())
                Tr.exit(tc, "registerInterposedSynchronization", npe);
            throw npe;
        }

        final TransactionImpl transaction = getTransaction();

        if (transaction == null) {
            final IllegalStateException ise = new IllegalStateException();

            if (tc.isEntryEnabled())
                Tr.exit(tc, "registerInterposedSynchronization", ise);
            throw ise;
        }

        transaction.registerInterposedSynchronization(sync);
    }

    @Override
    public int getTransactionStatus() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionStatus", this);

        final int status = ((TranManagerSet) TransactionManagerFactory.getTransactionManager()).getStatus();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionStatus", Util.printStatus(status));
        return status;
    }

    @Override
    public void setRollbackOnly() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRollbackOnly", this);

        ((TranManagerSet) TransactionManagerFactory.getTransactionManager()).setRollbackOnly();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setRollbackOnly");
    }

    @Override
    public boolean getRollbackOnly() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRollbackOnly", this);

        final TransactionImpl transaction = getTransaction();

        if (transaction == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getRollbackOnly", "IllegalStateException");
            throw new IllegalStateException();
        }

        final boolean rollbackOnly = transaction.getRollbackOnly();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRollbackOnly", rollbackOnly);
        return rollbackOnly;
    }
}