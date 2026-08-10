/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
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
package com.ibm.ws.jca.service;

import java.util.Collection;
import java.util.Iterator;

import javax.resource.spi.ActivationSpec;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.service.EndpointActivationService.ActivationParams;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * This service is notified of server quiesce, at which point it deactivates message endpoints.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = ServerQuiesceListener.class)
public class JCAQuiesceListener implements ServerQuiesceListener {
    private static final TraceComponent tc = Tr.register(JCAQuiesceListener.class);

    private ComponentContext componentContext;

    @Activate
    protected void activate(ComponentContext context) {
        componentContext = context;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        componentContext = null;
    }

    /**
     * Invoked when server is quiescing. Deactivate all endpoints.
     */
    @Override
    public void serverStopping() {
        BundleContext bundleContext = componentContext.getBundleContext();
        Collection<ServiceReference<EndpointActivationService>> refs;
        try {
            refs = bundleContext.getServiceReferences(EndpointActivationService.class, null);
        } catch (InvalidSyntaxException x) {
            FFDCFilter.processException(x, getClass().getName(), "61", this);
            throw new RuntimeException(x);
        }
        for (ServiceReference<EndpointActivationService> ref : refs) {
            EndpointActivationService eas = bundleContext.getService(ref);
            if (eas == null) {
                continue;
            }
            try {
                Iterator<ActivationParams> iterator = eas.endpointActivationParams.iterator();
                while (iterator.hasNext()) {
                    ActivationParams a = iterator.next();
                    // Check if this entry should be deactivated
                    if (a.messageEndpointFactory == null || a.messageEndpointFactory.isDeactivateOnQuiesce()) {
                        // Attempt to remove it - only one thread will succeed
                        if (eas.endpointActivationParams.remove(a)) {
                            // This thread successfully removed it, so deactivate it
                            try {
                                eas.endpointDeactivation((ActivationSpec) a.activationSpec, a.messageEndpointFactory);
                            } catch (Throwable x) {
                                FFDCFilter.processException(x, getClass().getName(), "71", this);
                            }
                        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            // If remove() returned false, another thread already removed it, so just continue
                            Tr.debug(this, tc, "already deactivated");
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Skipping endpoint deactivation on quiesce: " + a.messageEndpointFactory);
                    }
                }
            } finally {
                bundleContext.ungetService(ref);
            }
        }
    }
}
