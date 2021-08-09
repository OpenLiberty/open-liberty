/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.claims;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User claims for the id token, token introspection, identity assertion, and resource authorization.
 */
public class UserClaims {

    public static final String USER_CLAIMS_UNIQUE_SECURITY_NAME = "uniqueSecurityName";
    public static final String USER_CLAIMS_REALM_NAME = "realmName";

    protected Map<String, Object> claimsMap;
    protected String userName;
    protected String groupIdentifier;

    public UserClaims(String userName, String groupIdentifier) {
        claimsMap = new ConcurrentHashMap<String, Object>();
        this.userName = userName;
        this.groupIdentifier = groupIdentifier;
    }

    public UserClaims(Map<String, Object> claimsMap, String userName, String groupIdentifier) {
        this.claimsMap = claimsMap;
        this.userName = userName;
        this.groupIdentifier = groupIdentifier;
    }

    public boolean isEnabled() {
        return userName != null;
    }

    /**
     * Sets the user's groups claim.
     * @param groups
     */
    public void setGroups(List<String> groups) {
        claimsMap.put(groupIdentifier, groups);
    }

    /**
     * Returns the user's groups claim.
     */
    @SuppressWarnings("unchecked")
    public List<String> getGroups() {
        return (List<String>) claimsMap.get(groupIdentifier);
    }

    /**
     * Sets the user's realmName claim.
     */
    public void setRealmName(String realmName) {
        claimsMap.put(USER_CLAIMS_REALM_NAME, realmName);
    }

    /**
     * Returns the user's realmName claim.
     */
    public String getRealmName() {
        return (String) claimsMap.get(USER_CLAIMS_REALM_NAME);
    }

    /**
     * Sets the user's uniqueSecurityName claim.
     * @param uniqueSecurityName
     */
    public void setUniqueSecurityName(String uniqueSecurityName) {
        claimsMap.put(USER_CLAIMS_UNIQUE_SECURITY_NAME, uniqueSecurityName);
    }

    /**
     * Returns the user's uniqueSecurityName claim.
     */
    public String getUniqueSecurityName() {
        return (String) claimsMap.get(USER_CLAIMS_UNIQUE_SECURITY_NAME);
    }

    /**
     * Returns the claims map to be used when adding the user claims
     * as a payload to the id token or introspection.
     */
    public Map<String, Object> asMap() {
        return claimsMap;
    }

    /**
     * @return the groupIdentifier
     */
    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    /**
     * @return the groupIdentifier
     */
    public String getUserName() {
        return userName;
    }
}
