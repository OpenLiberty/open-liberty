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
package com.ibm.ws.security.intfc;

import java.util.List;

import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;

/**
 *
 */
public interface WSSecurityService {

    public final String KEY_WS_SECURITY_SERVICE = "wsSecurityService";

    public boolean isSecurityEnabled();

    /**
     * Gets the UserRegistry object for the given realm. If the realm name is null
     * returns the active registry. If the realm is not valid, or security is not
     * enabled, or no registry is configured, returns null.
     * 
     * @param realmName
     * @return UserRegistry object
     * @throws WSSecurityException if there is an internal error
     */
    public UserRegistry getUserRegistry(String realmName) throws WSSecurityException;

    /**
     * Gets the realms trusted for the given realm. If the realm name is null
     * returns the realms trusted for active user registry. If the realm is not valid,
     * or security is not enabled, or no registry is configured, returns an empty list.
     * 
     * @param realmName
     * @return list of trusted realms
     * @throws WSSecurityException if there is an internal error
     */
    public List<String> getInboundTrustedRealms(String realmName) throws WSSecurityException;

    /**
     * 
     * @param inboundRealm
     * @param localRealm
     * @return
     */
    public boolean isRealmInboundTrusted(String inboundRealm, String localRealm);
}
