/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21.cdi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(service = WebSphereCDIExtension.class)
public class JPAContainerCDIExtension implements Extension, WebSphereCDIExtension {
	
    //This is not actually used since weld will create a new instance of this class seperate from the one OSGI has populated. 
    //But this stays so that OSGI will manage the extensions lifecycle. 
    @Reference
    protected HibernateNotifier notUsed;

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager manager) {
    	HibernateNotifier hibernateNotifier = getPropertyProvideder();
    	hibernateNotifier.notifyHibernateAfterBeanDiscovery(manager);
    }
    
    public void beforeShutdown(@Observes BeforeShutdown event, BeanManager manager) {
    	HibernateNotifier hibernateNotifier = getPropertyProvideder();
    	hibernateNotifier.notifyHibernateBeforeShutdown(manager);
    }
 
    private HibernateNotifier getPropertyProvideder() {
        final Bundle bundle = FrameworkUtil.getBundle(HibernateNotifier.class);
        HibernateNotifier hibernateNotifier = AccessController.doPrivileged(new PrivilegedAction<HibernateNotifier>() {
            @Override
            public HibernateNotifier run() {
                BundleContext bCtx = bundle.getBundleContext();
                ServiceReference<HibernateNotifier> svcRef = bCtx.getServiceReference(HibernateNotifier.class);
                return svcRef == null ? null : bCtx.getService(svcRef);
            }
        });
        if (hibernateNotifier == null) {
            throw new IllegalStateException("Failed to get the HibernateNotifier.");
        }
        return hibernateNotifier;
    }
}
