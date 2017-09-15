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
package com.ibm.ws.ejbcontainer.jpa.internal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.ejbcontainer.runtime.EJBJPAContainer;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.jpa.JPAExPcBindingContext;
import com.ibm.ws.jpa.JPAPuId;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class EJBJPAContainerImpl implements EJBJPAContainer {

    private final AtomicServiceReference<JPAComponent> jpaComponentSR = new AtomicServiceReference<JPAComponent>("jpaComponent");
    private final AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");

    public void setJPAComponent(ServiceReference<JPAComponent> reference) {
        jpaComponentSR.setReference(reference);
    }

    public void unsetJPAComponent(ServiceReference<JPAComponent> reference) {
        jpaComponentSR.unsetReference(reference);
    }

    public void setEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.setReference(reference);
    }

    public void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.unsetReference(reference);
    }

    public void activate(ComponentContext cc) {
        jpaComponentSR.activate(cc);
        ejbContainerSR.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        jpaComponentSR.deactivate(cc);
        ejbContainerSR.deactivate(cc);
    }

    @Override
    public Object onCreate(String j2eeName,
                           boolean usesBeanManagedTx,
                           Object[] ivExPcPuIds,
                           Set unsynchronizedJPAPuIdSet) {
        return jpaComponentSR.getServiceWithException().onCreate(j2eeName, usesBeanManagedTx, (JPAPuId[]) ivExPcPuIds, (Set<JPAPuId>) unsynchronizedJPAPuIdSet);
    }

    @Override
    public void onEnlistCMT(Object exPcContext) {
        jpaComponentSR.getServiceWithException().onEnlistCMT((JPAExPcBindingContext) exPcContext);
    }

    @Override
    public void onRemoveOrDiscard(Object ivExPcContext) {
        jpaComponentSR.getServiceWithException().onRemoveOrDiscard((JPAExPcBindingContext) ivExPcContext);
    }

    @Override
    public Object[] getExtendedContextPuIds(Collection<InjectionBinding<?>> injectionBindings,
                                            String enterpriseBeanName,
                                            Set<String> persistenceRefNames,
                                            List<Object> bindingList) {
        return jpaComponentSR.getServiceWithException().getExtendedContextPuIds(injectionBindings, enterpriseBeanName, persistenceRefNames, bindingList);
    }

    @Override
    public Set scanForTxSynchronizationCollisions(List<Object> bindingList) {
        return jpaComponentSR.getServiceWithException().scanForTxSynchronizationCollisions(bindingList);
    }

    @Override
    public boolean hasAppManagedPC(Collection<InjectionBinding<?>> injectionBindings,
                                   String enterpriseBeanName,
                                   Set<String> persistenceRefNames) {
        return jpaComponentSR.getServiceWithException().hasAppManagedPC(injectionBindings, enterpriseBeanName, persistenceRefNames);
    }
}
