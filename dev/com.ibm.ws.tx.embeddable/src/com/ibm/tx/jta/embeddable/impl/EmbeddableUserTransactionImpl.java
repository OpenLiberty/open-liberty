package com.ibm.tx.jta.embeddable.impl;

/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.ws.uow.UOWScope;

public class EmbeddableUserTransactionImpl extends com.ibm.tx.jta.impl.UserTransactionImpl implements EmbeddableWebSphereUserTransaction {
    private static final TraceComponent tc = Tr.register(EmbeddableUserTransactionImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private volatile static EmbeddableUserTransactionImpl _instance = new EmbeddableUserTransactionImpl();

    public static EmbeddableUserTransactionImpl instance() {
        return _instance;
    }

    @Override
    protected TransactionManager getTM() {
        if (_tm instanceof EmbeddableTranManagerSet) {
            return _tm;
        }

        _tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        return _tm;
    }

    private static final ThreadLocal<Boolean> isEnabled = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
            return Boolean.TRUE;
        }
    };

    public static boolean isEnabled() {
        return isEnabled.get();
    }

    public static void setEnabled(boolean enabled) {
        isEnabled.set(enabled);
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "begin");

        checkEnabled();

        super.begin();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "begin");
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit");

        checkEnabled();

        final UOWScope coord = (UOWScope) getTM().getTransaction();
        if (coord == null /* || !coord.isGlobal() */) {
            final IllegalStateException ise = new IllegalStateException("No Global Transaction exists to commit.");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", ise);
            throw ise;
        }

        if (coord instanceof EmbeddableTransactionImpl) {
            final EmbeddableTransactionImpl tx = (EmbeddableTransactionImpl) coord;

            if (tx.isSubordinate() || tx.hasSuspendedAssociations()) {
                final SecurityException se = new SecurityException("Attempted commit in subordinate");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "commit", se);
                throw se;
            }
        }

        super.commit();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
    }

    @Override
    public int getStatus() throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getStatus");

        checkEnabled();

        final int ret = super.getStatus();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStatus", ret);

        return ret;
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback");

        checkEnabled();

        final UOWScope coord = (UOWScope) getTM().getTransaction();
        if (coord == null /* || !coord.isGlobal() */) {
            final IllegalStateException ise = new IllegalStateException("No Global Transaction exists to commit.");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", ise);
            throw ise;
        }

        if (coord instanceof EmbeddableTransactionImpl) {
            final EmbeddableTransactionImpl tx = (EmbeddableTransactionImpl) coord;

            if (tx.isSubordinate() || tx.hasSuspendedAssociations()) {
                final SecurityException se = new SecurityException("Attempted rollback in subordinate");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", se);
                throw se;
            }
        }

        super.rollback();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback");
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRollbackOnly");

        checkEnabled();

        super.setRollbackOnly();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setRollbackOnly");
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setTransactionTimeout", seconds);

        checkEnabled();

        super.setTransactionTimeout(seconds);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setTransactionTimeout");
    }

    private void checkEnabled() throws IllegalStateException {
        if (!isEnabled.get()) {
            final IllegalStateException ise = new IllegalStateException("UserTransaction method forbidden in this context");
            if (tc.isDebugEnabled())
                Tr.debug(tc, "checkEnabled", ise);
            throw ise;
        }
    }
}