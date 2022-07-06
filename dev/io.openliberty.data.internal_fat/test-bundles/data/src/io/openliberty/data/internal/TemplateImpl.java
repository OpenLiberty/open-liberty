/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import io.openliberty.data.Template;

/**
 */
public class TemplateImpl implements Template {
    private final DataPersistence persistence;

    public TemplateImpl() {
        persistence = AccessController.doPrivileged((PrivilegedAction<DataPersistence>) () -> {
            BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
            return bc.getService(bc.getServiceReference(DataPersistence.class));
        });
    }

    @Override
    public <T, K> void delete(Class<T> entityClass, K id) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T, K> Optional<T> find(Class<T> entityClass, K id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T insert(T entity) {
        return insert(entity, null);
    }

    @Override
    public <T> T insert(T entity, Duration ttl) {
        PersistenceServiceUnit punit = persistence.getPersistenceServiceUnit(entity.getClass());

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        T returnValue;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                if (ttl != null)
                    persistence.tranMgr.setTransactionTimeout((int) ttl.toSeconds() + 1); // TODO overflow
                persistence.tranMgr.begin();
            }

            em = punit.createEntityManager();

            if (ttl != null)
                setQueryTimeout(em, ttl);

            returnValue = em.merge(entity);
            em.flush();
        } catch (NotSupportedException | SystemException x) {
            throw new RuntimeException(x);
        } finally {
            if (em != null)
                em.close();

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                            persistence.tranMgr.rollback();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.commit();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new RuntimeException(x);
                    } finally {
                        if (ttl != null)
                            persistence.tranMgr.setTransactionTimeout(0); // restore default
                    }
                } else {
                    if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus())
                        persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new RuntimeException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
        return returnValue;
    }

    @Override
    public <T> Iterable<T> insert(Iterable<T> entities) {
        return insert(entities, null);
    }

    @Override
    public <T> Iterable<T> insert(Iterable<T> entities, Duration ttl) {
        Iterator<T> it = entities.iterator();
        if (!it.hasNext())
            return Collections.<T> emptyList();

        PersistenceServiceUnit punit = persistence.getPersistenceServiceUnit(it.next().getClass());

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        ArrayList<T> returnValue;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                if (ttl != null)
                    persistence.tranMgr.setTransactionTimeout((int) ttl.toSeconds() + 1); // TODO overflow
                persistence.tranMgr.begin();
            }

            em = punit.createEntityManager();

            if (ttl != null)
                setQueryTimeout(em, ttl);

            returnValue = new ArrayList<>();
            for (T e : entities)
                returnValue.add(em.merge(e));
            em.flush();
        } catch (NotSupportedException | SystemException x) {
            throw new RuntimeException(x);
        } finally {
            if (em != null)
                em.close();

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                            persistence.tranMgr.rollback();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.commit();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new RuntimeException(x);
                    } finally {
                        if (ttl != null)
                            persistence.tranMgr.setTransactionTimeout(0); // restore default
                    }
                } else {
                    if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus())
                        persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new RuntimeException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
        return returnValue;
    }

    @Override
    public <T> T update(T entity) {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO lock timeout could be more appropriate for inserts
    private void setQueryTimeout(EntityManager em, Duration timeToLive) {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                Class<?> Session = em.getClass().getClassLoader().loadClass("org.eclipse.persistence.sessions.Session");
                Object session = em.unwrap(Session);

                Session.getMethod("setQueryTimeoutUnitDefault", TimeUnit.class)
                                .invoke(session, TimeUnit.SECONDS);

                Session.getMethod("setQueryTimeoutDefault", int.class)
                                .invoke(session, (int) timeToLive.toSeconds()); // TODO overflow
                return null;
            });
        } catch (PrivilegedActionException x) {
            throw new RuntimeException(x.getCause());
        }
    }

    @Override
    public <T> Iterable<T> update(Iterable<T> entities) {
        // TODO Auto-generated method stub
        return null;
    }
}
