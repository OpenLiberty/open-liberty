/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * WAS JPA implementation of the Entity Manager base class used by the transaction-scoped and
 * extend-scoped entity managers.
 * 
 * This implements all the EntityManager interface methods but route all calls to the subclass via
 * the getEMInvocationInfo() to perform the scoped specific JPA functions.
 * 
 */
public abstract class JPAEntityManager implements EntityManager, Serializable
{
    private static final long serialVersionUID = -377956679492029198L;

    private static final TraceComponent tc = Tr.register(JPAEntityManager.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    /**
     * Whether this EntityManager does not automatically joins the active JTA transaction when invoked.
     * By default this is set to false (meaning the EntityManager will automatically join the active JTA transaction.)
     * 
     * In JPA 2.1 or later this value can be set with a @PersistenceContext "synchronization" field value:
     * -- SynchronizationType.SYNCHRONIZED (default) will set it to a value of false.
     * -- SynchronizationType.UNSYNCHRONIZED will set it to a value of true
     * 
     */
    protected boolean ivUnsynchronized = false;

    protected JPAEntityManager() {
        // For serialization
    }

    protected JPAEntityManager(boolean txUnsynchronized) {
        ivUnsynchronized = txUnsynchronized;
    }

    /**
     * Whether this EntityManager does not automatically joins the active JTA transaction when invoked.
     * By default this is set to false (meaning the EntityManager will automatically join the active JTA transaction.)
     * 
     * In JPA 2.1 or later this value can be set with a @PersistenceContext "synchronization" field value:
     * -- SynchronizationType.SYNCHRONIZED (default) will set it to a value of false.
     * -- SynchronizationType.UNSYNCHRONIZED will set it to a value of true
     * 
     */
    public boolean isTxUnsynchronized() {
        return ivUnsynchronized;
    }

    /**
     * Delegate to subclass to return the appropriate EntityManager.
     * 
     * @param requireTx Indicates if an active transaction must exist, otherwise a
     *            javax.persistence.TransactionRequiredException is thrown.
     * @return EntityManager
     */
    abstract EntityManager getEMInvocationInfo(boolean requireTx);

    /**
     * Delegate to subclass to return the appropriate EntityManager. This is intended
     * to support the following EntityManager API method contracts:
     * - TransactionRequiredException if there is no transaction and a lock mode other than
     * NONE is set. Set requireTx to false and mode to a LockModeType.
     * - TransactionRequiredException if invoked on a container-managed entity manager of type
     * PersistenceContextType.TRANSACTION and there is no transaction. Set mode to null.
     * - TransactionRequiredException if there is no transaction and if invoked on a container-managed
     * EntityManager instance with PersistenceContextType.TRANSACTION or with a lock mode
     * other than NONE. Set requireTx to true and mode to a LockModeType.
     * 
     * @param requireTx Indicates if an active transaction must exist, otherwise a
     *            javax.persistence.TransactionRequiredException is thrown.
     * @param mode If not null, an active transaction must exist only if mode is not NONE,
     *            otherwise a javax.persistence.TransactionRequiredException is thrown.
     * @return EntityManager
     */
    abstract EntityManager getEMInvocationInfo(boolean requireTx, LockModeType mode);

    /**
     * Close (or clear and pool) the specified Tx provider EntityManager,
     * if still open. <p>
     * 
     * This is called from the JPATxEmInvocation.afterCompletion() method,
     * and supports pooling of EntityManagers. <p>
     * 
     * @param em provider EntityManager to be closed (or pooled).
     * @param allowPooling indicates whether or not the EntityManager may
     *            be pooled.
     **/
    // d510184
    abstract void closeTxEntityManager(EntityManager em, boolean allowPooling);

    /**
     * Pool the specified NoTx provider EntityManager,
     * if still open. The pooling logic will close the em if the
     * pool is full. <p>
     * 
     * This is called from all of the JPANoTxEmInvocation methods,
     * and supports pooling of EntityManagers. <p>
     * 
     * @param noTxInvocation The non-transactional invocation object
     *            associated with the Entity Manager
     *            for this Unit of Work.
     */
    //572594
    abstract void closeNoTxEntityManager(JPANoTxEmInvocation noTxinvocation);

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#persist(java.lang.Object)
     */
    @Override
    public void persist(Object entity)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.persist(" + entity + ");\n" + toString());

        getEMInvocationInfo(true).persist(entity);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#merge(java.lang.Object)
     */
    @Override
    public <T> T merge(T entity)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.merge(" + entity + ");\n" + toString());

        return getEMInvocationInfo(true).merge(entity);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#remove(java.lang.Object)
     */
    @Override
    public void remove(Object entity)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.remove(" + entity + ");\n" + toString());

        getEMInvocationInfo(true).remove(entity);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#refresh(java.lang.Object)
     */
    @Override
    public void refresh(Object entity)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.refresh(" + entity + ");\n" + toString());

        getEMInvocationInfo(true).refresh(entity);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.find(" + entityClass + ',' + primaryKey + ");\n" + toString());

        return getEMInvocationInfo(false).find(entityClass, primaryKey);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getReference(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getReference(" + entityClass + ',' + primaryKey + ");\n" + toString());

        return getEMInvocationInfo(false).getReference(entityClass, primaryKey);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#flush()
     */
    @Override
    public void flush()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.flush();\n" + toString());

        getEMInvocationInfo(true).flush();
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#setFlushMode(javax.persistence.FlushModeType)
     */
    @Override
    public void setFlushMode(FlushModeType flushMode)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.setFlushMode(" + flushMode + ");\n" + toString());

        getEMInvocationInfo(false).setFlushMode(flushMode);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getFlushMode()
     */
    @Override
    public FlushModeType getFlushMode()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getFlushMode() + getEmDebugString();\n" + toString());

        return getEMInvocationInfo(false).getFlushMode();
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#lock(java.lang.Object, javax.persistence.LockModeType)
     */
    @Override
    public void lock(Object entity, LockModeType lockMode)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.lock(" + entity + ',' + lockMode + ");\n" + toString());

        getEMInvocationInfo(true).lock(entity, lockMode);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#clear()
     */
    @Override
    public void clear()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.clear();\n" + toString());

        getEMInvocationInfo(false).clear();
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object entity)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.contains(" + entity + ");\n" + toString());

        return getEMInvocationInfo(false).contains(entity);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createQuery(java.lang.String)
     */
    @Override
    public Query createQuery(String qlString)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createQuery(" + qlString + ");\n" + toString());

        return getEMInvocationInfo(false).createQuery(qlString);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNamedQuery(java.lang.String)
     */
    @Override
    public Query createNamedQuery(String name)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createNamedQuery(" + name + ");\n" + toString());

        return getEMInvocationInfo(false).createNamedQuery(name);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String)
     */
    @Override
    public Query createNativeQuery(String sqlString)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createNativeQuery(" + sqlString + ");\n" + toString());

