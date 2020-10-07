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

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * Gets the groups for the user from the Subject in the AuthCache or from the user registry.
 */
public class UserClaimsRetriever {

    private static final TraceComponent tc = Tr.register(UserClaimsRetriever.class);

    private final AuthCacheService authCacheService;
    private final UserRegistry userRegistry;

    /**
     * @param authCacheService
     * @param userRegistry
     */
    public UserClaimsRetriever(AuthCacheService authCacheService, UserRegistry userRegistry) {
        this.authCacheService = authCacheService;
        this.userRegistry = userRegistry;
    }

    /**
     * @param username
     * @param groupIdentifier
     * @return a Map containing the user's claims
     * @throws WSSecurityException
     */
    public UserClaims getUserClaims(String username, String groupIdentifier) throws WSSecurityException {
        UserClaims userClaims = new UserClaims(username, groupIdentifier);
        String realmName = userRegistry.getRealm();

        String cacheKey = getCacheKey(realmName, username);
        Subject subject = authCacheService.getSubject(cacheKey);
        if (subject == null) { // If we can not find the user from cache, we get it from RunAsSubject
            subject = WSSubject.getRunAsSubject();
            if (subject != null && !isUnAuthenticated(subject, username)) {
                realmName = getRealmName(subject);
            } else {
                subject = null;
            }
        }
        userClaims.setRealmName(realmName);

        if (subject != null) {
            populateClaimsFromCachedSubject(userClaims, subject);
        } else {
            populateClaimsFromRegistry(userClaims, realmName, username);
        }

        return userClaims;
    }

    /**
     * @param subject
     * @return
     * @throws Exception
     */
    String getRealmName(Subject subject) {
        WSCredential wsCredential = getWSCredential(subject);
        try {
            return wsCredential.getRealmName();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RealmName Exception:" + e.getClass().getName() + " MSG:" + e);
            }
        }
        return null;
    }

    private String getCacheKey(String realmName, String username) {
        return realmName + ":" + username;
    }

    @SuppressWarnings({ "unchecked" })
    @FFDCIgnore(Exception.class)
    private void populateClaimsFromCachedSubject(UserClaims userClaims, Subject subject) {
        WSCredential wsCredential = getWSCredential(subject);
        try {
            List<String> groups = extractGroups(wsCredential.getGroupIds());
            setClaims(userClaims, wsCredential.getUniqueSecurityName(), groups);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was an exception populating the user claims from the cached subject.", e);
            }
        }
    }

    /**
     * @param subject
     * @return
     */
    WSCredential getWSCredential(Subject subject) {
        SubjectHelper subjectHelper = new SubjectHelper();
        return subjectHelper.getWSCredential(subject);
    }

    boolean isUnAuthenticated(Subject subject, String username) {
        WSCredential cred = getWSCredential(subject);
        if (cred == null) {
            return true;
        }

        try {
            if (username.equals(cred.getSecurityName()) && !cred.isUnauthenticated()) {
                return false; // Authenticated
            }
        } catch (Exception e) {
            // ignore it
        }
        // un-authenticated
        return true;
    }

    private List<String> extractGroups(List<String> groupIds) {
        List<String> groups = new ArrayList<String>();
        for (String groupId : groupIds) {
            String group = AccessIdUtil.getUniqueId(groupId);
            if (group != null) {
                groups.add(group);
            }
        }
        return groups;
    }

    @FFDCIgnore(Exception.class)
    private void populateClaimsFromRegistry(UserClaims userClaims, String realmName, String username) {
        try {
            setClaims(userClaims, userRegistry.getUniqueUserId(username), userRegistry.getGroupsForUser(username));
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was an exception populating the user claims from the user registry.", e);
            }
        }
    }

    private void setClaims(UserClaims userClaims, String uniqueSecurityName, List<String> groups) {
        userClaims.setUniqueSecurityName(uniqueSecurityName);
        if (groups.isEmpty() == false) {
            userClaims.setGroups(groups);
        }
    }

}
