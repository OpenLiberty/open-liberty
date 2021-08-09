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
package com.ibm.wsspi.security.registry;

import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.security.intfc.WSSecurityService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Provides methods to retrieve user registry information
 * 
 * @author International Business Machines Corp.
 * @version WAS 8.5
 * @since WAS 7.0
 * @ibm-spi
 */
public class RegistryHelper {
    private static final TraceComponent tc = Tr.register(RegistryHelper.class);
    private final static AtomicServiceReference<WSSecurityService> wsSecurityServiceRef =
                    new AtomicServiceReference<WSSecurityService>(WSSecurityService.KEY_WS_SECURITY_SERVICE);

    public void setWsSecurityService(ServiceReference<WSSecurityService> reference) {
        wsSecurityServiceRef.setReference(reference);
    }

    public void unsetWsSecurityService(ServiceReference<WSSecurityService> reference) {
        wsSecurityServiceRef.unsetReference(reference);
    }

    public void activate(ComponentContext cc) {
        wsSecurityServiceRef.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        wsSecurityServiceRef.deactivate(cc);
    }

    /**
     * Gets the UserRegistry object for the given realm. If the realm name is null
     * returns the active registry. If the realm is not valid, or security is not enabled,
     * or no registry is configured, returns null.
     * 
     * @param realmName
     * @return UserRegistry object
     * @throws WSSecurityException if there is an internal error
     */
    public static UserRegistry getUserRegistry(String realmName) throws WSSecurityException {
        try {
            WSSecurityService ss = wsSecurityServiceRef.getService();
            if (ss == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No WSSecurityService, returning null");
                }
            } else {
                return ss.getUserRegistry(realmName);
            }
        } catch (WSSecurityException e) {
            String msg = "getUserRegistry for realm " + realmName + " failed due to an internal error: " + e.getMessage();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, msg, e);
            }
            throw new WSSecurityException(msg, e);
        }
        return null;
    }

    /**
     * <p>
     * The <code>getInboundTrustedRealms</code> method returns the list of inbound trusted realms
     * corresponding to the active user registry that matches this realm.
     * If the realm is null, it returns the inbound trusted realms for the realm (user registry)
     * based on the thread context.
     * The realm should be available in the process being called. If the process does
     * not host this realm, it will return an empty list.
     * If all realms are trusted, it will return "*" in the List
     * This method requires that the realm names are unique.
     * </p>
     * 
     * @param String (the realm name - null implies context based realm)
     * @return java.util.List<String> of trusted realms
     * @exception WSSecurityException
     **/
    public static List<String> getInboundTrustedRealms(String realmName) throws WSSecurityException {
        try {
            WSSecurityService ss = wsSecurityServiceRef.getService();
            if (ss == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No WSSecurityService, returning null");
                }
            } else {
                return ss.getInboundTrustedRealms(realmName);
            }
        } catch (WSSecurityException e) {
            String msg = "Failed due to an internal error: " + e.getMessage();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, msg, e);
            }
            throw new WSSecurityException(msg, e);
        }
        return null;
    }

    /**
     * Determine if the inbound realm is one of the trusted realms of the
     * specified local realm. If the local realm is null the realm of the
     * current active user registry will be used.
     * 
     * @param inboundRealm
     * @param localRealm
     * @return true - inbound realm is trusted, false - inbound reamn is not trusted
     */
    public static boolean isRealmInboundTrusted(String inboundRealm, String localRealm) {
        WSSecurityService ss = wsSecurityServiceRef.getService();
        if (ss == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No WSSecurityService, returning true");
            }
            return true;
        } else {
            return ss.isRealmInboundTrusted(inboundRealm, localRealm);
        }
    }
}
