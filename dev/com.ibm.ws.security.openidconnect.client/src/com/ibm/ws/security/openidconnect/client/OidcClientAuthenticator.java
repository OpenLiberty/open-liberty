/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.openidconnect.client.jose4j.OidcTokenImpl;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.security.openidconnect.clients.common.AuthorizationCodeHandler;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * This class handles OpenID Connect client for incoming web requests.
 */
public class OidcClientAuthenticator {

    private static final TraceComponent tc = Tr.register(OidcClientAuthenticator.class);

    OidcClientUtil oidcClientUtil = new OidcClientUtil();
    private SSLSupport sslSupport;
    private final JwKRetriever retriever = null;
    private Jose4jUtil jose4jUtil = null;
    private OIDCClientAuthenticatorUtil authenticatorUtil = null;

    private static final String SIGNATURE_ALG_HS256 = "HS256";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String SIGNATURE_ALG_NONE = "none";

    public OidcClientAuthenticator() {
        authenticatorUtil = new OIDCClientAuthenticatorUtil();
    }

    private final AuthorizationCodeHandler authzCodeHandler = null;

    /**
     * @param sslSupportRef
     * @param clientConfig
     */
    public OidcClientAuthenticator(AtomicServiceReference<SSLSupport> sslSupportRef) {
        this.sslSupport = sslSupportRef.getService();
        jose4jUtil = new Jose4jUtil(sslSupport);
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
    ProviderAuthenticationResult fixSubject(ProviderAuthenticationResult result) {
        if (result.getSubject() == null) {
            return result;
        }
        Set<Object> creds = result.getSubject().getPrivateCredentials();
        if (creds.size() > 0) {
            Object o = creds.iterator().next();
            if (o instanceof OidcTokenImplBase) {
                creds.remove(o);
                creds.add(new OidcTokenImpl((OidcTokenImplBase) o));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "idToken in subject is replaced with OidcTokenImpl");
                }
            }
        }
        return result;
    }

    /**
     * This method handle the redirect to the OpenID Connect server with query
     * parameters
     *
     * @param req
     * @param res
     * @param clientConfig
     * @return
     */
    ProviderAuthenticationResult handleRedirectToServer(HttpServletRequest req, HttpServletResponse res, OidcClientConfig clientConfig) {
        return authenticatorUtil.handleRedirectToServer(req, res, clientConfig);
    }

    /**
     * @param req
     * @param res
     */
    @SuppressWarnings("unchecked")
    Hashtable<String, String> getAuthzCodeAndStateFromCookie(IExtendedRequest req, HttpServletResponse res) {
        byte[] cookieValueBytes = req.getCookieValueAsBytes(ClientConstants.WAS_OIDC_CODE);
        if (cookieValueBytes == null || cookieValueBytes.length == 0) {
            return null;
        }

        OidcClientUtil.invalidateReferrerURLCookie(req, res, ClientConstants.WAS_OIDC_CODE);

        byte[] bParams = Base64Coder.base64Decode(cookieValueBytes);
        ByteArrayInputStream inParamsStream = new ByteArrayInputStream(bParams);
        ObjectInputStream inParamsObjStream;
        Hashtable<String, String> hashtable = null;
        try {
            inParamsObjStream = new ObjectInputStream(inParamsStream);
            hashtable = (Hashtable<String, String>) inParamsObjStream.readObject();
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getAuthzCodeAndState encounted an un-expected exception: " + e);
            }
        } catch (ClassNotFoundException ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getAuthzCodeAndState encounted an un-expected exception: " + ex);
            }
        }
        return hashtable;
    }

    @SuppressWarnings("rawtypes")
    void doIdAssertion(Hashtable<String, Object> customProperties, Payload payload, OidcClientConfig clientConfig) {
        if (clientConfig.isMapIdentityToRegistryUser())
            return;
        if (payload == null)
            return;

        String realm = (String) payload.get(clientConfig.getRealmIdentifier());
        if (realm == null || realm.isEmpty()) {
            realm = (String) payload.get(ClientConstants.ISS);
        }
        String uniqueSecurityName = (String) payload.get(clientConfig.getUniqueUserIdentifier());
        if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
            uniqueSecurityName = (String) payload.get(clientConfig.getUserIdentityToCreateSubject());
        }
        String uniqueID = new StringBuffer("user:").append(realm).append("/").append(uniqueSecurityName).toString();

        ArrayList groupIds = (ArrayList) payload.get(clientConfig.getGroupIdentifier());
        ArrayList<String> groups = new ArrayList<String>();
        if (groupIds != null && !groupIds.isEmpty()) {
            Iterator it = groupIds.iterator();
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
        if (clientConfig.isDisableLtpaCookie()) {
            customProperties.put(AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE, Boolean.TRUE);
        }
    }

    public String getIssuerIdentifier(OidcClientConfig clientConfig) {
        return authenticatorUtil.getIssuerIdentifier(clientConfig);
    }

    String getReqURL(HttpServletRequest req) {
        // due to some longstanding webcontainer strangeness, we have to do
        // some extra things for certain behind-proxy cases to get the right port.
        boolean rewritePort = false;
        Integer realPort = null;
        if (req.getScheme().toLowerCase().contains("https")) {
            realPort = new com.ibm.ws.security.common.web.WebUtils().getRedirectPortFromRequest(req);
        }
        int port = req.getServerPort();
        if (realPort != null && realPort.intValue() != port) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "serverport = " + port + "real port is " + realPort.toString() + ", url will be rewritten to use real port");
            }
            rewritePort = true;
        }

        StringBuffer reqURL = req.getRequestURL();
        if (rewritePort) {
            reqURL = new StringBuffer();
            reqURL.append(req.getScheme());
            reqURL.append("://");
            reqURL.append(req.getServerName());
            reqURL.append(":");
            reqURL.append(realPort);
            reqURL.append(req.getRequestURI());
            //reqURL = new StringBuffer(com.ibm.ws.security.common.web.WebUtils.rewriteURL(reqURL.toString(), null, realport.toString()));
        }
        String queryString = req.getQueryString();
        if (queryString != null) {
            reqURL.append("?");
            reqURL.append(OidcUtil.encodeQuery(queryString));
        }
        return reqURL.toString();
    }

    protected SSLContext getSSLContext(String tokenUrl, String sslConfigurationName, String clientId) throws SSLException {
        SSLContext sslContext = null;
        JSSEHelper jsseHelper = getJSSEHelper();

        if (jsseHelper != null) {
            sslContext = jsseHelper.getSSLContext(sslConfigurationName, null, null, true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sslContext (" + ") get: " + sslContext);
            }
        }

        if (sslContext == null) {
            if (tokenUrl != null && tokenUrl.startsWith("https")) {
                throw new SSLException(Tr.formatMessage(tc, "OIDC_CLIENT_HTTPS_WITH_SSLCONTEXT_NULL", new Object[] { "Null ssl conext", clientId }));
            }
        }
        return sslContext;
    }

    protected JSSEHelper getJSSEHelper() throws SSLException {
        if (sslSupport != null) {
            return sslSupport.getJSSEHelper();
        }
        return null;
    }

}
