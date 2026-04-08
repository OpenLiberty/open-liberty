/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jpa.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.jpa.JPAExPcBindingContext;
import com.ibm.ws.jpa.JPAExPcBindingContextAccessor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class JPAContextAccessor implements JPAExPcBindingContextAccessor {

    private final AtomicServiceReference<EJBContainer> ejbContainerSR = new AtomicServiceReference<EJBContainer>("ejbContainer");

    @Override
    public JPAExPcBindingContext getExPcBindingContext() {
        return (JPAExPcBindingContext) ejbContainerSR.getServiceWithException().getExPcBindingContext();
    }

    public void activate(ComponentContext cc) {
        ejbContainerSR.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        ejbContainerSR.deactivate(cc);
    }

    public void setEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.setReference(reference);
    }

    public void unsetEJBContainer(ServiceReference<EJBContainer> reference) {
        ejbContainerSR.unsetReference(reference);
    }

    @Override
    public RuntimeException newEJBException(String msg) {
        return ExceptionUtil.EJBException(msg, null);
    }

}