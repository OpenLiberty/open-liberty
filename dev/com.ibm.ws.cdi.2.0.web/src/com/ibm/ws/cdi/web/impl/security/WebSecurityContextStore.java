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
package com.ibm.ws.cdi.web.impl.security;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.interfaces.SecurityContextStore;

/**
 * Class stores web context information for real-time extraction of state data.
 */
@Component(
                name = "com.ibm.ws.cdi.WebSecurityContextStore",
                immediate = true,
                property = { "service.vendor=IBM", "service.ranking:Integer=100" })
public class WebSecurityContextStore implements SecurityContextStore {

    private static final TraceComponent tc = Tr.register(WebSecurityContextStore.class);

    private static final AtomicReference<WebSecurityContextStore> INSTANCE = new AtomicReference<WebSecurityContextStore>();

    /**
     * DS method to activate this component.
     *
     * @param compcontext
     */
    public void activate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass().getName());
        }
        INSTANCE.set(this);
    }

    /**
     * DS method to deactivate this component.
     *
     * @param compcontext
     */
    public void deactivate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating " + this.getClass().getName());
        }
        // Clear this as the active instance
        INSTANCE.compareAndSet(this, null);
    }

    /**
     * @return the active instance; may be null between deactivate and activate.
     */
    public static WebSecurityContextStore getCurrentInstance() {
        return INSTANCE.get();
    }

    /**
     * If a web module is a part of the BDA, it stores a ref to the HttpServletRequest
     */
    private final ThreadLocal<HttpServletRequest> httpServletRequests = new ThreadLocal<HttpServletRequest>();

    /**
     *
     * @return Principal from HttpServletRequest or null
     */
    @Override
    public Principal getCurrentPrincipal() {
        if (httpServletRequests.get() != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (httpServletRequests.get().getUserPrincipal() == null) {
                    Tr.debug(tc, "getCurrentPrincipal: null");
                } else {
                    Tr.debug(tc, "getCurrentPrincipal: " + httpServletRequests.get().getUserPrincipal().getName());
                }
            }
            return httpServletRequests.get().getUserPrincipal();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCurrentPrincipal: null web context");
        }
        return null;
    }

    public void storeHttpServletRequest(HttpServletRequest req) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "storeHttpServletRequest: " + req);
        }
        httpServletRequests.set(req);
    }

    public void removeHttpServletRequest() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHttpServletRequest: " + httpServletRequests.get());
        }
        httpServletRequests.remove();
    }

}
