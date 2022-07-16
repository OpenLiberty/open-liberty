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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;

import io.openliberty.data.IdNotFoundException;
import io.openliberty.data.MappingException;
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
        if (id == null || entityClass == null)
            throw new NullPointerException(id == null ? "id" : "entityClass");

        EntityInfo entityInfo = persistence.getEntityInfo(entityClass);

        if (entityInfo.keyName == null)
            throw new IdNotFoundException("Entity " + entityClass + " lacks a primary key column.");

        String jpql = "DELETE FROM " + entityInfo.name + " o WHERE o." + entityInfo.keyName + "=?1";

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                persistence.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            Query update = em.createQuery(jpql);
            update.setParameter(1, id);
            int updateCount = update.executeUpdate();

            System.out.println("delete " + entityClass.getName() + " " + id + "? " + (updateCount > 0));

            em.flush();

            failed = false;
        } catch (Exception x) {
            throw new MappingException(x);
        } finally {
            if (em != null)
                em.close();

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && !failed)
                            persistence.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.rollback();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new MappingException(x);
                    }
                } else if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus()) {
                    persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new MappingException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
    }

    @Override
    public <T, K> Optional<T> find(Class<T> entityClass, K id) {
        if (id == null || entityClass == null)
            throw new NullPointerException(id == null ? "id" : "entityClass");

        EntityInfo entityInfo = persistence.getEntityInfo(entityClass);

        if (entityInfo.keyName == null)
            throw new IdNotFoundException("Entity " + entityClass + " lacks a primary key column.");

        String jpql = "SELECT o FROM " + entityInfo.name + " o WHERE o." + entityInfo.keyName + "=?1";

        EntityManager em = null;
        boolean failed = true;
        try {
            em = entityInfo.persister.createEntityManager();

            TypedQuery<T> query = em.createQuery(jpql, entityClass);
            query.setParameter(1, id);
            List<T> results = query.getResultList();

            System.out.println("find " + entityClass.getName() + " " + id + ": " + results);

            failed = false;
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            if (em != null)
                em.close(); // also detaches the entity

            try {
                if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus())
                    persistence.tranMgr.setRollbackOnly();
            } catch (SystemException x) {
                throw new MappingException(x);
            }
        }
    }

    @Override
    public <T> T insert(T entity) {
        return insert(entity, null);
    }

    @Override
    public <T> T insert(T entity, Duration ttl) {
        EntityInfo entityInfo = persistence.getEntityInfo(entity.getClass());

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                if (ttl != null)
                    persistence.tranMgr.setTransactionTimeout((int) ttl.toSeconds() + 1); // TODO overflow
                persistence.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            if (ttl != null)
                setQueryTimeout(em, ttl);

            em.persist(entity);
            em.flush();

            failed = false;
        } catch (Exception x) {
            throw new MappingException(x);
        } finally {
            if (em != null)
                em.close(); // also detaches the entity

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && !failed)
                            persistence.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.rollback();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new MappingException(x);
                    } finally {
                        if (ttl != null)
                            persistence.tranMgr.setTransactionTimeout(0); // restore default
                    }
                } else if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus()) {
                    persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new MappingException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
        return entity;
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

        EntityInfo entityInfo = persistence.getEntityInfo(it.next().getClass());

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                if (ttl != null)
                    persistence.tranMgr.setTransactionTimeout((int) ttl.toSeconds() + 1); // TODO overflow
                persistence.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            if (ttl != null)
                setQueryTimeout(em, ttl);

            for (T e : entities)
                em.persist(e);
            em.flush();

            failed = false;
        } catch (Exception x) {
            throw new MappingException(x);
        } finally {
            if (em != null)
                em.close(); // also detaches the entities

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && !failed)
                            persistence.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.rollback();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new MappingException(x);
                    } finally {
                        if (ttl != null)
                            persistence.tranMgr.setTransactionTimeout(0); // restore default
                    }
                } else if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus()) {
                    persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new MappingException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
        return entities;
    }

    // TODO Lock timeout could be more appropriate for inserts, but there is no way to set it in JPA
    // and no standard for specifying it in JDBC.
    private void setQueryTimeout(EntityManager em, Duration timeToLive) throws Exception {
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
            throw (Exception) x.getCause();
        }
    }

    @Override
    public <T> T update(T entity) {
        EntityInfo entityInfo = persistence.getEntityInfo(entity.getClass());

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        T returnValue;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                persistence.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            returnValue = em.merge(entity);
            em.flush();

            failed = false;
        } catch (Exception x) {
            throw new MappingException(x);
        } finally {
            if (em != null)
                em.close(); // also detaches the entity

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && !failed)
                            persistence.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.rollback();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new MappingException(x);
                    }
                } else if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus()) {
                    persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new MappingException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
        return returnValue;
    }

    @Override
    public <T> Iterable<T> update(Iterable<T> entities) {
        Iterator<T> it = entities.iterator();
        if (!it.hasNext())
            return Collections.<T> emptyList();

        EntityInfo entityInfo = persistence.getEntityInfo(it.next().getClass());

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        ArrayList<T> returnValue;
        boolean requiresNewTransaction = false;
        try {
            if (requiresNewTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus()) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                persistence.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            returnValue = new ArrayList<>();
            for (T e : entities)
                returnValue.add(em.merge(e));
            em.flush();

            failed = false;
        } catch (Exception x) {
            throw new MappingException(x);
        } finally {
            if (em != null)
                em.close(); // also detaches the entities

            try {
                if (requiresNewTransaction) {
                    try {
                        int status = persistence.tranMgr.getStatus();
                        if (status == Status.STATUS_ACTIVE && !failed)
                            persistence.tranMgr.commit();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            persistence.tranMgr.rollback();
                    } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | RollbackException | SecurityException x) {
                        throw new MappingException(x);
                    }
                } else if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus()) {
                    persistence.tranMgr.setRollbackOnly();
                }
            } catch (SystemException x) {
                throw new MappingException(x);
            } finally {
                if (suspendedLTC != null)
                    persistence.localTranCurrent.resume(suspendedLTC);
            }
        }
        return returnValue;
    }
}
