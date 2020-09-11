/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.WebTrustAssociationUserException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.tai.TAIService;
import com.ibm.ws.security.authentication.tai.TAIUtil;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.SSOCookieHelper;
import com.ibm.ws.webcontainer.security.WebAuthenticator;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class TAIAuthenticator implements WebAuthenticator {
    private static final TraceComponent tc = Tr.register(TAIAuthenticator.class);
    private final AuthenticationResult AUTHN_CONTINUE_RESULT = new AuthenticationResult(AuthResult.CONTINUE, "Authentication continue");

    private TAIService taiService = null;
    private ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef = null;
    private SSOCookieHelper ssoCookieHelper = null;
    private AuthenticationService authenticationService = null;
    private static final String APPLICATION_AUTH_TYPE = "com.ibm.ws.security.tai.appAuthType";
    private boolean alreadyInitInFirstRequest = false;

    private final String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                                        AttributeNameConstants.WSCREDENTIAL_SECURITYNAME };

    //SAML SSO property for disable LTPA cookie.
    private final static String DISABLE_LTPA_AND_SESSION_NOT_ON_OR_AFTER = "com.ibm.ws.saml.spcookie.session.not.on.or.after";
    private static final String[] samlSsoDisableLtpaCookie = new String[] { DISABLE_LTPA_AND_SESSION_NOT_ON_OR_AFTER };

    //Map of TrustAssociationInterceptor - by TAI id
    //Order matters here: use a LinkedHashMap to preserve order across platforms
    Map<String, TrustAssociationInterceptor> invokeBeforeSSOTais = new LinkedHashMap<String, TrustAssociationInterceptor>();
    Map<String, TrustAssociationInterceptor> invokeAfterSSOTais = new LinkedHashMap<String, TrustAssociationInterceptor>();
    Map<String, Boolean> disableLtpaCookieTais = new HashMap<String, Boolean>();

    /**
     * Given an HTTP request, get the appropriate Trust Association Interceptor.
     * If there is any. If there are more than one, then only the first hit
     * gets called. If none, then this method returns AUTHN_CONTINUE_RESULT.
     *
     * @param taiService
     * @param interceptorServiceRef
     * @param authenticationService
     * @param ssoCookieHelper
     */
    public TAIAuthenticator(TAIService taiService, ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                            AuthenticationService authenticationService,
                            SSOCookieHelper ssoCookieHelper) {
        this.taiService = taiService;
        this.interceptorServiceRef = interceptorServiceRef;
        this.authenticationService = authenticationService;
        this.ssoCookieHelper = ssoCookieHelper;
    }

    @Override
    public AuthenticationResult authenticate(WebRequest webRequest) {
        return authenticate(webRequest, false);
    }

    public AuthenticationResult authenticate(WebRequest webRequest, boolean invokeBeforeSSO) {
        if (taiService != null && taiService.isInitAtFirstRequest() && !alreadyInitInFirstRequest) {
            // Initialize all TAI when the first request come in
            taiService.initializeTais();
            alreadyInitInFirstRequest = true;
        }

        AuthenticationResult authResult = AUTHN_CONTINUE_RESULT;
        TAIResult taiResult = null;
        String taiType = null;
        boolean isTargetIntercept = false;

        Map<String, TrustAssociationInterceptor> tais = getInterceptorServices(invokeBeforeSSO);
        if (skipTai(webRequest, tais, invokeBeforeSSO)) {
            return AUTHN_CONTINUE_RESULT;
        }

        HttpServletRequest req = webRequest.getHttpServletRequest();
        HttpServletResponse res = webRequest.getHttpServletResponse();
        if (webRequest.getLoginConfig() != null) {
            req.setAttribute(APPLICATION_AUTH_TYPE, webRequest.getLoginConfig().getAuthenticationMethod());
        }

        String taiId = null;
        try {
            Iterator<Entry<String, TrustAssociationInterceptor>> i = tais.entrySet().iterator();
            while (i.hasNext()) {
                Entry<String, TrustAssociationInterceptor> taiEntry = i.next();
                taiId = taiEntry.getKey();
                TrustAssociationInterceptor tai = taiEntry.getValue();
                if (tai.isTargetInterceptor(req)) {
                    isTargetIntercept = true;
                    taiType = tai.getType();
                    taiResult = tai.negotiateValidateandEstablishTrust(req, res);
                    if (webRequest.getLoginConfig() != null) {
                        req.removeAttribute(APPLICATION_AUTH_TYPE);
                    }
                    break;
                }
            }
            if (!isTargetIntercept) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "TAI authenticator" + (invokeBeforeSSO == true ? " before SSO " : " after SSO ") + "do not intercept this request");
                }
                return AUTHN_CONTINUE_RESULT;
            }
        } catch (WebTrustAssociationFailedException e) {
            Tr.error(tc, "SEC_TAI_VALIDATE_FAILED", new Object[] { e });
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
        } catch (WebTrustAssociationUserException e) {
            Tr.error(tc, "SEC_TAI_USER_EXCEPTION", new Object[] { e });
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
        } catch (Exception e) {
            Tr.error(tc, "SEC_TAI_GENERAL_EXCEPTION", new Object[] { e });
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
        }

        if (authResult.getStatus() == AuthResult.FAILURE) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "TAI throws an un-expected exception: " + authResult.getReason());
            }
            return authResult;
        }

        return handleTaiResult(taiResult, taiType, taiId, req, res);
    }

    private void getUserFeatureInterceptorWithNoTaiConfigured() {
        Set<String> ids = interceptorServiceRef.keySet();
        for (String id : ids) {
            TrustAssociationInterceptor tai = interceptorServiceRef.getService(id);
            getProperties(id, tai);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "invokeBeforeSSOTais " + invokeBeforeSSOTais.toString());
            Tr.debug(tc, "invokeAfterSSOTais  " + invokeAfterSSOTais.toString());
            Tr.debug(tc, "disableLtpaCookieTais " + disableLtpaCookieTais.toString());
        }
    }

    private void getProperties(String id, TrustAssociationInterceptor interceptor) {
        TAIUtil taiUtil = new TAIUtil(interceptorServiceRef, id);
        if (taiUtil.isInvokeBeforeSSO()) {
            invokeBeforeSSOTais.put(id, interceptor);
        }
        if (taiUtil.isInvokeAfterSSO()) {
            invokeAfterSSOTais.put(id, interceptor);
        }
        if (taiUtil.isDisableLtpaCookie() != null) {
            disableLtpaCookieTais.put(id, ((Boolean) taiUtil.isDisableLtpaCookie()).booleanValue());
        }

    }

    /**
     * TAIService can handle both shared liberty and user feature interceptors with TAI configured in server.xml
     * If there is no TAI configured in server.xml, the taiService is not available.
     */
    private Map<String, TrustAssociationInterceptor> getInterceptorServices(boolean invokeBeforeSSO) {
        if (taiService != null) {
            return taiService.getTais(invokeBeforeSSO);
        } else {
            // We only have the user feature interceptors and no TAI configured in server.xml
            getUserFeatureInterceptorWithNoTaiConfigured();
            if (invokeBeforeSSO)
                return invokeBeforeSSOTais;
            else
                return invokeAfterSSOTais;
        }
    }

    /**
     * @param webRequest
     * @param tais
     * @param invokeBeforeSSO
     * @return
     */
    private boolean skipTai(WebRequest webRequest, Map<String, TrustAssociationInterceptor> tais, boolean invokeBeforeSSO) {
        boolean skipIt = false;
        if (tais == null || tais.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There is no interceptor config to invoke" + (invokeBeforeSSO == true ? " before SSO " : " after SSO ") + ", skipping TAI...");
            }
            return true;
        }
        if (webRequest.isUnprotectedURI() &&
            (!webRequest.isProviderSpecialUnprotectedURI()) &&
            (taiService != null && !taiService.isInvokeForUnprotectedURI())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Skipping interceptor for unprotected URI...");
            }
            return true;
        }
        return skipIt;
    }

    /**
     * @param taiResult
     * @param taiType
     * @param taiId
     * @param req
     * @param res
     * @return
     */
    private AuthenticationResult handleTaiResult(TAIResult taiResult, String taiType, String taiId, HttpServletRequest req, HttpServletResponse res) {
        AuthenticationResult authResult;
        try {
            if (taiResult != null && taiResult.getStatus() == HttpServletResponse.SC_OK) {
                authResult = authenticateWithTAIResult(req, res, taiResult, taiId);
            } else {
                authResult = handleFallBackToAppAuthType(taiType, taiResult);
            }
        } catch (AuthenticationException e) {
            authResult = new AuthenticationResult(AuthResult.SEND_401, e.getMessage());
        }
        return authResult;
    }

    /**
     * @param taiType
     * @param taiResult
     * @return
     * @throws AuthenticationException
     */
    private AuthenticationResult handleFallBackToAppAuthType(String taiType, TAIResult taiResult) throws AuthenticationException {
        AuthenticationResult authResult = null;
        if (taiService != null && taiService.isFailOverToAppAuthType() ||
            taiResult != null && taiResult.getStatus() == HttpServletResponse.SC_CONTINUE) {
            if (taiResult == null) {
                return new AuthenticationResult(AuthResult.CONTINUE, "TAI allows fall back to application authentication type");
            } else {
                return new AuthenticationResult(AuthResult.CONTINUE, taiResult.getSubject());
            }
        }

        if (taiResult == null) {
            authResult = new AuthenticationResult(AuthResult.FAILURE, "taiResult is null");
        } else {
            // return TAI error code to the browser client or authenticating proxy
            authResult = new AuthenticationResult(AuthResult.TAI_CHALLENGE, "TrustAssociation Interception returns error", taiResult.getStatus());
        }

        return authResult;
    }

    /**
     * @param req
     * @param res
     * @param taiResult
     * @param taiId
     * @return
     * @throws AuthenticationException
     */
    private AuthenticationResult authenticateWithTAIResult(HttpServletRequest req, HttpServletResponse res, TAIResult taiResult, String taiId) throws AuthenticationException {
        AuthenticationResult authResult = null;
        String taiUserName = taiResult.getAuthenticatedPrincipal();
        if (taiUserName != null) {
            Subject taiSubject = taiResult.getSubject();
            if (taiSubject != null) {
                SubjectHelper subjectHelper = new SubjectHelper();
                WSCredential wsCred = subjectHelper.getWSCredential(taiSubject);
                if (wsCred == null && !isHashTableLogin(subjectHelper, taiSubject)) {
                    new AuthenticationResult(AuthResult.FAILURE, "Subject from TAI has no WSCrendential and required hashtable login for user: " + taiUserName);
                } else if (wsCred != null && wsCred.isUnauthenticated()) {
                    new AuthenticationResult(AuthResult.FAILURE, "Subject from TAI is invalid for user: " + taiUserName);
                }
                authResult = authenticateWithSubject(req, res, taiSubject, taiId);
            }
            if (authResult == null || authResult.getStatus() != AuthResult.SUCCESS) {
                authResult = loginWithTAIUserName(req, res, taiSubject, taiUserName, taiId);
                if (authResult == null || authResult.getStatus() != AuthResult.SUCCESS) {
                    authResult = new AuthenticationResult(AuthResult.CONTINUE, "authenticate failed.... allow to continue");
                }
            }
        } else {
            authResult = new AuthenticationResult(AuthResult.FAILURE, "TAI user name is null");
        }

        return authResult;
    }

    /**
     * @param subjectHelper
     * @param taiSubject
     * @return
     */
    private boolean isHashTableLogin(SubjectHelper subjectHelper, Subject taiSubject) {
        Hashtable<String, ?> customProperties = subjectHelper.getHashtableFromSubject(taiSubject, hashtableLoginProperties);
        if (customProperties != null &&
            customProperties.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME) != null &&
            customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID) != null) {
            return true;
        }
        return false;
    }

    /**
     * @param req
     * @param res
     * @param taiSubject
     * @param taiUserName
     * @param taiId
     * @return
     */
    private AuthenticationResult loginWithTAIUserName(HttpServletRequest req, HttpServletResponse res, Subject taiSubject, String taiUserName, String taiId) {
        AuthenticationResult authResult = null;
        Subject subject = createUserIdHashtableSubject(taiSubject, taiUserName);
        authResult = authenticateWithSubject(req, res, subject, taiId);
        return authResult;
    }

    /**
     * @param req
     * @param res
     * @param subject
     * @param taiId
     * @return
     * @throws AuthenticationException
     */
    @FFDCIgnore(AuthenticationException.class)
    private AuthenticationResult authenticateWithSubject(HttpServletRequest req, HttpServletResponse res, Subject subject, String taiId) {
        AuthenticationResult authResult;
        SubjectHelper subjectHelper = new SubjectHelper();
        try {
            AuthenticationData authenticationData = createAuthenticationData(req, res, subject);
            boolean addLtpaCookieToResp = isAddLtpaCookieToResp(subject, subjectHelper, taiId);
            Subject new_subject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, subject);
            authResult = new AuthenticationResult(AuthResult.SUCCESS, new_subject);
            if (addLtpaCookieToResp) {
                ssoCookieHelper.addSSOCookiesToResponse(new_subject, req, res);
                removeInternalProps(new_subject, subjectHelper, AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE);
            }
        } catch (AuthenticationException e) {
            authResult = new AuthenticationResult(AuthResult.FAILURE, e.getMessage());
        }
        return authResult;
    }

    protected void removeInternalProps(Subject subject, SubjectHelper subjectHelper, String propName) {
        Hashtable<String, Object> hashtable = (Hashtable<String, Object>) subjectHelper.getSensitiveHashtableFromSubject(subject);
        if (hashtable == null)
            return;

        Set<Object> publicCredentials = subject.getPublicCredentials();
        publicCredentials.remove(hashtable);
        hashtable.remove(propName);
        if (!hashtable.isEmpty()) {
            publicCredentials.add(hashtable);
        }

        Set<Object> privateCredentials = subject.getPrivateCredentials();
        privateCredentials.remove(hashtable);
        hashtable.remove(propName);
        if (!hashtable.isEmpty()) {
            privateCredentials.add(hashtable);
        }
    }

    private boolean isAddLtpaCookieToResp(Subject subject, SubjectHelper subjectHelper, String taiId) {
        //Check for SSO SAML disable LTPA property in the subject
        Hashtable<String, Object> hashtable = (Hashtable<String, Object>) subjectHelper.getHashtableFromSubject(subject, samlSsoDisableLtpaCookie);
        // found the SSO SAML disable LTPA property so do not add cookie to the response || Check for TAI disableLtpaCookie property
        if (hashtable != null || isDisableLtpaCookie(taiId)) {
            return addDisableLtpaCookiePropToSubject(subject, subjectHelper);
        }
        return true;
    }

    /**
     * @param subject
     * @param subjectHelper
     * @return
     */
    private boolean addDisableLtpaCookiePropToSubject(Subject subject, SubjectHelper subjectHelper) {
        Hashtable<String, Object> hashtable = (Hashtable<String, Object>) subjectHelper.getHashtableFromSubject(subject, hashtableLoginProperties);
        if (hashtable == null) {
            hashtable = new Hashtable<String, Object>();
        }
        hashtable.put(AuthenticationConstants.INTERNAL_DISABLE_SSO_LTPA_COOKIE, Boolean.TRUE);
        return false;
    }

    private boolean isDisableLtpaCookie(String taiId) {
        if (taiService != null) {
            return taiService.isDisableLtpaCookie(taiId);
        } else if (taiId != null && disableLtpaCookieTais.get(taiId) != null) {
            return disableLtpaCookieTais.get(taiId);
        }

        return false;
    }

    /**
     * @param taiSubject
     * @param taiUserName
     * @return
     */
    private Subject createUserIdHashtableSubject(Subject taiSubject, String taiUserName) {
        Subject newSubject = taiSubject;
        if (newSubject == null) {
            newSubject = new Subject();
        }

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        if (authenticationService == null || !authenticationService.isAllowHashTableLoginWithIdOnly())
            hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, taiUserName);
        newSubject.getPublicCredentials().add(hashtable);
        return newSubject;
    }

    @Trivial
    protected AuthenticationData createAuthenticationData(HttpServletRequest req, HttpServletResponse res, Subject subject) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
        authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, res);
        try {
            if (subject != null) {
                // Allow TAI to login and create LTPA Cookie
                // Also prevent it from login again
                Cookie ltpaCookie = WebSecurityHelperImpl.getLTPACookie(subject);
                if (ltpaCookie != null) {
                    authenticationData.set(AuthenticationData.TOKEN64, ltpaCookie.getValue());
                }
            }
        } catch (Exception e) {
            // this is OK if TAI does not have a SSO Cookie
        }
        return authenticationData;
    }

    @Override
    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse res, HashMap props) throws Exception {
        return null;
    }
}
