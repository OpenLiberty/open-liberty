/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.audit.OAuthAuditHandler;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache;
import com.ibm.oauth.core.internal.config.OAuthConfigurationImpl;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentImpl;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.mediator.OAuth20MediatorWrapper;
import com.ibm.oauth.core.internal.oauth20.mediator.impl.OAuth20MediatorDefaultImpl;
import com.ibm.oauth.core.internal.oauth20.mediator.impl.OAuthAuditHandlerMediator;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenCacheWrapper;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcOAuth20ClientProviderWrapper;

/**
 * Validates the OAuthComponentConfiguration values for an OAuth 2.0 environment
 * and reformats the parameters into convenient OAuth20ConfigProvider getter
 * methods.
 */
public class OAuth20ConfigurationImpl extends OAuthConfigurationImpl implements OAuth20ConfigValidator, OAuth20ConfigProvider {

    final static String CLASS = OAuth20ConfigurationImpl.class.getName();
    final static Logger _log = Logger.getLogger(CLASS);

    /**
     * Defines the implementation class for an id token issuer. Currently
     * only one internal implementation is supported
     */
    public static final String OAUTH20_ID_TOKENTYPEHANDLER_CLASSNAME = "oauth20.id.tokentypehandler.classname";

    /**
     * Defines the implementation class for an OAuth20GrantTypeHandlerFactory
     * only one internal implementation is supported
     */
    public static final String OAUTH20_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME = "oauth20.grant.type.handler.factory.classname";

    /**
     * Defines the implementation class for an OAuth20ResponseTypeHandlerFactory
     * only one internal implementation is supported
     */
    public static final String OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME = "oauth20.response.type.handler.factory.classname";

    // Setup variables
    protected OAuth20ComponentImpl _compimpl;
    protected boolean _validated;

    // Class variables
    protected OidcOAuth20ClientProvider _clientProvider;
    protected OAuth20TokenCache _tokenCache;
    protected int _maxAuthGrantLifetimeSeconds;
    protected int _codeLifetimeSeconds;
    protected int _codeLength;
    protected int _tokenLifetimeSeconds;
    protected int _accessTokenLength;
    protected boolean _issueRefreshToken;
    protected int _refreshTokenLength;
    protected long _refreshedAccessTokenLimit = 100;
    protected OAuth20TokenTypeHandler _tokenTypeHandler;
    protected OAuth20TokenTypeHandler _idTokenTypeHandler; // oidc10
    protected OAuth20GrantTypeHandlerFactory _grantTypeHandlerFactory; // oidc10
    protected OAuth20ResponseTypeHandlerFactory _responseTypeHandlerFactory; // oidc10
    protected OAuth20Mediator _mediators;
    protected boolean _allowPublicClients;
    protected HashSet<String> _allowedGrantTypes;
    protected OAuthAuditHandler _auditHandler;

    public OAuth20ConfigurationImpl(OAuth20ComponentImpl compIn, OAuthComponentConfiguration configIn) {
        super(configIn);
        _compimpl = compIn;
        _validated = false;
    }

    @Override
    public OAuth20ConfigProvider getConfigProvider() throws OAuthException {
        if (!_validated) {
            validate();
        }
        return this;
    }

    @Override
    public void validate() throws OAuthException {
        // validate classes, check instantiation
        processClientProvider();
        processTokenCache();
        processTokenTypeHandler();
        processIDTokenTypeHandler(); // oidc10
        processGrantTypeHandlerFactory(); // oidc10
        processResponseTypeHandlerFactory(); // oidc10
        processAuditHandler();
        processMediators();

        // extract allowed grant types
        processGrantTypes();

        // validate numbers, check not negative
        _maxAuthGrantLifetimeSeconds = validateNonNegativeInt(OAuthComponentConfigurationConstants.OAUTH20_MAX_AUTHORIZATION_GRANT_LIFETIME_SECONDS);
        _codeLifetimeSeconds = validateNonNegativeInt(OAuthComponentConfigurationConstants.OAUTH20_CODE_LIFETIME_SECONDS);
        _codeLength = validateNonNegativeInt(OAuthComponentConfigurationConstants.OAUTH20_CODE_LENGTH);
        _tokenLifetimeSeconds = validateNonNegativeInt(OAuthComponentConfigurationConstants.OAUTH20_TOKEN_LIFETIME_SECONDS);
        _accessTokenLength = validateNonNegativeInt(OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKEN_LENGTH);
        _refreshTokenLength = validateNonNegativeInt(OAuthComponentConfigurationConstants.OAUTH20_REFRESH_TOKEN_LENGTH);

        // validate booleans, just validate values are returned
        _issueRefreshToken = validateBoolean(OAuthComponentConfigurationConstants.OAUTH20_ISSUE_REFRESH_TOKEN);
        _allowPublicClients = validateBoolean(OAuthComponentConfigurationConstants.OAUTH20_ALLOW_PUBLIC_CLIENTS);

        // cleanup
        _compimpl = null;
        _oldconfig = null;
        _validated = true;

    }

