/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.wsspi.injectionengine.InjectionBinding;

/**
 * WAS JPA Service interface.
 */
public interface JPAComponent {

    /**
     * Returns the default JPA provider class name.
     * <p>
     *
     * The default JPA persistence provider is based on the following criteria:
     * <ul>
     * <li>The provider classname is retrieved from the server.xml configuration (set through the
     * admin console).
     * <li>If the server.xml configuration is not specified, then the system property,
     * JPA_PROVIDER_SYSTEM_PROPERTY_NAME will be used.
     * <li>Otherwise, The system default DEF_JPA_PROVIDER_CLASS_NAME will be used.
     * </ul>
     **/
    // LI3294-25
    public String getDefaultJPAProviderClassName();

    /**
     * Whether to capture the JPA provider enhanced bytecode and store it to disk.
     * This is intended to be a debug option to enable examination of enhanced bytecode for faults.
     *
     * @return true if the JPA feature's capture enhanced entity class bytecode debug option has
     *         been enabled (set to true), false if it is disabled or unset.
     *
     */
    public boolean getCaptureEnhancedEntityClassBytecode();

    /**
     * Returns the server's log directory.
     *
     * @return
     */
    public File getServerLogDirectory();

    public boolean isServerRuntime();

    public JPAProviderIntegration getJPAProviderIntegration();

    /**
     * Returns the EntityManagerFactory defines by the application/module/persistence unit spcified.
     * This is used by the resolver and naming object factory to retrieve the factory for
     *
     * @PersistenceUnit.
     *
     * @param puId
     *            Persistence unit id
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @param getEmfWrapper
     *            return a serializable EntityManagerFactory wrapper
     * @return EntityManagerFactory for the specified or null if none is found.
     */
    public EntityManagerFactory getEntityManagerFactory(JPAPuId puId,
                                                        J2EEName j2eeName, // d510184
                                                        boolean getEmfWrapper); // d416151.3.1

    /**
     * Returns the EntityManager defines by the application/module/persistence unit specified. This
     * is used by the naming object factory to retrieve the entity manager for
     *
     * @PersistenceContext.
     *
     * @param puId
     *            Persistence unit id
     * @param j2eeName
     *            JavaEE unique identifier for the component, identifying the
     *            java:comp/env context used.
     * @param refName
     *            Name of the PersistenceContext reference.
     * @param isExtendedContextType
     *            is the EntityManager extended scope.
     * @param properties
     *            additional properties to create the EntityManager
     *
     * @return EntityManager for the specified or null if none is found.
     */
    public EntityManager getEntityManager(JPAPuId puId,
                                          J2EEName j2eeName, // d510184
                                          String refName, // d510184
                                          boolean isExtendedContextType,
                                          boolean isUnsynchronized,
                                          Map<?, ?> properties);

    /**
     * Determine and collect the Persistence Unit Ids associated to @PersistencContext(type=Extended)
     * declared in the input injection meta data.
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
    public JPAPuId[] getExtendedContextPuIds(Collection<InjectionBinding<?>> injectionBindings,
                                             String ejbName,
                                             Set<String> persistenceRefNames,
                                             List<Object> bindingList); // F743-30682

    /**
     * Scans the provided binding map for Transaction Synchronization configuration conflicts
     * (All extended scope EntityManagers assigned to a common PU must have the same synchronization)\
     *
     * @param bindingList A List containing JPAPCtxtInjectionBinding objects associated with a SFSB.
     * @return A List of Extended Scope Persistence Context JPAPuId that are SynchronizationType.UNSYNCHRONIZED.
     *
     *         The method will thrown an IllegalStateException if it finds two or more mappings to a common JPAPuId
     *         and have mismatching Transaction Synchronicity.
     */
    public Set<JPAPuId> scanForTxSynchronizationCollisions(List<Object> bindingList);

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
    public boolean hasAppManagedPC(Collection<InjectionBinding<?>> injectionBindings,
                                   String ejbName,
                                   Set<String> persistenceRefNames); // F743-30682

    /**
     * Called from Stateful bean creation process to establish SFSBs and persistence binding
     * relationship.
     * <ul>
     * <li>Use Object and ComponentMetaData instead of BeanId and BeanMetaData respectively to
     * avoid build time circular dependency.
     * <li>This method should only be called from SFSB creation processing, hence asserts parentCMD
     * and childCMD must be BeanMetaData.
     * </ul>
     *
     * @param j2eeName
     * @param isBMT
     * @param puIds
     * @param unsynchronizedJPAPuIdSet is a Set of JPAPuId associated UNSYNCHRONIZED Container Managed
     *            Extended Scoped persistence contexts which are SynchronizationType.UNSYNCHRONIZED.
     */
    public JPAExPcBindingContext onCreate(String j2eeName,
                                          boolean isBMT,
                                          JPAPuId[] puIds,
                                          Set<JPAPuId> unsynchronizedJPAPuIdSet);

    /**
     * Called when a container-managed transaction Stateful bean is first
     * enlisted in a global transaction in order to enlist a the bean's extended
     * scope entity managers with the global transaction.
     */
    public void onEnlistCMT(JPAExPcBindingContext bindingContext);

    /**
     * Called from Stateful bean remove/destroy/discard process to remove SFSBs and persistence
     * binding relationship.
     *
     * @param bindingContext
     */
    public void onRemoveOrDiscard(JPAExPcBindingContext bindingContext);

    /**
     * Registers an extended-scope persistence context binding context accessor
     * with the JPA service. <p>
     *
     * For extended-scope persistence context support, the JPA service requires
     * context managment support from an EJB Container. This method provides
     * the mechanism for an EJB Container to register that context management
     * implementation. <p>
     *
     * The JPA service will use the registered accessor to obtain the currently
     * active extened-scoped persistence context binding object, as returned
     * from onCreate(). <p>
     **/
    // d515803
    public void registerJPAExPcBindingContextAccessor(JPAExPcBindingContextAccessor accessor);

    public void recycleJPAApplications();
}
