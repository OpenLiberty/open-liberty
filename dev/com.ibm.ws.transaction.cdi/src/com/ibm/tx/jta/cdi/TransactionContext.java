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
package com.ibm.tx.jta.cdi;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.transaction.Status;
import javax.transaction.TransactionScoped;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.TransactionSynchronizationRegistryFactory;
import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.TransactionScopeDestroyer;

/**
 * Implementation for the TransactionScoped annotation.
 */
public class TransactionContext implements AlterableContext, TransactionScopeDestroyer {

    private final UserTransaction ut = UserTransactionImpl.instance();

    private final TransactionSynchronizationRegistry tsr = TransactionSynchronizationRegistryFactory.getTransactionSynchronizationRegistry();

    private final String TXC_STORAGE_ID = "cdi_TXC";

    private static final TraceComponent tc = Tr.register(TransactionContext.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    @Override
    public <T> T get(Contextual<T> contextual) {
        return this.get(contextual, null);
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "get", new Object[] { contextual, creationalContext, this });

        performChecks(contextual);

        Map<String, InstanceAndContext<?>> storage = getStorage(true);
        InstanceAndContext<T> data = this.getByContextual(storage, contextual);

        if (data != null) {
            return data.instance;
        } else if (creationalContext == null) {
            return null;
        }

        final T t = contextual.create(creationalContext);
        final String contextId = ((PassivationCapable) contextual).getId();

        data = new InstanceAndContext<T>(contextual, creationalContext, t);
        storage.put(contextId, data);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "get", t);

        return t;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.context.spi.Context#getScope()
     */
    @Override
    public Class<? extends Annotation> getScope() {
        return TransactionScoped.class;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.context.spi.Context#isActive()
     */
    @Override
    public boolean isActive() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isActive", this);

        boolean isActive = false;

        try {
            switch (ut.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_UNKNOWN:
                case Status.STATUS_PREPARING:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_ROLLING_BACK:
                    isActive = true;
                    break;
                default:
                    isActive = false;
            }
        } catch (Exception e) {
            // drop through
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isActive", isActive);

        return isActive;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.context.spi.AlterableContext#destroy(javax.enterprise.context.spi.Contextual)
     */
    @Override
    public void destroy(Contextual<?> contextual) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "destroy(Contextual)", new Object[] { contextual, this });

        final Map<String, InstanceAndContext<?>> storage = getStorage(false);

        if (storage != null) {
            final String contextId = getContextualId(contextual);

            InstanceAndContext<?> data = this.getByContextual(storage, contextual);

            if (data != null) {
                storage.remove(contextId);
                destroyItem(contextId, data);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "destroy(Contextual)");
    }

    /**
     * Destroy the entire context. This causes @PreDestroy annotated methods to be
     * called on all the bean instances that this scope has created.
     */
    @Override
    public void destroy() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "destroy", this);

        // check the TSR is active - otherwise we can trigger exceptions when we
        // try to get data out of it.
        if (tsr.getTransactionKey() != null) {
            final Map<String, InstanceAndContext<?>> storage = getStorage(false);

            if (storage != null) {
                try {
                    for (InstanceAndContext<?> entry : storage.values()) {
                        final String id = getContextualId(entry.context);
                        destroyItem(id, entry);
                    }
                } finally {
                    tsr.putResource(TXC_STORAGE_ID, null);
                }
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Tran synchronization registry not available, skipping destroy");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "destroy");
    }

    // basic error checks we perform before each get/destroy call
    private void performChecks(Contextual<?> contextual) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "performChecks", this);

        if (!isActive()) {
            throw new ContextNotActiveException();
        }

        if (contextual == null) {
            throw new IllegalArgumentException("Contextual parameter should not be null");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "performChecks");
    }

    @SuppressWarnings("unchecked")
    private <T> InstanceAndContext<T> getByContextual(Map<String, InstanceAndContext<?>> storage, Contextual<T> contextual) {
        final String contextId = getContextualId(contextual);
        return (InstanceAndContext<T>) storage.get(contextId);
    }

    private static String getContextualId(Contextual<?> contextual) {
        return ((PassivationCapable) contextual).getId();
    }

    @SuppressWarnings("unchecked")
    private <T> Map<String, InstanceAndContext<?>> getStorage(boolean create) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getStorage", new Object[] { create, this });

        Map<String, InstanceAndContext<?>> storage = (Map<String, InstanceAndContext<?>>) tsr.getResource(TXC_STORAGE_ID);
        if (storage == null && create) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "No existing storage for transaction context, creating");

            storage = new HashMap<String, InstanceAndContext<?>>();
            tsr.putResource(TXC_STORAGE_ID, storage);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getStorage");
        return storage;
    }

    private <T> void destroyItem(String contextId, InstanceAndContext<T> item) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "destroyItem", new Object[] { item, this });

        item.context.destroy(item.instance, item.creationalContext);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "destroyItem");
    }

    private static class InstanceAndContext<T> {
        Contextual<T> context;
        CreationalContext<T> creationalContext;
        T instance;

        public InstanceAndContext(Contextual<T> context, CreationalContext<T> creationalContext, T instance) {
            this.context = context;
            this.creationalContext = creationalContext;
            this.instance = instance;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(getContextualId(this.context)).append(", ");
            sb.append(this.creationalContext.toString()).append(", ");
            sb.append(this.instance.toString()).append("]");
            return sb.toString();
        }
    }

}