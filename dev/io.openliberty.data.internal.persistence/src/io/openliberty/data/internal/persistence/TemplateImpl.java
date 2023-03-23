/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.cdi.DataExtensionProvider;
import jakarta.data.Template;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;

/**
 */
public class TemplateImpl implements Template {
    private static final TraceComponent tc = Tr.register(TemplateImpl.class);

    private final EntityDefiner definer;
    private final DataExtensionProvider provider;

    public TemplateImpl(DataExtensionProvider provider, EntityDefiner definer) {
        this.provider = provider;
        this.definer = definer;
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public <T, K> void delete(Class<T> entityClass, K id) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "delete", entityClass, id);

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        Throwable failure = null;
        boolean requiresNewTransaction = false;
        boolean committed = false;
        int updateCount = -1;
        try {
            CompletableFuture<EntityInfo> future = definer.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
            if (future == null)
                throw new IllegalArgumentException("Unrecognized entity class " + entityClass.getName());
            EntityInfo entityInfo = future.join();

            String keyName = entityInfo.getAttributeName("id");
            String jpql = "DELETE FROM " + entityInfo.name + " o WHERE o." + keyName + "=?1";

            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                suspendedLTC = provider.localTranCurrent.suspend();
                provider.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            Query update = em.createQuery(jpql);
            update.setParameter(1, id);
            updateCount = update.executeUpdate();

            em.flush();
        } catch (Throwable x) {
            failure = x;
        } finally {
            if (em != null)
                em.close();

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && failure == null)
                            provider.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.rollback();
                        committed = status == Status.STATUS_ACTIVE && failure == null;
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        failure = failure == null ? x : failure;
                    }
                } else if (failure != null && Status.STATUS_ACTIVE == provider.tranMgr.getStatus()) {
                    provider.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                failure = failure == null ? x : failure;
            } finally {
                if (suspendedLTC != null)
                    provider.localTranCurrent.resume(suspendedLTC);
            }
        }

        if (trace && tc.isEntryEnabled())
            if (failure == null)
                if (requiresNewTransaction)
                    Tr.exit(this, tc, "delete", updateCount + (committed ? " committed" : " rolled back"));
                else
                    Tr.exit(this, tc, "delete", updateCount);
            else
                Tr.exit(this, tc, "delete", failure);
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public <T, K> Optional<T> find(Class<T> entityClass, K id) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find", entityClass, id);

        EntityManager em = null;
        Throwable failure = null;
        Optional<T> found = null;
        try {
            CompletableFuture<EntityInfo> future = definer.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
            if (future == null)
                throw new IllegalArgumentException("Unrecognized entity class " + entityClass.getName());
            EntityInfo entityInfo = future.join();

            String keyName = entityInfo.getAttributeName("id");
            String jpql = "SELECT o FROM " + entityInfo.name + " o WHERE o." + keyName + "=?1";

            em = entityInfo.persister.createEntityManager();

            TypedQuery<T> query = em.createQuery(jpql, entityClass);
            query.setParameter(1, id);
            List<T> results = query.getResultList();

            found = results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Throwable x) {
            failure = x;
        } finally {
            if (em != null)
                em.close(); // also detaches the entity

            try {
                if (failure != null && Status.STATUS_ACTIVE == provider.tranMgr.getStatus())
                    provider.tranMgr.setRollbackOnly();
            } catch (SystemException x) {
                // ignore - a previous failure was already recorded
            }
        }

        if (trace && tc.isEntryEnabled())
            if (failure == null)
                Tr.exit(this, tc, "find", found);
            else
                Tr.exit(this, tc, "find", failure);
        if (failure == null)
            return found;
        else if (failure instanceof Error)
            throw (Error) failure;
        else
            throw RepositoryImpl.failure((Exception) failure);
    }

    @Override
    @Trivial
    public final <T> T insert(T entity) {
        return insert(entity, null);
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public <T> T insert(T entity, Duration ttl) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "insert", entity, ttl);

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        Throwable failure = null;
        boolean requiresNewTransaction = false;
        boolean committed = false;
        try {
            Class<?> entityClass = entity.getClass();
            CompletableFuture<EntityInfo> future = definer.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
            if (future == null)
                throw new IllegalArgumentException("Unrecognized entity class " + entityClass.getName());
            EntityInfo entityInfo = future.join();

            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                suspendedLTC = provider.localTranCurrent.suspend();
                if (ttl != null)
                    provider.tranMgr.setTransactionTimeout((int) ttl.toSeconds() + 1); // TODO overflow
                provider.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            if (ttl != null)
                setQueryTimeout(em, ttl);

            em.persist(entity);
            em.flush();
        } catch (Throwable x) {
            failure = x;
        } finally {
            if (em != null)
                em.close(); // also detaches the entity

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && failure == null)
                            provider.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.rollback();
                        committed = status == Status.STATUS_ACTIVE && failure == null;
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        failure = failure == null ? x : failure;
                    } finally {
                        if (ttl != null)
                            provider.tranMgr.setTransactionTimeout(0); // restore default
                    }
                } else if (failure != null && Status.STATUS_ACTIVE == provider.tranMgr.getStatus()) {
                    provider.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                failure = failure == null ? x : failure;
            } finally {
                if (suspendedLTC != null)
                    provider.localTranCurrent.resume(suspendedLTC);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "insert", failure == null //
                            ? requiresNewTransaction //
                                            ? new Object[] { entity, committed ? "committed" : "rolled back" } //
                                            : entity //
                            : failure);
        if (failure == null)
            return entity;
        else if (failure instanceof Error)
            throw (Error) failure;
        else
            throw RepositoryImpl.failure((Exception) failure);
    }

    @Override
    @Trivial
    public <T> Iterable<T> insert(Iterable<T> entities) {
        return insert(entities, null);
    }

    @FFDCIgnore(Throwable.class)
    @Trivial
    @Override
    public <T> Iterable<T> insert(Iterable<T> entities, Duration ttl) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "insert", entities, ttl);

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        Throwable failure = null;
        boolean requiresNewTransaction = false;
        boolean committed = false;
        try {
            Iterator<T> it = entities.iterator();
            T entity = it.next();
            Class<?> entityClass = entity.getClass();
            CompletableFuture<EntityInfo> future = definer.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
            if (future == null)
                throw new IllegalArgumentException("Unrecognized entity class " + entityClass.getName());
            EntityInfo entityInfo = future.join();

            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                suspendedLTC = provider.localTranCurrent.suspend();
                if (ttl != null)
                    provider.tranMgr.setTransactionTimeout((int) ttl.toSeconds() + 1); // TODO overflow
                provider.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            if (ttl != null)
                setQueryTimeout(em, ttl);

            do {
                em.persist(entity);
                entity = it.hasNext() ? it.next() : null;
            } while (entity != null);

            em.flush();
        } catch (Throwable x) {
            failure = x;
        } finally {
            if (em != null)
                em.close(); // also detaches the entities

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && failure == null)
                            provider.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.rollback();
                        committed = status == Status.STATUS_ACTIVE && failure == null;
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        if (failure == null)
                            failure = x;
                    } finally {
                        if (ttl != null)
                            provider.tranMgr.setTransactionTimeout(0); // restore default
                    }
                } else if (failure != null && Status.STATUS_ACTIVE == provider.tranMgr.getStatus()) {
                    provider.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                if (failure == null)
                    failure = x;
            } finally {
                if (suspendedLTC != null)
                    provider.localTranCurrent.resume(suspendedLTC);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "insert", failure == null //
                            ? requiresNewTransaction //
                                            ? new Object[] { entities, committed ? "committed" : "rolled back" } //
                                            : entities //
                            : failure);
        if (failure == null)
            return entities;
        else if (failure instanceof Error)
            throw (Error) failure;
        else
            throw RepositoryImpl.failure((Exception) failure);
    }

    // TODO Lock timeout could be more appropriate for inserts, but there is no way to set it in JPA
    // and no standard for specifying it in JDBC.
    private void setQueryTimeout(EntityManager em, Duration timeToLive) throws Exception {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                Class<?> Session = em.getClass().getClassLoader().loadClass("org.eclipse.persistence.sessions.Session");
                Object session = em.unwrap(Session);

                Session.getMethod("setQueryTimeoutUnitDefault", TimeUnit.class).invoke(session, TimeUnit.SECONDS);

                Session.getMethod("setQueryTimeoutDefault", int.class).invoke(session, (int) timeToLive.toSeconds()); // TODO overflow
                return null;
            });
        } catch (PrivilegedActionException x) {
            throw (Exception) x.getCause();
        }
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public <T> T update(T entity) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "update", entity);

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        Throwable failure = null;
        T returnValue = null;
        boolean requiresNewTransaction = false;
        boolean committed = false;
        try {
            Class<?> entityClass = entity.getClass();
            CompletableFuture<EntityInfo> future = definer.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
            if (future == null)
                throw new IllegalArgumentException("Unrecognized entity class " + entityClass.getName());
            EntityInfo entityInfo = future.join();

            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                suspendedLTC = provider.localTranCurrent.suspend();
                provider.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            returnValue = em.merge(entity);
            em.flush();
        } catch (Throwable x) {
            failure = x;
        } finally {
            if (em != null)
                em.close(); // also detaches the entity

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && failure == null)
                            provider.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.rollback();
                        committed = status == Status.STATUS_ACTIVE && failure == null;
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        failure = failure == null ? x : failure;
                    }
                } else if (failure != null && Status.STATUS_ACTIVE == provider.tranMgr.getStatus()) {
                    provider.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                failure = failure == null ? x : failure;
            } finally {
                if (suspendedLTC != null)
                    provider.localTranCurrent.resume(suspendedLTC);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "update", failure == null //
                            ? requiresNewTransaction //
                                            ? new Object[] { returnValue, committed ? "committed" : "rolled back" } //
                                            : entity //
                            : failure);
        if (failure == null)
            return returnValue;
        else if (failure instanceof Error)
            throw (Error) failure;
        else
            throw RepositoryImpl.failure((Exception) failure);
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public <T> Iterable<T> update(Iterable<T> entities) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "update", entities);

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        Throwable failure = null;
        boolean requiresNewTransaction = false;
        boolean committed = false;
        ArrayList<T> returnValue = new ArrayList<>();
        try {
            Iterator<T> it = entities.iterator();
            T entity = it.next();
            Class<?> entityClass = entity.getClass();
            CompletableFuture<EntityInfo> future = definer.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
            if (future == null)
                throw new IllegalArgumentException("Unrecognized entity class " + entityClass.getName());
            EntityInfo entityInfo = future.join();

            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                suspendedLTC = provider.localTranCurrent.suspend();
                provider.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            do {
                returnValue.add(em.merge(entity));
                entity = it.hasNext() ? it.next() : null;
            } while (entity != null);

            em.flush();
        } catch (Throwable x) {
            failure = x;
        } finally {
            if (em != null)
                em.close(); // also detaches the entities

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && failure == null)
                            provider.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.rollback();
                        committed = status == Status.STATUS_ACTIVE && failure == null;
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        if (failure == null)
                            failure = x;
                    }
                } else if (failure != null && Status.STATUS_ACTIVE == provider.tranMgr.getStatus()) {
                    provider.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                if (failure == null)
                    failure = x;
            } finally {
                if (suspendedLTC != null)
                    provider.localTranCurrent.resume(suspendedLTC);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "update", failure == null //
                            ? requiresNewTransaction //
                                            ? new Object[] { returnValue, committed ? "committed" : "rolled back" } //
                                            : returnValue //
                            : failure);
        if (failure == null)
            return returnValue;
        else if (failure instanceof Error)
            throw (Error) failure;
        else
            throw RepositoryImpl.failure((Exception) failure);
    }
}