    protected void processClientProvider() throws OAuthException {
        String className = _oldconfig.getConfigPropertyValue(OAuthComponentConfigurationConstants.OAUTH20_CLIENT_PROVIDER_CLASSNAME);
        Object newClass = processClass(className,
                OAuthComponentConfigurationConstants.OAUTH20_CLIENT_PROVIDER_CLASSNAME,
                OidcOAuth20ClientProvider.class);
        OidcOAuth20ClientProvider realClientProvider = (OidcOAuth20ClientProvider) newClass;
        OidcOAuth20ClientProviderWrapper clientProviderWrapper = new OidcOAuth20ClientProviderWrapper(realClientProvider, _compimpl.getOAuthStatisticsImpl());
        clientProviderWrapper.init(_oldconfig);
        _clientProvider = clientProviderWrapper;
    }

    protected void processTokenCache() throws OAuthException {
        String className = _oldconfig.getConfigPropertyValue(OAuthComponentConfigurationConstants.OAUTH20_TOKEN_CACHE_CLASSNAME);
        Object newClass = processClass(className,
                OAuthComponentConfigurationConstants.OAUTH20_TOKEN_CACHE_CLASSNAME,
                OAuth20TokenCache.class);
        OAuth20TokenCache realTokenCache = (OAuth20TokenCache) newClass;
        OAuth20TokenCacheWrapper tokenCacheWrapper = new OAuth20TokenCacheWrapper(realTokenCache, _compimpl.getOAuthStatisticsImpl());
        tokenCacheWrapper.init(_oldconfig);
        _tokenCache = tokenCacheWrapper;
    }

    protected void processTokenTypeHandler() throws OAuthException {
        String className = _oldconfig.getConfigPropertyValue(OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME);
        Object newClass = processClass(className,
                OAuthComponentConfigurationConstants.OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME,
                OAuth20TokenTypeHandler.class);
        _tokenTypeHandler = (OAuth20TokenTypeHandler) newClass;
        _tokenTypeHandler.init(_oldconfig);
    }