        return getEMInvocationInfo(false).createNativeQuery(sqlString);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String sqlString, Class resultClass)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createNativeQuery(" + sqlString + ',' + resultClass + ");\n" + toString());

        return getEMInvocationInfo(false).createNativeQuery(sqlString, resultClass);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.String)
     */
    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createNativeQuery(" + sqlString + ',' + resultSetMapping + ");\n" + toString());

        return getEMInvocationInfo(false).createNativeQuery(sqlString, resultSetMapping);
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#joinTransaction()
     */
    @Override
    public void joinTransaction()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.joinTransaction();\n" + toString());

        getEMInvocationInfo(true).joinTransaction();
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getDelegate()
     */
    @Override
    public Object getDelegate()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getDelegate();\n" + toString());

        return getEMInvocationInfo(false).getDelegate();
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#close()
     */
    @Override
    public void close()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.close();\n" + toString());

        // JPA 5.9.1 Container Responsibilities
        // The container must throw the IllegalStateException if the application calls
        //  EntityManager.close on a container-managed entity manager.
        throw new IllegalStateException("Can not close EntityManager session in container-managed entity manager (JPA 5.9.1).");
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#isOpen()
     */
    @Override
    public boolean isOpen()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.isOpen();\n" + toString());

        return getEMInvocationInfo(false).isOpen();
    }

    /**
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getTransaction()
     */
    @Override
    public EntityTransaction getTransaction()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getTransaction();\n" + toString());

        return getEMInvocationInfo(false).getTransaction();
    }

    // New JPA 2.0 API methods //F743-954, F743-954.1 d597764

    @Override
    public void detach(Object entity) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.detach(" + entity + ");\n" + toString());

        getEMInvocationInfo(false).detach(entity);
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createQuery();\n" + toString());

        return getEMInvocationInfo(false).createQuery(arg0);
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.find();\n" + toString());

        return getEMInvocationInfo(false, arg2).find(arg0, arg1, arg2);
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
                      Map<String, Object> arg3) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.find();\n" + toString());

        return getEMInvocationInfo(false, arg2).find(arg0, arg1, arg2, arg3);
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.find();\n" + toString());

        return getEMInvocationInfo(false).find(arg0, arg1, arg2);
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getEntityManagerFactory();\n" + toString());

        return getEMInvocationInfo(false).getEntityManagerFactory();
    }

    @Override
    public LockModeType getLockMode(Object arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getLockMode();\n" + toString());

        return getEMInvocationInfo(true).getLockMode(arg0);
    }

    @Override
    public Map<String, Object> getProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getProperties();\n" + toString());

        return getEMInvocationInfo(false).getProperties();
    }

    @Override
    public void setProperty(String arg0, Object arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.setProperties();\n" + toString());

        getEMInvocationInfo(false).setProperty(arg0, arg1);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getQueryBuilder();\n" + toString());

        return getEMInvocationInfo(false).getCriteriaBuilder();
    }

    @Override
    public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.lock();\n" + toString());

        getEMInvocationInfo(true).lock(arg0, arg1, arg2);
    }

    @Override
    public void refresh(Object arg0, LockModeType arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.refresh();\n" + toString());

        getEMInvocationInfo(true, arg1).refresh(arg0, arg1);
    }

    @Override
    public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.refresh();\n" + toString());

        getEMInvocationInfo(true, arg1).refresh(arg0, arg1, arg2);
    }

    @Override
    public void refresh(Object arg0, Map<String, Object> arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.refresh();\n" + toString());

        getEMInvocationInfo(true).refresh(arg0, arg1);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.unwrap();\n" + toString());

        return getEMInvocationInfo(false).unwrap(arg0);
    }

    @Override
    public Metamodel getMetamodel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.getMetaModel();\n" + toString());

        return getEMInvocationInfo(false).getMetamodel();
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createNamedQuery();\n" + toString());

        return getEMInvocationInfo(false).createNamedQuery(arg0, arg1);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "em.createQuery();\n" + toString());

        return getEMInvocationInfo(false).createQuery(arg0, arg1);
    }
}
