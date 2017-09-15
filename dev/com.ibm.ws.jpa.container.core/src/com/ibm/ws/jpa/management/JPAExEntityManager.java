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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAExPcBindingContext;
import com.ibm.ws.jpa.JPAExPcBindingContextAccessor;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;
import com.ibm.wsspi.injectionengine.InjectionBinding;

/**
 * Extend-scoped Entity Manager proxy makes available for application using either
 * 
 * @PersistenceContext(type=EXTENDED) dependency injection or JNDI lookup.
 */
public class JPAExEntityManager extends JPATxEntityManager
{
    private static final long serialVersionUID = 8772611731925961602L;

    private static final TraceComponent tc = Tr.register(JPAExEntityManager.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    // Reusable JPAPuId arrays indicates there is no extend-scoped persistence declared in a component.
    private static final JPAPuId NoPuIds[] = new JPAPuId[0];

    // SFSB binding info map. It keeps track of SFSB instances that are bound to a persistence context.
    // This will only be used by extend-scoped persistence contexts.
    private static final Map<ExPcBindingKey, ExPcBindingInfo> svExPcBindingMap =
                    new ConcurrentHashMap<ExPcBindingKey, ExPcBindingInfo>();

    /**
     * Constructor.
     */
    public JPAExEntityManager(JPAPuId puRefId,
                              JPAPUnitInfo puInfo,
                              J2EEName j2eeName,
                              String refName,
                              Map<?, ?> properties,
                              boolean isUnsynchronized,
                              AbstractJPAComponent abstractJPAComponent)
    {
        super(puRefId, puInfo, j2eeName, refName, properties, isUnsynchronized, abstractJPAComponent);
    }

    @Override
    protected Object readResolve() {
        // If necessary, create a JPARuntime for an uplevel JPA version.
        JPARuntime jpaRuntime = ivAbstractJPAComponent.getJPARuntime();
        return jpaRuntime.isDefault() ? this : jpaRuntime.createJPAExEntityManager(ivPuRefId, ivPuInfo, ivJ2eeName, ivRefName, ivProperties, ivUnsynchronized,
                                                                                   ivAbstractJPAComponent);
    }

    private static ExPcBindingInfo getExPcBindingInfo(ExPcBindingKey exPcBindingKey) {
        ExPcBindingInfo exPcInfo = svExPcBindingMap.get(exPcBindingKey);
        if (exPcInfo == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getExPcBindingInfo : Inconsistent internal error.");

            // Assert this should never happend, just to enable easier problem determination.
            throw new RuntimeException("Can not find " + exPcBindingKey
                                       + " in SFSB extend-scoped entity manager association cache.");
        }
        return exPcInfo;
    }

    /**
     * JPA 3.3 Persistence Context Lifetime
     * 
     * When an EntityManager with an extended persistence context is used, the persist, remove,
     * merge, and refresh operations may be called regardless of whether a transaction is active.
     * The effects of these operations will be committed to the database when the extended
     * persistence context is enlisted in a transaction and the transaction commits.
     * 
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.management.JPAEntityManager#getEMInvocationInfo(boolean,LockModeType)
     */
    @Override
    EntityManager getEMInvocationInfo(boolean requireTx, LockModeType mode)
    {
        // Obtain the extended-scoped persistence context binding accessor from
        // the JPA Service, which was registered by the EJB Container.     d515803
        JPAExPcBindingContextAccessor exPcAccessor = ivAbstractJPAComponent.getExPcBindingContext(); // F743-18776

        // This should never occur, but if it somehow does, then just throw
        // a meaningful exception to let us know what occurred.            d515803
        if (exPcAccessor == null)
        {
            throw new IllegalStateException("The EJB container has not registered an extended-scoped persistence context binding context accessor with the JPA service.  An accessor is required to provide extended-scoped persistence context management for stateful beans.");
        }

        JPAExPcBindingContext currentContext = exPcAccessor.getExPcBindingContext();

        //TODO: ejbteam:  The currentContext may be null when attempting to use a container managed extended
        // persistence context from an asynchbeans thread.   This will need to be fixed
        // in Pyxis, but until then we will throw a more meaningful exception. //473813
        if (currentContext == null)
        {
            Tr.error(tc, "NO_COMPONENT_CONTEXT_CWWJP0038E", ivPuRefId.getPuName()); // d479669

            throw new RuntimeException("The component context does not exist on the thread." +
                                       "  The server run time environment is not able to associate the operation" +
                                       " thread with any Java Platform, Enterprise Edition 5 (Java EE 5)" +
                                       " application component.  This condition can occur when the client attempts" +
                                       " EntityManager operations on a non-server application thread." +
                                       "  Make sure that a Java EE 5 application does not execute EntityManager" +
                                       " operations within static code blocks or in threads that are created by" +
                                       " the Java EE application.");
        }

        UOWCoordinator uowCoord = ivAbstractJPAComponent.getUOWCurrent().getUOWCoord();
        SynchronizationRegistryUOWScope uowSyncRegistry = getSynchronizationRegistryUOWScope(uowCoord);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getEMInvocationInfo", "tid=" + txIdToString(uowCoord), currentContext.thisToString());

        boolean globalTx = uowCoord.isGlobal();

        // Begins d597764
        if (!globalTx && mode != null && !LockModeType.NONE.equals(mode))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getEMInvocationInfo : TransactionRequiredException: " +
                            "No active transaction for " + ivPuRefId);

            throw new TransactionRequiredException("No active transaction for " + ivPuRefId);
        }
        // Ends d597764

