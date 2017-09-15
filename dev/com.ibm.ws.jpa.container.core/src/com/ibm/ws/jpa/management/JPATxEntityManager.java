/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
import static com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager.SYNC_TIER_OUTER;

import java.io.IOException;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Synchronization;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

/**
 * Transaction-scoped Entity Manager proxy makes available for application using either
 * PersistenceContext(type=TRANSACTION) dependency injection or JNDI lookup.
 */
public class JPATxEntityManager extends JPAEntityManager
{
    private static final long serialVersionUID = -100536503951157420L;

    private static final String CLASS_NAME = JPATxEntityManager.class.getName();

    private static final TraceComponent tc = Tr.register(JPATxEntityManager.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Persistence unit reference id.  getModJarName returns the module that
    // contains the persistence unit/context reference.
    protected JPAPuId ivPuRefId;

    // A cached reference to ivPuInfo.getArchivePuId.  This ID is used as a key
    // to locate an existing EM in a transaction.
    protected JPAPuId ivTxKeyPuId; // d689596

    // JavaEE unique identifier for the component, identifying the
    // java:comp/env context used.
    protected J2EEName ivJ2eeName; // d510184

    // Name of the PersistenceContext reference this EntityManager is for.
    protected String ivRefName; // d510184

    // EntityManager pool for the PersistenceContext reference.
    protected JPAEMPool ivEntityManagerPool; // d510184

    // Persistence unit info object extracted from persistence.xml
    protected JPAPUnitInfo ivPuInfo;

    // properties declared in @PersistenceContext annotation and/or <persistence-context-ref> xml.
    protected Map<?, ?> ivProperties;

    protected transient AbstractJPAComponent ivAbstractJPAComponent;

    /**
     * Constructor for Serialization, mainly used by SFSB passivation process.
     */
    @SuppressWarnings("unused")
    private JPATxEntityManager()
    {
        // Intentionally blank - see readObject()
    }

    JPATxEntityManager(JPAEMPool pool, AbstractJPAComponent abstractJPAComponent, boolean txIsUnsynchronized) {
        super(txIsUnsynchronized);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", pool, abstractJPAComponent, txIsUnsynchronized);

        ivEntityManagerPool = pool;
        ivAbstractJPAComponent = abstractJPAComponent;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    protected Object readResolve() {
        // If necessary, create a JPARuntime for an uplevel JPA version.
        JPARuntime jpaRuntime = ivAbstractJPAComponent.getJPARuntime();
        return jpaRuntime.isDefault() ? this : jpaRuntime.createJPATxEntityManager(ivPuRefId, ivPuInfo, ivJ2eeName, ivRefName, ivProperties, ivUnsynchronized,
                                                                                   ivAbstractJPAComponent);
    }

    /**
     * Constructor.
     */
    public JPATxEntityManager(JPAPuId puRefId,
                              JPAPUnitInfo puInfo,
                              J2EEName j2eeName,
                              String refName,
                              Map<?, ?> properties,
                              boolean isUnsynchronized,
                              AbstractJPAComponent abstractJPAComponent)
    {
        super(isUnsynchronized);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", puRefId, puInfo, j2eeName, refName, properties, abstractJPAComponent, isUnsynchronized);

        // (JPA 5.5 Controlling Transactions)
        // A container-managed entity manager must be a JTA entity manager. JTA entity managers are only specified
        //  for use in Java EE containers.
        if (puInfo.getTransactionType() != PersistenceUnitTransactionType.JTA)
        {
            throw new RuntimeException("See JPA spec 5.5: " + puRefId + " must specify JTA transaction type.");
        }
        ivPuRefId = puRefId;
        ivPuInfo = puInfo;
        ivJ2eeName = j2eeName;
        ivRefName = refName;
        ivTxKeyPuId = puInfo.getPuId(); // d689596, 

        // 5.9.1 Container Responsibilities
        // When the container creates an entity manager, it may pass a map of properties to the persistence provider
        // by using the EntityManagerFactory.createEntityManager(Map map) method. If
        // properties have been specified in the PersistenceContext annotation or the persistence-
        // context-ref deployment descriptor element, this method must be used and the map must
        // include the specified properties.
        ivProperties = properties;

        // puName in input puId may not have defined, i.e. use of default pu. To ensure puName is
        //  known by the entity manager process, populate the puName find in the scope key from
        //  puInfo instead.
        puRefId.setPuName(puInfo.getPersistenceUnitName());

        // Obtain an EntityManager pool for this PersistenceContext reference.  d510184
        ivEntityManagerPool = puInfo.getEntityManagerPool(j2eeName, refName,
                                                          properties);

        ivAbstractJPAComponent = abstractJPAComponent;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "<init>");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.management.JPAEntityManager#getEMInvocationInfo(boolean)
     */
    @Override
    protected EntityManager getEMInvocationInfo(boolean requireTx)
    {
        return getEMInvocationInfo(requireTx, null);
    }

    protected static String txIdToString(UOWCoordinator uowCoord)
    {
        if (uowCoord.isGlobal())
        {
            String string = uowCoord.toString();
            int index = string.indexOf('#');
            return index == -1 ? "NoTx" : string.substring(index + 5);
        }

        return Integer.toHexString(System.identityHashCode(uowCoord)) + "(LTC)";
    }

    protected JPATxEmInvocation createJPATxEmInvocation(UOWCoordinator uowCoord, EntityManager em) {
        return new JPATxEmInvocation(uowCoord, em, this, ivUnsynchronized);
    }

    protected JPANoTxEmInvocation createJPANoTxEmInvocation(UOWCoordinator uowCoord, EntityManager em) {
        return new JPANoTxEmInvocation(uowCoord, em, this, ivUnsynchronized);
    }

    protected static SynchronizationRegistryUOWScope getSynchronizationRegistryUOWScope(UOWCoordinator uowCoord) {
        return (SynchronizationRegistryUOWScope) uowCoord;
    }

    protected static JPAExEmInvocation getInvocation(SynchronizationRegistryUOWScope uowSyncRegistry, JPAPuId txKeyPuId) {
        return (JPAExEmInvocation) uowSyncRegistry.getResource(txKeyPuId);
    }

    protected static void setInvocation(SynchronizationRegistryUOWScope uowSyncRegistry, JPAPuId txKeyPuId, JPAExEmInvocation invocationEm) {
        uowSyncRegistry.putResource(txKeyPuId, invocationEm);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.management.JPAEntityManager#getEMInvocationInfo(TransactionRequirement,LockModeType)
     */
    @Override
    EntityManager getEMInvocationInfo(boolean requireTx, LockModeType mode)
    {
        UOWCoordinator uowCoord = ivAbstractJPAComponent.getUOWCurrent().getUOWCoord();
        SynchronizationRegistryUOWScope uowSyncRegistry = getSynchronizationRegistryUOWScope(uowCoord);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getEMInvocationInfo : " + requireTx + " : tid=" + txIdToString(uowCoord));

        boolean globalTx = uowCoord.isGlobal();

        // The container must throw the TransactionRequiredException if a transaction-scoped
        // persistence context is used, and the EntityManager persist, remove, merge, or refresh
        // method is invoked when no transaction is active.
        if (!globalTx && (requireTx || (mode != null && !LockModeType.NONE.equals(mode)))) //d597764
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getEMInvocationInfo : TransactionRequiredException: " +
                            "No active transaction for " + ivPuRefId);

            // The provider is not responsible to throw this exception for
            //  persist, remove, refresh and merge methods since these methods are supported in
            //  extend-scoped persistence context. The "container" must enforced this
            //  rule per JPA spec.
            throw new TransactionRequiredException("No active transaction for " + ivPuRefId);
        }

        // (JPA 5.6.3.1 Requirements for Persistence Context Propagation)
        // Persistence contexts are propagated by the container across component invocations as follows.
        //
        // If a component is called and there is no JTA transaction or the JTA transaction is not
        // propagated, the persistence context is not propagated.
        // * If an entity manager is then invoked from within the component:
        // * Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will
        //      result in use of a new persistence context (as described in section 5.6.1).
        // * Invocation of an entity manager defined with PersistenceContext-Type.EXTENDED will
        //      result in the use of the existing extended persistence context bound to that component.
        // * If the entity manager is invoked within a JTA transaction, the persistence context will
        //      be bound to the JTA transaction.
        //
        // If a component is called and the JTA transaction is propagated into that component:
        // * If the component is a stateful session bean to which an extended persistence context
        //      has been bound and there is a different persistence context bound to the JTA transaction,
        //      an EJBException is thrown by the container.
        // * Otherwise, if there is a persistence context bound to the JTA transaction, that
        //      persistence context is propagated and used.

        JPAExEmInvocation invocationEm = getInvocation(uowSyncRegistry, ivTxKeyPuId); // d683375, d689596
        if (invocationEm == null)
        {
            EntityManager em = ivEntityManagerPool.getEntityManager(globalTx, ivUnsynchronized); // d510184
            if (globalTx)
            {
                // 5.9.1 Container Responsibilities
                //  For the management of a transaction-scoped persistence context, if there is no
                //  EntityManager already associated with the JTA transaction:
                //  * The container creates a new entity manager by calling EntityManagerFactory.createEntityManager
                //      when the first invocation of an entity manager with PersistenceContextType.TRANSACTION
                //      occurs within the scope of a business method executing in the JTA transaction.
                //
                //  When the container creates an entity manager, it may pass a map of properties to the
                //      persistence provider by using the EntityManagerFactory.createEntityManager(Map map)
                //      method. If properties have been specified in the PersistenceContext annotation or
                //      the persistence-context-ref deployment descriptor element, this method must be
                //      used and the map must include the specified properties.

                JPATxEmInvocation txEmInvocation = createJPATxEmInvocation(uowCoord, em);

                // Register invocation object to transaction manager for clean up on commit/rollback.
                registerEmInvocation(uowCoord, txEmInvocation); //d638095.2

                invocationEm = txEmInvocation;
            }
            else
            {
                // 5.6.3.1 Requirements for Persistence Context Propagation
                // Persistence contexts are propagated by the container across component invocations as follows.
                // If a component is called and there is no JTA transaction or the JTA transaction is not propagated,
                //  the persistence context is not propagated.
                //  *   If an entity manager is then invoked from within the component:
                //      *   Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will
                //          result in use of a new persistence context (as described in section 5.6.1).
                //
                // 5.9.1 Container Responsibilities
                // [39] The container may choose to pool EntityManagers and instead of creating and
                //      closing in each case acquire one from its pool and call clear() on it.

                // For a specific UnitOfWork a new JPANoTxEmInvocation object and
                // Entity Manager instance will be created.
                // The invocation objects will be registered with the UOW and held
                // in a map keyed by the UnitOfWork id so that the same invocation
                // and entity manager are used for the entire LTC or Activity Session.
                // Both the invocation object and Entity Manager instance will be pooled
                // or closed during afterCompletion processing of the UOW.

                JPANoTxEmInvocation noTxEmInvocation = createJPANoTxEmInvocation(uowCoord, em);

                // Register invocation object to transaction manager for clean up on commit/rollback.
                // Currently registration for local transactions is not Tiered.  This may need to change
                // if conflicts arise.  //d638095.4

                LocalTransactionCoordinator ltCoord = (LocalTransactionCoordinator) uowCoord;
                ltCoord.enlistSynchronization(noTxEmInvocation); // F61571

                invocationEm = noTxEmInvocation;
            }

            setInvocation(uowSyncRegistry, ivTxKeyPuId, invocationEm); // d683375, d689596
        } else {
            // JPA 2.1: 7.6.4.1 Requirements for Persistence Context Propagation
            // If a component is called and the JTA transaction is propagated into that component:
            //   * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the 
            //     JTA transaction and the target component specifies a persistence context of type 
            //     SynchronizationType.SYNCHRONIZED, the IllegalStateException is thrown by the container.
            if (invocationEm.isTxUnsynchronized() && !isTxUnsynchronized()) {
                Tr.error(tc, "JPATXSYNC_ILLEGAL_PROPAGATION_CWWJP0046E");
                String msgTxt = "CWWJP0046E: An UNSYNCHRONIZED JPA persistence context cannot be propagated into a SYNCHRONIZED EntityManager.";
                throw new IllegalStateException(msgTxt);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getEMInvocationInfo : " + invocationEm);
        return invocationEm;
    }

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
    @Override
    void closeTxEntityManager(EntityManager em, boolean allowPooling)
    {
        if (em != null && em.isOpen())
        {
            if (allowPooling)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "closeTxEntityManager is pooling JTA em: " + em);

                ivEntityManagerPool.putEntityManager(em);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "closeTxEntityManager is closing JTA em: " + em);

                em.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.management.JPAEntityManager#poolNoTxEntityManager()
     */
    @Override
    void closeNoTxEntityManager(JPANoTxEmInvocation noTxInvocation)
    {
        EntityManager em = noTxInvocation.ivEm;
        if (em != null && em.isOpen())
        {
            // Pooling of this entity manager will only be allowed if the getDelegate()
            // method has not been invoked on the entity manager.  If it has then we will
            // need to close it instead.
            if (noTxInvocation.ivAllowPooling) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "closeNoTxEntityManager is pooling noTx em: " + em);

                ivEntityManagerPool.putEntityManager(em);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "closeNoTxEntityManager is closing noTx em: " + em);

                em.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + '@' +
               Integer.toHexString(System.identityHashCode(this)) +
               "[" + ivPuRefId + ", " + ivJ2eeName + "#" + ivRefName + "]";
    }

    /**
     * Instance serialization.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "writeObject : " + this); // d468174

        out.writeObject(ivPuRefId);
        out.writeObject(ivJ2eeName); // d510184
        out.writeObject(ivRefName); // d510184
        out.writeObject(ivProperties);
        out.writeBoolean(ivUnsynchronized);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "writeObject : " + this);
    }

    /*
     * Instance de-serialization.
     */
    private void readObject(java.io.ObjectInputStream in)
                    throws IOException,
                    ClassNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "readObject : " + this); // d468174

        ivAbstractJPAComponent = (AbstractJPAComponent) JPAAccessor.getJPAComponent();

        ivPuRefId = (JPAPuId) in.readObject();
        ivJ2eeName = (J2EEName) in.readObject(); // d510184
        ivRefName = (String) in.readObject(); // d510184
        ivProperties = (Map<?, ?>) in.readObject();
        ivUnsynchronized = in.readBoolean();

        //F743-16027 - using JPAAccessor to get JPAComponent, rather than using cached (possibly stale) static reference
        ivPuInfo = ivAbstractJPAComponent.findPersistenceUnitInfo(ivPuRefId); // F743-18776
        ivTxKeyPuId = ivPuInfo.getPuId(); // d689596

        // Obtain reference to the pool again.                             d510184
        ivEntityManagerPool = ivPuInfo.getEntityManagerPool(ivJ2eeName,
                                                            ivRefName,
                                                            ivProperties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "readObject : " + this);
    }

    /*
     * Register an Entity Manager invocation object with the current transaction.
     * We will register this object as SYNC_TIER_OUTER so that transaction events
     * will be triggered on this object after those of J2C, which will be registered as
     * SYNC_TIER_INNER. In this way J2C will clean up prior to our code pooling the
     * instances. This will avoid the potential of an EM instance being pulled from the
     * pool and used prior to J2C cleanup being performed.
     * This registration should never result in an exception. If one does occur we will
     * generate and throw a RuntimeException.
     * 
     * @throws RuntimeException
     *///d638095.4

    protected void registerEmInvocation(UOWCoordinator uowCoord, Synchronization emInvocation)
    {
        try {
            ivAbstractJPAComponent.getEmbeddableWebSphereTransactionManager().registerSynchronization(uowCoord,
                                                                                                      emInvocation,
                                                                                                      SYNC_TIER_OUTER); //d638095.4
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "registerEmInvocation experienced unexpected exception while registering with transaction: " + e);

            FFDCFilter.processException(e, CLASS_NAME + ".registerEmInvocation", "507", this);

            throw new RuntimeException("Registration of Entity Manager invocation with Transaction failed.", e);
        }
    }
}
