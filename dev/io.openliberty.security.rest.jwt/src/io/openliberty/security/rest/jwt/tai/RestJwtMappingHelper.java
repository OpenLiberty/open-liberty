/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.security.rest.jwt.tai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class RestJwtMappingHelper {

    private static final TraceComponent tc = Tr.register(RestJwtMappingHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String INTERNAL_DISABLE_SSO_LTPA_CACHE = "com.ibm.ws.authentication.internal.sso.disable.ltpa.cache";
    public static final String CLAIM_UPN = "upn";
    public static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    public static final String CLAIM_SUB = "sub";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_GROUPS = "groups";
    public static final String CLAIM_ROLES = "roles";

    private String username;
    private String realm;
    private final String registryRealm;
    private List<String> groups;
    private final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

    public RestJwtMappingHelper(@Sensitive JwtToken jwtToken) {
        this(jwtToken, null);
    }

    /**
     * @param registryRealm realm of the configured user registry. When non-null it is used as the realm instead of
     *                          the token issuer so that groups from the token match the access IDs used by role mappings
     *                          such as {@code <administrator-role group="..."/>}.
     */
    public RestJwtMappingHelper(@Sensitive JwtToken jwtToken, String registryRealm) {
        String methodName = "<init>";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, jwtToken);
        }

        this.registryRealm = registryRealm;
        if (jwtToken != null) {
            Claims claims = jwtToken.getClaims();
            if (claims != null) {
                mapClaims(claims);
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    private void mapClaims(Claims claims) {
        // Map username: check upn -> preferred_username -> sub -> username
        String user = claims.getClaim(CLAIM_UPN, String.class);
        if (user == null || user.isEmpty()) {
            user = claims.getClaim(CLAIM_PREFERRED_USERNAME, String.class);
        }
        if (user == null || user.isEmpty()) {
            user = claims.getSubject();
        }
        if (user == null || user.isEmpty()) {
            user = claims.getClaim(CLAIM_USERNAME, String.class);
        }
        this.username = user;

        // Map realm from the user registry if available, otherwise from issuer
        this.realm = (registryRealm != null && !registryRealm.isEmpty()) ? registryRealm : claims.getIssuer();

        // Map groups
        this.groups = extractGroups(claims);

        // Populate custom properties
        populateCustomProperties();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractGroups(Claims claims) {
        List<String> groupList = new ArrayList<String>();
        Object groupsObj = claims.get(CLAIM_GROUPS);
        if (groupsObj == null) {
            groupsObj = claims.get(CLAIM_ROLES);
        }

        if (groupsObj instanceof Collection<?>) {
            for (Object obj : (Collection<?>) groupsObj) {
                if (obj != null) {
                    groupList.add(obj.toString());
                }
            }
        } else if (groupsObj instanceof String) {
            String groupsStr = (String) groupsObj;
            if (!groupsStr.isEmpty()) {
                String[] split = groupsStr.split(",");
                for (String g : split) {
                    String trimmed = g.trim();
                    if (!trimmed.isEmpty()) {
                        groupList.add(trimmed);
                    }
                }
            }
        }

        return groupList;
    }

    private void populateCustomProperties() {
        if (username != null) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, username);

            String uniqueId = (realm != null && !realm.isEmpty()) ? realm + "/" + username : username;
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueId);

            if (realm != null && !realm.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            }

            if (groups != null && !groups.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
            }

            customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
            customProperties.put(INTERNAL_DISABLE_SSO_LTPA_CACHE, Boolean.TRUE);
        }
    }

    public Subject createSubject() {
        String methodName = "createSubject";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }

        Subject subject = new Subject();
        if (!customProperties.isEmpty()) {
            subject.getPrivateCredentials().add(customProperties);
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, subject);
        }
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public String getRealm() {
        return realm;
    }

    public List<String> getGroups() {
        return groups;
    }

    public Hashtable<String, Object> getCustomProperties() {
        return customProperties;
    }
}
