/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.internal;

import java.util.Hashtable;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.client.jose4j.OidcTokenImpl;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * This class handles OpenID Connect client for incoming web requests.
 */
public class OidcClientAuthenticator {

    private static final TraceComponent tc = Tr.register(OidcClientAuthenticator.class);

    OidcClientUtil oidcClientUtil = new OidcClientUtil();
    private SSLSupport sslSupport;
    private OIDCClientAuthenticatorUtil authenticatorUtil = null;

    public OidcClientAuthenticator() {
        authenticatorUtil = new OIDCClientAuthenticatorUtil();
    }

    /**
     * @param sslSupportRef
     * @param clientConfig
     */
    public OidcClientAuthenticator(AtomicServiceReference<SSLSupport> sslSupportRef) {
        this.sslSupport = sslSupportRef.getService();
        authenticatorUtil = new OIDCClientAuthenticatorUtil(sslSupport);
    }

    /**
     * Perform OpenID Connect client authenticate for the given web request.
     * Return an OidcAuthenticationResult which contains the status and subject
     *
     * A routine flow will come through here twice. First there's no state and it goes to handleRedirectToServer
     *
     * second time, oidcclientimpl.authenticate sends us here after the browser has been to the OP and
     * come back with a WAS_OIDC_STATE cookie, and an auth code or implicit token.
     *
     * @param HttpServletRequest
     * @param HttpServletResponse
     * @param OidcClientConfig
     * @return OidcAuthenticationResult
     */
    public ProviderAuthenticationResult authenticate(HttpServletRequest req,
            HttpServletResponse res,
            OidcClientConfig clientConfig) {

        ProviderAuthenticationResult result = authenticatorUtil.authenticate(req, res, clientConfig);
        result = discoverOPAgain(result, clientConfig);
        result = fixSubject(result);
        result = invokeUserResolverSPI(result, clientConfig);
        return result;
    }

    /**
     * @param result
     * @param clientConfig
     * @return
     */
    private ProviderAuthenticationResult discoverOPAgain(ProviderAuthenticationResult result, OidcClientConfig clientConfig) {
        com.ibm.ws.security.openidconnect.client.internal.OidcClientConfigImpl oidcConfigImpl = (com.ibm.ws.security.openidconnect.client.internal.OidcClientConfigImpl) clientConfig;
        if (result.getStatus() != AuthResult.SUCCESS && oidcConfigImpl.isDiscoveryInUse() && System.currentTimeMillis() > oidcConfigImpl.getNextDiscoveryTime()) {
            oidcConfigImpl.handleDiscoveryEndpoint(oidcConfigImpl.getDiscoveryEndpointUrl());
        } else if (result.getStatus() == AuthResult.SUCCESS && oidcConfigImpl.isDiscoveryInUse()) {
            oidcConfigImpl.setNextDiscoveryTime();
        }
        return result;
    }

    /**
     * if a user resolver spi implementation is present, reprocess the id token using the user resolver.
     *
     * @param result
     * @return
     */
    private ProviderAuthenticationResult invokeUserResolverSPI(ProviderAuthenticationResult result, OidcClientConfig config) {
        if (result.getCustomProperties() == null) {
            return result; // first time through, not what we're looking for
        }
        OidcTokenImplBase idToken = (OidcTokenImplBase) result.getCustomProperties().get(Constants.ID_TOKEN_OBJECT);
        if (idToken == null) {
            return result; // shouldn't happen
        } else {
            result.getCustomProperties().remove(Constants.ID_TOKEN_OBJECT);
        }
        AttributeToSubjectExt a2s = new AttributeToSubjectExt(config, idToken);
        if (!a2s.isTokenMappingSpi()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "token mapping SPI is not active");
            }
            return result;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "token mapping SPI is active, updating mapping.");
        }
        //clear any properties that were set by the first round of mapping, as results might change.
        Hashtable<String, Object> customProperties = result.getCustomProperties();
        customProperties.remove(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
        customProperties.remove(AttributeNameConstants.WSCREDENTIAL_REALM);
        customProperties.remove(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        result = a2s.doMapping(result.getCustomProperties(), result.getSubject());
        return result;

    }

    /**
     * If the id token was stored in the subject, replace it with one that implements the IdToken interface.
     * This is done to hide that CL API interface from OL.
     *
     * @param result
     * @return
     */
    public static ProviderAuthenticationResult fixSubject(ProviderAuthenticationResult result) {
        if (result.getSubject() == null) {
            return result;
        }
        Set<Object> creds = result.getSubject().getPrivateCredentials();
        Set<OidcTokenImplBase> oidcTokens = result.getSubject().getPrivateCredentials(OidcTokenImplBase.class);
        for (OidcTokenImplBase oidcToken : oidcTokens) {
            creds.remove(oidcToken);
            creds.add(new OidcTokenImpl(oidcToken));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "idToken in subject is replaced with OidcTokenImpl");
            }
        }
        return result;
    }

}
