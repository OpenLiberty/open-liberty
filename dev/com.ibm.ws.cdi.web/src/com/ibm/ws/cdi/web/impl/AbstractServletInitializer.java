/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.impl;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspFactory;

import org.jboss.weld.Container;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.web.factories.WeldListenerFactory;
import com.ibm.ws.cdi.web.impl.security.PrincipalServletRequestListener;
import com.ibm.ws.cdi.web.interfaces.CDIWebRuntime;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public abstract class AbstractServletInitializer implements ServletContainerInitializer {

    // because we use a package-info.java for trace options, just need this to register our group and message file
    private static final TraceComponent tc = Tr.register(AbstractServletInitializer.class);

    private final AtomicServiceReference<CDIWebRuntime> cdiWebRuntimeRef = new AtomicServiceReference<CDIWebRuntime>("cdiWebRuntime");

    public void activate(ComponentContext context) {
        cdiWebRuntimeRef.activate(context);
    }

    protected void deactivate(ComponentContext context) {
        cdiWebRuntimeRef.deactivate(context);
    }

    @Reference(name = "cdiWebRuntime", service = CDIWebRuntime.class)
    protected void setCdiWebRuntime(ServiceReference<CDIWebRuntime> ref) {
        cdiWebRuntimeRef.setReference(ref);
    }

    protected void unsetCdiWebRuntime(ServiceReference<CDIWebRuntime> ref) {
        cdiWebRuntimeRef.unsetReference(ref);
    }

    protected CDIWebRuntime getCDIWebRuntime() {
        return cdiWebRuntimeRef.getService();
    }

    protected abstract String getApplicationJ2EEName(IServletContext isc);

    @Override
    public void onStartup(java.util.Set<java.lang.Class<?>> c, ServletContext ctx) {

        // a bit mis-behaved here, but needed. And if ctx isn't an IServletContext then lots of things are broken.
        IServletContext isc = (IServletContext) ctx;

        //Unwrap any ServletContextFacades sitting on top
        while (isc instanceof ServletContextFacade) {
            isc = ((ServletContextFacade) isc).getIServletContext();
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "calling isCDIEnabled()");

        CDIWebRuntime cdiWebRuntime = getCDIWebRuntime();
        if (cdiWebRuntime != null && cdiWebRuntime.isCdiEnabled(isc)) {

            String contextID = getApplicationJ2EEName(isc);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CDI WEB APP: " + contextID);
            }
            isc.setInitParameter(Container.CONTEXT_ID_KEY, contextID);

            BeanManager beanManager = cdiWebRuntime.getCurrentBeanManager();

            if (beanManager == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Bean Manager not found: " + contextID);
                }
            } else {
                isc.addListener(new PrincipalServletRequestListener());

                JspFactory factory = JspFactory.getDefaultFactory();
                if (factory != null) {
                    JspApplicationContext applicationCtx = factory.getJspApplicationContext(ctx);
                    applicationCtx.addELContextListener(WeldListenerFactory.newWeldELContextListener());

                    beanManager.wrapExpressionFactory(applicationCtx.getExpressionFactory());
                    applicationCtx.addELResolver(beanManager.getELResolver());
                }
            }
        }

    }

}
