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
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.Synchronization;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;

/**
 * Invocation object used by JPAEntityManager to dispatch transaction-scoped EntityManager interface method calls.
 * Each EntityManager defines in the application, either using dependency injection or JNDI lookup, has one
 * of this invocation associated to it. Hence there is also a provider EntityManager (em) associated to this
 * innvocation object.
 */
public class JPATxEmInvocation extends JPAExEmInvocation implements Synchronization
{
    private static final TraceComponent tc = Tr.register(JPATxEmInvocation.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Indicates whether or not the associated provider EM may pooled or not.
    protected boolean ivPoolEM = true; // d510184

    protected JPATxEmInvocation(UOWCoordinator uowCoord, EntityManager em, JPAEntityManager jpaEm, boolean txIsUnsynchronized) {
        super(uowCoord, em, jpaEm, txIsUnsynchronized);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }

    @Override
    public void beforeCompletion()
    {
        // Nothing.
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.Synchronization#afterCompletion(int)
     */
    @Override
    public void afterCompletion(int status)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "afterCompletion : " + status + " : " + this);

        // JPA 5.9.1 Container Responsibilities
        // - After the JTA transaction has completed (either by transaction commit or rollback),
        // The container closes the entity manager by calling EntityManager.close. [39]
        //
        // [39] The container may choose to pool EntityManagers and instead of creating and
        // closing in each case acquire one from its pool and call clear() on it.

        // Note : em may be null now, if it was non-transactional.     d472866.1
        if (ivEm != null)
        {
            ivJpaEm.closeTxEntityManager(ivEm, ivPoolEM); // d510184
        }

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
        try
        {
            return ivEm.createNamedQuery(name);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String)
     */
    @Override
    public Query createNativeQuery(String sqlString)
    {
        try
        {
            return ivEm.createNativeQuery(sqlString);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
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
        try
        {
            return ivEm.createNativeQuery(sqlString, resultClass);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createNativeQuery(java.lang.String, java.lang.String)
     */
    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping)
    {
        try
        {
            return ivEm.createNativeQuery(sqlString, resultSetMapping);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#createQuery(java.lang.String)
     */
    @Override
    public Query createQuery(String qlString)
    {
        try
        {
            return ivEm.createQuery(qlString);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey)
    {
        try
        {
            return ivEm.find(entityClass, primaryKey);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getDelegate()
     */
    @Override
    public Object getDelegate()
    {
        // Once the provider EM has been handed out, it may not be pooled. d510184
        ivPoolEM = false;

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
        try
        {
            ivEm.flush();
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.persistence.EntityManager#getReference(java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey)
    {
        try
        {
            return ivEm.getReference(entityClass, primaryKey);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    // New JPA 2.0 API methods    //F743-954, F743-954.1 d597764

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> arg0) {
        try
        {
            return ivEm.createQuery(arg0);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
        try
        {
            return ivEm.find(arg0, arg1, arg2);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2,
                      Map<String, Object> arg3) {
        try {
            return ivEm.find(arg0, arg1, arg2, arg3);
        } finally
        {
            if (!inJTATransaction()) {
                ivEm.clear();
            }
        }
    }

    @Override
    public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
        try
        {
            return ivEm.find(arg0, arg1, arg2);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String arg0, Class<T> arg1) {
        try
        {
            return ivEm.createNamedQuery(arg0, arg1);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }

    @Override
    public <T> TypedQuery<T> createQuery(String arg0, Class<T> arg1) {
        try
        {
            return ivEm.createQuery(arg0, arg1);
        } finally
        {
            if (!inJTATransaction())
            {
                ivEm.clear();
            }
        }
    }
}
