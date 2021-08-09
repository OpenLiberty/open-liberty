/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.ibm.oauth.core.api.OAuthComponentInstance;
import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20AuthorizationCodeInvalidClientException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20BadParameterFormatException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InternalException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientSecretException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenRequestMethodException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MismatchedClientAuthenticationException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20PublicClientCredentialsException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20PublicClientForbiddenException;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.OAuthComponentImpl;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigValidator;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigurationImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactoryImpl;
import com.ibm.oauth.core.internal.oauth20.mediator.OAuth20MediatorFactory;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandler;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactoryImpl;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandlerFactory;
import com.ibm.oauth.core.internal.statistics.OAuthStatisticsImpl;
import com.ibm.oauth.core.util.WebUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.context.AuditManager;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.util.HashUtils;
import com.ibm.ws.security.oauth20.util.MessageDigestUtil;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class OAuth20ComponentImpl extends OAuthComponentImpl implements
        OAuth20Component, OAuth20ComponentInternal {

    final static String CLASS = OAuth20ComponentImpl.class.getName();
    Logger _log = Logger.getLogger(CLASS);

    protected static final TraceComponent tc = Tr.register(OAuth20ComponentImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    final static String HTTP_METHOD_POST = "POST";
    final static String RESPONSE_MODE = "response_mode";
    final static String FORM_POST = "form_post";

    // oidc10 : allow GrantTypeHandlerFactory to be plugged in
    OAuth20GrantTypeHandlerFactory _grantTypeHandlerFactory = null;
    // oidc10 : allow ResponseTypeHandlerFactory to be plugged in
    OAuth20ResponseTypeHandlerFactory _responseTypeHandlerFactory = null;

    static final String STATE_ENCODING = "UTF-8";

    OidcOAuth20ClientProvider _clientProvider = null;
    OAuth20TokenCache _tokenCache = null;
    boolean _allowPublicClients = false;
    protected OAuth20ConfigProvider _config20;

    public OAuth20ComponentImpl(OAuthComponentInstance parent, OAuthComponentConfiguration config, OAuth20ConfigProvider configProvider) throws OAuthException {
        super(parent, config);
        _config20 = configProvider;
        _grantTypeHandlerFactory = initGrantTypeHandlerFactory(_config20);
        _responseTypeHandlerFactory = initResponseTypeHandlerFactory(_config20);
        _clientProvider = _config20.getClientProvider();
        _tokenCache = _config20.getTokenCache();
        _allowPublicClients = _config20.isAllowPublicClients();
    }

    OAuth20ComponentImpl() {
        super(null, null);
    }

    public OAuth20ComponentImpl(OAuthComponentInstance parent,
            OAuthComponentConfiguration config) throws OAuthException {
        super(parent, config);

        /*
         * Instantiate a client configuration provider using the class load from
         * the config
         */
        OAuthComponentConfiguration configwrapper = super.getConfiguration();
        OAuth20ConfigValidator configValidator = new OAuth20ConfigurationImpl(
                this, configwrapper);
        configValidator.validate();

        _config20 = configValidator.getConfigProvider();
        _grantTypeHandlerFactory = initGrantTypeHandlerFactory(_config20); // _config had been initialized by OAuthComponentImp oidc10
        _responseTypeHandlerFactory = initResponseTypeHandlerFactory(_config20); // _config had been initialized by OAuthComponentImp oidc10

        _clientProvider = _config20.getClientProvider();
        _tokenCache = _config20.getTokenCache();
        _allowPublicClients = _config20.isAllowPublicClients();
    }

    OAuth20GrantTypeHandlerFactory initGrantTypeHandlerFactory(OAuth20ConfigProvider config)
            throws OAuthException {
        OAuth20GrantTypeHandlerFactory result = config.getGrantTypeHandlerFactory();
        if (result == null) {
            // System.out.println("Can not get the OAuth20GrantTypeHandlerFactoryImpl from WSSEC");
            result = new OAuth20GrantTypeHandlerFactoryImpl();
        }
        return result;
    }

    OAuth20ResponseTypeHandlerFactory initResponseTypeHandlerFactory(OAuth20ConfigProvider config)
            throws OAuthException {
        OAuth20ResponseTypeHandlerFactory result = config.getResponseTypeHandlerFactory();
        if (result == null) {
            // System.out.println("Can not get the OAuth20ResponseTypeHandlerFactoryImpl");
            result = new OAuth20ResponseTypeHandlerFactoryImpl();
            result.init(super.getConfiguration());
        }
        return result;
    }

    @Override
    public OAuth20ConfigProvider get20Configuration() {
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, "get20Configuration", "get20Configuration returns [" + _config20 + "]");
        }
        return _config20;
    }

    @Override
    public OAuthComponentConfiguration getConfiguration() {
        throw new UnsupportedOperationException(
                "called OAuth20ComponentImpl.getConfiguration(). Use OAuth20ComponentImpl.get20Configuration() instead.");
    }

    @Override
    public OAuthResult processAuthorization(HttpServletRequest request, HttpServletResponse response, AttributeList options) {
        String username = request.getUserPrincipal().getName();
        String clientId = request.getParameter(OAuth20Constants.CLIENT_ID);
        String redirectUri = request.getParameter(OAuth20Constants.REDIRECT_URI);
        String responseType = request.getParameter(OAuth20Constants.RESPONSE_TYPE);
        String state = request.getParameter(OAuth20Constants.STATE);
        // String[] scope = new String[0];
        String[] scope = null;
        if (options != null) {
            scope = options.getAttributeValuesByName(OAuth20Constants.SCOPE);
        }
        // if (request.getParameter(OAuth20Constants.SCOPE) != null) {
        // scope = request.getParameter(OAuth20Constants.SCOPE).split(" ");
        // }
        return processAuthorization(username, clientId, redirectUri, responseType, state, scope, options, request, response);
    }

    @Override
    public OAuthResult processAuthorization(String username, String clientId, String redirectUri, String responseType, String state, String[] authorizedScopes, HttpServletResponse response) {
        return processAuthorization(username, clientId, redirectUri, responseType, state, authorizedScopes, null, null, response);
    }

    public OAuthResult processAuthorization(String username, String clientId, String redirectUri, String responseType, String state,
            String[] authorizedScopes, AttributeList options, HttpServletRequest request, HttpServletResponse response) {
        String methodName = "processAuthorization";

        boolean errorOccurred = true;
        List<OAuth20Token> tokens = null;
        _log.entering(CLASS, methodName, new String[] { clientId, redirectUri, responseType, state,
                "{" + OAuth20Util.arrayToSpaceString(authorizedScopes) + "}" });
        OAuthResult result = null;
        OAuth20RequestContext requestContext = new OAuth20RequestContext();
        AttributeList attributeList = new AttributeList();
        OAuth20Mediator mediator = null;
        try {
            // get mediator
            mediator = OAuth20MediatorFactory.getMediator(this);

            attributeList = buildAuthorizationAttributeList(username, clientId, redirectUri, responseType, state, authorizedScopes);
            addRequestHeaderToAttributeList(request, attributeList);

            OAuth20Client client = getOAuth20Client(requestContext, clientId, null, redirectUri, false);

            processPKCEAndUpdateAttributeList(request, client, attributeList);
            // jwtAccessToken
            if (request != null) {
                String[] resource = (String[]) request.getAttribute(OAuth20Constants.OAUTH20_AUTHEN_PARAM_RESOURCE); // audiences
                if (resource != null) {
                    attributeList.setAttribute(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, resource);
                }
            }
            populateJwtAccessTokenData(client, attributeList);
            // oidc10
            boolean preInvokeMediation = false;
            if (options != null) {
                List<Attribute> attrs = options.getAllAttributes();
                for (Attribute attr : attrs) {
                    // if there is not a duplicate entry, put the attribute to attributeList.
                    if (attributeList.getAttributeValuesByNameAndType(attr.getName(), attr.getType()) == null) {
                        attributeList.setAttribute(attr.getName(), attr.getType(), attr.getValuesArray());
                    }
                    if (OAuth20Constants.EXTERNAL_MEDIATION.equals(attr.getType())) {
                        // "com.ibm.wsspi.security.oidc.external.mediation"
                        preInvokeMediation = true;
                    }
                }
            }

            JsonArray registeredRedirectUris = client.getRedirectUris();

            OAuth20ResponseTypeHandler rth = _responseTypeHandlerFactory.getHandler(responseType, this.get20Configuration());

            if (responseType.indexOf(OIDCConstants.TOKENTYPE_ID_TOKEN) != -1 && request != null) {
                populateFromRequestForOpenIDConnect(attributeList, request);
            }
            // validate the request
            boolean allowRegexpRedirects = client.getAllowRegexpRedirects();
            rth.validateRequestResponseType(attributeList, registeredRedirectUris, allowRegexpRedirects);

            if (preInvokeMediation) {
                mediator.mediateAuthorize(attributeList);
            }

            // build any necessary tokens to return
            OAuth20TokenFactory tokenFactory = new OAuth20TokenFactory(this);
            tokens = rth.buildTokensResponseType(attributeList, tokenFactory, registeredRedirectUris.get(0).getAsString());

            // build the response attributes
            rth.buildResponseResponseType(attributeList, tokens);

            // check for any state and send it back
            if (state != null && state.length() > 0) {
                attributeList.setAttribute(OAuth20Constants.STATE, OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE, new String[] { state });
            }

            // now invoke the mediator in case it has any work to do
            if (!preInvokeMediation) {
                mediator.mediateAuthorize(attributeList);
            }

            /*
             * Write back the response as a browser redirect, and populate
             * OAuthResult. If there is no redirect URI from the request, use
             * the registered one.
             */
            String responseRedirectURI = (redirectUri == null || redirectUri.length() == 0) ? client.getRedirectUris().get(0).getAsString() : redirectUri;

            // response_mode=form_post
            String strResponseMode = request != null ? request.getParameter(RESPONSE_MODE) : null;
            if (FORM_POST.equals(strResponseMode)) {
                postRedirect(response, attributeList, responseRedirectURI);
            } else {
                /*
                 * see draft-ietf-oauth-v2-11#section-4.2 - if the response contains
                 * a token, all of the response parameters should be included in the
                 * fragment component of the uri
                 */
                String accessToken = attributeList.getAttributeValueByName(OAuth20Constants.ACCESS_TOKEN);
                String idToken = attributeList.getAttributeValueByName(OAuth20Constants.ID_TOKEN);
                boolean implicit = (accessToken != null && accessToken.length() > 0) ||
                        (idToken != null && idToken.length() > 0); // check ID Token as well

                sendRedirect(response, attributeList, responseRedirectURI, implicit);
            }

            /*
             * Success!!
             */
            result = new OAuthResultImpl(OAuthResult.STATUS_OK, attributeList);
            errorOccurred = false;
        } catch (OAuthException e) {
            result = processAuthorizationException(attributeList, mediator, e);
        } catch (Exception e) {
            OAuthException oauthException = new OAuth20InternalException(e);
            result = processAuthorizationException(attributeList, mediator, oauthException);
        } finally {
            // Remove any tokens that were created if there was a failure in processing request
            if (errorOccurred && tokens != null) {
                ListIterator<OAuth20Token> tokenIter = tokens.listIterator();
                while (tokenIter.hasNext()) {
                    OAuth20Token token = tokenIter.next();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "processAuthorization: Found token to remove from cash.");
                        Tr.debug(tc, "Type: " + token.getType());
                        Tr.debug(tc, "Subtype: " + token.getSubType());
                        Tr.debug(tc, "ClientId: " + token.getClientId());
                        Tr.debug(tc, "Username: " + token.getUsername());
                    }
                    _tokenCache.remove(token.getId());
                }
            }
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    /**
     * @param request
     * @param client
     * @param attributeList
     * @throws OAuth20BadParameterFormatException
     * @throws OAuth20DuplicateParameterException
     * @throws OAuth20MissingParameterException
     */
    public void processPKCEAndUpdateAttributeList(HttpServletRequest request, OAuth20Client client, AttributeList attributeList) throws OAuth20DuplicateParameterException, OAuth20BadParameterFormatException, OAuth20MissingParameterException {
        String methodName = "processPKCEAndUpdateAttributeList";
        _log.entering(CLASS, methodName);
        String code_challenge = null;
        String code_challenge_method = null;
        if (request != null) {
            code_challenge = request.getParameter(OAuth20Constants.CODE_CHALLENGE);
            code_challenge_method = request.getParameter(OAuth20Constants.CODE_CHALLENGE_METHOD);
            // remember that PKCE may not be involved in any way - it may be ok to not have challenge and challenge_method
            if ((challengeHasValue(code_challenge) || (((OidcOAuth20Client) client).isProofKeyForCodeExchangeEnabled())) && !challengeHasValue(code_challenge_method)) {
                code_challenge_method = OAuth20Constants.CODE_CHALLENGE_METHOD_PLAIN;
            }
            if (challengeHasValue(code_challenge_method) && isValidCodeChallengeMethod(code_challenge_method) && code_challenge == null) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", "code_challenge", null);
            }

            if ((challengeHasValue(code_challenge_method)) && (!isValidCodeChallengeMethod(code_challenge_method))) {
                throw new OAuth20MissingParameterException("security.oauth20.pkce.invalid.method.error", code_challenge_method, null);
            }
        }
        // save challenge and method if set
        if (challengeHasValue(code_challenge) && challengeHasValue(code_challenge_method)) {
            addParameterToAttributeList(OAuth20Constants.CODE_CHALLENGE,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY, code_challenge, attributeList);

            addParameterToAttributeList(OAuth20Constants.CODE_CHALLENGE_METHOD,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY, code_challenge_method, attributeList);
        } else if (((OidcOAuth20Client) client).isProofKeyForCodeExchangeEnabled()) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", "code_challenge", null);
        }
        _log.exiting(CLASS, methodName);
    }

    /**
     * @param code_challenge
     * @return
     */
    private boolean challengeHasValue(String challenge) {

        if (challenge != null && challenge.length() > 0) {
            return true;
        }
        return false;
    }

    /**
     * @param code_challenge_method
     * @return
     */
    private boolean isValidCodeChallengeMethod(String code_challenge_method) {

        if (OAuth20Constants.CODE_CHALLENGE_METHOD_PLAIN.equals(code_challenge_method) || OAuth20Constants.CODE_CHALLENGE_METHOD_S256.equals(code_challenge_method)) {
            return true;
        }

        return false;
    }

    private OAuthResult processAuthorizationException(AttributeList attributeList, OAuth20Mediator mediator, OAuthException e) {
        OAuthResult result;
        try {
            if (mediator != null) {
                mediator.mediateAuthorizeException(attributeList, e);
            }
            result = new OAuthResultImpl(OAuthResult.STATUS_FAILED, attributeList, e);
        } catch (OAuth20MediatorException me) {
            // mediator developer SHOULD include the OAuthException in
            // OAuth20MediatorException
            result = new OAuthResultImpl(OAuthResult.STATUS_FAILED, attributeList, me);
        }
        return result;
    }

    @Override
    public OAuthResult processTokenRequest(String authenticatedClient, HttpServletRequest request, HttpServletResponse response) {
        String methodName = "processTokenRequest";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        boolean errorOccurred = true;
        List<OAuth20Token> newTokens = null;

        _log.entering(CLASS, methodName, new String[] { authenticatedClient });
        OAuthResult result = null;

        AttributeList attributeList = (AttributeList) request.getAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
        if (attributeList == null) {
            attributeList = new AttributeList();
        }
        OAuth20Mediator mediator = null;
        OAuth20RequestContext requestContext = new OAuth20RequestContext();
        try {
            // get mediator
            mediator = OAuth20MediatorFactory.getMediator(this);

            /*
             * not actually used in the method, but the retrieval does
             * validation we need
             */
            OAuth20Client client = null;

            String clientId = request.getParameter(OAuth20Constants.CLIENT_ID);
            String grantType = request.getParameter(OAuth20Constants.GRANT_TYPE);

            /*
             * build the AttributeList from the request parameters
             */
            if (clientId == null) {
                clientId = authenticatedClient;
            }
            // attributeList = buildTokenAttributeList(clientId, request);

            // make sure the client used the HTTP POST method
            if (!HTTP_METHOD_POST.equalsIgnoreCase(request.getMethod())) {
                throw new OAuth20InvalidTokenRequestMethodException("security.oauth20.error.invalid.tokenrequestmethod", request.getMethod());
            }

            /*
             * If the component consumer authenticated the client, build the
             * client based on the authenticatedClient id, otherwise look for a
             * client secret and if that's there authenticate the client that
             * way. If the authenticatedClient is not provided, AND there is no
             * client secret, then this must be a public client, so check if
             * that's allowed before proceeding.
             */
            if (authenticatedClient != null && authenticatedClient.length() > 0) {
                /*
                 * Verify the clientId (if supplied) matches the
                 * authenticatedClient
                 */
                /*
                 * if (clientId == null) {
                 * clientId = authenticatedClient;
                 * }
                 */
                if (!clientId.equals(authenticatedClient)) {
                    Tr.error(tc, "security.oauth20.detail.error.mismatched.clientauthentication", clientId, authenticatedClient);
                    throw new OAuth20MismatchedClientAuthenticationException("security.oauth20.error.mismatched.clientauthentication", clientId, authenticatedClient);
                }
                client = getOAuth20Client(requestContext, authenticatedClient, null, null, true);
            } else {

                // This piece of code is not reachable since the ClientAuthentication will always set authenticatedClient
                // But the provider is pluginbable. Keep this piece of code for future implementation
                String clientSecret = request.getParameter(OAuth20Constants.CLIENT_SECRET);

                // check if public client and public clients allowed
                if (clientSecret == null || clientSecret.length() == 0) {
                    // public clients may not use client_credentials grant type
                    if (grantType != null && grantType.equals(OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS)) {
                        throw new OAuth20PublicClientCredentialsException("security.oauth20.error.publicclient.credential", clientId);
                    }

                    // check if public clients are allowed at all by config
                    if (!_allowPublicClients) {
                        throw new OAuth20PublicClientForbiddenException("security.oauth20.error.publicclient.forbidden", clientId);
                    }
                }

                client = getOAuth20Client(requestContext, clientId, clientSecret, null, true);
            }
            // JWT token put redirect_uri in attributeList
            attributeList = buildTokenAttributeList(grantType, client, request, attributeList);
            addRequestHeaderToAttributeList(request, attributeList);

            /*
             * Determine the grant type handler to use to process the request
             */
            OAuth20GrantTypeHandler gth = _grantTypeHandlerFactory.getHandler(_parent.getInstanceId(), grantType, this.get20Configuration());

            List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            List<String> tokenKeys = gth.getKeysGrantType(attributeList);

            /*
             * get the list of token(s) needed by this request from the cache
             */
            if (tokenKeys != null) {
                ListIterator<String> keyIter = tokenKeys.listIterator();
                while (keyIter.hasNext()) {
                    String key = keyIter.next();
                    OAuth20Token token = getOAuth20Token(requestContext, key, OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT, grantType, true);
                    tokens.add(token);
                }
                if (tokens.size() >= 1) {
                    OAuth20Token code = tokens.get(0);
                    String code_challenge = null;
                    String code_challenge_method = null;
                    if (client instanceof OidcOAuth20Client) {
                        code_challenge = code.getCodeChallenge();
                        code_challenge_method = code.getCodeChallengeMethod();
                        if ((((OidcOAuth20Client) client).isProofKeyForCodeExchangeEnabled()) || code_challenge != null) {
                            // if code has code_challenge, then we expect that the request will have code_verifier
                            // it is error 1) if the code_verifier is missing or 2) if the code_verifier does not match with the verifier that we derive from the code_challenge
                            //
                            handlePKCEVerification(code, code_challenge, code_challenge_method, attributeList);
                        } else if (requestHasCodeVerifier(attributeList) && code_challenge == null) {
                            String message = Tr.formatMessage(tc, "security.oauth20.pkce.error.mismatch.codeverifier", "null", attributeList
                                    .getAttributeValueByName(OAuth20Constants.CODE_VERIFIER));
                            throw new InvalidGrantException(message, null);
                        }
                    }
                }
            }

            populateFromRequestForOpenIDConnect(attributeList, request);

            // validate the request
            gth.validateRequestGrantType(attributeList, tokens);

            // build any necessary tokens to return
            OAuth20TokenFactory tokenFactory = new OAuth20TokenFactory(this);
            newTokens = gth.buildTokensGrantType(attributeList, tokenFactory, tokens);

            // remove the old token(s) from the cache
            ListIterator<OAuth20Token> tokenIter = tokens.listIterator();
            while (tokenIter.hasNext()) {
                OAuth20Token token = tokenIter.next();
                removeOldToken(token);
                // _tokenCache.remove(token.getId());
            }

            gth.buildResponseGrantType(attributeList, newTokens);

            // now invoke the mediator in case it has any work to do
            mediator.mediateToken(attributeList);

            // now write back the response as JSON
            String data = buildResponseDataString(attributeList, true, false); // true means will do Json encode later
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName, "Response attributes: " + data);
            }

            response.setHeader(OAuth20Constants.HEADER_CACHE_CONTROL, OAuth20Constants.HEADERVAL_CACHE_CONTROL);
            response.setHeader(OAuth20Constants.HEADER_PRAGMA, OAuth20Constants.HEADERVAL_PRAGMA);
            response.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE, OAuth20Constants.HTTP_CONTENT_TYPE_JSON);

            String json = OAuth20Util.JSONEncode(data);
            try {
                PrintWriter pw = response.getWriter();
                pw.write(json);
                pw.flush();
            } catch (IOException e) {
                String[] objs = new String[] { clientId, e.getMessage() };
                throw new OAuth20InternalException("security.oauth20.error.token.internal.exception", e, objs);
            }

            /*
             * Success!!
             */
            result = new OAuthResultImpl(OAuthResult.STATUS_OK, attributeList);
            errorOccurred = false;
        } catch (OAuthException e) {
            result = processException(attributeList, mediator, e);
        } catch (Exception e) {
            String[] objs = new String[] { request.getParameter(OAuth20Constants.CLIENT_ID), e.getMessage() };
            OAuthException oauthException = new OAuth20InternalException("security.oauth20.error.token.internal.exception", (e.getCause() == null) ? e : e.getCause(), objs);
            result = processException(attributeList, mediator, oauthException);
        } finally {
            // Remove any tokens that were created if there was a failure in processing request
            if (errorOccurred && newTokens != null) {
                ListIterator<OAuth20Token> tokenIter = newTokens.listIterator();
                while (tokenIter.hasNext()) {
                    OAuth20Token token = tokenIter.next();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "processTokenRequest: Found token to remove from cash.");
                        Tr.debug(tc, "Type: " + token.getType());
                        Tr.debug(tc, "Subtype: " + token.getSubType());
                        Tr.debug(tc, "ClientId: " + token.getClientId());
                        Tr.debug(tc, "Username: " + token.getUsername());
                    }
                    _tokenCache.remove(token.getId());
                }
            }
            _log.exiting(CLASS, methodName, result);
        }
        return result;

    }

    /**
     * @param attributeList
     * @return
     */
    private boolean requestHasCodeVerifier(AttributeList attributeList) {
        String code_verifier = attributeList
                .getAttributeValueByName(OAuth20Constants.CODE_VERIFIER);
        if (code_verifier != null && code_verifier.length() > 0) {
            return true;
        }
        return false;
    }

    /**
     *
     * @param code_challenge
     * @param code_challenge_method
     * @throws OAuth20AuthorizationCodeInvalidClientException
     * @throws OAuth20MissingParameterException
     * @throws InvalidGrantException
     */
    public void handlePKCEVerification(OAuth20Token code, String code_challenge, String code_challenge_method, AttributeList attributeList) throws OAuth20AuthorizationCodeInvalidClientException, OAuth20MissingParameterException, InvalidGrantException {

        String methodName = "handlePKCEVerification";
        _log.entering(CLASS, methodName);

        String code_verifier = attributeList
                .getAttributeValueByName(OAuth20Constants.CODE_VERIFIER);
        if (code_verifier == null) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                    "code_verifier", null);
        } else {
            if (!isCodeVerifierLengthAcceptable(code_verifier)) {
                String message = Tr.formatMessage(tc, "security.oauth20.pkce.codeverifier.length.error", code_verifier.length());
                throw new InvalidGrantException(message, null);
            }
            if (OAuth20Constants.CODE_CHALLENGE_METHOD_PLAIN.equals(code_challenge_method) && !code_challenge.equals(code_verifier)) {
                String message = Tr.formatMessage(tc, "security.oauth20.pkce.error.mismatch.codeverifier", code_challenge, code_verifier);
                throw new InvalidGrantException(message, null);
                // throw new OAuth20AuthorizationCodeInvalidClientException("security.oauth20.error.invalid.authorizationcode",
                // code.getTokenString(), code.getClientId());
            } else if (OAuth20Constants.CODE_CHALLENGE_METHOD_S256.equals(code_challenge_method)) {
                String derived_code_challenge = HashUtils.encodedDigest(code_verifier, OAuth20Constants.CODE_CHALLENGE_ALG_METHOD_SHA256, OAuth20Constants.CODE_VERIFIER_ASCCI);
                if (!code_challenge.equals(derived_code_challenge)) {
                    String message = Tr.formatMessage(tc, "security.oauth20.pkce.error.mismatch.codeverifier", code_challenge, code_verifier);
                    throw new InvalidGrantException(message, null);
                }
            }
        }

        _log.exiting(CLASS, methodName);
    }

    /**
     * @param code_verifier
     * @return
     */
    public boolean isCodeVerifierLengthAcceptable(String code_verifier) {

        if (code_verifier != null && (code_verifier.length() >= OAuth20Constants.CODE_VERIFIER_MIN_LENGTH && code_verifier.length() <= OAuth20Constants.CODE_VERIFIER_MAX_LENGTH)) {
            return true;
        }

        return false;
    }

    /**
     * @param token
     */
    private void removeOldToken(OAuth20Token token) {
        boolean isAuthorizationGrantTypeAndCodeSubType = OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT.equals(token.getType()) && OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.equals(token.getSubType());
        String key = token.getId();
        if (isAuthorizationGrantTypeAndCodeSubType && _tokenCache instanceof OAuth20EnhancedTokenCache) {
            key = MessageDigestUtil.getDigest(key);
            ((OAuth20EnhancedTokenCache) _tokenCache).removeByHash(key);
        } else {
            _tokenCache.remove(key);
        }

    }

    @Override
    public OAuthResult processAppTokenRequest(boolean isAppPasswordRequest, String authenticatedClient, HttpServletRequest request, HttpServletResponse response) {
        boolean errorOccurred = true;
        List<OAuth20Token> newTokens = null;
        OAuthResult result = null;
        AttributeList attributeList = (AttributeList) request.getAttribute(OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
        if (attributeList == null) {
            attributeList = new AttributeList();
        }
        OAuth20Mediator mediator = null;
        OAuth20Client client = null;
        OAuth20RequestContext requestContext = new OAuth20RequestContext();
        try {
            // get mediator
            mediator = OAuth20MediatorFactory.getMediator(this);

            String clientId = authenticatedClient;
            client = getOAuth20Client(requestContext, clientId, null, null, true);

            // JWT token put redirect_uri in attributeList
            String grantType = isAppPasswordRequest ? OAuth20Constants.APP_PASSWORD : OAuth20Constants.APP_TOKEN;
            attributeList = buildTokenAttributeList(grantType, client, request, attributeList);
            addRequestHeaderToAttributeList(request, attributeList); // add proxy host if present.

            getAccessTokenFromHttpHeader(request, attributeList);
            /*
             * Determine the grant type handler to use to process the request
             * In this case the grant type is going to be fixed, "app-token".
             */
            OAuth20GrantTypeHandler gth = _grantTypeHandlerFactory.getHandler(_parent.getInstanceId(), grantType, this.get20Configuration());

            List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();
            List<String> tokenKeys = gth.getKeysGrantType(attributeList);

            /*
             * get the list of token(s) needed by this request from the cache
             */
            if (tokenKeys != null) {
                ListIterator<String> keyIter = tokenKeys.listIterator();
                while (keyIter.hasNext()) {
                    String key = keyIter.next();
                    OAuth20Token token = getOAuth20Token(requestContext, key, OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT, grantType, true);
                    tokens.add(token);
                }
            }

            // examine the request and add a bunch of stuff to the attributeList
            populateFromRequestForOpenIDConnect(attributeList, request);

            // validate the request (probably not needed because TokenExchange already did it)
            gth.validateRequestGrantType(attributeList, tokens);

            // build any necessary tokens to return
            OAuth20TokenFactory tokenFactory = new OAuth20TokenFactory(this);
            newTokens = gth.buildTokensGrantType(attributeList, tokenFactory, tokens);

            // remove the old token(s) from the cache
            ListIterator<OAuth20Token> tokenIter = tokens.listIterator();
            while (tokenIter.hasNext()) {
                OAuth20Token token = tokenIter.next();
                _tokenCache.remove(token.getId());
            }

            gth.buildResponseGrantType(attributeList, newTokens);

            // now invoke the mediator in case it has any work to do
            mediator.mediateToken(attributeList);

            // now write back the response as JSON
            String data = buildResponseDataString(attributeList, true, false); // true means will do Json encode later
            // TODO: FIXME
            // if (finestLoggable) {
            // _log.logp(Level.FINEST, CLASS, methodName, "Response attributes: " + data);
            // }

            response.setHeader(OAuth20Constants.HEADER_CACHE_CONTROL, OAuth20Constants.HEADERVAL_CACHE_CONTROL);
            response.setHeader(OAuth20Constants.HEADER_PRAGMA, OAuth20Constants.HEADERVAL_PRAGMA);
            response.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE, OAuth20Constants.HTTP_CONTENT_TYPE_JSON);

            String json = OAuth20Util.JSONEncode(data);

            AuditManager auditManager = new AuditManager();
            auditManager.setAgent(json);

            try {
                PrintWriter pw = response.getWriter();
                pw.write(json);
                pw.flush();
            } catch (IOException e) {
                String[] objs = new String[] { clientId, e.getMessage() };
                throw new OAuth20InternalException("security.oauth20.error.token.internal.exception", e, objs);
            }

            /*
             * Success!!
             */
            result = new OAuthResultImpl(OAuthResult.STATUS_OK, attributeList);
            errorOccurred = false;
        } catch (OAuthException e) {
            result = processException(attributeList, mediator, e);
        } catch (Exception e) {
            String[] objs = new String[] { request.getParameter(OAuth20Constants.CLIENT_ID), e.getMessage() };
            OAuthException oauthException = new OAuth20InternalException("security.oauth20.error.token.internal.exception", (e.getCause() == null) ? e : e.getCause(), objs);
            result = processException(attributeList, mediator, oauthException);
        } finally {
            // Remove any tokens that were created if there was a failure in processing request
            if (errorOccurred && newTokens != null) {
                ListIterator<OAuth20Token> tokenIter = newTokens.listIterator();
                while (tokenIter.hasNext()) {
                    OAuth20Token token = tokenIter.next();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "processTokenRequest: Found token to remove from cash.");
                        Tr.debug(tc, "Type: " + token.getType());
                        Tr.debug(tc, "Subtype: " + token.getSubType());
                        Tr.debug(tc, "ClientId: " + token.getClientId());
                        Tr.debug(tc, "Username: " + token.getUsername());
                    }
                    _tokenCache.remove(token.getId());
                }
            }

        }
        return result;

    }

    void getAccessTokenFromHttpHeader(HttpServletRequest request, AttributeList attributeList) throws OAuth20DuplicateParameterException, OAuth20BadParameterFormatException {
        // if access token came in as a request param, we need to ignore it,
        // we're supposed to read it off of the access_token http header
        if (attributeList.getAttributeValueByNameAndType(OAuth20Constants.ACCESS_TOKEN, OAuth20Constants.ATTRTYPE_PARAM_OAUTH) != null) {
            attributeList.setAttribute(OAuth20Constants.ACCESS_TOKEN, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, new String[] {});
        }
        addAccessTokenHeaderToAttributeList(request, attributeList);

    }

    /**
     * @param client
     * @param attributeList
     */
    void populateJwtAccessTokenData(OAuth20Client client, AttributeList attributeList) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "populateJwtAccessTokenData client:" + client);
        }
        String clientSecret = client.getClientSecret();
        attributeList.setAttribute(OAuth20Constants.CLIENT_SECRET,
                OAuth20Constants.ATTRTYPE_PARAM_OAUTH,
                new String[] { clientSecret });

        String[] resource = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
        if (resource == null && client instanceof OidcOAuth20Client) {
            JsonArray jsonAudiences = ((OidcOAuth20Client) client).getResourceIds();
            if (jsonAudiences != null) {
                String[] audiences = new String[jsonAudiences.size()];
                int iCnt = 0;
                for (JsonElement jsonAudience : jsonAudiences) {
                    audiences[iCnt] = jsonAudience.getAsString();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "audience:" + audiences[iCnt]);
                    }
                    iCnt++;
                }
                attributeList.setAttribute(OAuth20Constants.RESOURCE,
                        OAuth20Constants.ATTRTYPE_PARAM_OAUTH,
                        audiences);
            }
        }
    }

    private OAuthResult processException(AttributeList attributeList, OAuth20Mediator mediator, OAuthException e) {
        OAuthResult result;
        try {
            if (mediator != null) {
                mediator.mediateTokenException(attributeList, e);
            }
            result = new OAuthResultImpl(OAuthResult.STATUS_FAILED, attributeList, e);
        } catch (OAuth20MediatorException me) {
            // mediator developer SHOULD include the OAuthException in
            // OAuth20MediatorException
            result = new OAuthResultImpl(OAuthResult.STATUS_FAILED, attributeList, me);
        }
        return result;
    }

    @Override
    public OAuthResult processResourceRequest(HttpServletRequest request) {
        String methodName = "processResourceRequest(HttpServletRequest)";

        _log.entering(CLASS, methodName);
        OAuthResult result = null;
        AttributeList attributeList = new AttributeList();
        OAuth20RequestContext requestContext = new OAuth20RequestContext();
        try {
            /*
             * Normalize the request parameters into an attribute list
             */
            attributeList = buildResourceAttributeList(request);

            if (request.getRequestURI().contains("/" + OAuth20Constants.APP_PASSWORD_URI) ||
                    request.getRequestURI().contains("/" + OAuth20Constants.APP_TOKEN_URI)) {
                getAccessTokenFromHttpHeader(request, attributeList);
            }

            /*
             * Use our shared code to do the rest
             */
            result = processResourceRequestInternal(requestContext,
                    attributeList);
        } catch (OAuthException e) {
            result = new OAuthResultImpl(OAuthResult.STATUS_FAILED,
                    attributeList, e);
        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    @Override
    public OAuthResult processResourceRequest(AttributeList attributeList) {
        String methodName = "processResourceRequest(AttributeList)";

        _log.entering(CLASS, methodName);
        OAuthResult result = null;
        OAuth20RequestContext requestContext = new OAuth20RequestContext();
        try {
            result = processResourceRequestInternal(requestContext,
                    attributeList);
        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    OAuthResult processResourceRequestInternal(
            OAuth20RequestContext requestContext, AttributeList attributeList) {
        String methodName = "processResourceRequestInternal";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        _log.entering(CLASS, methodName, new Object[] { attributeList });
        OAuthResult result = null;
        OAuth20Mediator mediator = null;
        try {
            mediator = OAuth20MediatorFactory.getMediator(this);
            if (attributeList != null) {
                /*
                 * Instantiate a TokenTypeHandler for this request
                 */
                OAuth20TokenTypeHandler tth = OAuth20TokenTypeHandlerFactory
                        .getHandler(this);
                String tokenType = tth.getTypeTokenType();

                /*
                 * Get the list of keys (then tokens) needed for the token type
                 * handler to validate the access token
                 */
                List<String> tokenKeys = tth.getKeysTokenType(attributeList);
                List<OAuth20Token> tokens = new ArrayList<OAuth20Token>();

                if (tokenKeys != null) {
                    ListIterator<String> keyIter = tokenKeys.listIterator();
                    while (keyIter.hasNext()) {
                        String key = keyIter.next();
                        // Check whether the token is opaque or jwt type
                        // change the lookup based on the token type
                        String tokenLookupStr = key;
                        if (OidcOAuth20Util.isJwtToken(key)) {
                            tokenLookupStr = com.ibm.ws.security.oauth20.util.HashUtils.digest(key);
                        }
                        OAuth20Token token = getOAuth20Token(requestContext,
                                tokenLookupStr, OAuth20Constants.TOKENTYPE_ACCESS_TOKEN,
                                tokenType, false);
                        tokens.add(token);
                    }
                }

                // validate the request
                tth.validateRequestTokenType(attributeList, tokens);

                // if all went well, set the response decision to TRUE
                attributeList.setAttribute(OAuth20Constants.AUTHORIZED,
                        OAuth20Constants.ATTRTYPE_RESPONSE_DECISION,
                        new String[] { "TRUE" });

                // and build the rest of the response
                tth.buildResponseTokenType(attributeList, tokens);

                // now invoke the mediator in case it has any work to do

                mediator.mediateResource(attributeList);

                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "Response attributes: " + attributeList);
                }

                /*
                 * Success!! Filter out the attributes we want returned to the
                 * caller and return them.
                 */
                AttributeList enforcementPointAttributeList = new AttributeList();
                Attribute[] responseAttributes = attributeList
                        .getAttributesByType(OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE);
                Attribute[] decisionAttributes = attributeList
                        .getAttributesByType(OAuth20Constants.ATTRTYPE_RESPONSE_DECISION);
                addResponseAttributes(enforcementPointAttributeList,
                        responseAttributes);
                addResponseAttributes(enforcementPointAttributeList,
                        decisionAttributes);
                result = new OAuthResultImpl(OAuthResult.STATUS_OK,
                        enforcementPointAttributeList);
            } else {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.ACCESS_TOKEN, null);
            }
        } catch (OAuthException e) {
            try {
                if (mediator != null) {
                    mediator.mediateResourceException(attributeList, e);
                }
                result = new OAuthResultImpl(OAuthResult.STATUS_FAILED,
                        attributeList, e);
            } catch (OAuth20MediatorException me) {
                // mediator developer SHOULD include the OAuthException in
                // OAuth20MediatorException
                result = new OAuthResultImpl(OAuthResult.STATUS_FAILED,
                        attributeList, me);
            }

        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    @Override
    public OidcOAuth20ClientProvider getClientProvider() {
        return _clientProvider;
    }

    @Override
    public OAuth20TokenCache getTokenCache() {
        return _tokenCache;
    }

    @Override
    public OAuthStatisticsImpl getStatisticsImpl() {
        return _stats;
    }

    /**
     * Uses the client provider to retrieve and validate that the OAuth20Client
     * exists and is enabled. If a secret or redirectUri are provided, they are
     * validated.
     *
     * @param clientId
     * @param redirectUri
     * @return
     */
    OAuth20Client getOAuth20Client(OAuth20RequestContext requestContext,
            String clientId, String clientSecret, String redirectUri,
            boolean isTokenRequest) throws OAuthException {
        String methodName = "getOAuth20Client";
        _log.entering(CLASS, methodName, new Object[] { clientId, clientSecret,
                redirectUri, isTokenRequest });
        OAuth20Client result = null;
        try {
            // make sure the client ID parameter was passed
            if (clientId == null || clientId.length() == 0) {
                throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter",
                        OAuth20Constants.CLIENT_ID, null);
            }

            // try request context cache first, otherwise external provider
            result = requestContext.getRequestClientCache().get(clientId);
            if (result == null) {
                result = _clientProvider.get(clientId);
            }

            // validate the client ID
            if (result == null || !clientId.equals(result.getClientId())
                    || !result.isEnabled()) {
                throw new OAuth20InvalidClientException("security.oauth20.error.invalid.client", clientId,
                        isTokenRequest);
            }

            // validate the client secret if present
            if (clientSecret != null && clientSecret.length() > 0) {
                if (!_clientProvider.validateClient(clientId, clientSecret)) {
                    throw new OAuth20InvalidClientSecretException("security.oauth20.error.invalid.clientsecret", clientId);
                }
            }

            JsonArray registered = result.getRedirectUris();

            /*
             * if the client has a registered redirect URI, make sure it is
             * valid, it's OK if it doesn't have one, we can just display the
             * response to the screen
             */

            boolean allowRegexpRedirects = result.getAllowRegexpRedirects();

            if (registered != null && registered.size() > 0
                    && !OidcOAuth20Util.validateRedirectUris(registered, allowRegexpRedirects)) {
                throw new OAuth20InvalidRedirectUriException("security.oauth20.error.invalid.registered.redirecturi",
                        OidcOAuth20Util.getSpaceDelimitedString(registered), null);
            }

            /*
             * Validate the redirect URI if present. If it's not present that's
             * OK as we will use the registered URI or display to screen. If it
             * is present it must match or be a child of the registered URI.
             */

            if (redirectUri != null && redirectUri.length() > 0) {
                if (registered == null || !OAuth20Util.validateRedirectUri(redirectUri)) {
                    throw new OAuth20InvalidRedirectUriException("security.oauth20.error.invalid.redirecturi", redirectUri, null);
                } else if (!OidcOAuth20Util.jsonArrayContainsString(registered, redirectUri, allowRegexpRedirects)) {
                    throw new OAuth20InvalidRedirectUriException("security.oauth20.error.invalid.redirecturi.mismatch",
                            redirectUri, OidcOAuth20Util.getSpaceDelimitedString(registered), null);
                }
            }

            // cache it in request context for better performance
            if (result != null) {
                requestContext.getRequestClientCache().put(clientId, result);
            }

        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    AttributeList buildAuthorizationAttributeList(String username,
            String clientId, String redirectUri, String responseType,
            String state, String[] authorizedScopes) throws OAuthException {
        String methodName = "buildAuthorizationAttributeList";
        _log.entering(CLASS, methodName);
        AttributeList result = new AttributeList();
        try {
            /*
             * Identify that we are the processAuthorization flow
             */
            result
                    .setAttribute(
                            OAuth20Constants.REQUEST_TYPE,
                            OAuth20Constants.ATTRTYPE_REQUEST,
                            new String[] { OAuth20Constants.REQUEST_TYPE_AUTHORIZATION });

            /*
             * Add attributes provided from the API caller
             */
            OAuth20Util.validateRequiredAttribute(OAuth20Constants.USERNAME,
                    username);
            addParameterToAttributeList(OAuth20Constants.USERNAME,
                    OAuth20Constants.ATTRTYPE_REQUEST, username, result);

            OAuth20Util.validateRequiredAttribute(OAuth20Constants.CLIENT_ID,
                    clientId);
            addParameterToAttributeList(OAuth20Constants.CLIENT_ID,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY, clientId, result);

            if (redirectUri != null && redirectUri.length() > 0) {
                addParameterToAttributeList(OAuth20Constants.REDIRECT_URI,
                        OAuth20Constants.ATTRTYPE_PARAM_QUERY, redirectUri,
                        result);
            }

            OAuth20Util.validateRequiredAttribute(
                    OAuth20Constants.RESPONSE_TYPE, responseType);
            addParameterToAttributeList(OAuth20Constants.RESPONSE_TYPE,
                    OAuth20Constants.ATTRTYPE_PARAM_QUERY, responseType, result);

            if (state != null && state.length() > 0) {
                addParameterToAttributeList(OAuth20Constants.STATE,
                        OAuth20Constants.ATTRTYPE_PARAM_QUERY, state, result);
            }
            if (authorizedScopes != null && authorizedScopes.length > 0) {
                Set<String> scopeSet = new HashSet<String>();
                for (int i = 0; i < authorizedScopes.length; i++) {
                    String scope = authorizedScopes[i];
                    if (scopeSet.add(scope)) {
                        addParameterToAttributeList(OAuth20Constants.SCOPE,
                                OAuth20Constants.ATTRTYPE_REQUEST, scope,
                                result);
                    }
                }
            }
        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    AttributeList buildTokenAttributeList(String grantType,
            OAuth20Client client,
            HttpServletRequest request,
            AttributeList attribList) throws OAuthException {
        String methodName = "buildTokenAttributeList";
        _log.entering(CLASS, methodName);
        // handle resource(jwt audiences)
        String[] resource = (String[]) request.getAttribute(OAuth20Constants.OAUTH20_AUTHEN_PARAM_RESOURCE); // audiences
        if (resource != null) {
            attribList.setAttribute(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, resource);
        }
        String clientId = client.getClientId();
        populateJwtAccessTokenData(client, attribList);
        String[] clientRedirectUris = getRedirectUris(client);
        try {
            /*
             * Identify that we are the processToken flow
             */
            attribList.setAttribute(OAuth20Constants.REQUEST_TYPE,
                    OAuth20Constants.ATTRTYPE_REQUEST,
                    new String[] { OAuth20Constants.REQUEST_TYPE_ACCESS_TOKEN });

            /*
             * Add the validated client_id, regardless of how the client
             * actually authenticated this will be ok.
             */
            attribList.setAttribute(OAuth20Constants.CLIENT_ID,
                    OAuth20Constants.ATTRTYPE_PARAM_OAUTH,
                    new String[] { clientId });

            /*
             * Add the validated client_id and its redirect uri
             */
            attribList.setAttribute(OIDCConstants.CLIENT_REDIRECT_URI,
                    OAuth20Constants.ATTRTYPE_PARAM_OAUTH,
                    clientRedirectUris);
            /*
             * Add attributes provided from the request.
             */
            populateFromQueryString(request, attribList);
            populateFromRequest(request, attribList);
            overrideUserName(request, attribList);

            if (OAuth20Constants.GRANT_TYPE_JWT.equalsIgnoreCase(grantType)) {
                String client_secret = request.getParameter(OAuth20Constants.CLIENT_SECRET);
                if (client_secret == null || client_secret.length() == 0) {
                    client_secret = client.getClientSecret();
                    addParameterToAttributeList(OAuth20Constants.CLIENT_SECRET,
                            OAuth20Constants.ATTRTYPE_RESPONSE_META,
                            client_secret,
                            attribList);
                }
            }

        } finally {
            _log.exiting(CLASS, methodName, attribList);
        }
        return attribList;
    }

    /*
     * If ClientAuthentication.validateResourceOwnerCredentials determined that the
     * security name did not match what was supplied on the query parameter
     * (probably due to some fancy ldap configuration)
     * then switch it here to the security name.
     */
    void overrideUserName(HttpServletRequest request, AttributeList attribList) {
        String override = (String) request.getAttribute(OAuth20Constants.RESOURCE_OWNER_OVERRIDDEN_USERNAME);
        if (override != null) {
            String name = attribList.getAttributeValueByName(OAuth20Constants.RESOURCE_OWNER_USERNAME);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "changing token attribute:" + OAuth20Constants.RESOURCE_OWNER_USERNAME + " from: " + name + " to: " + override);
            }
            attribList.setAttribute(OAuth20Constants.RESOURCE_OWNER_USERNAME, OAuth20Constants.ATTRTYPE_PARAM_BODY, new String[] { override });
        }
    }

    AttributeList buildResourceAttributeList(HttpServletRequest request)
            throws OAuthException {
        String methodName = "buildResourceAttributeList";
        _log.entering(CLASS, methodName);
        AttributeList result = new AttributeList();
        try {
            /*
             * Identify that we are the processResource flow
             */
            result.setAttribute(OAuth20Constants.REQUEST_TYPE,
                    OAuth20Constants.ATTRTYPE_REQUEST,
                    new String[] { OAuth20Constants.REQUEST_TYPE_RESOURCE });

            /*
             * Add attributes provided from the request.
             */
            populateRequestMetaAttributes(request, result);
            populateFromAuthorizationHeader(request, result);
            populateFromQueryString(request, result);
            populateFromRequest(request, result);
        } finally {
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    private String buildRedirectUri(String redirectUri, String data, boolean implicit) {
        String methodName = "buildRedirectUri";
        _log.entering(CLASS, methodName);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "redirectUri:'" + redirectUri + "' data:'" + data + "' implicit:" + implicit);
        }
        String result = null;

        try {
            // split the query and fragment out
            String redirect = OAuth20Util.stripQueryAndFragment(redirectUri);
            String query = OAuth20Util.getQuery(redirectUri);

            StringBuffer redirectBuffer = new StringBuffer();
            redirectBuffer.append(redirect);

            StringBuffer queryBuffer = new StringBuffer();
            if (query != null) {
                queryBuffer.append(query);
            }

            StringBuffer fragmentBuffer = new StringBuffer();

            // build the query and fragment to return
            if (implicit) {
                fragmentBuffer.append(data.toString());
            } else {
                if (queryBuffer.length() > 0) {
                    queryBuffer.append("&");
                }
                queryBuffer.append(data.toString());
            }

            if (queryBuffer.length() > 0) {
                redirectBuffer.append("?" + queryBuffer.toString());
            }
            if (fragmentBuffer.length() > 0) {
                redirectBuffer.append("#" + fragmentBuffer.toString());
            }

            result = redirectBuffer.toString();
        } finally {
            _log.exiting(CLASS, methodName, result);
        }

        return result;
    }

    private OAuth20Token getOAuth20Token(OAuth20RequestContext requestContext,
            String key, String tokenType, String subType, boolean isTokenRequest)
            throws OAuthException {
        String methodName = "getOAuth20Token";
        _log.entering(CLASS, methodName,
                new Object[] { key, tokenType, subType });

        OAuth20Token result = null;
        boolean isAuthorizationGrantTypeAndCodeSubType = OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT.equals(tokenType) && OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.equals(subType);
        try {
            if (isAuthorizationGrantTypeAndCodeSubType && _tokenCache instanceof OAuth20EnhancedTokenCache) {

                String codekey = MessageDigestUtil.getDigest(key);
                result = ((OAuth20EnhancedTokenCache) _tokenCache).getByHash(codekey);
            } else {
                result = _tokenCache.get(key);
            }

            if (result == null || !tokenType.equalsIgnoreCase(result.getType())
                    || !subType.equalsIgnoreCase(result.getSubType())) {
                throw new OAuth20InvalidTokenException("security.oauth20.error.invalid.token", key, tokenType, subType,
                        isTokenRequest);
            }

            // check if the token has expired
            if (OAuth20TokenHelper.isTokenExpired(result)) {
                _tokenCache.remove(key);
                throw new OAuth20InvalidTokenException("security.oauth20.error.invalid.token.expired", key, tokenType, subType,
                        isTokenRequest);
            }

            /*
             * If the token is associated with a valid client and that client is
             * disabled, delete the token. We first look in our request cache
             * for the client but if it's not there we'll get it from the
             * external client provider. If we go to the provider, we will put
             * it in the request context cache if we find a client. Be aware
             * that no checks will be done on other runtime properties for the
             * client if this is the first place it is cached. Our
             * processTokenRequest method is structured to check the client
             * parameters before calling getOAuth20Token so this is ok.
             */
            String clientId = result.getClientId();
            OAuth20Client client = requestContext.getRequestClientCache().get(
                    clientId);
            if (client == null) {
                client = _clientProvider.get(result.getClientId());
                if (client != null) {
                    requestContext.getRequestClientCache()
                            .put(clientId, client);
                }

            }
            if (client == null || !client.isEnabled()) {
                _tokenCache.remove(key);
                throw new OAuth20InvalidTokenException("security.oauth20.error.invalid.token.client.not.available", key, tokenType, subType,
                        isTokenRequest);
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }

        return result;
    }

    private void populateRequestMetaAttributes(HttpServletRequest request,
            AttributeList attributeList) throws OAuthException {
        String methodName = "populateRequestMetaAttributes";
        _log.entering(CLASS, methodName);
        try {
            String hostname = request.getServerName();
            String method = request.getMethod();
            String path = request.getRequestURI();
            String scheme = request.getScheme();
            int port = request.getLocalPort();

            addParameterToAttributeList(OAuth20Constants.HOST,
                    OAuth20Constants.ATTRTYPE_REQUEST, hostname, attributeList);
            addParameterToAttributeList(OAuth20Constants.METHOD,
                    OAuth20Constants.ATTRTYPE_REQUEST, method, attributeList);
            addParameterToAttributeList(OAuth20Constants.PATH,
                    OAuth20Constants.ATTRTYPE_REQUEST, path, attributeList);
            addParameterToAttributeList(OAuth20Constants.SCHEME,
                    OAuth20Constants.ATTRTYPE_REQUEST, scheme, attributeList);
            if (port > 0
                    && (!(port == 80 && scheme != null && scheme
                            .equalsIgnoreCase("http")) || !(port == 443
                                    && scheme != null && scheme
                                            .equalsIgnoreCase("https")))) {
                addParameterToAttributeList(OAuth20Constants.PORT,
                        OAuth20Constants.ATTRTYPE_REQUEST, "" + port,
                        attributeList);
            }

        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * Read all OAuth attributes from the authorization header of the request
     * and add them to the attributeList with the type:
     * <code>urn:ibm:names:oauth:param</code>
     *
     * @param request
     * @param attributeList
     */
    private void populateFromAuthorizationHeader(HttpServletRequest request,
            AttributeList attributeList) throws OAuthException {
        String methodName = "populateFromAuthorizationHeader";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            String aznHeader = request
                    .getHeader(OAuth20Constants.HTTP_HEADER_AUTHORIZATION);

            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName,
                        "Authorization header (length="
                                + (aznHeader == null ? -1 : aznHeader.length())
                                + "): " + aznHeader);
            }

            if (aznHeader != null && aznHeader.length() > 0) {
                /*
                 * Check if the Authorization header matches our Bearer header
                 * regular expression. We only need to look once since our
                 * pattern matches beginning and end.
                 */
                String token = WebUtils.getBearerTokenFromAuthzHeader(aznHeader);
                if (token != null) {
                    if (finestLoggable) {
                        _log.logp(Level.FINEST, CLASS, methodName,
                                "Found bearer token: " + token);
                    }
                    addParameterToAttributeList(OAuth20Constants.ACCESS_TOKEN,
                            OAuth20Constants.ATTRTYPE_PARAM_OAUTH, token,
                            attributeList);
                } else {
                    if (finestLoggable) {
                        _log
                                .logp(Level.FINEST, CLASS, methodName,
                                        "Authorization header does not appear to contain Bearer token data");
                    }
                }
            } else {
                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "No Authorization header");
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * Read all attributes from the query string of the request and add them to
     * the attributeList with the type:
     * <code>urn:ibm:names:oauth:query:param</code>
     *
     * @param request
     * @param attributeList
     */
    private void populateFromQueryString(HttpServletRequest request,
            AttributeList attributeList) throws OAuthException {
        String methodName = "populateFromQueryString";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            String queryString = request.getQueryString();

            if (queryString != null) {
                String[] params = queryString.split("&");
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        // Split name=value on first equals
                        String[] nameval = params[i].split("=", 2);
                        if (nameval != null
                                && (nameval.length == 1 || nameval.length == 2)) {
                            String name = URLDecoder.decode(nameval[0],
                                    OAuth20Constants.UTF8);

                            /*
                             * Empty or no values are ok - we store them with
                             * the empty string as the value
                             */
                            String value = "";
                            if (nameval.length == 2) {
                                value = URLDecoder.decode(nameval[1],
                                        OAuth20Constants.UTF8);
                            }

                            /*
                             * Use query param type except for access token
                             */
                            String attrType = OAuth20Constants.ATTRTYPE_PARAM_QUERY;
                            if (name != null
                                    && name
                                            .equalsIgnoreCase(OAuth20Constants.ACCESS_TOKEN)) {
                                attrType = OAuth20Constants.ATTRTYPE_PARAM_OAUTH;
                            }

                            /*
                             * Add the parameter to our list
                             */
                            addParameterToAttributeList(name, attrType, value,
                                    attributeList);

                            if (finestLoggable) {
                                _log.logp(Level.FINEST, CLASS, methodName, "["
                                        + name + "=" + value);
                            }

                        } else {
                            if (finestLoggable) {
                                _log.logp(Level.FINEST, CLASS, methodName,
                                        "Ignoring parameter string with no value: "
                                                + params[i]);
                            }
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new OAuth20InternalException("security.oauth20.error.internal.unsupported.encoding.exception", e);
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * Add a name/value pair parameter to a map, checking for duplicate OAuth
     * parameters
     *
     * @param name
     * @param type
     * @param value
     * @param attributeList
     * @throws OAuth20DuplicateParameterException
     * @throws OAuth20BadParameterFormatException
     */
    private void addParameterToAttributeList(String name, String type, String value, AttributeList attributeList)
            throws OAuth20DuplicateParameterException,
            OAuth20BadParameterFormatException {
        String methodName = "addParameterToAttributeList";
        if ("client_secret".equals(name)) {
            _log.entering(CLASS, methodName, new Object[] { name, type, "secret_removed" });
        } else {
            _log.entering(CLASS, methodName, new Object[] { name, type, value });
        }

        try {
            Attribute a = attributeList.getAttributeByNameAndType(name, type);

            if (a == null) {
                // establish the array list and add it to the map
                attributeList.setAttribute(name, type, new String[] {});
                a = attributeList.getAttributeByNameAndType(name, type);
            }

            List<String> values = a.getValues();

            /*
             * scope parameter is specially defined in the OAuth 2.0 spec, it is
             * supposed to be a space delimited string, so if we received it, we
             * parse the space delimited string and add each value separately
             */
            if (name.equals(OAuth20Constants.SCOPE)) {
                if (value != null) {
                    String[] scopes = value.split(" ");
                    if (scopes != null) {
                        Set<String> scopeSet = new HashSet<String>();
                        for (int i = 0; i < scopes.length; i++) {
                            String scope = scopes[i].trim();
                            if (scope != null && scope.length() > 0) {
                                /*
                                 * Perform special validation of characters in a
                                 * scope.
                                 */
                                if (OAuth20Util.validateScopeString(scope)) {
                                    scopeSet.add(scope);
                                } else {
                                    throw new OAuth20BadParameterFormatException("security.oauth20.error.parameter.format", OAuth20Constants.SCOPE, scope);
                                }
                            }
                        }
                        values.addAll(scopeSet);
                    }
                }
            }

            /*
             * OAuth 2.0 protocol request parameters cannot occur more than once
             * except for scope
             */
            else if (OAuth20Constants.OAuth20RequestParametersSet.contains(name) && values.size() > 0) {
                throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", name);
            }

            /*
             * any other parameter is added normally. The attribute is already
             * in the attributeList, so just add the values
             */
            else {
                if (value != null && value.length() > 0) {
                    values.add(value);
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    /**
     * Populate the attribute list with parameters found in the post body of a
     * request.
     *
     * @param request
     * @param attributeList
     * @throws OAuthException
     */
    private void populateFromRequest(HttpServletRequest request, AttributeList attributeList) throws OAuthException {
        String methodName = "populateFromRequest";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        try {
            /*
             * Check the content-type is application/x-www-form-urlencoded and
             * that the transfer encoding is NOT chunked. If those conditions
             * are met we'll read parameters from the post body
             */
            if (containsFormData(request)) {
                Enumeration<String> e = request.getParameterNames();
                while (e.hasMoreElements()) {
                    String name = e.nextElement();
                    String[] values = request.getParameterValues(name);

                    /*
                     * Use body param type except for access token
                     */
                    String attrType = OAuth20Constants.ATTRTYPE_PARAM_BODY;
                    if (name != null && name.equalsIgnoreCase(OAuth20Constants.ACCESS_TOKEN)) {
                        attrType = OAuth20Constants.ATTRTYPE_PARAM_OAUTH;
                    }

                    /*
                     * get the existing parameters with the same name that were
                     * in the query string
                     */
                    String[] existingVals = attributeList.getAttributeValuesByNameAndType(name, OAuth20Constants.ATTRTYPE_PARAM_QUERY);

                    // make a copy of the list of existing parameter values
                    ArrayList<String> valueList = new ArrayList<String>();
                    if (existingVals != null) {
                        valueList.addAll(Arrays.asList(existingVals));
                    }

                    if (values != null) {
                        for (int j = 0; j < values.length; j++) {
                            String value = values[j];

                            /*
                             * if this value is actually a query param, remove
                             * it from the copied list, and move on
                             */
                            if (valueList.contains(value)) {
                                valueList.remove(value);
                            }
                            /*
                             * otherwise, this is actually a post body
                             * parameter, so we will add it to the map as such
                             */
                            else {
                                addParameterToAttributeList(name, attrType, value, attributeList);
                                if (finestLoggable) {
                                    if ("client_secret".equals(name)) {
                                        _log.logp(Level.FINEST, CLASS, methodName, name + "=" + "secret_removed");
                                    } else {
                                        _log.logp(Level.FINEST, CLASS, methodName, name + "=" + value);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    private void addRequestHeaderToAttributeList(HttpServletRequest request, AttributeList attributeList) throws OAuth20DuplicateParameterException, OAuth20BadParameterFormatException {
        String methodName = "addRequestHeaderToAttributeList";
        _log.entering(CLASS, methodName);
        String hostname = null;
        if (request != null) {
            hostname = request.getHeader(OAuth20Constants.PROXY_HOST);
        }
        if (hostname != null && !hostname.isEmpty()) {
            addParameterToAttributeList(OAuth20Constants.PROXY_HOST, OAuth20Constants.ATTRTYPE_PARAM_HEADER, hostname, attributeList);
        }
        _log.exiting(CLASS, methodName);
    }

    private void addAccessTokenHeaderToAttributeList(HttpServletRequest request, AttributeList attributeList) throws OAuth20DuplicateParameterException, OAuth20BadParameterFormatException {
        String methodName = "addAccessTokenHeaderToAttributeList";
        _log.entering(CLASS, methodName);
        String token = null;
        if (request != null) {
            token = request.getHeader(OAuth20Constants.ACCESS_TOKEN);
        }
        if (token != null && !token.isEmpty()) { // add as param since that's how we used to process it.
            addParameterToAttributeList(OAuth20Constants.ACCESS_TOKEN, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, token, attributeList);
        }
        _log.exiting(CLASS, methodName);
    }

    private void populateFromRequestForOpenIDConnect(AttributeList attributeList, HttpServletRequest request) throws OAuth20DuplicateParameterException, OAuth20BadParameterFormatException {
        String methodName = "populateFromRequestForOpenIDConnect";
        _log.entering(CLASS, methodName);
        String hostname = request.getServerName();
        String scheme = request.getScheme();
        int port = request.getLocalPort();
        String path = request.getRequestURI();
        int lastSlashIndex = path.lastIndexOf("/");
        String issuerIdentifier = scheme + "://" + hostname + ":" + port + path.substring(0, lastSlashIndex);
        addParameterToAttributeList(OAuth20Constants.ISSUER_IDENTIFIER, OAuth20Constants.ATTRTYPE_REQUEST, issuerIdentifier, attributeList);
        if (request.getAttribute(OAuth20Constants.OIDC_REQUEST_OBJECT_ATTR_NAME) != null) {
            addParameterToAttributeList(OAuth20Constants.REQUEST_FEATURE, OAuth20Constants.ATTRTYPE_REQUEST, OAuth20Constants.REQUEST_FEATURE_OIDC, attributeList);
        }
        _log.exiting(CLASS, methodName);
    }

    private boolean containsFormData(HttpServletRequest request) {
        String methodName = "containsFormData";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        boolean result = false;

        try {
            String contentType = request
                    .getHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE);
            String transferEncoding = request
                    .getHeader(OAuth20Constants.HTTP_HEADER_TRANSFER_ENCODING);
            boolean isChunked = (transferEncoding != null && transferEncoding
                    .toLowerCase().indexOf("chunked") >= 0);

            if (contentType != null
                    && contentType.toLowerCase().indexOf(
                            OAuth20Constants.HTTP_CONTENT_TYPE_FORM) >= 0
                    && !isChunked) {
                result = true;

                if (finestLoggable) {
                    _log
                            .logp(Level.FINEST, CLASS, methodName,
                                    "Content type is application/x-www-form-urlencoded and request is not chunked");
                }
            }
        } finally {
            _log.exiting(CLASS, methodName, "" + result);
        }

        return result;
    }

    private String buildResponseDataString(AttributeList attributeList, boolean bJsonLater, boolean bImplicit) {
        StringBuffer data = new StringBuffer();

        Attribute[] attributes = attributeList
                .getAttributesByType(OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        // for each attribute
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                String name = attributes[i].getName();
                List<String> values = attributes[i].getValues();

                /*
                 * If it's the scope attribute, values must be space separated,
                 * but for all other attributes we'll include them as separate
                 * name=value entries
                 */
                if (name.equals(OAuth20Constants.SCOPE)) {
                    if (bJsonLater || bImplicit) {
                        if (data.length() > 0)
                            data.append("&"); // already has parameter in it.
                        data.append(name + "=");
                        if (values.size() > 0) {
                            for (int j = 0; j < values.size(); j++) {
                                String value = values.get(j);
                                if (!bJsonLater) {
                                    value = encode(value, STATE_ENCODING);
                                }
                                data.append(value);

                                // if this is not the last scope, add a space
                                if (j < (values.size() - 1)) {
                                    data.append(bJsonLater ? " " : "%20"); // %20 is the url encode of " "
                                }
                            }
                        }
                    }
                } else {
                    // for each value in the attribute
                    for (int j = 0; j < values.size(); j++) {
                        String value = values.get(j);
                        if (!bJsonLater) {
                            value = encode(value, STATE_ENCODING);
                        }
                        if (data.length() > 0)
                            data.append("&"); // already has parameter in it.
                        data.append(name + "=" + value);

                        // if this is not the last value, add an ampersand
                        if (j < values.size() - 1) {
                            data.append("&");
                        }
                    }
                }

            }
        }

        return data.toString();
    }

    /**
     * @param value
     * @param stateEncoding
     * @return
     */
    protected String encode(String value, String stateEncoding) {
        try {
            value = URLEncoder.encode(value, stateEncoding);
        } catch (UnsupportedEncodingException e) {
            _log.logp(Level.WARNING, CLASS, "encode", "Can NOT URLEncode value:" + value + " encoding:" + stateEncoding);
        }
        return value;
    }

    private void addResponseAttributes(AttributeList attributeList,
            Attribute[] attrs) {
        if (attrs != null) {
            for (int i = 0; i < attrs.length; i++) {
                List<String> vals = attrs[i].getValues();
                String[] valsArray = new String[vals.size()];
                for (int j = 0; j < vals.size(); j++) {
                    valsArray[j] = vals.get(j);
                }
                attributeList.setAttribute(attrs[i].getName(), attrs[i]
                        .getType(), valsArray);
            }
        }
    }

    private String[] getRedirectUris(OAuth20Client client) {
        String[] redirectUris = null;
        JsonArray redirectUriArray = client.getRedirectUris();

        if (redirectUriArray == null) {
            redirectUris = new String[0]; // return an empty array
        } else {
            redirectUris = new String[redirectUriArray.size()];
            for (int iI = 0; iI < redirectUris.length; iI++) {
                redirectUris[iI] = redirectUriArray.get(iI).getAsString();
            }
        }
        return redirectUris;
    }

    /**
     *
     * @param response
     * @param attributeList
     * @param responseRedirectURI
     * @param implicit
     * @throws OAuth20InternalException
     */
    public void postRedirect(HttpServletResponse response,
            AttributeList attributeList,
            String redirectUri) throws OAuth20InternalException {
        /**
         * Example of post redirect:
         *
         * HTTP/1.1 200 OK
         * Content-Type: text/html;charset=UTF-8
         * Cache-Control: no-cache, no-store
         * Pragma: no-cache
         *
         * <html>
         * <head><title>Submit This Form</title></head>
         * <body onload="javascript:document.forms[0].submit()">
         * <form method="post" action="https://client.example.org/callback">
         * <input type="hidden" name="state"
         * value="DcP7csa3hMlvybERqcieLHrRzKBra"/>
         * <input type="hidden" name="id_token"
         * value="eyJhbGciOiJSUzI1NiIsImtpZCI6IjEifQ.eyJzdWIiOiJqb2huIiw
         * iYXVkIjoiZmZzMiIsImp0aSI6ImhwQUI3RDBNbEo0c2YzVFR2cllxUkIiLC
         * Jpc3MiOiJodHRwczpcL1wvbG9jYWxob3N0OjkwMzEiLCJpYXQiOjEzNjM5M
         * DMxMTMsImV4cCI6MTM2MzkwMzcxMywibm9uY2UiOiIyVDFBZ2FlUlRHVE1B
         * SnllRE1OOUlKYmdpVUciLCJhY3IiOiJ1cm46b2FzaXM6bmFtZXM6dGM6U0F
         * NTDoyLjA6YWM6Y2xhc3NlczpQYXNzd29yZCIsImF1dGhfdGltZSI6MTM2Mz
         * kwMDg5NH0.c9emvFayy-YJnO0kxUNQqeAoYu7sjlyulRSNrru1ySZs2qwqq
         * wwq-Qk7LFd3iGYeUWrfjZkmyXeKKs_OtZ2tI2QQqJpcfrpAuiNuEHII-_fk
         * IufbGNT_rfHUcY3tGGKxcvZO9uvgKgX9Vs1v04UaCOUfxRjSVlumE6fWGcq
         * XVEKhtPadj1elk3r4zkoNt9vjUQt9NGdm1OvaZ2ONprCErBbXf1eJb4NW_h
         * nrQ5IKXuNsQ1g9ccT5DMtZSwgDFwsHMDWMPFGax5Lw6ogjwJ4AQDrhzNCFc
         * 0uVAwBBb772-86HpAkGWAKOK-wTC6ErRTcESRdNRe0iKb47XRXaoz5acA"/>
         * </form>
         * </body>
         * </html>
         */
        StringBuffer sb = new StringBuffer();
        try {
            // split the query and fragment out
            String redirect = OAuth20Util.stripQueryAndFragment(redirectUri);
            String query = OAuth20Util.getQuery(redirectUri);

            sb.append("<HTML xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
            sb.append("<HEAD><title>Submit This Form</title></HEAD>");
            sb.append("<BODY onload=\"javascript:document.forms[0].submit()\">");
            sb.append("<FORM name=\"redirectform\" id=\"redirectform\" action=\"");
            sb.append(redirect);
            sb.append("\" method=\"" + HTTP_METHOD_POST + "\">");

            // add the query string from the redirectUri first
            addHiddenInputs(sb, query);

            // add values from attributeList
            addHiddenInputs(sb, attributeList);

            sb.append("<button type=\"submit\" name=\"redirectform\">Process request</button>");
            sb.append("</FORM></BODY></HTML>");
        } catch (Exception e) {
            // This should not happen
            throw new OAuth20InternalException(e); // Let OAuth20InternalException handle the unexpected Exception
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "... expect to be redirected by the browser (\"" + HTTP_METHOD_POST + "\")\n" +
                    sb.toString());
        }

        // HTTP 1.1.
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        // HTTP 1.0.
        response.setHeader("Pragma", "no-cache");
        // Proxies.
        response.setDateHeader("Expires", 0);
        response.setContentType("text/html");
        try {
            PrintWriter out = response.getWriter();
            out.println(sb.toString());
            out.flush();
        } catch (IOException e) {
            // Error handling , in case
            String[] objs = new String[] { redirectUri, e.getMessage() };
            throw new OAuth20InternalException("security.oauth20.error.authorization.internal.ioexception", e, objs);
        }
    }

    void addHiddenInputs(StringBuffer sb, AttributeList attributeList) {
        Attribute[] attributes = attributeList
                .getAttributesByType(OAuth20Constants.ATTRTYPE_RESPONSE_ATTRIBUTE);

        // for each attribute
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                String key = attributes[i].getName();
                List<String> values = attributes[i].getValues();

                /*
                 * If it's the scope attribute, values must be space separated,
                 * but for all other attributes we'll include them as separate
                 * name=value entries
                 */
                if (key.equals(OAuth20Constants.SCOPE)) {
                    StringBuffer scopeSb = new StringBuffer();
                    if (values.size() > 0) {
                        for (int j = 0; j < values.size();) {
                            String value = values.get(j++);
                            scopeSb.append(value);

                            // if this is not the last scope, add a space
                            if (j < values.size()) {
                                scopeSb.append(" ");
                            }
                        }
                    }
                    addHiddenInput(sb, key, com.ibm.ws.security.oauth20.web.WebUtils.htmlEncode(scopeSb.toString()));
                } else {
                    // for each value in the attribute
                    for (int j = 0; j < values.size(); j++) {
                        String value = values.get(j);
                        addHiddenInput(sb, key, com.ibm.ws.security.oauth20.web.WebUtils.htmlEncode(value));
                    }
                }
            }
        }
    }

    /**
     * @param sb
     * @param query
     */
    void addHiddenInputs(StringBuffer sb, String query) {
        if (query == null || query.isEmpty())
            return;
        String[] entries = query.split("&");
        for (String entry : entries) {
            int index = entry.indexOf("=");
            if (index < 0) {
                addHiddenInput(sb, entry, "");
            } else {
                String key = entry.substring(0, index);
                String value = entry.substring(index + 1);
                addHiddenInput(sb, key, com.ibm.ws.security.oauth20.web.WebUtils.htmlEncode(value));
            }
        }
    }

    /**
     * @param sb
     * @param key
     * @param value
     */
    private void addHiddenInput(StringBuffer sb, String key, String value) {
        key = key.trim();
        value = value.trim();
        sb.append("<input type=\"hidden\" name=\"" + key + "\"");
        sb.append(" value=\"" + value + "\" />");
    }

    /**
     *
     * @param response
     * @param attributeList
     * @param responseRedirectURI
     * @param implicit
     * @throws OAuth20InternalException
     */
    void sendRedirect(HttpServletResponse response,
            AttributeList attributeList,
            String responseRedirectURI,
            boolean implicit) throws OAuth20InternalException {
        String methodName = "sendRedirect";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        /*
         * build the response data parameters
         */
        String data = buildResponseDataString(attributeList, false, implicit); // false means no Json encode later

        /*
         * build the redirect URI
         */
        String redirect = buildRedirectUri(responseRedirectURI, data.toString(), implicit);

        /*
         * Send it with some recommended headers
         */
        if (finestLoggable) {
            _log.logp(Level.FINEST, CLASS, methodName, "_SSO OP redirecting to [" + redirect + "]");
        }
        response.setHeader(OAuth20Constants.HEADER_CACHE_CONTROL, OAuth20Constants.HEADERVAL_CACHE_CONTROL);
        response.setHeader(OAuth20Constants.HEADER_PRAGMA, OAuth20Constants.HEADERVAL_PRAGMA);
        try {
            response.sendRedirect(redirect);
        } catch (IOException e) {
            String[] objs = new String[] { responseRedirectURI, e.getMessage() };
            throw new OAuth20InternalException("security.oauth20.error.authorization.internal.ioexception", e, objs);
        }
    }

}