    // oidc10
    protected void processIDTokenTypeHandler() throws OAuthException {
        String className = _oldconfig.getConfigPropertyValue(OAUTH20_ID_TOKENTYPEHANDLER_CLASSNAME);
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, "processIDTokenTypeHandler", className);
        }
        if (className != null) {
            try {
                Object newClass = processClass(className,
                        OAUTH20_ID_TOKENTYPEHANDLER_CLASSNAME,
                        OAuth20TokenTypeHandler.class);
                _idTokenTypeHandler = (OAuth20TokenTypeHandler) newClass;
                _idTokenTypeHandler.init(_oldconfig);
            } catch (OAuthException e) {
                // Eat ClassNotFoundException caused when OpenID Connect is not there
                Throwable cause = e.getCause();
                if (cause instanceof ClassNotFoundException == false) {
                    throw e;
                }
            }
        }
    }

    // oidc10
    protected void processGrantTypeHandlerFactory() throws OAuthException {
        String className = _oldconfig.getConfigPropertyValue(OAUTH20_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME);
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, "processGrantTypeHandlerFactory", className);
        }
        if (className != null) {
            try {
                Object newClass = processClass(className,
                        OAUTH20_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME,
                        OAuth20GrantTypeHandlerFactory.class);
                _grantTypeHandlerFactory = (OAuth20GrantTypeHandlerFactory) newClass;
            } catch (OAuthException e) {
                // Eat ClassNotFoundException caused when OpenID Connect is not there
                Throwable cause = e.getCause();
                if (cause instanceof ClassNotFoundException == false) {
                    throw e;
                }
            }
        }
    }

    // oidc10
    protected void processResponseTypeHandlerFactory() throws OAuthException {
        String className = _oldconfig.getConfigPropertyValue(OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME);
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, "processResponseTypeHandlerFactory", className);
        }
        if (className != null) {
            try {
                Object newClass = processClass(className,
                        OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME,
                        OAuth20ResponseTypeHandlerFactory.class);
                _responseTypeHandlerFactory = (OAuth20ResponseTypeHandlerFactory) newClass;
                _responseTypeHandlerFactory.init(_oldconfig);
            } catch (OAuthException e) {
                // Eat ClassNotFoundException caused when OpenID Connect is not there
                Throwable cause = e.getCause();
                if (cause instanceof ClassNotFoundException == false) {
                    throw e;
                }
            }
        }
    }

    protected void processMediators() throws OAuthException {
        List<OAuth20Mediator> mediatorList = new ArrayList<OAuth20Mediator>();
        String[] mediatorClassNames = _oldconfig.getConfigPropertyValues(OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES);

        if (mediatorClassNames == null || mediatorClassNames.length == 0) {
            if (_log.isLoggable(Level.FINEST)) {
                _log.logp(Level.FINEST, CLASS, "processMediators",
                        "No mediator in configuration - using default mediator");
            }
            mediatorClassNames = new String[] { OAuth20MediatorDefaultImpl.class.getName() };
        }
        for (String mediatorClassName : mediatorClassNames) {
            Object newClass = processClass(mediatorClassName,
                    OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES,
                    OAuth20Mediator.class);
            mediatorList.add((OAuth20Mediator) newClass);
        }

        // add OAuthAuditHandlerMediator as pre-defined mediator to the end of list
        if (_auditHandler != null) {
            if (_log.isLoggable(Level.FINEST)) {
                _log.logp(Level.FINEST, CLASS, "processMediators",
                        "Audit handler defined and instantiated, adding OAuthAuditHandlerMediator to mediator chain");
            }
            OAuth20Mediator auditMediator = new OAuthAuditHandlerMediator(_auditHandler);
            mediatorList.add(auditMediator);
        }

        _mediators = new OAuth20MediatorWrapper(mediatorList, _compimpl.getStatisticsImpl());
        _mediators.init(_oldconfig);
    }

    protected void processGrantTypes() throws OAuthException {
        String[] allowedGrantTypeNames = _oldconfig.getConfigPropertyValues(OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED);
        if (allowedGrantTypeNames == null || allowedGrantTypeNames.length == 0) {
            // No grant types specified, invalid configuration
            throw new OAuthConfigurationException("security.oauth.error.config.notspecified.exception", OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, "", null);
        }

        _allowedGrantTypes = new HashSet<String>();
        for (String name : allowedGrantTypeNames) {
            if (!OAuth20Constants.ALL_GRANT_TYPES_SET.contains(name)) {
                // Unrecognized parameter for grant types
                throw new OAuthConfigurationException("security.oauth.error.invalidconfig.exception", OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, name, null);
            }
            _allowedGrantTypes.add(name);
        }
    }

    protected void processAuditHandler() throws OAuthException {
        Object instance = null;
        String auditHandlerClassName = _oldconfig.getConfigPropertyValue(OAuthComponentConfigurationConstants.OAUTH20_AUDITHANDLER_CLASSNAME);
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, "processAuditHandler",
                    "Audit handler class name: " + auditHandlerClassName);
        }
        if (auditHandlerClassName == null) {
            instance = null;
        } else {
            instance = processClass(auditHandlerClassName,
                    OAuthComponentConfigurationConstants.OAUTH20_AUDITHANDLER_CLASSNAME,
                    OAuthAuditHandler.class);
        }
        _auditHandler = (OAuthAuditHandler) instance;
        if (_log.isLoggable(Level.FINEST)) {
            _log.logp(Level.FINEST, CLASS, "processAuditHandler",
                    "Instantiated audit handler : " + _auditHandler);
        }
    }

    /*
     * Getters
     */

    @Override
    public int getAccessTokenLength() {
        return _accessTokenLength;
    }

    @Override
    public OidcOAuth20ClientProvider getClientProvider() {
        return _clientProvider;
    }

    @Override
    public int getCodeLength() {
        return _codeLength;
    }

    @Override
    public int getCodeLifetimeSeconds() {
        return _codeLifetimeSeconds;
    }

    @Override
    public int getMaxAuthGrantLifetimeSeconds() {
        return _maxAuthGrantLifetimeSeconds;
    }

    @Override
    public OAuth20Mediator getMediators() {
        return _mediators;
    }

    @Override
    public int getRefreshTokenLength() {
        return _refreshTokenLength;
    }

    @Override
    public long getRefreshedAccessTokenLimit() {
        return _refreshedAccessTokenLimit;
    }

    @Override
    public OAuth20TokenCache getTokenCache() {
        return _tokenCache;
    }

    @Override
    public int getTokenLifetimeSeconds() {
        return _tokenLifetimeSeconds;
    }

    @Override
    public OAuth20TokenTypeHandler getTokenTypeHandler() {
        return _tokenTypeHandler;
    }

    @Override
    public OAuth20TokenTypeHandler getIDTokenTypeHandler() {
        return _idTokenTypeHandler;
    }

    @Override
    public OAuth20GrantTypeHandlerFactory getGrantTypeHandlerFactory() {
        return _grantTypeHandlerFactory;
    }

    @Override
    public OAuth20ResponseTypeHandlerFactory getResponseTypeHandlerFactory() {
        return _responseTypeHandlerFactory;
    }

    @Override
    public boolean isAllowPublicClients() {
        return _allowPublicClients;
    }

    @Override
    public boolean isIssueRefreshToken() {
        return _issueRefreshToken;
    }

    @Override
    public boolean isGrantTypeAllowed(String grantType) {
        return _allowedGrantTypes.contains(grantType);
    }

    @Override
    public OAuthAuditHandler getAuditHandler() {
        return _auditHandler;
    }

}
