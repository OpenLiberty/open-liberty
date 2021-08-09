/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.clients.common.AttributeToSubject;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.oauth20.UserCredentialResolver;
import com.ibm.wsspi.security.oauth20.UserIdentityException;

/**
 * This class extends AttributeToSubject to add support for the UserCredentialResolver SPI
 */
public class AttributeToSubjectExt extends AttributeToSubject {
    public static final TraceComponent tc = Tr.register(AttributeToSubject.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String JOBJ_TYPE = "jobj";
    public static final String PAYLOAD_TYPE = "payload";

    static public final String KEY_USER_RESOLVER = "userResolver"; //service name from the bnd
    static ConcurrentServiceReferenceMap<String, UserCredentialResolver> activatedUserResolverRef = new ConcurrentServiceReferenceMap<String, UserCredentialResolver>(KEY_USER_RESOLVER);

    public static void setActivatedUserResolverRef(ConcurrentServiceReferenceMap<String, UserCredentialResolver> userResolverRef) {
        activatedUserResolverRef = userResolverRef;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "activatedUserResolverRef size():" + activatedUserResolverRef.size());
        }
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ UserIdentityException.class, IOException.class })
    public AttributeToSubjectExt(OidcClientConfig clientConfig, JSONObject jobj, String accessToken) {
        earlyinit(clientConfig, accessToken);
        String jobjStr = null;
        //userName = (String) jobj.get(ClientConstants.SUB);
        if (isTokenMappingSpi()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "activatedUserResolverRef size():" + activatedUserResolverRef.size());
            }
            try {
                jobjStr = jobj.serialize();
                getTheTokenMappingFromSpi(jobjStr, clientConfig);
                //userName = getUserFromUserResolver(jobjStr);
            } catch (IOException e) {
                Tr.error(tc, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                return;
            } catch (UserIdentityException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SPI implementation throws an exception for user mapping!!", jobj.toString());
                }
                Tr.error(tc, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                return;
            }
        }

        initialize(clientConfig, jobj, accessToken);

    }

    @SuppressWarnings("unchecked")
    public AttributeToSubjectExt(OidcClientConfig clientConfig, Payload payload, String idToken) {
        earlyinit(clientConfig, idToken);
        if (isTokenMappingSpi()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "activatedUserResolverRef size():" + activatedUserResolverRef.size());
            }
            try {
                String[] jwtParts = JsonTokenUtil.splitTokenString(idToken);
                if (jwtParts.length > 1) {
                    //jwtPart[0] = header, jwtParts[1] = payload
                    //payloadStr = jwtParts[1]; //payload
                    String decodedPayload = Base64Coder.base64Decode(jwtParts[1]);
                    getTheTokenMappingFromSpi(decodedPayload, clientConfig);
                }
                //userName = getUserFromUserResolver(jobjStr);
            } catch (UserIdentityException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SPI implementation throws an exception for user mapping!!");
                }
                Tr.error(tc, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                return;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SPI implementation throws an exception for user mapping!!");
                }
                Tr.error(tc, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                return;
            }

        }
        initializep(clientConfig, payload, idToken);

    }

    String getUserFromUserResolver(String jobjStr) throws UserIdentityException {
        String userid = null;
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            userid = userIdResolver.mapToUser(jobjStr);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "spi returns user id = ", userid);
        }
        return userid;
    }

    /**
     * @param jobjStr
     * @return
     * @throws UserIdentityException
     */
    @SuppressWarnings("unused")
    private String getRealmFromUserResolver(String jobjStr) throws UserIdentityException {
        String realm = null;
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            realm = userIdResolver.mapToRealm(jobjStr);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "spi returns the realm = ", realm);
        }
        return realm;
    }

    protected boolean isTokenMappingSpi() {
        if (activatedUserResolverRef.size() > 0) {
            return true;
        }
        return false;
    }

    protected void getTheTokenMappingFromSpi(String jobjStr, OidcClientConfig clientConfig) throws UserIdentityException {
        Iterator<UserCredentialResolver> userIdResolvers = activatedUserResolverRef.getServices();
        if (userIdResolvers.hasNext()) {
            UserCredentialResolver userIdResolver = userIdResolvers.next();
            userName = userIdResolver.mapToUser(jobjStr);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "spi returns user id = ", userName);
            }
            //TODO log a warning message if the userName is null
            //The OpenID Connect client relying party (RP) or the resource server did not receive a valid [{user/realm/group/unique user name}] value from
            //the token mapping SPI. The received value is [{null or empty}] and is ignored and the runtime continues with the default configuration of the OpenID Connect client.

            if (!clientConfig.isMapIdentityToRegistryUser()) {
                realm = userIdResolver.mapToRealm(jobjStr);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "spi returns the realm = ", realm);
                }
                uniqueSecurityName = userIdResolver.mapToUserUniqueID(jobjStr);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "spi returns the unique security name = ", uniqueSecurityName);
                }
                List<String> groups = userIdResolver.mapToGroups(jobjStr);
                if (groups != null && !groups.isEmpty()) {
                    groupIds = new ArrayList<String>(groups);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "spi returns the groups and size = ", groupIds.size());
                    }
                }
            }
        }

    }

    public AttributeToSubjectExt(OidcClientConfig clientConfig, OidcTokenImplBase idToken) {
        super(clientConfig, idToken);

        if (isTokenMappingSpi()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "activatedUserResolverRef size():" + activatedUserResolverRef.size());
            }
            try {
                getTheTokenMappingFromSpi(idToken.getAllClaimsAsJson(), clientConfig);
            } catch (UserIdentityException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SPI implementation throws an exception for user mapping!!");
                }
                Tr.error(tc, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                return;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "SPI implementation throws an exception for user mapping!!");
                }
                Tr.error(tc, "PROPAGATION_TOKEN_INTERNAL_ERR", e.getLocalizedMessage(), clientConfig.getValidationMethod(), clientConfig.getValidationEndpointUrl());
                return;
            }
        }

    }

}
