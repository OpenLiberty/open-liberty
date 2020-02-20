/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.caller;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.ws.wssecurity.token.TokenUtils;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class SAMLAuthenticator {
    public static final TraceComponent tc = Tr.register(SAMLAuthenticator.class, WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    WebProviderAuthenticatorHelper authHelper;

    private Map<String, Object> callerConfig = null;
    private Saml20Token samltoken = null;

    //protected UserData userData;

    public SAMLAuthenticator(Map<String, Object> callerConfig, Saml20Token token) {
        this.callerConfig = callerConfig;
        samltoken = token;
        authHelper = TokenUtils.getAuthHelper();
    }

    /**
     * 
     * @return
     */
    //@FFDCIgnore({ SamlCallerTokenException.class })
    public AuthenticationResult authenticate() throws Exception {

        try {

            //if (userData != null) {
            //RequestUtil.removeCookie(request, resp, Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(providerName));

            Saml20Token saml20Token = this.samltoken;//userData.getSamlToken();
            //SsoRequest samlRequest = (SsoRequest) request.getAttribute(Constants.ATTRIBUTE_SAML20_REQUEST);
            AssertionToSubject mapAssertToSubjectUtil = new AssertionToSubject(this.callerConfig, saml20Token);
            String user = mapAssertToSubjectUtil.getUser();
            Hashtable<String, Object> hashtable = createHashtable(mapAssertToSubjectUtil, saml20Token, user);
            AuthenticationResult result = authenticateLogin(hashtable, user);
            if (result.getStatus() != AuthResult.SUCCESS) {
                if ("User".equalsIgnoreCase((String) callerConfig.get(CallerConstants.MAP_TO_UR))) {
                    Tr.error(tc, "error_authenticate_maptouser", new Object[] { user });
                }
                else {
                    Tr.error(tc, "error_authenticate", new Object[] { result.getReason() });
                }
            }
            return (result);
            //TAIResult taiResult = authenticateLogin(request, resp, saml20Token, hashtable, user);
            //if (taiResult.getStatus() == HttpServletResponse.SC_OK) { // set SpCookie
            //    samlRequest.setSpCookie(request, resp);
            //}
            //return taiResult;
            //}
        } catch (SamlCallerTokenException e) {
            // unexpected exception which is too complicated to handle further
            // especially, we are throwing a WebTrustAssociationFailedException to the caller
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception: " + e.getCause());
            }
            Tr.error(tc, "failed_to_obtain_subject_info", e.getLocalizedMessage());
            throw e;
            // Since we have handle the error. No need to throw the Exception to ask the webcontainer to handle the error.
            //result = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception: " + e.getCause());
            }
            Tr.error(tc, "failed_to_authenticate", e.getLocalizedMessage());
            //result = TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
            throw e;
        }
    }

    /**
     * @param hashtable
     * @param user
     * @return
     */
    private AuthenticationResult authenticateLogin(Hashtable<String, Object> hashtable, String user) {
        // TODO Auto-generated method stub

        Subject subject = new Subject();
        if ((Boolean) callerConfig.get(CallerConstants.INCLUDE_TOKEN)) {
            subject.getPrivateCredentials().add(this.samltoken);
        }

        AuthenticationResult authResult = null;
        boolean isUserMapToUR = false;
        if ("User".equalsIgnoreCase(((String) callerConfig.get(CallerConstants.MAP_TO_UR)))) {
            isUserMapToUR = true;
        }
        authResult = authHelper.loginWithUserName(null, null, user, subject, hashtable, isUserMapToUR);
        return authResult;

    }

    /*
     * TAIResult authenticateLogin(HttpServletRequest request,
     * HttpServletResponse resp,
     * Saml20Token saml20Token,
     * Hashtable<String, Object> hashtable,
     * String user) throws WebTrustAssociationFailedException, SamlCallerTokenException {
     * Subject subject = new Subject();
     * //subject.getPublicCredentials().add(hashtable);
     * if (ssoConfig.isIncludeTokenInSubject()) {
     * subject.getPrivateCredentials().add(saml20Token);
     * }
     * AuthenticationResult authResult = null;
     * authResult = authHelper.loginWithUserName(request, resp, user,
     * subject, hashtable,
     * Constants.MapToUserRegistry.User.equals(ssoConfig.getMapToUserRegistry()));
     * 
     * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
     * Tr.debug(tc, "authHelper authResult:" + authResult);
     * }
     * if (authResult.getStatus() == AuthResult.SUCCESS) {
     * // get principal and subject from authResult
     * Subject authenticatedSubject = authResult.getSubject();
     * return TAIResult.create(HttpServletResponse.SC_OK,
     * RequestUtil.getUserName(authenticatedSubject),
     * authenticatedSubject);
     * } else {
     * // error message
     * throw new SamlCallerTokenException("SAML20_USER_CANNOT_AUTHENTICATED",
     * //SAML20_USER_CANNOT_AUTHENTICATED=CWWKS5072E: The user [0] cannot be authenticated by the Service Provider.
     * null,
     * new Object[] { user });
     * }
     * }
     */
    Hashtable<String, Object> createHashtable(AssertionToSubject mapAssertToSubject,
                                              Saml20Token saml20Token,
                                              String user) throws SamlCallerTokenException, WSSecurityException, RemoteException {

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();

        String realm = null;
        String uniqueID = null;
        List<String> groups = null;
        if ("No".equalsIgnoreCase((String) callerConfig.get(CallerConstants.MAP_TO_UR))) {
            realm = mapAssertToSubject.getRealm();
            uniqueID = mapAssertToSubject.getUserUniqueIdentity(user, realm);
            groups = mapAssertToSubject.getGroupUniqueIdentity(realm);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            if (!groups.isEmpty()) {
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
            }
        } else if ("User".equalsIgnoreCase((String) callerConfig.get(CallerConstants.MAP_TO_UR))) {
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_USERID, user);

        } else if ("Group".equalsIgnoreCase((String) callerConfig.get(CallerConstants.MAP_TO_UR))) {
            realm = mapAssertToSubject.getRealm();
            uniqueID = mapAssertToSubject.getUserUniqueIdentity(user, realm);
            groups = mapAssertToSubject.getGroupUniqueIdentityFromRegistry(realm);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, user);
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            if (!groups.isEmpty()) {//Now we need call user registry
                putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
            }
        }

        putValue(hashtable, AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);

        // In the case disableLtpaToken is true, this needs to be true all the time
        // Otherwise, the SP_Toekn will not be created...
        // And yes, the isAllowCustomCacheKey did check disableLtpaToken, too.
        if ((Boolean) callerConfig.get(CallerConstants.ALLOW_CACHE_KEY))/* (ssoConfig.isAllowCustomCacheKey()) */{
            // add cacheKey in case LTPA and subject lifetime are different
            String cache_key = mapAssertToSubject.getCustomCacheKeyValue();
            putValue(hashtable, AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, cache_key);
        }

        //mapAssertToSubject.handleSessionNotOnOrAfter(hashtable, saml20Token);

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
