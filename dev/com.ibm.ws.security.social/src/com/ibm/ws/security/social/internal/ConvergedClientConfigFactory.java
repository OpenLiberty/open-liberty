/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social.internal;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.social.error.SocialLoginException;

public class ConvergedClientConfigFactory {
    public ConvergedClientConfig createConfig(OidcLoginConfigImpl oc) {
        ConvergedClientConfig cc = new ConvergedClientConfig();
        cc.setAuthorizationEndpoint(oc.getAuthorizationEndpoint());
        cc.setClientId(oc.getClientId());
        cc.setClientSecret(oc.getClientSecret());
        cc.setClientSideRedirect(oc.isClientSideRedirectSupported);
        cc.setContextRoot(oc.getContextRoot());
        cc.setGroupIdentifier(oc.getGroupNameAttribute());
        cc.setJwkEndpointUrl(oc.getJwkEndpointUrl());
        cc.setMapIdentityToRegistryUser(oc.getMapToUserRegistry());
        cc.setNonceEndabled(oc.createNonce());
        try {
            cc.setPublicKey(oc.getPublicKey());
        } catch (SocialLoginException e) {
            // ffdc generated
        }
        cc.setRealmIdentifier(oc.getRealmNameAttribute());
        cc.setRealmName(oc.getRealmName());
        cc.setRedirectToRPHostAndPort(oc.getRedirectToRPHostAndPort());
        cc.setResources(spaceDelimStringToArray(oc.getResource()));
        cc.setResponseType(oc.getResponseType());
        cc.setScope(oc.getScope());
        cc.setSharedKey(oc.getSharedKey());
        cc.setSignatureAlgorithm(oc.getSignatureAlgorithm());
        cc.setSSLConfigurationName(oc.getSslRef());
        cc.setSSLRef(oc.getSslRef());
        cc.setTokenEndpoint(oc.getTokenEndpoint());
        cc.setTokenEndpointAuthMethod(oc.getTokenEndpointAuthMethod());
        cc.setUniqueId(oc.getUniqueId());
        cc.setUniqueUserIdentifier(oc.getUserUniqueIdAttribute());
        cc.setUserIdentifier(oc.getUserNameAttribute());
        cc.setUserIdentityToCreateSubject(oc.getUserNameAttribute());
        
        cc.setClockSkewSec((int) (oc.getClockSkew() / 1000));
        cc.setIsHostNameVerificationEnabled(oc.isHostNameVerificationEnabled());
        cc.setJwkSet(oc.getJwkSet());        
        cc.setIssuer(oc.getIssuer());
        cc.setAudiences(oc.getAudiences());
        cc.setInitialized();

        return cc;
    }

    private String[] spaceDelimStringToArray(String resources) {
        ArrayList<String> al = new ArrayList<String>();
        if (resources != null && resources.length() > 0) {
            StringTokenizer st = new StringTokenizer(resources, " ");
            while (st.hasMoreTokens()) {
                al.add(st.nextToken());
            }
        }
        return al.toArray((new String[0]));
    }

}
