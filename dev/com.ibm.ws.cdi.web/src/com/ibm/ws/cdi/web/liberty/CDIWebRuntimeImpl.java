/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.web.liberty;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.web.interfaces.CDIWebRuntime;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

@Component(name = "com.ibm.ws.cdi.web.liberty.CDIWebRuntimeImpl", service = CDIWebRuntime.class, immediate = true, property = { "service.vendor=IBM" })
public class CDIWebRuntimeImpl implements CDIWebRuntime {

    private static final TraceComponent tc = Tr.register(CDIWebRuntimeImpl.class);

    private final AtomicServiceReference<CDIService> cdiServiceRef = new AtomicServiceReference<CDIService>("cdiService");

    protected void activate(ComponentContext context) {
        cdiServiceRef.activate(context);
    }

    protected void deactivate(ComponentContext context) {
        cdiServiceRef.deactivate(context);
    }

    @Reference(name = "cdiService", service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref) {
        cdiServiceRef.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref) {
        cdiServiceRef.unsetReference(ref);
    }

    private CDIRuntime getCDIRuntime() {
        CDIService cdiService = cdiServiceRef.getService();
        CDIRuntime cdiRuntime = (CDIRuntime) cdiService;
        return cdiRuntime;
    }

    @Override
    public boolean isCdiEnabled(IServletContext isc) {
        boolean cdiOn = false;

        //Unwrap any ServletContextFacades sitting on top
        while (isc instanceof ServletContextFacade) {
            isc = ((ServletContextFacade) isc).getIServletContext();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "checking to see whether cdi is enabled");

        Object cdiEnabled = isc.getAttribute(CDI_ENABLED_ATTR);
        if (cdiEnabled != null) {
            cdiOn = (Boolean) cdiEnabled;
        } else {
            cdiOn = setCdiEnabled(isc);
        }

        return cdiOn;

    }

    private boolean setCdiEnabled(IServletContext isc) {
        boolean cdiOn = false;

        //Unwrap any ServletContextFacades sitting on top
        while (isc instanceof ServletContextFacade) {
            isc = ((ServletContextFacade) isc).getIServletContext();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "checking to see whether cdi is enabled");

        try {
            ModuleMetaData moduleMetaData = CDIServiceUtils.getModuleMetaData(isc.getModuleContainer());
            cdiOn = getCDIRuntime().isModuleCDIEnabled(moduleMetaData);
        } catch (CDIException e) {
            cdiOn = Boolean.FALSE;
        }

        isc.setAttribute(CDI_ENABLED_ATTR, cdiOn);

        WebAppConfig webAppConfig = isc.getWebAppConfig();
        webAppConfig.setJCDIEnabled(cdiOn);

        return cdiOn;

    }

    /** {@inheritDoc} */
    @Override
    public BeanManager getModuleBeanManager(ModuleMetaData moduleMetaData) {
        return getCDIRuntime().getModuleBeanManager(moduleMetaData);
    }

    /** {@inheritDoc} */
    @Override
    public BeanManager getCurrentBeanManager() {
        return getCDIRuntime().getCurrentBeanManager();
    }
}