        // JPA 5.6.3.1 Requirements for Persistence Context Propagation
        //
        // Persistence contexts are propagated by the container across component invocations as follows.
        //
        // If a component is called and there is no JTA transaction or the JTA transaction is not
        // propagated, the persistence context is not propagated.
        // * If an entity manager is then invoked from within the component:
        // ** Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will
        //      result in use of a new persistence context (as described in section 5.6.1).
        // ** Invocation of an entity manager defined with PersistenceContext-Type.EXTENDED will
        //      result in the use of the existing extended persistence context bound to that component.
        // ** If the entity manager is invoked within a JTA transaction, the persistence context will
        //      be bound to the JTA transaction.
        //
        // If a component is called and the JTA transaction is propagated into that component:
        // * If the component is a stateful session bean to which an extended persistence context
        //      has been bound and there is a different persistence context bound to the JTA transaction,
        //      an EJBException is thrown by the container.
        // * (JPA 2.1: 7.6.4.1) If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED 
        //      associated with the JTA transaction and the target component specifies a persistence context of type 
        //      SynchronizationType.SYNCHRONIZED, the IllegalStateException is thrown by the container.
        // * Otherwise, if there is a persistence context bound to the JTA transaction, that
        //      persistence context is propagated and used.
        //
        // (JPA 2.1: 7.6.4.1) Note that a component with a persistence context of type  
        // SynchronizationType.UNSYNCHRONIZED may be called by a component propagating either a persistence context of  
        // type SynchronizationType.UNSYNCHRONIZED or a persistence context of type SynchronizationType.SYNCHRONIZED 
        // into it.

        // JPA 5.9.1 Container Responsibilities
        //
        // For stateful session beans with extended persistence contexts:
        // * The container creates an entity manager by calling
        //   EntityManagerFactory.createEntityManager when a stateful session bean is created that
        //   declares a dependency on an entity manager with PersistenceContextType.EXTENDED.
        //   (See section 5.6.2).
        // * The container closes the entity manager by calling EntityManager.close after the
        //   stateful session bean and all other stateful session beans that have inherited the
        //   same persistence context as the EntityManager have been removed.

        // ExPC is different than TxPC on how the em is determined. However the transaction propagation
        //  semantics may be required to handle mix Tx/Em Pc invocations and ExPc exception rules.

        ExPcBindingKey exPcBindingKey = new ExPcBindingKey(currentContext.getBindId(), ivPuRefId);
        ExPcBindingInfo exPcInfo = getExPcBindingInfo(exPcBindingKey);

        // creation of provider EntityManager is deferred until first use.
        if (exPcInfo.em == null)
        {
            exPcInfo.em = ivEntityManagerPool.getEntityManager(globalTx, ivUnsynchronized); // d510184
            exPcInfo.txKeyPuId = ivTxKeyPuId;
        }
        EntityManager em = exPcInfo.em;

