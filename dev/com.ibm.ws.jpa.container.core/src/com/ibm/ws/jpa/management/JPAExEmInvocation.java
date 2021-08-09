/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
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
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * The per-transaction EntityManager proxy for a persistence unit.
 * 
 * Extend-scoped invocation is the super-class of transaction-scoped invocation because extend-scoped has a
 * simpler invocation pattern whereas transaction-scoped need to ensure the persistence context is cleared
 * on each method invocation when it is run under no JTA transaction.
 */
public class JPAExEmInvocation implements EntityManager
{
    private static final TraceComponent tc = Tr.register(JPAExEmInvocation.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Scope key for this em invocation object to identify transaction and tx-mapping info.
    protected UOWCoordinator ivUOWCoord;

    // Provider Entity Manager associated to a transaction within this context.
    protected EntityManager ivEm;

    /**
     * The JPA EntityManager that created this invocation object, or null if
     * this invocation was created for an extended persistence context when a
     * stateful session bean was enlisted in a transaction.
     */
    protected JPAEntityManager ivJpaEm;

    /**
     * Whether this EntityManager does not automatically joins the active JTA transaction when invoked.
     * By default this is set to false (meaning the EntityManager will automatically join the active JTA transaction.)
     * 
     * In JPA 2.1 or later this value can be set with a @PersistenceContext "synchronization" field value:
     * -- SynchronizationType.SYNCHRONIZED (default) will set it to a value of false.
     * -- SynchronizationType.UNSYNCHRONIZED will set it to a value of true
     * 
     */
    protected boolean ivUnsynchronized;

    /**
     * Constructor for Serialization, mainly used by SFSB passivation process.
     */
    public JPAExEmInvocation()
    {
        // Intentionally left blank - for serialization
    }

    /**
     * Constructor.
     */
    protected JPAExEmInvocation(UOWCoordinator uowCoord, EntityManager em, JPAEntityManager jpaEm, boolean txUnsynchronized)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> : tid = " + JPATxEntityManager.txIdToString(uowCoord) +
                         ", ivEm = " + em + ", ivJpaEm = " + jpaEm + ", txUnsynchronized = " + txUnsynchronized);

        ivUOWCoord = uowCoord;
        ivEm = em;
        ivJpaEm = jpaEm;
        ivUnsynchronized = txUnsynchronized;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    /**
     * Returns true is the current invocation is in a JTA transaction.
     */
    protected final boolean inJTATransaction()
    {
        return ivUOWCoord.isGlobal();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "{" + this.getClass().getSimpleName() + ":" +
               JPATxEntityManager.txIdToString(ivUOWCoord) + ", em=" + ivEm + ", JPAEM=" + ivJpaEm + '}';
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#clear()
     */
    @Override
    public void clear()
    {
        ivEm.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#close()
     */
    @Override
    public void close()
    {
        // JPA 5.9.1 Container Responsibilities
        // The container must throw the IllegalStateException if the application calls
        //  EntityManager.close on a container-managed entity manager.
        throw new IllegalStateException("Can not close EntityManager session in container-managed entity manager (JPA 5.9.1).");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object entity)
    {
        return ivEm.contains(entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNamedQuery(java.lang.String)
     */
    @Override
    public Query createNamedQuery(String name)
    {
        return ivEm.createNamedQuery(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String)
     */
    @Override
    public Query createNativeQuery(String sqlString)
    {
        return ivEm.createNativeQuery(sqlString);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.Class)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Query createNativeQuery(String sqlString, Class resultClass)
    {
        return ivEm.createNativeQuery(sqlString, resultClass);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.String)
     */
    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping)
    {
        return ivEm.createNativeQuery(sqlString, resultSetMapping);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createQuery(java.lang.String)
     */
    @Override
    public Query createQuery(String qlString)
    {
        return ivEm.createQuery(qlString);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey)
    {
        return ivEm.find(entityClass, primaryKey);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#flush()
     */
    @Override
    public void flush()
    {
        ivEm.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getDelegate()
     */
    @Override
    public Object getDelegate()
    {
        return ivEm.getDelegate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getFlushMode()
     */
    @Override
    public FlushModeType getFlushMode()
    {
        return ivEm.getFlushMode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getReference(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey)
    {
        return ivEm.getReference(entityClass, primaryKey);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getTransaction()
     */
    @Override
    public EntityTransaction getTransaction()
    {
        return ivEm.getTransaction();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#isOpen()
     */
    @Override
    public boolean isOpen()
    {
        return ivEm.isOpen();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#joinTransaction()
     */
    @Override
    public void joinTransaction()
    {
        ivEm.joinTransaction();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#lock(java.lang.Object, javax.persistence.LockModeType)
     */
    @Override
    public void lock(Object entity, LockModeType lockMode)
    {
        ivEm.lock(entity, lockMode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#merge(java.lang.Object)
     */
    @Override
    public <T> T merge(T entity)
    {
        // no need to set up try/finally in tx-scoped to clear persistence context since this
        // method require active transaction and is checked in getEMInvocationInfo.
        return ivEm.merge(entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#persist(java.lang.Object)
     */
    @Override
    public void persist(Object entity)
    {
        // no need to set up try/finally in tx-scoped to clear persistence context since this
        // method require active transaction and is checked in getEMInvocationInfo.
        ivEm.persist(entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#refresh(java.lang.Object)
     */
    @Override
    public void refresh(Object entity)
    {
        // no need to set up try/finally in tx-scoped to clear persistence context since this
        // method require active transaction and is checked in getEMInvocationInfo.
        ivEm.refresh(entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#remove(java.lang.Object)
     */
    @Override
    public void remove(Object entity)
    {
        // no need to set up try/finally in tx-scoped to clear persistence context since this
        // method require active transaction and is checked in getEMInvocationInfo.
        ivEm.remove(entity);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#setFlushMode(javax.persistence.FlushModeType)
     */
    @Override
    public void setFlushMode(FlushModeType flushMode)
    {
        ivEm.setFlushMode(flushMode);
    }

    // New JPA 2.0 API methods    //F743-954, F743-954.1 d597764

    @Override
    public void detach(Object arg0) {
        ivEm.detach(arg0);
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        return ivEm.createQuery(arg0);
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        return ivEm.find(arg0, arg1, arg2);
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
                      Map<String, Object> arg3) {
        return ivEm.find(arg0, arg1, arg2, arg3);
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        return ivEm.find(arg0, arg1, arg2);
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return ivEm.getEntityManagerFactory();
    }

    @Override
    public LockModeType getLockMode(Object arg0) {
        return ivEm.getLockMode(arg0);
    }

    @Override
    public Map<String, Object> getProperties() {
        return ivEm.getProperties();
    }

    @Override
    public void setProperty(String arg0, Object arg1) {
        ivEm.setProperty(arg0, arg1);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return ivEm.getCriteriaBuilder();
    }

    @Override
    public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        ivEm.lock(arg0, arg1, arg2);
    }

    @Override
    public void refresh(Object arg0, LockModeType arg1) {
        ivEm.refresh(arg0, arg1);
    }

    @Override
    public void refresh(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
        ivEm.refresh(arg0, arg1, arg2);
    }

    @Override
    public void refresh(Object arg0, Map<String, Object> arg1) {
        ivEm.refresh(arg0, arg1);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) {
        return ivEm.unwrap(arg0);
    }

    @Override
    public Metamodel getMetamodel() {
        return ivEm.getMetamodel();
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        return ivEm.createNamedQuery(arg0, arg1);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        return ivEm.createQuery(arg0, arg1);
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
}
