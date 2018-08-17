/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class AttributeToSubject {
    public static final TraceComponent tc = Tr.register(AttributeToSubject.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String JOBJ_TYPE = "jobj";
    public static final String PAYLOAD_TYPE = "payload";
    protected String realm = null;
    protected String uniqueSecurityName = null;
    protected String userName = null;

    protected String tokenString = null;
    protected String customCacheKey = null;
    protected String clientId = null;

    protected ArrayList<String> groupIds = null;
    protected ConvergedClientConfig clientConfig;

    public AttributeToSubject() {
    }

    public AttributeToSubject(ConvergedClientConfig clientConfig, JSONObject jobj, String accessToken) {
        earlyinit(clientConfig, accessToken);
        initialize(clientConfig, jobj, accessToken);
    }

    public void earlyinit(ConvergedClientConfig clientConfig, String tokenStr) {
        tokenString = tokenStr;
        this.clientConfig = clientConfig;
        clientId = clientConfig.getClientId();
    }

    @SuppressWarnings("unchecked")
    public void initialize(ConvergedClientConfig clientConfig, JSONObject jobj, String accessToken) {

        if (userName == null || userName.isEmpty()) {
            userName = getTheUserName(clientConfig, jobj);
        }

        if (userName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "user name = ", userName);
            }
            customCacheKey = userName + tokenString.hashCode();

            if (!clientConfig.isMapIdentityToRegistryUser()) {
                if (realm == null || realm.isEmpty()) {
                    realm = getTheRealmName(clientConfig, jobj, null);
                }

                if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                    uniqueSecurityName = getTheUniqueSecurityName(clientConfig, jobj, null);
                }

                if (groupIds == null || groupIds.isEmpty()) {
                    if (jobj.get(clientConfig.getGroupIdentifier()) != null) {
                        if (jobj.get(clientConfig.getGroupIdentifier()) instanceof ArrayList<?>) {
                            groupIds = (ArrayList<String>) jobj.get(clientConfig.getGroupIdentifier());
                        } else { // try if there is a single string identified as group
                            try {
                                String group = (String) jobj.get(clientConfig.getGroupIdentifier());
                                groupIds = new ArrayList<String>();
                                groupIds.add(group);
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
    }

    public AttributeToSubject(ConvergedClientConfig clientConfig, Payload payload, String idToken) {
        earlyinit(clientConfig, idToken);
        initializep(clientConfig, payload, idToken);
    }

    @SuppressWarnings("unchecked")
    public void initializep(ConvergedClientConfig clientConfig, Payload payload, String idToken) {

        String uid = null;
        if (userName == null || userName.isEmpty()) {
            String attrUsedToCreateSubject = "userIdentifier";

            uid = clientConfig.getUserIdentifier();
            if (uid != null && !uid.isEmpty()) {
                userName = (String) payload.get(uid);
            } else {
                attrUsedToCreateSubject = "userIdentityToCreateSubject";
                uid = clientConfig.getUserIdentityToCreateSubject();
                if (uid == null || uid.isEmpty()) {
                    uid = ClientConstants.SUB;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "The userIdentityToCreateSubject config attribute is null or empty; defaulting to " + uid);
                    }
                }
                userName = (String) payload.get(uid);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "user name = '" + userName + "' and the user identifier = " + uid + " " + (userName == null));
            }
            if (userName == null) {
                Tr.error(tc, "OIDC_CLIENT_ID_TOKEN_MISSING_CLAIM", new Object[] { clientId, uid, attrUsedToCreateSubject });
                return;
            }
        }

        customCacheKey = userName + tokenString.toString().hashCode();

        if (!clientConfig.isMapIdentityToRegistryUser()) {
            if (realm == null || realm.isEmpty()) {
                realm = getTheRealmName(clientConfig, null, payload);
            }

            if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                uniqueSecurityName = getTheUniqueSecurityName(clientConfig, null, payload);
            }

            if (groupIds == null || groupIds.isEmpty()) {
                groupIds = (ArrayList<String>) payload.get(clientConfig.getGroupIdentifier());
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

    public Hashtable<String, Object> handleCustomProperties() {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        if (clientConfig.isIncludeCustomCacheKeyInSubject()) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
            customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        }
        return customProperties;

    }

    public ProviderAuthenticationResult doMapping(Hashtable<String, Object> customProperties, Subject subject) {
        if (!clientConfig.isMapIdentityToRegistryUser()) {
            String uniqueID = new StringBuffer("user:").append(realm).append("/").append(uniqueSecurityName).toString();

            ArrayList<String> groups = new ArrayList<String>();
            if (groupIds != null && !groupIds.isEmpty()) {
                Iterator<String> it = groupIds.iterator();
                while (it.hasNext()) {
                    String group = new StringBuffer("group:").append(realm).append("/").append(it.next()).toString();
                    groups.add(group);
                }
            }

            customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
            if (realm != null && !realm.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            }
            if (groups != null && !groups.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
            }
        }

        if (!customProperties.containsKey(ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS)) {
            // save the current time to be the approximate base for calculating the expiration time of access_token
            long storingTime = new Date().getTime();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find custom property for credential storage time, so recording current time = " + storingTime);
            }
            customProperties.put(ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS, Long.valueOf(storingTime)); // this is GMT/UTC time already
        }
        ProviderAuthenticationResult oidcResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, userName, subject, customProperties, null);
        return oidcResult;
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

    protected String getTheUserName(ConvergedClientConfig clientConfig, JSONObject jobj) {

        if (jobj != null) {
            String uid = clientConfig.getUserIdentifier();
            if (uid != null && !uid.isEmpty()) {
                if (jobj.get(uid) != null) {
                    if (jobj.get(uid) instanceof String) {
                        userName = (String) jobj.get(uid);
                    } else {
                        Tr.error(tc, "PROPAGATION_TOKEN_INCORRECT_CLAIM_TYPE", new Object[] { clientConfig.getUserIdentifier(), "userIdentifier" });
                    }
                } else {
                    Tr.error(tc, "PROPAGATION_TOKEN_MISSING_USERID", new Object[] { clientConfig.getUserIdentifier(), "userIdentifier" });
                }
            } else {
                String uidToCreateSub = clientConfig.getUserIdentityToCreateSubject();
                if (jobj.get(uidToCreateSub) != null) {
                    if (jobj.get(uidToCreateSub) instanceof String) {
                        userName = (String) jobj.get(uidToCreateSub);
                    } else {
                        Tr.error(tc, "PROPAGATION_TOKEN_INCORRECT_CLAIM_TYPE", new Object[] { uidToCreateSub, "userIdentityToCreateSubject" });
                    }
                } else {
                    Tr.error(tc, "PROPAGATION_TOKEN_MISSING_USERID", new Object[] { uidToCreateSub, "userIdentityToCreateSubject" });
                }
            }
            return userName;
        }
        return null;

    }

    protected String getTheRealmName(ConvergedClientConfig clientConfig, JSONObject jobj, Payload payload) {

        if (jobj != null) {
            //look at the realmName first and then the realmIdentifier and iss
            String realmName = null;
            realmName = clientConfig.getRealmName();
            if (realmName != null && !realmName.isEmpty()) {
                realm = realmName;
            } else {
                if (jobj.get(clientConfig.getRealmIdentifier()) != null) {
                    if (jobj.get(clientConfig.getRealmIdentifier()) instanceof String) {
                        realm = (String) jobj.get(clientConfig.getRealmIdentifier());
                    }
                }

                if (realm == null || realm.isEmpty()) {
                    realm = (String) jobj.get(ClientConstants.ISS);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "realm name = ", realm);
            }
            return realm;
        } else if (payload != null) {
            //look at the realmName first and then the realmIdentifier and iss
            String realmName = null;
            realmName = clientConfig.getRealmName();
            if (realmName != null && !realmName.isEmpty()) {
                realm = realmName;
            } else {
                realm = (String) payload.get(clientConfig.getRealmIdentifier());
                if (realm == null || realm.isEmpty()) {
                    realm = (String) payload.get(ClientConstants.ISS);
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "realm name = ", realm);
            }
            return realm;
        }
        return null;

    }

    protected String getTheUniqueSecurityName(ConvergedClientConfig clientConfig, JSONObject jobj, Payload payload) {

        if (jobj != null) {
            if (jobj.get(clientConfig.getUniqueUserIdentifier()) != null) {
                if (jobj.get(clientConfig.getUniqueUserIdentifier()) instanceof String) {
                    uniqueSecurityName = (String) jobj.get(clientConfig.getUniqueUserIdentifier());
                }
            }
            if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                uniqueSecurityName = userName;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unique security name = ", uniqueSecurityName);
            }
            return uniqueSecurityName;
        } else if (payload != null) {
            uniqueSecurityName = (String) payload.get(clientConfig.getUniqueUserIdentifier());
            if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                uniqueSecurityName = userName;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unique security name = ", uniqueSecurityName);
            }
            return uniqueSecurityName;
        }
        return null;

    }

    @SuppressWarnings("unchecked")
    public AttributeToSubject(ConvergedClientConfig clientConfig, OidcTokenImplBase idToken) {
        this.clientConfig = clientConfig;
        clientId = clientConfig.getClientId();
        tokenString = idToken.getAllClaimsAsJson();

        String uid = null;
        String attrUsedToCreateSubject = "userIdentifier";
        if (userName == null || userName.isEmpty()) {
            uid = clientConfig.getUserIdentifier();
            if (uid != null && !uid.isEmpty()) {
                userName = (String) idToken.getClaim(uid);
            } else {
                attrUsedToCreateSubject = "userIdentityToCreateSubject";
                uid = clientConfig.getUserIdentityToCreateSubject();
                if (uid != null && !uid.isEmpty()) {
                    //uid = ClientConstants.SUB;
                    userName = (String) idToken.getClaim(uid);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "The userIdentityToCreateSubject config attribute is used");
                    }
                }

            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "user name = '" + userName + "' and the user identifier = " + uid);
            }
        }

        if (userName == null) {
            Tr.error(tc, "OIDC_CLIENT_JWT_MISSING_CLAIM", new Object[] { clientId, uid, attrUsedToCreateSubject });
            //Tr.error(tc, "OIDC_CLIENT_MISSING_PRINCIPAL_ERR", new Object[] { clientId });
        } else {
            customCacheKey = userName + tokenString.toString().hashCode();

            if (!clientConfig.isMapIdentityToRegistryUser()) {
                if (realm == null || realm.isEmpty()) {
                    realm = clientConfig.getRealmName();
                    if (realm == null) {
                        if (idToken.getClaim(clientConfig.getRealmIdentifier()) != null) {
                            if (idToken.getClaim(clientConfig.getRealmIdentifier()) instanceof String) {
                                realm = (String) idToken.getClaim(clientConfig.getRealmIdentifier());
                            }
                        }
                        if (realm == null || realm.isEmpty()) {
                            realm = (String) idToken.getClaim(ClientConstants.ISS);
                        }
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "realm name = ", realm);
                    }
                }

                if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                    if (idToken.getClaim(clientConfig.getUniqueUserIdentifier()) instanceof String) {
                        uniqueSecurityName = (String) idToken.getClaim(clientConfig.getUniqueUserIdentifier());
                    }
                    if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
                        uniqueSecurityName = userName;
                    }
                }
                if (groupIds == null || groupIds.isEmpty()) {
                    Object objGroupIds = idToken.getClaim(clientConfig.getGroupIdentifier());
                    if (objGroupIds != null) {
                        if (objGroupIds instanceof ArrayList<?>) {
                            groupIds = (ArrayList<String>) objGroupIds;
                        } else {
                            if (groupIds == null)
                                groupIds = new ArrayList<String>();
                            groupIds.add((String) objGroupIds);
                        }
                    }
                    if (groupIds != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "objGroupIds" + objGroupIds + " groups size = ", groupIds.size());
                    }
                }
            }
        }
    }

}
