/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.ibm.wsspi.injectionengine.InjectionBinding;

/**
 * Integration between the EJB and JPA containers.
 */
public interface EJBJPAContainer
{
    /**
     * Determine if a stateful session bean has declared any extended
     * persistence context references.
     * 
     * @param injectionBindings the injection bindings for the bean
     * @param enterpriseBeanName the stateful session bean name
     * @param ivPersistenceRefNames the output set of of extended persistence
     *            context names referenced by the stateful session bean, or null
     * @param bindingList
     *            A return-value List containing JPAPCtxtInjectionBinding objects.
     *            If bindingList is not null, then for each @PersistenceContext associated with each
     *            InjectionBinding, it will insert an JPAPCtxtInjectionBinding type entry to the List.
     *            If null, this data will not be returned.
     * @return an array of opaque persistence contexts; updates bindingMap with JNDI -> JPAPCtxtInjectionBinding
     *         entries.
     */
    Object[] getExtendedContextPuIds(Collection<InjectionBinding<?>> injectionBindings,
                                     String enterpriseBeanName,
                                     Set<String> ivPersistenceRefNames,
                                     List<Object> bindingList);

    /**
     * Scans the provided bindingMap for JPA @PersistenceContext Transaction Synchronization configuration
     * conflicts (All extended scope EntityManagers assigned to a common PU must have the same synchronization)
     * 
     * @param bindingList - a List containing a SFSB's JPAPCtxtInjectionBinding objects.
     * @return a Set of JPAPuId identifying CMEX which are SynchronizationType.UNSYNCHRONIZED.
     */
    Set scanForTxSynchronizationCollisions(List<Object> bindingList);

    /**
     * Returns true if a stateful session bean has declared any persistence unit
     * references.
     * 
     * @param injectionBindings the injection bindings for the bean
     * @param enterpriseBeanName the stateful session bean name
     * @param ivPersistenceRefNames the output set of persistence unit reference
     *            names, or null
     */
    boolean hasAppManagedPC(Collection<InjectionBinding<?>> injectionBindings,
                            String enterpriseBeanName,
                            Set<String> ivPersistenceRefNames);

    /**
     * Notification that an instance of a stateful session bean instance with an
     * extended persistence context (non-zero {@link #getExtendedContextPuIds})
     * has been created. If this method returns a non-null result, then
     * the {@link #onRemoveOrDiscard} method must be called when the bean is
     * removed or discarded.
     * 
     * @param string the J2EEName of the EJB
     * @param usesBeanManagedTx true if the EJB uses bean managed transactions
     * @param exPcPuIds the result of {@link #getExtendedContextPuIds}
     * @param unsynchronizedJPAPuIdSet is a Set of JPAPuId associated UNSYNCHRONIZED Container Managed
     *            Extended Scoped persistence contexts which are SynchronizationType.UNSYNCHRONIZED.
     * @return an extended persistence context
     */
    Object onCreate(String string,
                    boolean usesBeanManagedTx,
                    Object[] exPcPuIds,
                    Set unsynchronizedJPAPuIdSets);

    /**
     * Notification that an instance of a stateful session bean with
     * container-managed transactions and an extended persistence context
     * (non-null {@link #onCreate}) has been enlisted in a global transaction.
     * 
     * @param exPcContext the result of {@link #onCreate}
     */
    void onEnlistCMT(Object exPcContext);

    /**
     * Notification that an instance of a stateful session bean with an extended
     * persistence context (non-null {@link #onCreate}) is being removed or
     * destroyed.
     * 
     * @param exPcContext the result of {@link #onCreate}
     */
    void onRemoveOrDiscard(Object exPcContext);
}
