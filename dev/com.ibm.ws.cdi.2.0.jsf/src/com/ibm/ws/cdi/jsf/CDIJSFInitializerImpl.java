/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jsf;

import javax.el.ELResolver;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.application.Application;

import org.jboss.weld.module.web.el.WeldELContextListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.jsf.shared.cdi.CDIJSFInitializer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(name = "com.ibm.ws.jsf.cdi.CDIJSFInitializer", property = { "service.vendor=IBM", "service.ranking:Integer=100" })
public class CDIJSFInitializerImpl implements CDIJSFInitializer {

    private final AtomicServiceReference<CDIService> cdiServiceRef = new AtomicServiceReference<CDIService>("cdiService");

    /** {@inheritDoc} */
    @Override
    public void initializeCDIJSFELContextListenerAndELResolver(Application application) {
        CDIService cdiService = cdiServiceRef.getService();
        if (cdiService != null) {
            BeanManager beanManager = cdiService.getCurrentBeanManager();
            if (beanManager != null) {
                application.addELContextListener(new WeldELContextListener());

                ELResolver elResolver = beanManager.getELResolver();
                application.addELResolver(elResolver);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initializeCDIJSFViewHandler(Application application) {
        CDIService cdiService = cdiServiceRef.getService();
        if (cdiService != null) {
            BeanManager beanManager = cdiService.getCurrentBeanManager();
            if (beanManager != null) {
                application.addELContextListener(new WeldELContextListener());

                CDIRuntime cdiRuntime = (CDIRuntime) cdiService;
                String contextID = cdiRuntime.getCurrentApplicationContextID();
                application.setViewHandler(new IBMViewHandler(application.getViewHandler(), contextID));
            }
        }
    }

    public void activate(ComponentContext context) {
        cdiServiceRef.activate(context);
    }

    public void deactivate(ComponentContext context) {
        cdiServiceRef.deactivate(context);
    }

    @Reference(name = "cdiService", service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref) {
        cdiServiceRef.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref) {
        cdiServiceRef.unsetReference(ref);
    }
}
