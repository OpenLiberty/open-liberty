/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.liberty;

import com.ibm.ws.cdi.web.impl.AbstractTerminalListenerRegistration;
import com.ibm.ws.cdi.web.interfaces.CDIWebRuntime;
import com.ibm.ws.webcontainer31.osgi.listener.PostEventListenerProvider;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Register WeldTeminalListener on the servlet context. This listener needs to be the last HttpSessionlistener.
 */
@Component(name = "com.ibm.ws.cdi.weldTerminalListener",
                service = PostEventListenerProvider.class,
                immediate = true,
                property = { "service.vendor=IBM", "service.ranking:Integer=-1" })
public class WeldTerminalListenerRegistration extends AbstractTerminalListenerRegistration implements PostEventListenerProvider {
    
    private final AtomicServiceReference<CDIWebRuntime> cdiWebRuntimeRef = new AtomicServiceReference<CDIWebRuntime>(
                    "cdiWebRuntime");

    protected void activate(ComponentContext context) {
        cdiWebRuntimeRef.activate(context);
    }

    protected void deactivate(ComponentContext context) {
        cdiWebRuntimeRef.deactivate(context);
    }

    @Override
    protected CDIWebRuntime getCDIWebRuntime() {
        return cdiWebRuntimeRef.getService();
    }

    protected void unsetCdiWebRuntime(ServiceReference<CDIWebRuntime> ref) {
        cdiWebRuntimeRef.unsetReference(ref);
    }
    
    @Reference(name = "cdiWebRuntime", service = CDIWebRuntime.class)
    protected void setCdiWebRuntime(ServiceReference<CDIWebRuntime> ref) {
        cdiWebRuntimeRef.setReference(ref);
    }
}
