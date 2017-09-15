/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.Reference;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.jpa.JPAExPcBindingContext;
import com.ibm.ws.jpa.JPAExPcBindingContextAccessor;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.injectionengine.InjectionBinding;

/**
 * Abstract JPA service implementation module. This class and the classes it
 * depends on are intended to have minimal dependencies on WAS classes.
 * Integration dependencies should be isolated to JPAComponentImpl.
 */
public abstract class AbstractJPAComponent
                implements JPAComponent
{
    private static final TraceComponent tc = Tr.register
                    (AbstractJPAComponent.class,
                     JPA_TRACE_GROUP,
                     JPA_RESOURCE_BUNDLE_NAME);

    // List of installed application in the form of JPAApplInfo objects.
    protected Map<String, JPAApplInfo> applList = Collections.synchronizedMap(new HashMap<String, JPAApplInfo>());

    protected UOWCallback ivCallBackHandler;

    // Indicates whether or not the TX callback instance has been registered.
    protected boolean ivTxCallbackRegistered = false; // d515803

    public void initialize()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        ivCallBackHandler = new JPAUserTxCallBackHandler(this);

        // add JPA component instance to the component accessor
        JPAAccessor.setJPAComponent(this); // d416151.3.5

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }

    public void destroy()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "destroy");

        applList = null;

        // add JPA component instance to the component accessor
        JPAAccessor.setJPAComponent(null); // d416151.3.5

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "destroy");
    }

    /**
     * Process application "starting" event.
     */
    protected JPAApplInfo startingApplication(JPAApplInfo applInfo) throws RuntimeWarning {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "startingApplication : " + applInfo.getApplName());
        }

        String applName = applInfo.getApplName();
        JPAApplInfo oldApplInfo = applList.get(applName);

        if (oldApplInfo == null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "added to applList - applName: " + applName + " applInfo: " + applInfo);
            }
            applList.put(applInfo.getApplName(), applInfo);
        } else {
            Tr.warning(tc, "APPL_STARTED_CWWJP0019W", applName); // d406994.1
            applInfo = oldApplInfo;
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found in applList - applName: " + applName + " applInfo: " + applInfo);
            }
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "Current application list : " + getSortedAppNames());
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "startingApplication : " + applName);
        }
        return applInfo;
    }

    /**
     * Process application "started" event.
     */
    public void startedApplication(JPAApplInfo applInfo)
                    throws RuntimeWarning // d406994.2
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "startedApplication : " + applInfo.getApplName());

        // To save footprint, if an application does not have any JPA access, remove
        // the unnecessary appInfo object from list.
        if (applInfo.getScopeSize() == 0)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "App has no JPA access - removing from applList");
            applList.remove(applInfo.getApplName());
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "Current application list : " + getSortedAppNames());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "startedApplication : " + applInfo.getApplName());
    }

    /**
     * Process application "destroying" event.
     */
    public void destroyingApplication(JPAApplInfo applInfo)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "destroyingApplication : " + applInfo.getApplName());

        applInfo.closeAllScopeModules();
        applList.remove(applInfo.getApplName()); // d643462

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "Current application list : " + getSortedAppNames());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "destroyingApplication : " + applInfo.getApplName());
    }

    /**
     * Locate the persistence unit information by application/module/unit name.
     */
    JPAPUnitInfo findPersistenceUnitInfo(JPAPuId puId)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findPersistenceUnitInfo : " + puId);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "Current application list : " + getSortedAppNames());

        JPAPUnitInfo rtnVal = null;
        JPAApplInfo applInfo = applList.get(puId.getApplName());
        if (applInfo != null)
        {
            rtnVal = applInfo.getPersistenceUnitInfo(puId.getModJarName(), puId.getPuName());
        } else
        {
            Tr.warning(tc, "APPL_NOT_FOUND_DURING_PU_LOOKUP_CWWJP0010W",
                       puId.getApplName(), puId.getModJarName(), puId.getPuName());
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "findPersistenceUnitInfo : " + rtnVal);
        return rtnVal;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.JPAComponent#getEntityManagerFactory
     */
    @Override
    public EntityManagerFactory getEntityManagerFactory
                    (JPAPuId puId,
                     J2EEName j2eeName, // d510184
                     boolean getEmfWrapper) // d416151.3.1
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEntityManagerFactory : " + puId + ", " + j2eeName);

        EntityManagerFactory rtnFactory = null;
        JPAPUnitInfo puInfo = findPersistenceUnitInfo(puId);
        if (puInfo != null)
        {
            rtnFactory = puInfo.getEntityManagerFactory(j2eeName); // d510184
            if (getEmfWrapper) // d416151.3.1 d510184
            {
                rtnFactory = getJPARuntime().createJPAEMFactory(puId, j2eeName, rtnFactory); // d416151.3.1 d510184, d706751
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEntityManagerFactory : " + rtnFactory);
        return rtnFactory;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.JPAComponent#getEntityManager
     */
    @Override
    public EntityManager getEntityManager
                    (JPAPuId puId,
                     J2EEName j2eeName, // d510184
                     String refName, // d510184
                     boolean isExtendedContextType,
                     boolean isUnsynchronized,
                     Map<?, ?> properties)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEntityManager : " + puId + ", " + j2eeName);

        EntityManager em = null;
        JPAPUnitInfo puInfo = findPersistenceUnitInfo(puId);
        if (puInfo != null)
        {
            em = isExtendedContextType ?
                            getJPARuntime().createJPAExEntityManager(puId, puInfo, j2eeName, refName, properties, isUnsynchronized, this) :
                            getJPARuntime().createJPATxEntityManager(puId, puInfo, j2eeName, refName, properties, isUnsynchronized, this);
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEntityManager : " + em);
        return em;

    }

    @Override
    public JPAExPcBindingContext onCreate(String j2eeName, boolean isBMT, JPAPuId[] puIds, Set<JPAPuId> unsynchronizedJPAPuIdSet)
    {
        return JPAExEntityManager.boundSfsbToExtendedPC(j2eeName, isBMT, puIds, unsynchronizedJPAPuIdSet, this);
    }

    @Override
    public void onEnlistCMT(JPAExPcBindingContext bindingContext)
    {
        JPAExEntityManager.joinExtendedPC(bindingContext, this);
    }

    @Override
    public void onRemoveOrDiscard(JPAExPcBindingContext bindingContext)
    {
        JPAExEntityManager.unboundSfsbFromExtendedPC(bindingContext);
    }

    public abstract Reference createPersistenceUnitReference(boolean ejbInWar,
                                                             JPAPuId puId,
                                                             J2EEName j2eeName,
                                                             String refName,
                                                             boolean isSFSB);

    public abstract Reference createPersistenceContextReference(boolean ejbInWar,
                                                                JPAPuId puId,
                                                                J2EEName j2eeName,
                                                                String refName,
                                                                boolean isExtendedContextType,
                                                                boolean isSFSB,
                                                                Properties properties,
                                                                boolean isUnsynchronized);

    /**
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.JPAComponent#getExtendedContextPuIds
     */
    // switch to 'bindings' d511673
    @Override
    public JPAPuId[] getExtendedContextPuIds(Collection<InjectionBinding<?>> injectionBindings,
                                             String ejbName,
                                             Set<String> persistenceRefNames,
                                             List<Object> bindingList)
    {
        return JPAExEntityManager.getExtendedContextPuIds(injectionBindings, ejbName, persistenceRefNames, bindingList); // F743-30682
    }

    /**
     * Scans the provided binding map for Transaction Synchronization configuration conflicts
     * (All extended scope EntityManagers assigned to a common PU must have the same synchronization)\
     * 
     * @param bindingList A List containing JPAPCtxtInjectionBinding objects associated with a SFSB.
     * @return A Map of <JPAPuId, Boolean> types which denote an extended scope persistence context associated
     *         with that JPAPuID's transaction synchronicity.
     * 
     *         The method will thrown an IllegalStateException if it finds two or more mappings to a common JPAPuId
     *         and have mismatching Transaction Synchronicity.
     */
    @Override
    public Set<JPAPuId> scanForTxSynchronizationCollisions(List<Object> bindingList) {
        return JPAExEntityManager.scanForTxSynchronizationCollisions(bindingList);
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jpa.JPAComponent#hasAppManagedPC
     */
    //465813                           // switch to 'bindings' d511673
    @Override
    public boolean hasAppManagedPC(Collection<InjectionBinding<?>> injectionBindings,
                                   String ejbName,
                                   Set<String> persistenceRefNames)
    {
        return JPAExEntityManager.hasAppManagedPC(injectionBindings, ejbName, persistenceRefNames); // F743-30682
    }

    public abstract JPARuntime getJPARuntime();

    public abstract int getEntityManagerPoolCapacity();

    public abstract String getDataSourceBindingName(String dsName, boolean transactional);

    public abstract boolean isIgnoreDataSourceErrors();

    public abstract JPAExPcBindingContextAccessor getExPcBindingContext();

    public abstract UOWCurrent getUOWCurrent();

    public abstract EmbeddableWebSphereTransactionManager getEmbeddableWebSphereTransactionManager();

    /**
     * Registers the singleton instance of JPAUserTxCallBackHandler with the
     * transaction service. <p>
     * 
     * This method must be called before the first extended-scoped persistence
     * context becomes active. <p>
     * 
     * Multiple call will result in multiple register attempts, but since this
     * is a singleton object, the transaction service will ignore subsequent
     * attempts. <p>
     **/
    // d515803
    public void registerLTCCallback()
    {
        getUOWCurrent().registerLTCCallback(ivCallBackHandler); // F743-18776
        ivTxCallbackRegistered = true;
    }

    /**
     * Add any additional environment specific properties to the set of
     * integration-level properties used on the call to
     * PersistenceProvider.createContainerEntityManagerFactory.
     * 
     * The default behavior is that no additional integration-level properties
     * will be added.
     * 
     * @param xmlSchemaVersion the schema version of the persistence.xml
     * @param integrationProperties the current set of integration-level properties
     */
    // F743-12524
    public void addIntegrationProperties(String xmlSchemaVersion,
                                         Map<String, Object> integrationProperties)
    {
        // No additional properties added by default.
    }

    /**
     * Convenience method for trace that obtains a sorted list of the known JPA
     * application names. This method is thread safe.
     */
    protected Set<String> getSortedAppNames()
    {
        if (applList == null)
        {
            return Collections.emptySet();
        }

        Set<String> appNames = applList.keySet();

        // Per Collections.synchronizedMap javadoc, must synchronize on map when
        // iterating over any of the collection views.
        synchronized (applList)
        {
            return new TreeSet<String>(appNames);
        }
    }
}
