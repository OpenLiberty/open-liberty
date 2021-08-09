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
package com.ibm.ws.security.intfc.internal;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class WSSecurityServiceImpl implements WSSecurityService {

    private static final TraceComponent tc = Tr.register(WSSecurityServiceImpl.class);
    static final String KEY_SECURITY_SERVICE = "securityService";
    private final AtomicServiceReference<SecurityService> securityServiceRef = new AtomicServiceReference<SecurityService>(KEY_SECURITY_SERVICE);

    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        securityServiceRef.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSecurityEnabled() {
        // if this bundle got loaded then security is enabled
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public UserRegistry getUserRegistry(String realmName) throws WSSecurityException {
        final String METHOD = "getUserRegistry";
        UserRegistry userRegistry = getActiveUserRegistry();
        if (userRegistry != null) {
            try {
                if (realmName != null && !realmName.equals(userRegistry.getRealm())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, METHOD + " Supplied realm not valid: " + realmName);
                    }
                    userRegistry = null;
                }
            } catch (RemoteException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, METHOD + " Internal error getting current registry realm", e);
                }
                throw new WSSecurityException("Internal error getting realm", e);
            }
        }
        return userRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInboundTrustedRealms(String realmName) throws WSSecurityException {
        final String METHOD = "getInboundTrustedRealms";
        List<String> trustedRealmList = new ArrayList<String>();
        UserRegistry userRegistry = getActiveUserRegistry();
        if (userRegistry != null) {
            try {
                if (realmName != null && !realmName.equals(userRegistry.getRealm())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, METHOD + " Supplied realm not valid: " + realmName);
                    }
                } else {
                    trustedRealmList.add(userRegistry.getRealm());
                }
            } catch (RemoteException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, METHOD + " Internal error getting current registry realm", e);
                }
                throw new WSSecurityException("Internal error getting realm", e);
            }
        }
        return trustedRealmList;
    }

    /**
     *
     * @param inboundRealm
     * @param localRealm
     * @return
     */
    @Override
    public boolean isRealmInboundTrusted(String inboundRealm, String localRealm) {
        final String METHOD = "getInboundTrustedRealms";
        if (inboundRealm == null)
            return false;
        boolean trusted = true;
        try {
            UserRegistry userRegistry = getActiveUserRegistry();
            if (userRegistry != null) {
                if (localRealm != null && !localRealm.equals(userRegistry.getRealm())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, METHOD + " Local realm not valid: " + localRealm);
                    }
                    trusted = false;
                } else {
                    if (!inboundRealm.equals(userRegistry.getRealm())) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, METHOD + " Inbound realm not trusted");
                        }
                        trusted = false;
                    }
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, METHOD + " Internal error", e);
            }
        }
        return trusted;
    }

    /**
     * Returns the active UserRegistry. If the active user registry is an instance of
     * com.ibm.ws.security.registry.UserRegistry, then it will be wrapped. Otherwise,
     * if the active user registry is an instance of CustomUserRegistryWrapper, then the
     * underlying com.ibm.websphere.security.UserRegistry will be returned.
     *
     * @return UserRegistry the active user registry.
     * @throws WSSecurityException
     */
    private UserRegistry getActiveUserRegistry() throws WSSecurityException {
        final String METHOD = "getUserRegistry";
        UserRegistry activeUserRegistry = null;
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, METHOD + " securityServiceRef " + securityServiceRef);
            }
            SecurityService ss = securityServiceRef.getService();
            if (ss == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, METHOD + " No SecurityService, returning null");
                }
            } else {
                UserRegistryService urs = ss.getUserRegistryService();
                if (urs == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, METHOD + " No UserRegistryService, returning null");
                    }
                } else {
                    if (urs.isUserRegistryConfigured()) {
                        com.ibm.ws.security.registry.UserRegistry userRegistry = urs.getUserRegistry();
                        activeUserRegistry = urs.getExternalUserRegistry(userRegistry);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, METHOD + " Security enabled but no registry configured");
                        }
                    }
                }
            }
        } catch (RegistryException e) {
            String msg = "getUserRegistryWrapper failed due to an internal error: " + e.getMessage();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, msg, e);
            }
            throw new WSSecurityException(msg, e);
        }
        return activeUserRegistry;
    }
}