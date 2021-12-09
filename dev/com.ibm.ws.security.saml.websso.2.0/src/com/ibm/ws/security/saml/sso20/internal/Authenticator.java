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
package com.ibm.ws.security.saml.sso20.internal;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.ErrorHandler;
import com.ibm.ws.security.saml.error.ErrorHandlerImpl;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * This object takes requests from the TrustAssociationInterceptorImpl classes and sends them to the
 * corresponding objects. Acts as a controller object.
 */
public class Authenticator {
    public static final TraceComponent tc = Tr.register(Authenticator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    WebProviderAuthenticatorHelper authHelper;
    private final String providerName;
    private SsoConfig ssoConfig = null;
    protected UserData userData;

    public Authenticator(SsoSamlService samlService, UserData userData) {
        this.authHelper = samlService.getAuthHelper();
        this.userData = userData;
        providerName = samlService.getProviderId();
        ssoConfig = samlService.getConfig();
    }

    /**
     * @param request
     * @param response
     * @param cookieData
     * @return
     */
    @FFDCIgnore({ SamlException.class })
    public TAIResult authenticate(HttpServletRequest request, HttpServletResponse resp) throws WebTrustAssociationFailedException {
        TAIResult result = TAIResult.create(HttpServletResponse.SC_CONTINUE);

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "userData: " + userData);
            }
            if (userData != null) {
                RequestUtil.removeCookie(request, resp, Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(providerName));

                Saml20Token saml20Token = userData.getSamlToken();
                SsoRequest samlRequest = (SsoRequest) request.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
                AssertionToSubject mapAssertToSubjectUtil = new AssertionToSubject(samlRequest, ssoConfig, saml20Token);
                String user = mapAssertToSubjectUtil.getUser();
                Hashtable<String, Object> hashtable = createHashtable(mapAssertToSubjectUtil, saml20Token, user);

                TAIResult taiResult = authenticateLogin(request, resp, saml20Token, hashtable, user);
                if (taiResult.getStatus() == HttpServletResponse.SC_OK) { // set SpCookie
                    samlRequest.createSpCookieIfDisableLtpa(request, resp);
                }
                return taiResult;
            }
        } catch (SamlException e) {
            ErrorHandler errorHandler = ErrorHandlerImpl.getInstance();
            try {
                errorHandler.handleException(request, resp, e);
            } catch (Exception e1) {
                // unexpected exception complicated to handle further
                // especially, we are throwing a WebTrustAssociationFailedException to the caller
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpceted exception during errorHandling" + e);
                }
            }
            // Since we have handled the error. No need to throw the Exception to ask the web container to handle the error.
            result = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception, id:(" + providerName + "," + e + ")");
            }
            result = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        }

        return result;
    }

    /**
     * @param request
     * @param response
     * @param cookieData
     * @return
     * @throws WebTrustAssociationFailedException
     */
    @FFDCIgnore({ SamlException.class })
    public TAIResult authenticateRS(HttpServletRequest request, HttpServletResponse resp, SsoRequest samlRequest) throws SamlException {
        TAIResult result = null;
        try {
            result = TAIResult.create(HttpServletResponse.SC_CONTINUE);
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "userData: " + userData);
                }
                if (userData != null) {
                    Saml20Token saml20Token = userData.getSamlToken();
                    AssertionToSubject mapAssertToSubjectUtil = new AssertionToSubject(samlRequest, ssoConfig, saml20Token);
                    String user = mapAssertToSubjectUtil.getUser();
                    Hashtable<String, Object> hashtable = createHashtable(mapAssertToSubjectUtil, saml20Token, user);

                    TAIResult taiResult = authenticateLogin(request, resp, saml20Token, hashtable, user);
                    return taiResult;
                }
            } catch (SamlException e) {
                //ErrorHandler errorHandler = ErrorHandlerImpl.getInstance();
                result = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception, id:(" + providerName + "," + e + ")");
            }
            try {
                result = TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
            } catch (WebTrustAssociationFailedException we) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected exception, id:(" + providerName + "," + we + ")");
                }
            }
        }

        return result;
    }

    TAIResult authenticateLogin(HttpServletRequest request,
                                HttpServletResponse resp,
                                Saml20Token saml20Token,
                                Hashtable<String, Object> hashtable,
                                String user) throws WebTrustAssociationFailedException, SamlException {
        Subject subject = new Subject();
        //subject.getPublicCredentials().add(hashtable);
        if (ssoConfig.isIncludeTokenInSubject()) {
            subject.getPrivateCredentials().add(saml20Token);
        }
        AuthenticationResult authResult = null;
        authResult = authHelper.loginWithUserName(request, resp, user,
                                                  subject, hashtable,
                                                  Constants.MapToUserRegistry.User.equals(ssoConfig.getMapToUserRegistry()));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authHelper authResult:" + authResult);
        }
        if (authResult.getStatus() == AuthResult.SUCCESS) {
            // get principal and subject from authResult
            Subject authenticatedSubject = authResult.getSubject();
            return TAIResult.create(HttpServletResponse.SC_OK,
                                    RequestUtil.getUserName(authenticatedSubject),
                                    authenticatedSubject);
        } else {
            // error message
            throw new SamlException("SAML20_USER_CANNOT_AUTHENTICATED",
                            //SAML20_USER_CANNOT_AUTHENTICATED=CWWKS5072E: The user [0] cannot be authenticated by the Service Provider.
                            null,
                            new Object[] { user });
        }
    }

    Hashtable<String, Object> createHashtable(AssertionToSubject mapAssertToSubject,
                                              Saml20Token saml20Token,
                                              String user) throws SamlException, WSSecurityException, RemoteException {

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();

        String realm = null;
        String uniqueID = null;
        List<String> groups = null;

        switch (ssoConfig.getMapToUserRegistry()) {
            case No:
                realm = mapAssertToSubject.getRealm();
                uniqueID = mapAssertToSubject.getUserUniqueIdentity(user, realm);
                groups = mapAssertToSubject.getGroupUniqueIdentity(realm);
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_REALM, realm);
                if (!groups.isEmpty()) {
                    putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
                }
                break;
            case User:
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_USERID, user);

                break;
            case Group:
                realm = mapAssertToSubject.getRealm();
                uniqueID = mapAssertToSubject.getUserUniqueIdentity(user, realm);
                groups = mapAssertToSubject.getGroupUniqueIdentityFromRegistry(realm);
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_REALM, realm);
                if (!groups.isEmpty()) {//Now we need call user registry
                    putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
                }
                break;
        }

        putValue(hashtable, AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);

        // In the case disableLtpaToken is true, this needs to be true all the time
        // Otherwise, the SP_Toekn will not be created...
        // And yes, the isAllowCustomCacheKey did check disableLtpaToken, too.
        if (ssoConfig.isAllowCustomCacheKey()) {
            // add cacheKey in case LTPA and subject lifetime are different
            String cache_key = mapAssertToSubject.getCustomCacheKeyValue(providerName);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cache_key);
        }

        mapAssertToSubject.handleSessionNotOnOrAfter(hashtable, saml20Token);

        return hashtable;
    }

    /**
     * @param hashtable
     * @param wscredentialUniqueid
     * @param uniqueID
     */
    void putValue(Hashtable<String, Object> hashtable,
                  String key,
                  Object value) {
        if (value == null)
            return;
        hashtable.put(key, value);
    }

}
