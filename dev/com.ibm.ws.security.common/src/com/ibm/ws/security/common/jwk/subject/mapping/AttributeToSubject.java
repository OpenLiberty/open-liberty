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
package com.ibm.ws.security.common.jwk.subject.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;

/**
 *
 */
public class AttributeToSubject {
    public static final TraceComponent tc = Tr.register(AttributeToSubject.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    String realm = null;
    String uniqueSecurityName = null;
    String userName = null;
    String clientId = null;

    ArrayList<String> groupIds = null;
    String userApiRespId = null;

    @SuppressWarnings("unchecked")
    public AttributeToSubject(String jsonstr, String userAttr, String uniqueNameAttr,
            String realmName, String realmAttr, String groupAttr, boolean mapToUr, String userApiRespIdentifier) {
        userName = getTheUserName(userAttr, jsonstr, userApiRespIdentifier);

        if (userName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "user name = ", userName);
            }
            //customCacheKey = userName + tokenString.hashCode();

            if (!mapToUr) {
                if (realm == null || realm.isEmpty()) {
                    realm = getTheRealmName(realmName, realmAttr, jsonstr);
                }

                if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                    uniqueSecurityName = getTheUniqueSecurityName(uniqueNameAttr, jsonstr);
                }
                Object group = null;
                if (groupAttr != null) {
                    try {
                        group = JsonUtils.claimFromJsonObject(jsonstr, groupAttr);
                    } catch (JoseException e) {

                    }
                }
                if (group != null) {
                    if (group instanceof ArrayList<?>) {
                        groupIds = (ArrayList<String>) group;
                    } else { // try if there is a single string identified as group
                        try {
                            String groupName = (String) group;
                            groupIds = new ArrayList<String>();
                            groupIds.add(groupName);
                        } catch (ClassCastException cce) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "can not get meaningful group due to CCE.");
                            }
                        }
                    }
                }
                if (groupIds != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "groups size = ", groupIds.size());
                }
            }
        }
    }

    public boolean checkUserNameForNull() {
        if (userName == null || userName.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There is no principal");
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean checkForNullRealm() {
        if (realm == null || realm.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There is no realm");
            }
            return true;
        } else {
            return false;
        }
    }

    public String getMappedUser() {
        return userName;
    }

    public String getMappedUniqueUser() {
        return uniqueSecurityName;
    }

    public String getMappedRealm() {
        return realm;
    }

    public ArrayList<String> getMappedGroups() {
        return groupIds;
    }

    private String getTheUserName(String userNameAttr, String jsonstr, String userApiRespIdentifier) {
        if (jsonstr != null) {
            if (userNameAttr != null && !userNameAttr.isEmpty()) {
                Object user = null;
                try {
                    user = JsonUtils.claimFromJsonObject(jsonstr, userNameAttr);
                } catch (JoseException e) {

                }
                if (user != null) {
                    if (user instanceof String) {
                        userName = (String) user;
                    } else {
                        if (userApiRespIdentifier != null) {
                            userName = handleDifferentDataTypes(user, userNameAttr, userApiRespIdentifier);
                        } else {
                            Tr.error(tc, "SUBJECT_MAPPING_INCORRECT_CLAIM_TYPE", new Object[] { userNameAttr, "userNameAttribute" });
                        }
                    }
                } else {
                    try {
                        userName = parseTheJsonAgain(jsonstr, userNameAttr);
                    } catch (JoseException e) {

                    }
                }
            }
        }
        if (userName == null) {
            Tr.error(tc, "SUBJECT_MAPPING_MISSING_ATTR", new Object[] { userNameAttr, "userNameAttribute" });
        }
        return userName;
    }

    private String parseTheJsonAgain(String jsonstr, String attr) throws JoseException {
        Map map = JsonUtils.claimsFromJsonObject(jsonstr);
        Set keys = map.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object obj = map.get(key);
            if (obj instanceof Map) {
                if (((Map) obj).containsKey(attr)) {
                    try {
                        return (String) ((Map) obj).get(attr);
                    } catch (ClassCastException cce) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private String handleDifferentDataTypes(Object user, String userNameAttr, String userApiRespIdentifier) {
        // TODO Auto-generated method stub
        String name = null;
        boolean logerror = false;
        try {
            if (user instanceof Map) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "response is a map");
                }
                name = (String) ((Map) user).get(userNameAttr);

            } else if (user instanceof ArrayList<?>) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "response is an array list");
                }
                try {
                    ArrayList<Map> list = (ArrayList<Map>) user;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "response is an array list of maps");
                    }
                    for (int i = 0; i < list.size(); i++) {
                        Map map = (Map) list.get(i);
                        if (map.containsKey(userApiRespIdentifier) && (Boolean) map.get(userApiRespIdentifier)) {
                            name = (String) map.get(userNameAttr);
                            break;
                        }
                    }
                } catch (ClassCastException cce) {
                    logerror = true;
                }
            } else {
                logerror = true;
            }

        } catch (Exception e) {
            logerror = true;
        }
        if (logerror) {
            Tr.error(tc, "SUBJECT_MAPPING_INCORRECT_CLAIM_TYPE", new Object[] { userNameAttr, "userNameAttribute" });
        }
        return name;
    }

    protected String getTheRealmName(String realmNameFromConfig, String realmAttr, String jsonstr) {

        if (realmNameFromConfig != null) {
            realm = realmNameFromConfig;
            return realm;
        }
        if (jsonstr != null) {
            //look at the realmName first and then the realmIdentifier and iss
            if (realmAttr != null && !realmAttr.isEmpty()) {
                Object realmfrom = null;
                try {
                    realmfrom = JsonUtils.claimFromJsonObject(jsonstr, realmAttr);
                } catch (JoseException e) {

                }
                if (realmfrom != null) {
                    if (realmfrom instanceof String) {
                        realm = (String) realmfrom;
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "realm name = ", realm);
            }
        }
        return realm;
    }

    protected String getTheUniqueSecurityName(String uniqueUserAttr, String jsonstr) {

        if (jsonstr != null) {
            if (uniqueUserAttr != null && !uniqueUserAttr.isEmpty()) {
                Object unique = null;
                try {
                    unique = JsonUtils.claimFromJsonObject(jsonstr, uniqueUserAttr);
                } catch (JoseException e) {

                }
                if (unique != null) {
                    if (unique instanceof String) {
                        uniqueSecurityName = (String) unique;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "unique security name = ", uniqueSecurityName);
                    }
                }
            }
            if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                uniqueSecurityName = userName;
            }
        }
        return uniqueSecurityName;
    }
}
