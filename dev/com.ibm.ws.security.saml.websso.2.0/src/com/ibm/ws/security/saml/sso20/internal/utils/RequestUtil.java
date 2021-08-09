/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;


import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.impl.KnownSamlUrl;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 *
 */
public class RequestUtil extends KnownSamlUrl {
    private static final TraceComponent tc = Tr.register(RequestUtil.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    static SimpleDateFormat cookieDateFormater;
    static {
        cookieDateFormater = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
        cookieDateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * @param requestId
     * @param req
     */
    public static void cacheRequestInfo(String requestId, SsoSamlService ssoService, HttpRequestInfo requestInfo) {
        Cache cache = ssoService.getAcsCookieCache(ssoService.getProviderId());
        cache.put(requestId, requestInfo);
    }

    public static String getAcsUrl(HttpServletRequest req, String samlCtxPath, String acsProviderId, SsoConfig samlConfig) {
        String urlContextPath = getCtxRootUrl(req, samlCtxPath, samlConfig);
        return urlContextPath + acsProviderId + "/acs";
    }

    public static String getSloUrl(HttpServletRequest req, String samlCtxPath, String spId, SsoConfig samlConfig) {
        String urlContextPath = getCtxRootUrl(req, samlCtxPath, samlConfig);
        return urlContextPath + spId + "/slo";
    }

    public static String getEntityUrl(HttpServletRequest req, String samlCtxPath, String acsProviderId, SsoConfig samlConfig) {
        String urlContextPath = getCtxRootUrl(req, samlCtxPath, samlConfig);
        return urlContextPath + acsProviderId;
    }

    public static String getCtxRootUrl(HttpServletRequest req, String samlCtxPath, SsoConfig samlConfig) {
        String spHostAndPort = samlConfig == null ? null : samlConfig.getSpHostAndPort();
        if (spHostAndPort != null && !spHostAndPort.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "spHostAndPort is:" + spHostAndPort);
            }
            if (spHostAndPort.startsWith("http")) {
                return spHostAndPort + samlCtxPath;
            } else {
                return "https://" + spHostAndPort + samlCtxPath;
            }
        } else {
            String hostName = req.getServerName();
            Integer httpsPort = getRedirectPortFromRequest(req);
            if (httpsPort == null && req.isSecure()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The redirect SSL port is null from request. Trying to get http port");
                }
                int port = req.getServerPort();
                String httpSchema = ((javax.servlet.ServletRequest) req).getScheme();
                // return whatever in the req
                return httpSchema + "://" + hostName + (port > 0 && port != 443 ? ":" + port : "") + samlCtxPath;
            } else {
                return "https://" + hostName + (httpsPort == null ? "" : ":" + httpsPort) + samlCtxPath;
            }
        }

    }

    static protected Integer getRedirectPortFromRequest(HttpServletRequest req) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            return (Integer) ((IPrivateRequestAttributes) sr).getPrivateAttribute("SecurityRedirectPort");
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getRedirectUrl called for non-IPrivateRequestAttributes object", req);
            }
            return null;
        }

    }

    /**
     * Drill down through any possible HttpServletRequestWrapper objects.
     *
     * @param sr
     * @return
     */
    static HttpServletRequest getWrappedServletRequestObject(HttpServletRequest sr) {
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        return sr;
    }

    public static void createCookie(HttpServletRequest req,
                                    HttpServletResponse response,
                                    String cookieName,
                                    String cookieValue) {
        // cookieName and cookieValue has been verified as non-null
        WebAppSecurityConfig webAppSecurityConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        ReferrerURLCookieHandler referrerURLCookieHandler = webAppSecurityConfig.createReferrerURLCookieHandler();
        Cookie c = referrerURLCookieHandler.createCookie(cookieName,
                                                         cookieValue,
                                                         req);
        response.addCookie(c);
    }

    public static void removeCookie(HttpServletRequest req,
                                    HttpServletResponse response,
                                    String cookieName) {
        // cookieName and cookieValue has been verified as non-null
        WebAppSecurityConfig webAppSecurityConfig = WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
        if (webAppSecurityConfig != null) {
            ReferrerURLCookieHandler referrerURLCookieHandler = webAppSecurityConfig.createReferrerURLCookieHandler();
            Cookie c = referrerURLCookieHandler.createCookie(cookieName,
                                                             "",
                                                             req);
            c.setMaxAge(0);
            response.addCookie(c);
        }
    }

    /**
     * @param req
     * @param res
     */
    public static String getCookieId(IExtendedRequest req, HttpServletResponse res, String cookieName) throws SamlException {
        byte[] cookieValueBytes = req.getCookieValueAsBytes(cookieName);
        if (cookieValueBytes == null || cookieValueBytes.length == 0) {
            return null;
        }
        String result = null;
        try {
            result = new String(cookieValueBytes, Constants.UTF8); // no need to do Base64 decode
        } catch (UnsupportedEncodingException e) {
            // This should not happen, since UTF8 is OK in almost all situations
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception, id:(" + e + ")");
            }
            throw new SamlException(e); // Let SamlException handle the unexpected Exception
        }
        return result;
    }

    public static String convertBytesToString(byte[] bytes) {
        String result = null;
        try {
            result = new String(bytes, Constants.UTF8); // no need to do Base64 decode
        } catch (UnsupportedEncodingException e) {
            // This ought to be OK
            // The worst case is: The request is not handled
        }
        return result;
    }

    @FFDCIgnore({ SamlException.class })
    public static Credential getDecryptingCredential(SsoSamlService ssoService) throws SamlException {
        BasicX509Credential credential = null;
        try {
            PrivateKey privateKey = ssoService.getPrivateKey();         
            if (privateKey == null) {
                // error handling
                throw new SamlException("SAML20_NO_PRIVATE_KEY",
                                //SAML20_NO_PRIVATE_KEY=CWWKS5073E: Cannot find an private key from Service Provider [{0}].
                                null, true, new Object[] { ssoService.getProviderId(), ssoService.getConfig().getKeyStoreRef() });
            }
            Certificate certificate = ssoService.getSignatureCertificate();
            if (certificate == null) {
                // error handling
                throw new SamlException("SAML20_NO_CERT",
                                //SAML20_NO_CERT=CWWKS5074E: Cannot find a signature certificate from Service Provider [{0}].
                                null, true, new Object[] { ssoService.getProviderId(), ssoService.getConfig().getKeyStoreRef() });
            }
            credential = new BasicX509Credential((X509Certificate) certificate);           
            credential.setPrivateKey(privateKey);
        } catch (SamlException e) { // declared exceptions: KeyStoreException,  CertificateException
            throw e;
        } catch (Exception e) { // declared exceptions: KeyStoreException,  CertificateException
            throw new SamlException(e); // let the SamlException handle the unexpected Exception
        }
        return credential;
    }

    @FFDCIgnore({ SamlException.class })
    public static Credential getSigningCredential(SsoSamlService ssoService) throws SamlException {
        BasicX509Credential credential = null;//v3
        try {

            PrivateKey privateKey = ssoService.getPrivateKey();
            if (privateKey == null) {
                // error handling
                throw new SamlException("SAML20_NO_PRIVATE_KEY",
                                //SAML20_NO_PRIVATE_KEY=CWWKS5073E: Cannot find an private key from Service Provider [{0}].
                                null, true, new Object[] { ssoService.getProviderId(), ssoService.getConfig().getKeyStoreRef() });
            }
            Certificate certificate = ssoService.getSignatureCertificate();
            if (certificate == null) {
                // error handling
                throw new SamlException("SAML20_NO_CERT",
                                //SAML20_NO_CERT=CWWKS5074E: Cannot find a signature certificate from Service Provider [{0}].
                                null, true, new Object[] { ssoService.getProviderId(), ssoService.getConfig().getKeyStoreRef() });
            }
            credential = new BasicX509Credential((X509Certificate) certificate);
            
            credential.setPrivateKey(privateKey);

        } catch (SamlException e) {
            throw e;
        } catch (Exception e) { // declared exceptions: KeyStoreException,  CertificateException
            throw new SamlException(e); // let the SamlException handle the unexpected Exception
        }
        return credential;
    }

    /**
     * A. validate at redirectToRelayState() in com/ibm/ws/security/saml/sso20/acs/AcsHandler
     * B. Must not contain InResponseTo for unsolicited SSO
     *
     * @throws SamlException
     */
    public static void validateInResponseTo(BasicMessageContext<?, ?> context, String inResponseTo) throws SamlException {
        HttpRequestInfo requestInfo = context.getCachedRequestInfo();
        String externalRelayState = context.getExternalRelayState();
        if (requestInfo == null) { // this is not sp_init
            if (inResponseTo != null && !inResponseTo.isEmpty()) {
                // SAML20_NOT_SP_INIT_WITH_IN_RESPONSE_TO=CWWKS5031W: The SAML Response indicated it responds to an Service Provider AuthnRequest [{0}].
                // But cannot find the AuthnRequest through the Relay State [{1}] in the request. Something is not right.
                //Tr.warning(tc, "SAML20_SP_UNSOLICITED_WITH_IN_RESPONSE_TO", new Object[] { inResponseTo, externalRelayState });
                throw new SamlException("SAML20_SP_UNSOLICITED_WITH_IN_RESPONSE_TO", null, // cause
                                new Object[] { inResponseTo });
            }
        } else {
            String inResponseToId = requestInfo.getInResponseToId();
            if (!ForwardRequestInfo.safeCompare(inResponseTo, inResponseToId)) {
                throw new SamlException("SAML20_NO_INRESPONSETO",
                                // SAML20_NO_INRESPONSETO=CWWKS5067E: The SAML Response [{0}] in the Service Provider Solicited request must contain a valid InResponseTo attribute. The InResponse is [{1}] in the SAML Response and [{2}] in the AuthnRequest.
                                null, // cause
                                new Object[] { externalRelayState, inResponseTo, inResponseToId });
            }
        }
    }

    /**
     * @param req
     * @param samlService
     * @param typeResponse
     */
    public static SsoRequest setSamlRequest(HttpServletRequest req, SsoSamlService samlService, Constants.EndpointType type) {
        SsoRequest samlRequest = new SsoRequest(samlService.getProviderId(), type, req, Constants.SamlSsoVersion.SAMLSSO20, samlService);
        req.setAttribute(Constants.ATTRIBUTE_SAML20_REQUEST, samlRequest);
        return samlRequest;
    }

    /**
     * Gets the username from the principal of the subject.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    public static String getUserName(Subject subject) {
        if (subject == null)
            return null;
        Set<Principal> principals = subject.getPrincipals();
        Iterator<Principal> principalsIterator = principals.iterator();
        if (principalsIterator.hasNext()) {
            Principal principal = principalsIterator.next();
            return principal.getName();
        }
        return null;
    }

    public static boolean isUnprotectedUrlForSaml(HttpServletRequest request) {
        String ctxPath = request.getContextPath();
        if (IBM_JMX_CONNECTOR_REST.equals(ctxPath))
            return true;
        return isSamlUnprotectedUrl(request, ctxPath);
    }

    static boolean isSamlUnprotectedUrl(HttpServletRequest request, String ctxPath) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Context path:" + ctxPath);
        }
        if (isLogoutRequestFromIdP(request, ctxPath)) {
            return false; //making the logout endpoint and slo request from IdP protected, so we will have an authenticated subject on the thread
        }
        return (SAML_CONTEXT_PATH.equals(ctxPath));
    }

//    public static boolean isLogoutRequestFromApplication(HttpServletRequest request, String ctxPath) {
//        return (SAML_CONTEXT_PATH.equals(ctxPath) && request.getRequestURI().endsWith("/logout"));
//    }

    public static boolean isLogoutRequestFromIdP(HttpServletRequest request, String ctxPath) {
        return (SAML_CONTEXT_PATH.equals(ctxPath) && request.getRequestURI().endsWith("/slo") && (request.getParameter(Constants.SAMLRequest) != null));
    }

    /**
     * Returns whether an ACS cookie is included in the request or if an ACS cookie is in the cache associated with the provider
     * specified by the request. If a webapp makes multiple asynchronous requests for content, the original cookie that was set by
     * the ACS might come back multiple times. If it hasn't already been processed and cleared from the cache, then we likely need
     * to do further processing. Otherwise, the cookie can likely be ignored.
     */
    public static boolean isUnprocessedAcsCookiePresent(ConcurrentServiceReferenceMap<String, SsoSamlService> ssoSamlServiceRef, IExtendedRequest req, SsoRequest samlRequest) {
        String spProviderId = samlRequest.getProviderName();
        String acsCookieValue = getAcsCookieValueFromRequest(req, spProviderId);
        if (acsCookieValue != null && !acsCookieValue.isEmpty()) {
            // TODO: Do we want to validate the ACS Cookie now?
            // Or allow the SAMLResponseTAI handle it, even it's expired?
            return isAcsCookieInCache(ssoSamlServiceRef, spProviderId, acsCookieValue);
        }
        return false;
    }

    public static String getAcsCookieValueFromRequest(IExtendedRequest req, String spProviderId) {
        String cookieName = Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(spProviderId);
        byte[] cookieValueBytes = req.getCookieValueAsBytes(cookieName);
        if (cookieValueBytes != null) {
            return convertBytesToString(cookieValueBytes);
        }
        return null;
    }

    static boolean isAcsCookieInCache(ConcurrentServiceReferenceMap<String, SsoSamlService> ssoSamlServiceRef, String spProviderId, String acsCookieValue) {
        Cache cache = getAcsCookieCacheForProvider(ssoSamlServiceRef, spProviderId);
        if (cache == null) {
            return false;
        }
        return (cache.get(acsCookieValue) != null);
    }

    public static Cache getAcsCookieCacheForProvider(ConcurrentServiceReferenceMap<String, SsoSamlService> ssoSamlServiceRef, String spProviderId) {
        SsoSamlService samlService = ssoSamlServiceRef.getService(spProviderId);
        if (samlService != null) {
            return samlService.getAcsCookieCache(spProviderId);
        }
        return null;
    }

}
