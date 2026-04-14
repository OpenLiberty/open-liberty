/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.cdi;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;

import io.openliberty.data.internal.DataProvider;
import io.openliberty.data.internal.EntityManagerBuilder;
import io.openliberty.data.internal.Util;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;

/**
 * Manages the life cycle of persistence context (an EntityManager)
 * for each (request scope, repository group) combination in which a
 * stateful repository is accessed. A repository group is represented
 * by an EntityManagerBuilder. The same EntityManager should be used
 * throughout the life cycle of the request scope and closed when the
 * scope ends.
 */
@RequestScoped
public class StatefulPersistenceContext {
    private static final TraceComponent tc = //
                    Tr.register(StatefulPersistenceContext.class);

    /**
     * Keeps track of the active EntityManager instance for each
     * repository group (EntityManagerBuilder) that is used within
     * the scope of the current request.
     */
    private final Map<EntityManagerBuilder, EntityManager> entityManagers = //
                    new ConcurrentHashMap<>();

    /**
     * Closes all EntityManagers at the end of the request scope.
     */
    @PreDestroy
    private void dispose() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        for (Iterator<Entry<EntityManagerBuilder, EntityManager>> it = //
                        entityManagers.entrySet().iterator(); //
                        it.hasNext();)
            try {
                Entry<EntityManagerBuilder, EntityManager> entry = it.next();
                DataProvider provider = entry.getKey().provider;
                EntityManager em = entry.getValue();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "flush and close " + em);

                // TODO consider having the spec provide a way for the user
                // to request a flush when they want changes persisted,
                // and then we could make this into an error path and
                // discard insteading flushing unresolved changes.

                boolean startedTransaction = false;
                LocalTransactionCoordinator suspendedLTC = null;
                try {
                    if (!em.isJoinedToTransaction()) {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_NO_TRANSACTION) {
                            suspendedLTC = provider.localTranCurrent.suspend();
                            provider.tranMgr.begin();
                            startedTransaction = true;
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc,
                                         "started global tran",
                                         "suspended LTC: " + suspendedLTC);
                        } else {
                            if (trace && tc.isDebugEnabled())
                                Tr.debug(this, tc, Util.txStatusToString(status));
                        }
                        em.joinTransaction();
                    }
                    em.flush(); // must run in a transaction
                } catch (Exception x) {
                    // TODO better NLS message for error writing changes to database
                    Tr.error(tc, "CWWKD1000.repo.general.err", "", "", x.toString());
                    throw x;
                } finally {
                    try {
                        if (startedTransaction)
                            try {
                                int status = provider.tranMgr.getStatus();
                                if (status == Status.STATUS_MARKED_ROLLBACK) {
                                    // TODO NLS message for rolled back changes
                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "roll back global tran",
                                                 Util.txStatusToString(status));
                                    provider.tranMgr.rollback();
                                } else if (status != Status.STATUS_NO_TRANSACTION) {
                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "commit global tran",
                                                 Util.txStatusToString(status));
                                    provider.tranMgr.commit();
                                }
                            } finally {
                                if (suspendedLTC != null) {
                                    if (trace && tc.isDebugEnabled())
                                        Tr.debug(this, tc, "resume LTC: " + suspendedLTC);
                                    provider.localTranCurrent.resume(suspendedLTC);
                                }
                            }
                    } catch (Exception x) {
                        // TODO better NLS message for error comitting changes to database
                        Tr.error(tc, "CWWKD1000.repo.general.err", "", "", x.toString());
                        throw x;
                    } finally {
                        em.close();
                    }
                }
            } finally {
                it.remove();
            }
    }

    /**
     * Obtains the EntityManager instance for the current request scope and
     * the given builder, which represents a group of one or more repositories
     * that share the same dataStore. If one does not exist yet, then the
     * builder is used to create a new EntityManager to associate with the
     * current request scope.
     *
     * @param builder builder that creates an EntityManager for a group of
     *                    one or more repositories that have the same dataStore.
     * @return EntityManager instance.
     */
    public EntityManager get(EntityManagerBuilder builder) {
        return entityManagers.computeIfAbsent(builder,
                                              EntityManagerBuilder::createEntityManager);
    }
}
