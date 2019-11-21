/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.util.RateLimiter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.error.impl.BrowserAndServerLogMessage;
import com.ibm.ws.security.oauth20.error.impl.OAuth20TokenRequestExceptionHandler;
import com.ibm.ws.security.oauth20.exception.OAuth20BadParameterException;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

/**
 *  This class handles processing of app-password and app-token requests.
 *  The name derives from both of these request types being basically an exchange of
 *  one's initial ID token obtained from interactive login,
 *  for something that's much longer lived, an app-password or an app-token.
 */
public class TokenExchange {
    public static final String CT_APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final String HDR_AUTHORIZATION = "Authorization";
    public static final String CT_APPLICATION_URLENC = "application/x-www-form-urlencoded";
    public static final int PARAM_MAX_LENGTH = 255;
    public static final String HTTP_METHOD_DELETE = "DELETE";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String NO_STORE = "no-store";
    public static final String NO_CACHE = "no-cache";
    public static final String PRAGMA = "Pragma";
    public static final String APP_ID_LOOKUP_KEY = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.APP_ID;
    public static final String APP_TOKEN_LOOKUP_KEY = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.APP_ID;
    public static final String ROLE_REQUIRED = OAuth20Constants.TOKEN_MANAGER_ROLE; // Role required to access services on behalf of other users
    public static final String[] ALLOWED_AT_GT = {
            OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE,
            OAuth20Constants.GRANT_TYPE_IMPLICIT, OAuth20Constants.GRANT_TYPE_IMPLICIT_INTERNAL,
            OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN, OAuth20Constants.GRANT_TYPE_JWT };
    public static final HashSet<String> ALLOWED_AT_GT_SET = new HashSet<String>(Arrays
            .asList(ALLOWED_AT_GT));
    public static final String[] ALLOWED_RT_GT = {
            OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE,
            OAuth20Constants.GRANT_TYPE_IMPLICIT };
    public static final HashSet<String> ALLOWED_RT_GT_SET = new HashSet<String>(Arrays
            .asList(ALLOWED_RT_GT));
    private static TraceComponent tc = Tr.register(TokenExchange.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final ThreadLocal<Map<String, Object>> auditMap = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<String, Object>(32);
        }
    };

    public int numberRevoked = 0;

    private final AuditManager auditManager = new AuditManager();

    private Enumeration<Locale> requestLocales = null;

    // internal class to encapsulate authentication and request info for convenient use.
    private final class AuthResult {
        AuthResult(boolean isAdmin, String user, String targetUser, String clientId, String appId) {

            this.isAdmin = isAdmin;
            this.user = setToNullIfEmpty(user);
            this.targetUser = setToNullIfEmpty(targetUser);
            this.clientId = setToNullIfEmpty(clientId);
            this.appOrTokenId = setToNullIfEmpty(appId);

            auditMap.get().put(AuditConstants.IS_ADMIN, isAdmin);
            auditMap.get().put(AuditConstants.USER, user);
            auditMap.get().put(AuditConstants.TARGET_USER, targetUser);
            auditMap.get().put(AuditConstants.CLIENT_ID, clientId);
            auditMap.get().put(AuditConstants.APP_OR_TOKEN_ID, appOrTokenId);
            auditMap.get().put(AuditConstants.APPLICATION_ID, appId);

        }

        private String setToNullIfEmpty(String in) {
            if (in != null && in.length() == 0) {
                return null;
            }
            return in;
        }

        final boolean isAdmin; // user is in tokenManager role
        final String user; // who the user is
        final String targetUser; // request contained user_name param, admin could act for another user.
        final String clientId; // clientId extracted from authorization header.
        final String appOrTokenId; // request contained app_id or token_id url fragment
    }

    /*
     * Parameter checking was performed upstream in OAuthEndpointServices.
     */

    protected void processAppPassword(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        processCommon(true, provider, request, response);
    }

    protected void processAppToken(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        processCommon(false, provider, request, response);
    }

    private void processCommonAudit(boolean isAppPasswordRequest, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        auditMap.get().clear();
        auditMap.get().put(AuditConstants.IS_APP_PASSWORD_REQUEST, isAppPasswordRequest);
        auditMap.get().put(AuditConstants.PROVIDER, provider.getID());
        auditMap.get().put(AuditConstants.REQUEST, request);
        auditMap.get().put(AuditConstants.RESPONSE, response);
        String endpoint = isAppPasswordRequest ? OAuth20Constants.APP_PASSWORD_URI : OAuth20Constants.APP_TOKEN_URI;
        auditMap.get().put(AuditConstants.ENDPOINT, endpoint);
        if (OidcOAuth20Util.isJwtToken(request.getHeader(OAuth20Constants.ACCESS_TOKEN))) {
            auditMap.get().put(AuditConstants.CREDENTIAL_TYPE, AuditConstants.JWT);
        }

    }

    private void processCommon(boolean isAppPasswordRequest, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        requestLocales = request.getLocales();

        processCommonAudit(isAppPasswordRequest, provider, request, response);

        if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET)) {
            processCommonGet(isAppPasswordRequest, provider, request, response);
        } else if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_POST)) {
            processCommonPost(isAppPasswordRequest, provider, request, response);
        } else if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_DELETE)) {
            processCommonDelete(isAppPasswordRequest, provider, request, response);
        } else {
            String endpoint = isAppPasswordRequest ? OAuth20Constants.APP_PASSWORD_URI : OAuth20Constants.APP_TOKEN_URI;
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_UNSUPPORTED_METHOD", new Object[] { request.getMethod(), endpoint });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg.getBrowserErrorMessage());
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
            auditMap.get().put(AuditConstants.DETAILED_ERROR, AuditConstants.BAD_REQUEST_UNSUPPORTED_METHOD);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
        }

    }

    private String processAuditRole(HttpServletRequest request) {
        String role = "";
        if (request.isUserInRole(AuditConstants.TOKEN_MANAGER))
            role += AuditConstants.TOKEN_MANAGER.concat(", ");
        if (request.isUserInRole(AuditConstants.CLIENT_MANAGER))
            role += AuditConstants.CLIENT_MANAGER.concat(", ");
        if (request.isUserInRole(AuditConstants.AUTHENTICATED))
            role += AuditConstants.AUTHENTICATED.concat(", ");

        return role;

    }

    private void processCommonDelete(boolean appPasswordRequest, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) {

        String[] clientIdAndSecret = extractClientIdAndSecretFromAuthHeader(request, response);
        if (clientIdAndSecret != null && clientIdAndSecret[0] != null) {
            auditMap.get().put(AuditConstants.CLIENT_ID, clientIdAndSecret[0]);
        }

        String role = processAuditRole(request);
        if (role.length() > 0)
            auditMap.get().put(AuditConstants.INITIATOR_ROLE, role.substring(0, role.length() - 2));

        AuthResult authRes = validateClientAndAuthenticate(appPasswordRequest, provider, request, response);

        if (authRes == null) {
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }

        boolean result = deleteAppPasswordOrToken(appPasswordRequest, authRes, provider);

        if (!result) { // something unexpected went wrong, there's no oauth error message for "internal error", so send http500.
            sendHttp500(response);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.SUCCESS);
        auditMap.get().put(AuditConstants.NUMBER_REVOKED, numberRevoked);
        Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());

        try {
            response.flushBuffer();
        } catch (IOException e) {
            // ffdc gets generated.
        }

    }

    /**
     * Request a new app-password or app-token
     * @param appPassWordRequest - true if for an app password, false if for an app token
     * @param provider1489
     * @param request
     * @param response
     */
    private void processCommonPost(boolean appPasswordRequest, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) {

        String[] clientIdAndSecret = extractClientIdAndSecretFromAuthHeader(request, response);
        if (clientIdAndSecret != null && clientIdAndSecret[0] != null) {
            auditMap.get().put(AuditConstants.CLIENT_ID, clientIdAndSecret[0]);
        }

        String role = processAuditRole(request);
        if (role.length() > 0)
            auditMap.get().put(AuditConstants.INITIATOR_ROLE, role.substring(0, role.length() - 2));

        boolean paramsOk = checkContentType(CT_APPLICATION_URLENC, request);
        if (!paramsOk) {
            sendInvalidRequestError(null, response);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }

        AuthResult authRes = validateClientAndAuthenticate(appPasswordRequest, provider, request, response);
        if (authRes == null) {
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }

        String appName = request.getParameter(OAuth20Constants.APP_NAME);
        appName = (appName == null) ? null : WebUtils.htmlEncode(request.getParameter(OAuth20Constants.APP_NAME));
        auditMap.get().put(AuditConstants.APPNAME, appName);

        // we could just silently replace the app-password for this app,
        // but that might be confusing, so fail if there's already one in use
        Collection<OAuth20Token> tokenCollection = getTokensForUser(appPasswordRequest, authRes, provider);
        BrowserAndServerLogMessage errorMsg = checkAppNameValidAndNotInUse(appPasswordRequest, authRes, appName, provider, tokenCollection);
        if (errorMsg != null) {
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendInvalidRequestError(errorMsg.getBrowserErrorMessage(), response);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }

        // check that user has not reached their maximum quantity limit of app-passwords or tokens
        errorMsg = checkTokenQuantityLimit(appPasswordRequest, authRes, provider, tokenCollection);
        if (errorMsg != null) {
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendInvalidRequestError(errorMsg.getBrowserErrorMessage(), response);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }

        // request is valid, create the app password or token, write them to the response.
        OAuthResult result = null;
        result = requestAppPasswordOrTokenJson(appPasswordRequest, authRes, appName, provider, request, response);

        if (result.getStatus() != OAuthResult.STATUS_OK) {
            OAuth20TokenRequestExceptionHandler handler = new OAuth20TokenRequestExceptionHandler();
            handler.handleResultException(request, response, result);
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
            auditMap.get().put(AuditConstants.DETAILED_ERROR, AuditConstants.FAILURE_TO_RETURN_REQUEST);
        } else {
            if (auditManager != null) {
                auditMap.get().put("respBody", auditManager.getAgent());
            }
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.SUCCESS);
        }
        Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
    }

    /**
     * check that after password/token creation, user will not be over max allowed number of tokens.
     * (amount applies individually to each type)
     * @param isAppPasswordRequest
     * @param authRes
     * @param provider
     * @param tokenCollection
     * @return an error message if they will be, null otherwise.
     */
    private BrowserAndServerLogMessage checkTokenQuantityLimit(boolean isAppPasswordRequest, AuthResult authRes, OAuth20Provider provider, Collection<OAuth20Token> tokenCollection) {

        boolean okToCreate = false;
        long maxQuantity = provider.getAppTokenOrPasswordLimit();
        okToCreate = tokenCollection.size() < maxQuantity;

        if (!okToCreate) {
            return new BrowserAndServerLogMessage(tc, "OAUTH_INVALID_REQUEST_TOO_MANY_TOKENS", new Object[] { authRes.user });
        } else {
            return null;
        }
    }

    /**
     * List the app id's or tokens
     * @param provider
     * @param request
     * @param response
     */
    private void processCommonGet(boolean appPasswordRequest, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) {

        String[] clientIdAndSecret = extractClientIdAndSecretFromAuthHeader(request, response);
        if (clientIdAndSecret != null && clientIdAndSecret[0] != null) {
            auditMap.get().put(AuditConstants.CLIENT_ID, clientIdAndSecret[0]);
        }

        String role = "";
        if (request.isUserInRole(AuditConstants.TOKEN_MANAGER))
            role += AuditConstants.TOKEN_MANAGER.concat(", ");
        if (request.isUserInRole(AuditConstants.CLIENT_MANAGER))
            role += AuditConstants.CLIENT_MANAGER.concat(", ");
        if (request.isUserInRole(AuditConstants.AUTHENTICATED))
            role += AuditConstants.AUTHENTICATED.concat(", ");

        if (role.length() > 0)
            auditMap.get().put(AuditConstants.INITIATOR_ROLE, role.substring(0, role.length() - 2));

        AuthResult authRes = validateClientAndAuthenticate(appPasswordRequest, provider, request, response);
        auditMap.get().put(AuditConstants.AUTH_RESULT, authRes);

        if (authRes == null) {
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
            if (response.getStatus() == 400)
                auditMap.get().put(AuditConstants.DETAILED_ERROR, AuditConstants.BAD_REQUEST);
            else if (response.getStatus() == 401)
                auditMap.get().put(AuditConstants.DETAILED_ERROR, AuditConstants.INVALID_CLIENT);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }

        String responseJson = listTokensJson(appPasswordRequest, authRes, provider);
        if (responseJson == null) { // some internal error occurred
            sendHttp500(response);
            auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
            auditMap.get().put(AuditConstants.DETAILED_ERROR, AuditConstants.INTERNAL_ERROR);
            Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());
            return;
        }
        sendGoodResponse(responseJson, response);
        auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.SUCCESS);
        Audit.audit(Audit.EventID.APPLICATION_PASSWORD_TOKEN_01, auditMap.get());

    }

    /**
     * validate the client and authenticate the user.  Send an appropriate rfc6749 sec 5.2 response if not successful.
     * @return an AuthResult if successful, null otherwise.
     */
    private AuthResult validateClientAndAuthenticate(boolean appPasswordRequest, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) {
        AuthResult result = null;
        String[] clientIdAndSecret = extractClientIdAndSecretFromAuthHeader(request, response);
        if (clientIdAndSecret == null) {
            return result;
        }
        if (!isAccessTokenPresent(request)) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_INVALID_REQUEST_NO_TOKEN", new Object[] { request.getMethod(), request.getRequestURI() });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            RateLimiter.limit();
            errorMsg.setLocales(requestLocales);
            sendInvalidRequestError(errorMsg.getBrowserErrorMessage(), response);
            return result;
        }

        // call authenticate here
        String user = null;
        if (!clientIdAndSecretValid(provider, clientIdAndSecret)) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_INVALID_REQUEST_AUTHN_FAIL", new Object[] { request.getMethod(), request.getRequestURI() });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            RateLimiter.limit();
            errorMsg.setLocales(requestLocales);
            sendInvalidClientError(errorMsg.getBrowserErrorMessage(), response);
            return result;
        } else {
            user = authenticate(provider, request.getHeader(OAuth20Constants.ACCESS_TOKEN), request, response, clientIdAndSecret);

        }
        if (user == null) {
            RateLimiter.limit();
            return result;
        }
        String clientId = clientIdAndSecret[0];
        // see if client is configured to allow use of app passwords/tokens
        if (!checkClientAuthorization(appPasswordRequest, provider, clientId, request.getMethod())) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_UNAUTHORIZED_CLIENT", new Object[] { clientId });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendUnauthorizedClientError(errorMsg.getBrowserErrorMessage(), response);
            return result;
        }

        boolean isAdmin = false;
        String targetUser = null;
        if (!(request.getMethod().equalsIgnoreCase(HTTP_METHOD_POST))) {
            targetUser = request.getParameter(OAuth20Constants.PARAM_USER_ID);
        }

        // if targetUser present, see if user is an admin
        if (targetUser != null) {
            if (!checkParamLength(OAuth20Constants.PARAM_USER_ID, targetUser, request.getRequestURI(), response)) {
                return result;
            }
            targetUser = escapeIllegalCharacters(targetUser);
            isAdmin = isAdminUser(user, request);
            if (!isAdmin && !targetUser.equals(user)) { // non-admin requesting processing of somebody else's token is invalid
                BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_SERVER_USERNAME_PARAM_NOT_SUPPORTED", new Object[] { request.getMethod(), request.getRequestURI() });
                Tr.error(tc, errorMsg.getServerErrorMessage());
                errorMsg.setLocales(requestLocales);
                sendInvalidRequestError(errorMsg.getBrowserErrorMessage(), response);
                return result;
            }
        }

        String appOrTokenId = getAppIdOrTokenId(request);
        appOrTokenId = appOrTokenId != null ? WebUtils.htmlEncode(appOrTokenId) : null;

        // auth success, construct the AuthResult
        return new AuthResult(isAdmin, user, targetUser, clientId, appOrTokenId);
    }

    /**
     * extract app-id or token-id from URL.
     * This is only valid for delete requests where app_id or token-id may be specified on the URL, for example:
     * /oidc/endpoint/<provider_name>/app-password/<app_id>/
     * @param request
     * @return
     */
    private String getAppIdOrTokenId(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase(HTTP_METHOD_DELETE)) {
            return null;
        }
        String appid = null;
        String path = request.getPathInfo();
        String apppws = "/" + OAuth20Constants.APP_PASSWORD_URI + "/";
        String apptoks = "/" + OAuth20Constants.APP_TOKEN_URI + "/";
        int apppw = path.indexOf(apppws);
        int apptok = path.indexOf(apptoks);
        if (apppw > 0) {
            appid = path.substring(apppw + apppws.length());
        } else if (apptok > 0) {
            appid = path.substring(apptok + apptoks.length());
        }
        if (appid == null || appid.length() == 0) {
            return null;
        }
        return appid;

    }

    /**
     * @param request
     * @return
     */
    private boolean isAccessTokenPresent(HttpServletRequest request) {
        String at = request.getHeader(OAuth20Constants.ACCESS_TOKEN);
        if (at != null && !at.isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * Given an access token, examine it and determine if it is valid and who it represents.
     * Also perform checks of type and clientId to see if token is valid to be used for exchange.
     * @param provider
     * @param accessTokenStr
     * @param request
     * @param response
     * @param clientIdAndSecret
     * @return user name if valid, null otherwise.
     * @throws OAuthException
     */
    @FFDCIgnore({ OAuth20BadParameterException.class, OAuthException.class })
    private String authenticate(OAuth20Provider provider, String accessTokenStr, HttpServletRequest request, HttpServletResponse response, String[] clientIdAndSecret) {
        String result = null;
        String tokenLookupStr = accessTokenStr;
        if (OidcOAuth20Util.isJwtToken(accessTokenStr)) {
            tokenLookupStr = com.ibm.ws.security.oauth20.util.HashUtils.digest(accessTokenStr);
        }
        OAuth20Token accessToken = provider.getTokenCache().get(tokenLookupStr); // it's also null when the token expires
        if (accessToken != null) {
            String tokenType = accessToken.getType();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "token type: " + tokenType);
            } // disallow refresh and id tokens
            if (!tokenType.equals(OAuth20Constants.ACCESS_TOKEN)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "token type not an access token, return null");
                }
                handleInvalidAccessTokenError(request, response, "OAUTH_INVALID_ATINREQUEST_AUTHN_FAIL"); // CWWKS1489E
                return null;
            } // disallow app-passwords and app-tokens
            String grantType = accessToken.getGrantType();
            String refreshTokenId = ((OAuth20TokenImpl) accessToken).getRefreshTokenKey();
            OAuth20Token refreshToken = null;
            if (refreshTokenId != null) {
                refreshToken = provider.getTokenCache().get(refreshTokenId);
            }
            // disallow types that might go around a 2-factor flow
            if (!validateGrantType(grantType, accessToken, refreshToken, request, response)) { // CWWKS1497E
                return null;
            }

            if (!clientInAccessTokenMatchesClientInAuthHeader(accessToken, clientIdAndSecret)) {
                handleInvalidAccessTokenError(request, response, "OAUTH_CLIENTINREQ_DONOT_MATCH_AT"); // CWWKS1492E
                return null;
            }
            result = accessToken.getUsername();
            if (result != null && HTTP_METHOD_POST.equalsIgnoreCase(request.getMethod())) {
                try {
                    buildAndSaveAttributeList(accessToken, request, response);
                } catch (OAuth20BadParameterException e) {
                    result = null;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "token user name reset to null due to an exception: " + e);
                    }

                } catch (OAuthException e) {
                    result = null;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "token user name reset to null due to an exception: " + e);
                    }
                }
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "token user name : " + result);
            }
        } else {
            handleInvalidAccessTokenError(request, response, "OAUTH_INVALID_ATINREQUEST_AUTHN_FAIL"); // CWWKS1489E
            return null;
        }
        return result;
    }

    /**
     * @param grantType
     * @param accessToken
     * @param refreshToken
     * @param response
     * @param request
     */
    private boolean validateGrantType(String grantType, OAuth20Token accessToken, OAuth20Token refreshToken, HttpServletRequest request, HttpServletResponse response) {

        if (grantType == null || !isGrantTypeInAllowedGrantTypes(grantType)) {
            Tr.error(tc, "WRONG_TOKEN_GRANTTYPE", grantType, Arrays.toString(ALLOWED_AT_GT)); // CWWKS1497E
            handleInvalidAccessTokenError(request, response, "OAUTH_INVALID_ATINREQUEST_AUTHN_FAIL"); // CWWKS1489E
            return false;
        }
        if (OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN.equals(grantType)) {
            // refreshed access token
            // do more checking to make sure that refresh token associated with this access token has original grant type set to authorization_code
            if (refreshToken == null || !validateRefreshTokenGrantType(refreshToken)) {
                Tr.error(tc, "WRONG_TOKEN_GRANTTYPE", grantType, Arrays.toString(ALLOWED_RT_GT)); // CWWKS1497E
                handleInvalidAccessTokenError(request, response, "OAUTH_INVALID_ATINREQUEST_AUTHN_FAIL"); // CWWKS1489E
                return false;
            }
        }
        return true;
    }

    /**
     * @param refreshToken
     * @return
     */
    private boolean validateRefreshTokenGrantType(OAuth20Token refreshToken) {
        String originalGT = getRefreshTokenOriginalGrantType(refreshToken);
        if (ALLOWED_RT_GT_SET.contains(originalGT)) {
            return true;
        }
        return false;
    }

    /**
     * @param refreshToken
     */
    private String getRefreshTokenOriginalGrantType(OAuth20Token refreshToken) {
        String[] buf = refreshToken.getExtensionProperty(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.REFRESH_TOKEN_ORIGINAL_GT);
        return buf == null ? null : buf[0];
    }

    /**
     * @param grantType
     */
    private boolean isGrantTypeInAllowedGrantTypes(String grantType) {

        if (ALLOWED_AT_GT_SET.contains(grantType)) {
            return true;
        }
        return false;
    }

    /**
     * @param accessToken
     * @param clientIdAndSecret
     * @return
     */
    private boolean clientInAccessTokenMatchesClientInAuthHeader(OAuth20Token accessToken, String[] clientIdAndSecret) {

        if (accessToken.getClientId() != null && accessToken.getClientId().equals(clientIdAndSecret[0])) {
            return true;
        }
        return false;
    }

    /**
     * @param response
     * @param request
     * @param msgKey
     *
     */
    private void handleInvalidAccessTokenError(HttpServletRequest request, HttpServletResponse response, String msgKey) {
        BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, msgKey, new Object[] { request.getMethod(), request.getRequestURI() });
        Tr.error(tc, errorMsg.getServerErrorMessage());
        errorMsg.setLocales(requestLocales);
        sendInvalidRequestError(errorMsg.getBrowserErrorMessage(), response);

    }

    /**
     * @param accessToken
     * @param response
     * @throws OAuthException
     */
    private void buildAndSaveAttributeList(OAuth20Token accessToken, HttpServletRequest request, HttpServletResponse response) throws OAuthException {
        if (accessToken != null) {
            AttributeList attributeList = (AttributeList) request.getAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
            boolean reqHasAttributes = true;
            if (attributeList == null) {
                attributeList = new AttributeList();
                reqHasAttributes = false;
            }
            addUserToAttributeList(accessToken.getUsername(), attributeList);
            addTokenExtensionAttributes(request, attributeList, response);
            String[] scope = accessToken.getScope();
            if (scope != null) {
                attributeList.setAttribute(OAuth20Constants.SCOPE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, scope);

            }
            String redirectUri = accessToken.getRedirectUri();
            if (redirectUri != null) {
                attributeList.setAttribute(OAuth20Constants.REDIRECT_URI, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, new String[] { redirectUri });
            }
            if (!reqHasAttributes) {
                request.setAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, attributeList);
            }
        }
    }

    /**
     * @param grantType
     * @param client
     * @param request
     * @param attributeList
     */
    private void addTokenExtensionAttributes(HttpServletRequest request, AttributeList attributeList, HttpServletResponse response) throws OAuthException {

        addUsedByExtensionAttribute(request, attributeList, response);
        /*
         * String usedFor = request.getParameter(OAuth20Constants.USED_FOR);
         * if (usedFor != null) {
         * String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.USED_FOR;
         * attributeList.setAttribute(key, OAuth20Constants.EXTERNAL_CLAIMS, new String[] { usedFor });
         *
         * }
         */
        String app_name = request.getParameter(OAuth20Constants.APP_NAME);
        if (app_name != null) {
            String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.APP_NAME;
            attributeList.setAttribute(key, OAuth20Constants.EXTERNAL_CLAIMS, new String[] { escapeIllegalCharacters(app_name) });

        }
    }

    /**
     * sanitize params we cannot htmlencode because we want to support non-ascii values,
     * i.e. app_name and user_id, possibly others
     */
    private String escapeIllegalCharacters(String in) {
        return in.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replace("'", "&#39;")
                .replaceAll("\"", "&quot;");
    }

    /**
     * check param length and send an error response if param is too long.
     * @param paramName
     * @param param
     * @param URI
     * @param resp
     * @return true if param is acceptable
     */
    private boolean checkParamLength(String paramName, String param, String URI, HttpServletResponse resp) {
        if (param.length() > PARAM_MAX_LENGTH) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_PARAMETER_VALUE_LENGTH_TOO_LONG", new Object[] { paramName, URI, PARAM_MAX_LENGTH });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendInvalidRequestError(errorMsg.getBrowserErrorMessage(), resp);
            return false;
        }
        return true;
    }

    private void addUsedByExtensionAttribute(HttpServletRequest request, AttributeList attributeList, HttpServletResponse response) throws OAuth20BadParameterException {
        String usedBy = request.getParameter(OAuth20Constants.USED_BY);
        if (usedBy == null) {
            return;
        }
        if (!checkParamLength(OAuth20Constants.USED_BY, usedBy, request.getRequestURI(), response)) {
            String errorMsg = Tr.formatMessage(tc, "OAUTH_PARAMETER_VALUE_LENGTH_TOO_LONG", new Object[] { OAuth20Constants.USED_BY, request.getRequestURI(), PARAM_MAX_LENGTH });
            throw new OAuth20BadParameterException(errorMsg, new Object[] { OAuth20Constants.USED_BY, usedBy });
        }

        usedBy = WebUtils.htmlEncode(usedBy);
        String[] usedByArray = usedBy.split(",");
        String key = OAuth20Constants.EXTERNAL_CLAIMS_PREFIX + OAuth20Constants.USED_BY;
        attributeList.setAttribute(key, OAuth20Constants.EXTERNAL_CLAIMS, usedByArray);
    }

    /**
     * @param request
     * @param attributeList
     * @throws OAuthException
     */
    private void addUserToAttributeList(String username, AttributeList attributeList) throws OAuthException {
        // String username = request.getParameter("user");
        OAuth20Util.validateRequiredAttribute(OAuth20Constants.USERNAME, username);
        attributeList.setAttribute(OAuth20Constants.USERNAME, OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST, new String[] { username });

    }

    /**
     * @param user
     * @return true if user is in tokenManager role
     */
    private boolean isAdminUser(String user, HttpServletRequest request) {

        if (request.isUserInRole(ROLE_REQUIRED)) {
            return true;
        }
        return false;
    }

    /**
     * Send invalid_request oauth error
     * @param errorDescription optional error description
     * @param response
     */
    private void sendInvalidRequestError(String errorDescription, HttpServletResponse response) {
        // {"error_description":"CWWKS1406E: The token request had an invalid client credential. The request URI was \/oidc\/endpoint\/OP\/token.","error":"invalid_request"}
        // send 400 per RFC6749 5.2
        WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST,
                Constants.ERROR_CODE_INVALID_REQUEST, errorDescription);
        auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);

        auditMap.get().put(AuditConstants.DETAILED_ERROR, Constants.ERROR_CODE_INVALID_REQUEST);

    }

    /**
     * @param errorDescription
     * @param response
     */
    private void sendInvalidClientError(String errorDescription, HttpServletResponse response) {
        WebUtils.sendErrorJSON(response, HttpServletResponse.SC_UNAUTHORIZED,
                Constants.ERROR_CODE_INVALID_CLIENT, errorDescription);
        auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);

        auditMap.get().put(AuditConstants.DETAILED_ERROR, Constants.ERROR_CODE_INVALID_CLIENT);

    }

    /**
     * @param errorDescription
     * @param response
     */
    private void sendUnauthorizedClientError(String errorDescription, HttpServletResponse response) {
        WebUtils.sendErrorJSON(response, HttpServletResponse.SC_BAD_REQUEST,
                Constants.ERROR_CODE_UNAUTHORIZED_CLIENT, errorDescription);
        auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);

        auditMap.get().put(AuditConstants.DETAILED_ERROR, Constants.ERROR_CODE_UNAUTHORIZED_CLIENT);

    }

    /**
     * Check that a registered client is configured to allow app-passwords or app-tokens.
     * @param appPasswordRequest - true if we're checking for app-password, false if we are checking for app-token
     * @param provider
     * @param clientId
     * @return true if client is configured to allow the password or token, or false if not
     */
    private boolean checkClientAuthorization(boolean appPasswordRequest, OAuth20Provider provider, String clientId, String requestMethod) {
        boolean result = false;
        OidcOAuth20ClientProvider cp = provider.getClientProvider();
        try {
            OidcBaseClient bc = cp.get(clientId);
            if (bc == null) {
                return false;
            }
            if (HTTP_METHOD_POST.equalsIgnoreCase(requestMethod)) {
                // Only the create operation requires appPasswordsAllowed or appTokensAllowed to be set to true
                result = appPasswordRequest ? bc.isAppPasswordAllowed() : bc.isAppTokenAllowed();
                result = bc.isEnabled() && result;
            } else {
                result = bc.isEnabled();
            }
        } catch (OidcServerException e) {
            // ffdc
        }
        return result;
    }

    /**
     * Check that client Id and secret match what is in the provider configuration
     * @param clientIdAndSecret
     * @return true if they do.
     */
    boolean clientIdAndSecretValid(OAuth20Provider provider, @Sensitive String[] clientIdAndSecret) {
        OidcBaseClient bc;
        try {
            OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
            if (clientProvider == null) {
                return false;
            }
            bc = clientProvider.get(clientIdAndSecret[0]);
        } catch (OidcServerException e) {
            // ffdc
            return false;
        }
        String storedSecret = null;
        if (bc != null) {
            storedSecret = bc.getClientSecret();
        }
        if (storedSecret == null) {
            return false;
        }
        return storedSecret.equals(clientIdAndSecret[1]);
    }

    /**
     * parse the authorization header and extract client id and secret
     * @param req
     * @return array containing id, secret or null if could not be extracted.
     */
    private @Sensitive String[] extractClientIdAndSecretFromAuthHeader(HttpServletRequest req, HttpServletResponse response) {
        String clientId = null;
        String clientSecret = null;
        String authHeader = req.getHeader(HDR_AUTHORIZATION);
        if (authHeader == null) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_INVALID_REQUEST_NO_AUTHHEADER", new Object[] { req.getMethod(), req.getRequestURI() });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendInvalidClientError(errorMsg.getBrowserErrorMessage(), response);
            return null;
        }
        if (!authHeader.startsWith("Basic ")) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_AUTH_HEADER_NOT_BASIC_AUTH", new Object[] { req.getMethod(), req.getRequestURI() });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendInvalidClientError(errorMsg.getBrowserErrorMessage(), response);
            return null;
        }

        String hdrValue = ClientAuthnData.decodeAuthorizationHeader(authHeader, req.getHeader(ClientAuthnData.AUTHORIZATION_ENCODING));
        int idx = hdrValue.indexOf(':');
        if (idx < 0) {
            clientId = hdrValue;
        } else {
            clientId = hdrValue.substring(0, idx);
            clientSecret = hdrValue.substring(idx + 1);
        }
        boolean valid = clientId != null && clientId.length() > 0 && clientSecret != null && clientSecret.length() > 0;
        if (!valid) {
            BrowserAndServerLogMessage errorMsg = new BrowserAndServerLogMessage(tc, "OAUTH_INVALID_REQUEST_NO_ID_SECRET", new Object[] { req.getMethod(), req.getRequestURI() });
            Tr.error(tc, errorMsg.getServerErrorMessage());
            errorMsg.setLocales(requestLocales);
            sendInvalidClientError(errorMsg.getBrowserErrorMessage(), response);
        }
        return valid ? new String[] { clientId, clientSecret } : null;
    }

    private void sendGoodResponse(String responseJson, HttpServletResponse response) {
        response.setHeader(CACHE_CONTROL, NO_STORE);
        response.setHeader(PRAGMA, NO_CACHE);
        response.setContentType(CT_APPLICATION_JSON_UTF8);
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            // write out in UTF-8 bytes format
            byte[] b = responseJson.getBytes("UTF-8");
            response.getOutputStream().write(b);
            response.flushBuffer();
        } catch (IOException e) {
            // ffdc gets generated.
        }
    }

    private void sendHttp500(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        auditMap.get().put(AuditConstants.AUDIT_OUTCOME, AuditConstants.FAILURE);
        auditMap.get().put(AuditConstants.DETAILED_ERROR, AuditConstants.INTERNAL_ERROR);

        try {
            response.getOutputStream().print("INTERNAL SERVER ERROR");
            response.flushBuffer();
        } catch (IOException e) {
            // ffdc gets generated.
        }
    }

    /**
     * delete (revoke) selected password or token from db
     * Validation that the user is in tokenAdmin role has already been performed.
     * @param appPasswordRequest
     * @param authRes
     * @return true if password or token deleted, or if the app_id was invalid, or of no such
     * Return false if internal error (db problem, etc.).
     *
     */
    private boolean deleteAppPasswordOrToken(boolean isAppPasswordRequest, AuthResult authRes, OAuth20Provider provider) {

        // admin vs. not, and app-password vs app-token is handled by getTokensForUser
        Collection<OAuth20Token> tokens = getTokensForUser(isAppPasswordRequest, authRes, provider);
        String tokenId = authRes.appOrTokenId;
        numberRevoked = 0;
        for (OAuth20Token token : tokens) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "considering removing token: " + token.getId());
            }
            boolean removeByAppId = isAppPasswordRequest && tokenId != null && tokenId.equals(getAppIdFromToken(token));
            boolean removeByTokenId = !isAppPasswordRequest && tokenId != null && tokenId.equals(getTokenIdFromToken(token));
            if (tokenId == null || removeByAppId || removeByTokenId) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "removing token: " + token.getId());
                }
                String tokenLookupKey = token.getId();
                if (provider.isLocalStoreUsed()) {
                    String encode = provider.getAccessTokenEncoding();
                    if (OAuth20Constants.PLAIN_ENCODING.equals(encode)) { // must be app-password or app-token
                        tokenLookupKey = EndpointUtils.computeTokenHash(tokenLookupKey);
                    } else {
                        tokenLookupKey = EndpointUtils.computeTokenHash(tokenLookupKey, encode);
                    }
                }
                provider.getTokenCache().removeByHash(tokenLookupKey);
                numberRevoked++;
            }
        }
        return true;
    }

    /**
     * @param ctApplicationUrlenc
     * @param request
     * @return true if content type is correct, false otherwise.
     */
    private boolean checkContentType(String ctApplicationUrlenc, HttpServletRequest request) {
        // TODO Auto-generated method stub
        // do we even need to check this?
        return true;
    }

    /**
     *
     * @param authRes
     * @param appName - an html encoded string
     * @param provider
     * @param tokenCollection
     * @return null if appName is good, or an oauth error description if not.
     */
    private BrowserAndServerLogMessage checkAppNameValidAndNotInUse(boolean isAppPasswordRequest, AuthResult authRes, String appName, OAuth20Provider provider, Collection<OAuth20Token> tokenCollection) {
        boolean inuse = false;
        boolean missing = appName == null || appName.length() == 0;
        if (missing) {
            return new BrowserAndServerLogMessage(tc, "OAUTH_COVERAGE_MAP_MISSING_TOKEN_PARAM", new Object[] { OAuth20Constants.APP_NAME });
        }

        boolean badLength = appName.length() > PARAM_MAX_LENGTH;
        if (!badLength)
            inuse = isAppNameInUse(tokenCollection, appName);
        if (badLength || inuse) {
            return new BrowserAndServerLogMessage(tc, "OAUTH_SERVER_APPNAME_IN_USE_OR_TOO_LONG", new Object[] { appName, provider.getID() });
        }
        return null;
    }

    /**
     * produce the app-password or app-token json.  it will be written directly to the response.
     * @param authRes
     * @param appName
     * @param request
     * @param response
     * @return The result
     */
    private OAuthResult requestAppPasswordOrTokenJson(boolean isAppPasswordRequest, AuthResult authRes, String appName, OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute("user", authRes.user);
        return provider.getComponent().processAppTokenRequest(isAppPasswordRequest, authRes.clientId, request, response);
    }

    /**
     * List app passwords or app tokens for a user.
     * If listing for a user other than the one logged in, the user name will be in authRes.targetUser.
     * Validation / authentication should be performed before calling this.
     * @param appPasswordRequest - true if for app_passwords, false if for app_tokens
     * @param authRes
     * @param provider
     * @return Json string representing the tokens.
     */
    private String listTokensJson(boolean isAppPasswordRequest, AuthResult authRes, OAuth20Provider provider) {
        return tokensToJson(isAppPasswordRequest, getTokensForUser(isAppPasswordRequest, authRes, provider));
    }

    private Collection<OAuth20Token> getTokensForUser(boolean isAppPasswordRequest, AuthResult authRes, OAuth20Provider provider) {
        String userName = authRes.targetUser != null ? authRes.targetUser : authRes.user;
        return EndpointUtils.getTokensForUser(isAppPasswordRequest, false, userName, authRes.clientId, provider);
    }

    /**
     * give the collection of app-tokens or app-passwords for a user, see if a given appname is already in use
     * @param tokens
     * @return
     */
    private boolean isAppNameInUse(Collection<OAuth20Token> tokens, String appName) {
        for (OAuth20Token token : tokens) {
            if (token.getAppName().equals(appName)) {
                return true;
            }
        }
        return false;
    }

    private String tokensToJson(boolean appPasswordRequest, Collection<OAuth20Token> tokens) {
        StringBuffer sb = new StringBuffer();
        boolean haveTokens = false;
        String arrayType = appPasswordRequest ? "\"app-passwords\"" : "\"app-tokens\"";
        sb.append("{");
        sb.append(arrayType);
        sb.append(":[");
        for (OAuth20Token token : tokens) {
            haveTokens = true;
            sb.append(tokenToJson(token));
            sb.append(",");
        }
        if (haveTokens) {
            sb.deleteCharAt(sb.length() - 1); // remove trailing ,
        }
        sb.append("]}");
        return sb.toString();
    }

    private String tokenToJson(OAuth20Token token) {
        StringBuffer sb = new StringBuffer();
        if (token == null) {
            return null;
        }
        String appId = getAppIdFromToken(token);
        sb.append("{");
        sb.append("\"user\":\"");
        sb.append(EndpointUtils.escapeQuotesForJson(token.getUsername()));
        sb.append("\",\"name\":\"");
        sb.append(EndpointUtils.escapeQuotesForJson(token.getAppName()));
        sb.append("\"");
        if (appId != null) {
            sb.append(",\"app_id\":\"");
            sb.append(appId);
            sb.append("\"");
        }
        sb.append(",\"created_at\":");
        sb.append(token.getCreatedAt());
        sb.append(",\"expires_at\":");
        sb.append((token.getLifetimeSeconds() * 1000L) + token.getCreatedAt());
        if (token.getUsedBy() != null) {
            sb.append(",\"used_by\":\"");
            sb.append(token.getUsedBy()[0]);
            sb.append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String getAppIdFromToken(OAuth20Token token) {
        String appId = null;
        String[] appIds = token.getExtensionProperties().get(APP_ID_LOOKUP_KEY);
        if (appIds != null) {
            appId = appIds[0];
        }
        return appId;
    }

    private String getTokenIdFromToken(OAuth20Token token) {
        String tokenId = null;
        String[] tokenIds = token.getExtensionProperties().get(APP_TOKEN_LOOKUP_KEY);
        if (tokenIds != null) {
            tokenId = tokenIds[0];
        }
        return tokenId;
    }

}
