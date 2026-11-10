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
import com.ibm.ws.security.AccessIdUtil;
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
    public static final String CLAIM_GROUP = "group";

    private String username;
    private String userAccessId;
    private String realm;
    private List<String> groups;
    private final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

    /**
     * The node agent builds the JWT with either a server access ID or a user access ID as the subject, for example
     * {@code server:defaultWIMFileBasedRealm/server:ndcell_node1_nodeagent} or
     * {@code user:defaultWIMFileBasedRealm/adminUser}, and with plain group names in the groups claim. The realm is
     * taken from the access ID and is used to turn each plain group name into a group access ID,
     * {@code group:<realm>/<name>}, which is what the role mappings in server.xml match against.
     * <p>
     * Either the subject or a group may carry the access ID. If the subject is a plain name, the realm is taken from
     * the first group that is a group access ID, and the user access ID is built from it.
     *
     * @throws IllegalArgumentException if neither the subject nor any group is an access ID, so there is no realm
     */
    public RestJwtMappingHelper(@Sensitive JwtToken jwtToken) {
        if (jwtToken != null) {
            Claims claims = jwtToken.getClaims();
            if (claims != null) {
                mapClaims(claims);
            }
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
        List<String> groupNames = extractGroups(claims);

        if (AccessIdUtil.isUserAccessId(user) || AccessIdUtil.isServerAccessId(user)) {
            // Use the access ID as is, with the realm and the name taken from it, so that no user registry lookup is
            // needed to build the credential.
            this.userAccessId = user;
            this.realm = AccessIdUtil.getRealm(user);
            this.username = AccessIdUtil.getUniqueId(user);
        } else {
            // The subject is a plain name. Take the realm from the first group access ID instead.
            String groupRealm = null;
            for (String group : groupNames) {
                if (AccessIdUtil.isGroupAccessId(group)) {
                    groupRealm = AccessIdUtil.getRealm(group);
                    break;
                }
            }
            if (user == null || user.isEmpty() || groupRealm == null) {
                throw new IllegalArgumentException("Neither the subject of the JWT nor any of its groups is an access ID, so there is no realm. Subject: " + user);
            }
            this.realm = groupRealm;
            this.username = user;
            this.userAccessId = AccessIdUtil.createAccessId(AccessIdUtil.TYPE_USER, groupRealm, user);
        }

        // Map groups, building a group access ID from the realm for each plain group name
        this.groups = toGroupAccessIds(groupNames, this.realm);

        // Populate custom properties
        populateCustomProperties();
    }

    private List<String> toGroupAccessIds(List<String> groupNames, String groupRealm) {
        List<String> accessIds = new ArrayList<String>();
        for (String group : groupNames) {
            if (AccessIdUtil.isGroupAccessId(group)) {
                accessIds.add(group);
            } else {
                accessIds.add(AccessIdUtil.createAccessId(AccessIdUtil.TYPE_GROUP, groupRealm, group));
            }
        }
        return accessIds;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractGroups(Claims claims) {
        List<String> groupList = new ArrayList<String>();
        Object groupsObj = claims.get(CLAIM_GROUPS);
        if (groupsObj == null) {
            groupsObj = claims.get(CLAIM_ROLES);
        }
        if (groupsObj == null) {
            groupsObj = claims.get(CLAIM_GROUP);
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

            String uniqueId = userAccessId != null ? userAccessId : (realm != null && !realm.isEmpty()) ? realm + "/" + username : username;
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
        Subject subject = new Subject();
        if (!customProperties.isEmpty()) {
            subject.getPrivateCredentials().add(customProperties);
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
