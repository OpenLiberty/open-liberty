/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.Synchronization;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * Invocation object used by JPAEntityManager to dispatch
 * non-transaction-scoped or local-transaction-scoped EntityManager interface method calls.
 * Each EntityManager defined in the application, either using dependency injection or JNDI lookup, has one
 * of this invocation associated to it. Hence there is also a provider EntityManager (em) associated to this
 * innvocation object.
 */
public class JPANoTxEmInvocation extends JPAExEmInvocation implements Synchronization
{
    private static final TraceComponent tc = Tr.register(JPANoTxEmInvocation.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    protected boolean ivAllowPooling = true;

    /**
     * Constructor.
     */
    protected JPANoTxEmInvocation(UOWCoordinator uowCoord, EntityManager em, JPAEntityManager jpaEm, boolean txIsUnsynchronized) {
        super(uowCoord, em, jpaEm, txIsUnsynchronized);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "JPANoTxEmInvocation created");
    }

    @Override
    public void beforeCompletion()
    {
        // Nothing.
    }

    /*
     * This afterCompletion method will be called when the Unit of Work
     * (LTC or ActivitySession) that this Invocation context has been
     * registered with commits or is rolled back. At this point we can
     * be sure that the Entity Manager and Broker are no longer needed and
     * can be pooled or closed.
     * 
     * @see javax.transaction.Synchronization#afterCompletion(int)
     */
    @Override
    public void afterCompletion(int status)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "afterCompletion : " + status + " : " + this);

        ivJpaEm.closeNoTxEntityManager(this);
        ivEm = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "afterCompletion");
    }

    // 5.6.1 Container-managed Transaction-scoped Persistence Context
    // If the entity manager is invoked outside the scope of a transaction, any entities
    // loaded from the database will immediately become detached at the end of the
    // method call.
    //
    // 5.9.1 Container Responsibilities
    // [39] The container may choose to pool EntityManagers and instead of creating and
    // closing in each case acquire one from its pool and call clear() on it.
    //
    // For methods in the EntityManager interface that loaded entities from the database,
    // check must be made to determined if it is invoked not in a active JTA transaction
    // and clear the persistence context accordingly.

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
     * @see javax.persistence.EntityManager#getDelegate()
     */
    @Override
    public Object getDelegate()
    {
        ivAllowPooling = false;
        return ivEm.getDelegate();
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
     * @see javax.persistence.EntityManager#getReference(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey)
    {
        return ivEm.getReference(entityClass, primaryKey);
    }
}
