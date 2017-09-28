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
package com.ibm.ws.cdi.impl;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Iterator;

import org.jboss.weld.security.spi.SecurityServices;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.interfaces.SecurityContextStore;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * DS to provide Security services to CDI
 *
 * @see org.jboss.weld.security.spi.SecurityServices
 */
@Component(
                name = "com.ibm.ws.cdi.impl.DefaultSecurityService",
                property = { "service.vendor=IBM", "service.ranking:Integer=50" })
public class DefaultSecurityService implements SecurityServices {

    private static final TraceComponent tc = Tr.register(DefaultSecurityService.class);

    private final ConcurrentServiceReferenceSet<SecurityContextStore> securityContextStores = new ConcurrentServiceReferenceSet<SecurityContextStore>("securityContextStore");

    /**
     * DS method to activate this component.
     *
     * @param compcontext
     */
    protected void activate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass().getName());
        }
        this.securityContextStores.activate(compcontext);
    }

    /**
     * DS method to deactivate this component.
     *
     * @param compcontext
     */
    protected void deactivate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating " + this.getClass().getName());
        }
        this.securityContextStores.deactivate(compcontext);
    }

    @Reference(name = "securityContextStore", service = SecurityContextStore.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.AT_LEAST_ONE)
    protected void addSecurityContextStore(ServiceReference<SecurityContextStore> ref) {
        this.securityContextStores.addReference(ref);
    }

    protected void removeSecurityContextStore(ServiceReference<SecurityContextStore> ref) {
        this.securityContextStores.removeReference(ref);
    }

    @Override
    public Principal getPrincipal() {
        Principal principal = AccessController.doPrivileged(new PrivilegedAction<Principal>() {
            @Override
            public Principal run() {
                Principal principal = null; // no principal by default
                Iterator<SecurityContextStore> contextStores = securityContextStores.getServices();
                while (contextStores.hasNext() && principal == null) {
                    SecurityContextStore securityContextStore = contextStores.next();
                    if (securityContextStore != null) {
                        principal = securityContextStore.getCurrentPrincipal();
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Principal from " + securityContextStore + ": " + principal);
                        }
                    }
                }
                return principal;
            }
        });

        return principal;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.api.Service#cleanup()
     */
    @Override
    public void cleanup() {
        // noop

    }

}
