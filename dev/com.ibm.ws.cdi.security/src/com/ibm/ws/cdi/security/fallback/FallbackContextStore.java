/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.security.fallback;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.cdi.internal.interfaces.SecurityContextStore;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Class stores web context information for real-time extraction of state data.
 */
@Component(name = "com.ibm.ws.cdi.security.fallback.FallbackContextStore", immediate = true, property = { "service.vendor=IBM", "service.ranking:Integer=-1" })
public class FallbackContextStore implements SecurityContextStore {

    private static final TraceComponent tc = Tr.register(FallbackContextStore.class);

    static final String KEY_UNAUTH_SERVICE = "unauthenticatedSubjectServiceRef";
    private final AtomicServiceReference<UnauthenticatedSubjectService> unauthenticatedSubjectServiceRef = new AtomicServiceReference<UnauthenticatedSubjectService>(KEY_UNAUTH_SERVICE);

    /**
     * DS method to activate this component.
     *
     * @param compcontext
     */
    protected void activate(ComponentContext compcontext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass().getName());
        }
        this.unauthenticatedSubjectServiceRef.activate(compcontext);
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
        this.unauthenticatedSubjectServiceRef.deactivate(compcontext);
    }

    @Reference(service = UnauthenticatedSubjectService.class, name = KEY_UNAUTH_SERVICE)
    protected void setUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        unauthenticatedSubjectServiceRef.setReference(ref);
    }

    protected void unsetUnauthenticatedSubjectService(ServiceReference<UnauthenticatedSubjectService> ref) {
        unauthenticatedSubjectServiceRef.unsetReference(ref);
    }

    @Override
    public Principal getCurrentPrincipal() {
        Principal principal = AccessController.doPrivileged(new PrivilegedAction<Principal>() {
            @Override
            public Principal run() {
                Principal principal = getPrincipalFromWSSubject();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Principal from WSSubject : {0}", principal);
                }

                return principal;
            }
        });

        if (principal == null) {
            principal = getUnauthenticatedPrincipal();
        }

        return principal;
    }

    private Principal getPrincipalFromWSSubject() {
        Principal principal = null;

        // Get hold of the current subject
        Subject subject = null;
        try {
            subject = WSSubject.getRunAsSubject();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Current subject: ", subject);
            }
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to get current subject", e);
            }
        }

        // If we have a subject, extract the first principal
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Number of principals: ", principals.size());
            }
            if (!principals.isEmpty()) {
                principal = principals.iterator().next();
            }
        }

        return principal;

    }

    private Principal getUnauthenticatedPrincipal() {
        if (unauthenticatedSubjectServiceRef.getService() != null) {
            UnauthenticatedSubjectService uss = unauthenticatedSubjectServiceRef.getService();
            Set<Principal> principals = uss.getUnauthenticatedSubject().getPrincipals();
            if (!principals.isEmpty()) {
                return principals.iterator().next();
            }
        }
        return null;
    }

}
