/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal;

import java.util.Hashtable;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.credentials.CredentialProvider;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * This class really needs to be moved to a more "internal" place in Security.
 * But I don't know where to put it right now so we'll leave it in Web until we do EJB.
 */
public class UnauthenticatedSubjectServiceImpl implements UnauthenticatedSubjectService, UserRegistryChangeListener {
    private static final TraceComponent tc = Tr.register(UnauthenticatedSubjectServiceImpl.class);

    static final String KEY_SECURITY_SERVICE = "securityService";
    static final String KEY_CREDENTIALS_SERVICE = "credentialsService";

    protected final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);
    protected final AtomicServiceReference<CredentialsService> credentialsServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIALS_SERVICE);

    private Subject unauthenticatedSubject = null;
    private final Object unauthenticatedSubjectLock = new Object() {};

    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    protected void setCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialsServiceRef.setReference(ref);
    }

    protected void unsetCredentialsService(ServiceReference<CredentialsService> ref) {
        credentialsServiceRef.unsetReference(ref);
    }

    /**
     * When CredentialProviders come and go, reset the unauthenticated subject.
     * 
     * @param ref
     */
    protected void setCredentialProvider(ServiceReference<CredentialProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resetting unauthenticatedSubject as new CredentialProvider has been set");
        }
        synchronized (unauthenticatedSubjectLock) {
            unauthenticatedSubject = null;
        }
    }

    /**
     * When CredentialProviders come and go, reset the unauthenticated subject.
     * 
     * @param ref
     */
    protected void unsetCredentialProvider(ServiceReference<CredentialProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resetting unauthenticatedSubject as CredentialProvider has been unset");
        }
        synchronized (unauthenticatedSubjectLock) {
            unauthenticatedSubject = null;
        }
    }

    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
        credentialsServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
        credentialsServiceRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyOfUserRegistryChange() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Resetting unauthenticatedSubject as UserRegistry configuration has changed");
        }
        synchronized (unauthenticatedSubjectLock) {
            unauthenticatedSubject = null;
        }
    }

    /**
     * Attempt to retrieve the registry realm for the configured registry.
     * <p>
     * It is possible that no registry is configured. If that is the case,
     * return the default realm name.
     * 
     * @return realm name. {@code null} is not returned.
     */
    @FFDCIgnore(RegistryException.class)
    private String getUserRegistryRealm() {
        String realm = "defaultRealm";
        try {
            SecurityService securityService = securityServiceRef.getService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            if (userRegistryService.isUserRegistryConfigured()) {
                realm = userRegistryService.getUserRegistry().getRealm();
            }
        } catch (RegistryException re) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RegistryException while trying to get the realm", re);
            }
        }
        return realm;
    }

    /**
     * Return the unauthenticated subject. If we don't already have one create it.
     * 
     * @return unauthenticated subject
     */
    @Override
    @FFDCIgnore(Exception.class)
    public Subject getUnauthenticatedSubject() {
        if (unauthenticatedSubject == null) {
            CredentialsService cs = credentialsServiceRef.getService();
            String unauthenticatedUserid = cs.getUnauthenticatedUserid();
            try {
                Subject subject = new Subject();
                Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
                hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, unauthenticatedUserid);
                hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                              AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, getUserRegistryRealm(), unauthenticatedUserid));
                subject.getPublicCredentials().add(hashtable);
                SecurityService securityService = securityServiceRef.getService();
                AuthenticationService authenticationService = securityService.getAuthenticationService();
                Subject tempUnauthenticatedSubject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_UNAUTHENTICATED, subject);
                tempUnauthenticatedSubject.setReadOnly();
                synchronized (unauthenticatedSubjectLock) {
                    if (unauthenticatedSubject == null) {
                        unauthenticatedSubject = tempUnauthenticatedSubject;
                    }
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Internal error creating UNAUTHENTICATED subject.", e);
                }
            }
        }
        return unauthenticatedSubject;
    }
}