        if (globalTx)
        {
            JPAExEmInvocation invocationEm = getInvocation(uowSyncRegistry, ivTxKeyPuId); // d683375, d689596
            if (invocationEm == null)
            {
                invocationEm = ivAbstractJPAComponent.getJPARuntime().createExEmInvocation(uowCoord, em, ivUnsynchronized);
                setInvocation(uowSyncRegistry, ivTxKeyPuId, invocationEm); // d683375, d689596

                // JPA 5.9.1 Container Responsibilities
                //
                // For stateful session beans with extended persistence contexts:
                // ** (JPA 2.1: 7.9.1) When a business method of the stateful session bean is invoked, if the stateful 
                //   session bean uses container managed transaction demarcation, and the entity manager is not already 
                //   associated with the current JTA transaction, the container associates the entity manager with the 
                //   current JTA transaction and, if the persistence context is of type SynchronizationType.SYNCHRONIZED, 
                //   the container calls EntityManager.joinTransaction. If there is a different persistence context 
                //   already associated with the JTA transaction, the container throws the EJBException.
                // ** (JPA 2.1: 7.9.1) When a business method of the stateful session bean is invoked, if the stateful 
                //   session bean uses bean managed transaction demarcation and a UserTransaction is begun within the 
                //   method, the container associates the persistence context with the JTA transaction and, if the 
                //   persistence context is of type SynchronizationType.SYNCHRONIZED, the container calls 
                //   EntityManager.joinTransaction. 
                boolean isCmt = !currentContext.isBmt();
                boolean isBmtNUserTxBegunInMethod = currentContext.isBmt()
                                                    && currentContext.hasBmtUserTxBegunInMethod();
                if (!ivUnsynchronized && (isCmt || isBmtNUserTxBegunInMethod))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "CMT=" + isCmt + ", UserTxBegun=" + isBmtNUserTxBegunInMethod + " -> em=" + em);
                    em.joinTransaction();
                }
            }
            else
            {
                // JPA 5.9.1 Container Responsibilities
                //
                // For stateful session beans with extended persistence contexts:
                // ** If there is a different persistence context already associated with the JTA
                //    transaction, the container throws the EJBException.
                if (invocationEm.ivEm != em)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getEMInvocationInfo : Different persistence context already associate with a JTA tx.");
                    throw exPcAccessor.newEJBException
                                    ("5.9.1 A persistence context is associated to the current JTA transaction " +
                                     "and it is different than the extend-scoped persistence context bound to this SFSB.");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getEMInvocationInfo : " + em);
        return em;
    }

    /**
     * Determine and collect the Persistence Unit Ids associated to @ PersistencContext(type=Extended)
     * declared in the input injection meta data. It will also populate the Map provided by a
     * argument bindingMap with a set of PC-JNDI-Names acting as keys that map to the instance of
     * JPAPCtxtInjectionBinding associated with the PC-JNDI name.
     * 
     * <ul>
     * <li>Use ComponentMetaData instead of BeanMetaData to avoid build time circular dependency.
     * <li>This method should only need to called from SFSB creation processing and stored in the
     * component metadata, hence asserts cmd must be BeanMetaData.
     * </ul>
     * 
     * @param injectionBindings
     *            InjectionBindings to retrieve PuIds from.
     * @param ejbName
     *            The EJB name.
     * @param persistenceRefNames
     *            The set of persistence unit and context reference names to be
     *            updated, or <tt>null</tt> if not needed
     * @param bindingList
     *            A return-value List containing JPAPCtxtInjectionBinding objects.
     *            If bindingList is not null, then for each @PersistenceContext associated with each
     *            InjectionBinding, it will insert an JPAPCtxtInjectionBinding type entry to the List.
     *            If null, this data will not be returned.
     * @return Returns array of JPAPuId with container-managed extend-scoped persistence contexts;
     *         or JPAPuId[0] if none is found; updates bindingMap with JNDI -> JPAPCtxtInjectionBinding
     *         entries.
     */
    // switch to 'bindings' d511673
    static JPAPuId[] getExtendedContextPuIds(Collection<InjectionBinding<?>> injectionBindings,
                                             String ejbName,
                                             Set<String> persistenceRefNames,
                                             List<Object> bindingList)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getExtendedContextPuIds : " +
                         ((injectionBindings != null)
                                         ? ("<" + injectionBindings.size() + "> :" + injectionBindings)
                                         : "No Injection Bindings"));

        JPAPuId[] rtnValue = NoPuIds;
        if (injectionBindings != null && injectionBindings.size() > 0)
        {
            // Build a set of extended persistence contexts for the 'bindings'.
            // A set is used to eliminate duplicate JPAPuIds.
            HashSet<JPAPuId> extendedPuIds = new HashSet<JPAPuId>();
            for (InjectionBinding<?> binding : injectionBindings)
            {
                if (binding instanceof JPAPCtxtInjectionBinding)
                {
                    JPAPCtxtInjectionBinding pcBinding = (JPAPCtxtInjectionBinding) binding;
                    if (pcBinding.containsComponent(ejbName)) // F743-30682
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "extended persistence-context-ref name=" + pcBinding.getJndiName() +
                                         " contains component=" + ejbName);

                        if (pcBinding.isExtendedType())
                        {
                            // The unitName in the user-define PersistenceContext annotation may not be
                            // defined, update the puId with a valid and unique persistence unit name.
                            JPAPuId puId = pcBinding.ivPuId;
                            String puName = puId.getPuName();
                            if (puName == null || puName.length() == 0)
                            {
                                //F743-16027 - using JPAAccessor to get JPAComponent, rather than using cached (possibly stale) static reference
                                JPAPUnitInfo puInfo = ((AbstractJPAComponent) JPAAccessor.getJPAComponent()).findPersistenceUnitInfo(puId); // F743-18776
                                if (puInfo != null)
                                {
                                    puId.setPuName(puInfo.getPersistenceUnitName());
                                }
                            }
                            extendedPuIds.add(puId);

                            if (bindingList != null) {
                                bindingList.add(pcBinding);
                            }
                        }

                        if (persistenceRefNames != null) // F743-30682
                        {
                            persistenceRefNames.add(pcBinding.getJndiName());
                        }
                    }
                }
            }
            if (extendedPuIds.size() > 0)
            {
                rtnValue = extendedPuIds.toArray(new JPAPuId[extendedPuIds.size()]);
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getExtendedContextPuIds : " + rtnValue.length);
        return rtnValue;
    }

    /**
     * Scans the provided binding map for Transaction Synchronization configuration conflicts
     * (All extended scope EntityManagers assigned to a common PU must have the same synchronization)\
     * 
     * @param bindingList A List containing JPAPCtxtInjectionBinding objects associated with a SFSB.
     * @return A Set of <JPAPuId> types which denote an extended scope persistence context associated
     *         with SynchronizationType.UNSYNCHRONIZED.
     * 
     *         The method will thrown an IllegalStateException if it finds two or more mappings to a common JPAPuId
     *         and have mismatching SynchronizationType.
     */
    static Set<JPAPuId> scanForTxSynchronizationCollisions(List<Object> bindingList) {
        if (bindingList == null) {
            return new HashSet<JPAPuId>();
        }

        HashMap<JPAPuId, Boolean> knownPuIdMap = new HashMap<JPAPuId, Boolean>();
        HashSet<JPAPuId> returnSet = new HashSet<JPAPuId>();

        for (Object o : bindingList) {
            JPAPCtxtInjectionBinding injBinding = (JPAPCtxtInjectionBinding) o;

            JPAPuId puid = injBinding.ivPuId;
            Boolean isUnsynchronized = injBinding.isUnsynchronized() ? Boolean.TRUE : Boolean.FALSE;

            if (Boolean.TRUE.equals(isUnsynchronized)) {
                returnSet.add(puid);
            }

            Boolean pVal = knownPuIdMap.put(puid, isUnsynchronized);
            if (pVal != null) {
                // JPAPuId has been seen before, verify that the sync type remains the same.
                if (!pVal.equals(isUnsynchronized)) {
                    // Mismatching SynchronizationType
                    Tr.error(tc, "JPATXSYNC_INCOMPATIBLE_CWWJP0044E", puid);
                    String msgTxt = "CWWJP0044E: Multiple extended persistence context definitions of the persistence unit " +
                                    puid +
                                    " have been declared with unequal synchronization configuration.";
                    throw new IllegalStateException(msgTxt);
                }
            }
        }

        return returnSet;
    }

    /**
     * Returns a boolean indicating if Application Managed Persistence Context is in force.
     * This is determined by the existence of a PersistenceUnit annotation or xml ref.
     * 
     * @param injectionBindings
     *            to search for extend-scoped PersistenceUnit.
     * @param ejbName
     *            The EJB name.
     * @param persistenceRefNames
     *            The set of persistence unit and context reference names to be
     *            updated, or <tt>null</tt> if not needed
     * @return boolean - indicating if a PersistenceUnit is found.
     */
    // d465813                        // switch to 'bindings' d511673
    static boolean hasAppManagedPC(Collection<InjectionBinding<?>> injectionBindings,
                                   String ejbName,
                                   Set<String> persistenceRefNames)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "hasAppManagedPC : " +
                         ((injectionBindings != null)
                                         ? ("<" + injectionBindings.size() + "> :" + injectionBindings)
                                         : "No Injection Bindings"));

        boolean rtnValue = false;
        if (injectionBindings != null && injectionBindings.size() > 0)
        {
            for (InjectionBinding<?> binding : injectionBindings)
            {
                if (binding instanceof JPAPUnitInjectionBinding)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "persistence-unit-ref name=" + binding.getJndiName() +
                                     " contains component=" + ejbName);

                    JPAPUnitInjectionBinding puBinding = (JPAPUnitInjectionBinding) binding;
                    if (puBinding.containsComponent(ejbName)) // F743-30682
                    {
                        rtnValue = true;

                        if (persistenceRefNames != null) // F743-30682
                        {
                            persistenceRefNames.add(puBinding.getJndiName());
                        }
                    }
                }
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "hasAppManagedPC : " + rtnValue);
        return rtnValue;
    }

    /**
     * When SFSB instance is created (either using bean home create(), @ PersistenceContext
     * dependency injection or JNDI lookup), this method is called to determine the extend-scoped
     * persistence context inheritance and bind the SFSBs to the associated persistence context. The
     * current EJB component calling sequence is tracked and maintained by the JPAExPcBindingContext
     * object in the current thread.
     * 
     * @param j2eeName
     *            Caller's name for debug identification.
     * @param isBMT
     *            True if caller uses Bean-managed transaction.
     * @param exPcPuIds
     *            Array of PuIds declared as extend-scoped persistence context in SFSB caller.
     * @param unsynchronizedJPAPuIdSet
     *            Set of JPAPuIds that are SynchronizationType.UNSYNCHRONIZED.
     * @return A binding context object
     */
    static JPAExPcBindingContext boundSfsbToExtendedPC(String j2eeName,
                                                       boolean isBMT,
                                                       JPAPuId[] exPcPuIds,
                                                       Set<JPAPuId> unsynchronizedJPAPuIdSet,
                                                       AbstractJPAComponent abstractJPAComponent)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "boundSfsbToExtendedPC : " + j2eeName + ", isBMT=" + isBMT +
                         ", ExPcPuIds=" + Arrays.toString(exPcPuIds) +
                         ", unsynchronizedJPAPuIdSet=" + unsynchronizedJPAPuIdSet);
        if (unsynchronizedJPAPuIdSet == null) {
            unsynchronizedJPAPuIdSet = new HashSet<JPAPuId>();
        }

        // The first time an extended-scoped persistence context is created that
        // is for a BMT bean, a TX callback object must be registered to enable
        // proper joinTransaction semantics. The static flag check is just for
        // performance and does not require synchronization as the TX service
        // will ignore duplicate registers.                                d515803
        if (isBMT && !abstractJPAComponent.ivTxCallbackRegistered)
        {
            abstractJPAComponent.registerLTCCallback();
        }

        // Obtain the extended-scoped persistence context binding accessor from
        // the JPA Service, which was registered by the EJB Container.     d515803
        JPAExPcBindingContextAccessor exPcAccessor = abstractJPAComponent.getExPcBindingContext(); // F743-18776

        // This should never occur, but if it somehow does, then just throw
        // a meaningful exception to let us know what occurred.            d515803
        if (exPcAccessor == null)
        {
            throw new IllegalStateException("The EJB container has not registered an extended-scoped persistence context binding context accessor with the JPA service.  An accessor is required to provide extended-scoped persistence context management for stateful beans.");
        }

        JPAExPcBindingContext childContext = null;

        if (exPcPuIds != null && exPcPuIds.length > 0)
        {
            childContext = new JPAExPcBindingContext(j2eeName, isBMT, exPcPuIds);
            long childBindId = childContext.getBindId();

            // retrieve the caller's binding context from the thread
            JPAExPcBindingContext parentContext = exPcAccessor.getExPcBindingContext();

            //d458689 start - clean up warnings.
            boolean notInheritingParent;
            long parentBindId;
            JPAPuId parentPuIds[];
            if (parentContext == null)
            {
                notInheritingParent = true;
                parentBindId = -1;
                parentPuIds = NoPuIds;
            }
            else
            {
                notInheritingParent = false;
                parentBindId = parentContext.getBindId();
                parentPuIds = parentContext.getExPcPuIds();
            }
            // d458689 end

            boolean createNewBindingInfo = notInheritingParent || parentPuIds.length == 0;
            for (JPAPuId puId : exPcPuIds)
            {
                ExPcBindingInfo exPcInfo = null;
                ExPcBindingKey childBindingKey = new ExPcBindingKey(childBindId, puId);
                boolean txUnsynchronized = unsynchronizedJPAPuIdSet.contains(puId);
                if (createNewBindingInfo || !parentHasSameExPc(parentPuIds, puId))
                {
                    // if "non-SFSB caller or SFSB caller but has no ExPC declared" or
                    // does not have any extended PC in parent SFSB
                    exPcInfo = new ExPcBindingInfo(childBindingKey, txUnsynchronized);
                } else
                {
                    ExPcBindingKey parentBindingKey = new ExPcBindingKey(parentBindId, puId);
                    exPcInfo = svExPcBindingMap.get(parentBindingKey);

                    // Ensure tx synchronization type is the same.
                    if (!(txUnsynchronized == exPcInfo.isTxUnsynchronized())) {
                        Tr.error(tc, "JPATXSYNC_INCOMPATIBLE_INHERITANCE_CWWJP0045E", puId);
                        String msgTxt = "CWWJP0045E: A superclass has injected an extended persistence context for persistence unit " +
                                        puId +
                                        " that has a synchronization attribute incompatible with an extended persistence context injection in a subclass.";
                        throw new IllegalStateException(msgTxt);
                    }

                    exPcInfo.addNewlyBoundSfsb(childBindingKey);
                }
                svExPcBindingMap.put(childBindingKey, exPcInfo);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Added PuId:" + puId, exPcInfo);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "[BindId=" + parentBindId + "] --instantiate--> [BindId="
                             + childBindId + "]  exPcBindingMap size=" + svExPcBindingMap.size(),
                         toStringPuIds("ExPC declared in caller component", parentPuIds),
                         toStringPuIds("ExPC declared in callee component", exPcPuIds),
                         svExPcBindingMap);
            }
        } else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "No ExPc processing is needed.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "boundSfsbToExtendedPC : " +
                        (childContext != null ? childContext.thisToString() : "null"));

        return childContext;
    }

    /**
     * When a container-managed stateful session bean is enlisted in a
     * transaction, this method is called to join all its EntityManager, if
     * any, with the transaction.
     */
    static void joinExtendedPC(JPAExPcBindingContext bindingContext, AbstractJPAComponent jpaComponent) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "enlistExtendedPC: " + bindingContext.thisToString());

        if (bindingContext.isBmt()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "enlistExtendedPC: incorrectly called with BMT context");
            throw new IllegalArgumentException();
        }

        UOWCoordinator uowCoord = jpaComponent.getUOWCurrent().getUOWCoord();
        if (!uowCoord.isGlobal()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "enlistExtendedPC: incorrectly called outside global tx");
            throw new IllegalStateException();
        }

        SynchronizationRegistryUOWScope uowSyncRegistry = getSynchronizationRegistryUOWScope(uowCoord);

        // New this binding key only once and update the puid in the loop
        ExPcBindingKey bindingKey = new ExPcBindingKey(bindingContext.getBindId(), null);
        for (JPAPuId puId : bindingContext.getExPcPuIds()) {
            bindingKey.puId = puId;
            ExPcBindingInfo exPcBindingInfo = getExPcBindingInfo(bindingKey);
            boolean txUnsynchronized = exPcBindingInfo.isTxUnsynchronized();

            if (exPcBindingInfo.em == null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "not joining unused lazy persistence context");

                // Per spec, we should associate an invocation with the
                // transaction to either give an error now if an incompatible
                // EM is already associated with the transaction, to give an
                // error if a subsequent SFSB is enlisted with a different ExPC,
                // or to ensure that this ExPC is used by all EMs (ExPC or
                // otherwise) that access the PU from this transaction.  We have
                // never done this eager enlistment (and instead done lazy
                // enlistment and lazy EM acquisition), so we can't change now
                // without risking a change in behavior.
            } else {
                JPAExEmInvocation invocationEm = getInvocation(uowSyncRegistry, exPcBindingInfo.txKeyPuId);
                if (invocationEm == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "joining " + exPcBindingInfo.txKeyPuId);

                    invocationEm = jpaComponent.getJPARuntime().createExEmInvocation(uowCoord, exPcBindingInfo.em, txUnsynchronized);
                    setInvocation(uowSyncRegistry, exPcBindingInfo.txKeyPuId, invocationEm);

                    // JPA 5.9.1 Container Responsibilities
                    //
                    // For stateful session beans with extended persistence contexts:
                    // ** When a business method of the stateful session bean is invoked, if the stateful
                    //    session bean uses container managed transaction demarcation, and the entity
                    //    manager is not already associated with the current JTA transaction, the
                    //    container associates the entity manager with the current JTA transaction and
                    //    calls EntityManager.joinTransaction.

                    // Verify that the tx synchronizations are compatible
                    if (txUnsynchronized != invocationEm.ivUnsynchronized) {
                        Tr.error(tc, "JPATXSYNC_ILLEGAL_PROPAGATION_CWWJP0046E");
                        String msgTxt = "CWWJP0046E: An UNSYNCHRONIZED JPA persistence context cannot be propagated into a SYNCHRONIZED EntityManager.";
                        throw new IllegalStateException(msgTxt);
                    }
                    if (!txUnsynchronized) {
                        exPcBindingInfo.em.joinTransaction();
                    }
                } else {
                    // JPA 5.9.1 Container Responsibilities
                    //
                    // For stateful session beans with extended persistence contexts:
                    // ** If there is a different persistence context already associated with the JTA
                    //    transaction, the container throws the EJBException.
                    if (invocationEm.ivEm != exPcBindingInfo.em) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            Tr.exit(tc, "enlistExtendedPC : Different persistence context already associate with a JTA tx.");
                        throw jpaComponent.getExPcBindingContext().newEJBException
                                        ("5.9.1 A persistence context is associated to the current JTA transaction " +
                                         "and it is different than the extend-scoped persistence context bound to this SFSB.");
                    }

                    // Verify that the tx synchronizations are compatible
                    if (txUnsynchronized != invocationEm.ivUnsynchronized) {
                        Tr.error(tc, "JPATXSYNC_INCOMPATIBLE_PROPAGATION_CWWJP0047E",
                                 exPcBindingInfo.txKeyPuId,
                                 (invocationEm.ivUnsynchronized ? "UNSYNCHRONIZED" : "SYNCHRONIZED"),
                                 bindingContext.getName());

                        StringBuilder sb = new StringBuilder();
                        sb.append("CWWJP0047E: The persistence context associated with persistence unit '");
                        sb.append(exPcBindingInfo.txKeyPuId);
                        sb.append("' has SynchronizationType of type ");
                        sb.append((invocationEm.ivUnsynchronized ? "UNSYNCHRONIZED" : "SYNCHRONIZED"));
                        sb.append(", which is incompatible with the extended persistence context ");
                        sb.append("declared by ");
                        sb.append(bindingContext.getName());
                        throw new IllegalStateException(new String(sb));
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "enlistExtendedPC");
    }

    /**
     * When SFSB instances are removed or discard, this method is called to unbind the SFSB from the
     * associated persistence context. When the last SFSB is removed from the bound collection, the
     * associated EntityManager is closed.
     * 
     * @param bindingContext
     *            to be unbound
     */
    static void unboundSfsbFromExtendedPC(JPAExPcBindingContext bindingContext)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unboundSfsbFromExtendedPC : " + bindingContext);

        JPAPuId puIds[] = bindingContext.getExPcPuIds();
        long bindId = bindingContext.getBindId();

        // New this binding key only once and update the puid in the loop
        ExPcBindingKey bindingKey = new ExPcBindingKey(bindId, null);
        for (JPAPuId puId : puIds)
        {
            bindingKey.puId = puId;
            ExPcBindingInfo exPcBindingInfo = svExPcBindingMap.remove(bindingKey);
            if (exPcBindingInfo != null)
            {
                // JPA 5.9.1 Container Responsibilities
                //
                // The container closes the entity manager by calling EntityManager.close after the
                // stateful session bean and all other stateful session beans that have inherited
                // the same persistence context as the EntityManager have been removed.
                if (exPcBindingInfo.removeRemovedOrDiscardedSfsb(bindingKey) == 0)
                {
                    EntityManager em = exPcBindingInfo.getEntityManager();
                    if (em != null)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "All SFSBs using the same extend-scoped persistence "
                                         + "context have been removed, closing EntityManager " + em);
                        if (em.isOpen()) // d442445
                            em.close();
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "unboundSfsbFromExtendedPC : exPcBindMap size=" + svExPcBindingMap.size());
    }

    /**
     * Returns true if the caller and callee have declared the same @ PersistneceContext
     * in their components.
     */
    private static final boolean parentHasSameExPc(JPAPuId parentPuIds[], JPAPuId puId)
    {
        for (JPAPuId parentPuId : parentPuIds)
        {
            if (parentPuId.equals(puId))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to convert the input puids to a String for tr.debug().
     */
    private static final String toStringPuIds(String desc, JPAPuId[] puids)
    {
        StringBuilder sbuf = new StringBuilder(desc);
        sbuf.append('\n');
        for (JPAPuId puid : puids)
        {
            sbuf.append("   ")
                            .append(puid)
                            .append('\n');
        }
        return sbuf.toString();
    }

    /**
     * Key uses to identify the association between persistence context and bound SFSBs.
     */
    static class ExPcBindingKey
    {
        // An unique value identifying an instance of a SFSB.
        long bindId;
        // Persistence unit identifier.
        JPAPuId puId;

        /*
         * Constructor.
         * Asserts both input parameters can not be null.
         */
        ExPcBindingKey(long abindId, JPAPuId apuId)
        {
            this.bindId = abindId;
            this.puId = apuId;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            return (int) (bindId ^ (bindId >>> 32)) + puId.hashCode();
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object o)
        {
            boolean rtnValue = false;
            if (o instanceof ExPcBindingKey)
            {
                rtnValue = equals((ExPcBindingKey) o);
            }
            return rtnValue;
        }

        /**
         * Overloaded version of equals(Object) specifically for ExPcBindingKey.
         */
        public boolean equals(ExPcBindingKey bindingKey)
        {
            return bindId == bindingKey.bindId && puId.equals(bindingKey.puId);
        }

        /**
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "{ExPcBindingKey: BindId=" + bindId + ", puId=" + puId + '}';
        }
    }

    /**
     * Info object encapsulates the binding relationship between persistence context
     * (i.e. EntityManager) and all the SFSBs bound to the context.
     * 
     * exPcBindingMap <------------------> ExPcBindingInfo <------> ExPcBindingKey
     * * ExPcBindingKey * 1 *
     */
    static class ExPcBindingInfo
    {
        // The EntityManager shared among all the bound extend-scoped persistence contexts
        EntityManager em;
        // The transaction key to use for the EM.
        JPAPuId txKeyPuId;
        // list of SFSBs identify by theirs ExPcBindingKey.
        List<ExPcBindingKey> bindingKeys;

        boolean txUnsynchronized;

        /*
         * Constructor
         */
        ExPcBindingInfo(ExPcBindingKey bindingKey, boolean txUnsynchronized)
        {
            addNewlyBoundSfsb(bindingKey);
            this.em = null;
            this.txUnsynchronized = txUnsynchronized;
        }

        /*
         * Getter for the associated entity manager.
         */
        EntityManager getEntityManager()
        {
            return em;
        }

        /*
         * Add a new SFSB to the persistence context binding list.
         */
        void addNewlyBoundSfsb(ExPcBindingKey bindingKey)
        {
            if (bindingKeys == null)
            {
                bindingKeys = new ArrayList<ExPcBindingKey>();
            }
            bindingKeys.add(bindingKey);
        }

        /*
         * Remove a SFSB from the persistence context binding list and return
         * the # of remaining SFSB still bound.
         */
        @SuppressWarnings("synthetic-access")
        int removeRemovedOrDiscardedSfsb(ExPcBindingKey bindingKey)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(tc, "removeRemovedOrDiscardedSfsb : " + bindingKey);

            bindingKeys.remove(bindingKey);
            int sfsbRemainsBound = bindingKeys.size();
            if (sfsbRemainsBound == 0)
            {
                bindingKeys = null;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "removeRemovedOrDiscardedSfsb : " +
                            "# SFSBs still bound to this extended context=" + sfsbRemainsBound);
            return sfsbRemainsBound;
        }

        boolean isTxUnsynchronized() {
            return txUnsynchronized;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return "{ExPcBindingInfo: em=" + em + "\n" + Arrays.toString(bindingKeys.toArray()) + '}';
        }
    }
}
