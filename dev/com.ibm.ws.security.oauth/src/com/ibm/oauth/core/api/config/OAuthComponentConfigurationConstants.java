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
package com.ibm.oauth.core.api.config;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

/**
 * This interface outlines all the configuration properties required by the core
 * component.
 * 
 * @see OAuthComponentConfiguration
 * @see SampleComponentConfiguration
 */
public interface OAuthComponentConfigurationConstants {

    /*
     * Common configuration constants for the core
     */

    /*
     * Configuration properties for OAuth 2.0
     */

    /**
     * Defines the implementation class for the client provider which must
     * implement the
     * {@link com.ibm.oauth.core.api.oauth20.client.OAuth20ClientProvider}
     * interface. This configuration property MUST be defined and should be a
     * single string represents the class name of the implementation class for
     * your client configuration provider. The
     * {@link SampleComponentConfiguration} class does <b>not</b> contain a
     * default value for this property. You can see an example of it being set
     * in the examples source in
     * <code>com.ibm.oauth.examples.config.OAuthComponentConfigurationTestImpl</code>
     * 
     */
    public static final String OAUTH20_CLIENT_PROVIDER_CLASSNAME = "oauth20.client.provider.classname";

    /**
     * Defines the implementation class for the token cache which must implement
     * the {@link com.ibm.oauth.core.api.oauth20.token.OAuth20TokenCache}
     * interface. This configuration property MUST be defined and should be a
     * single string represents the class name of the implementation class for
     * your token cache. The {@link SampleComponentConfiguration} class does
     * <b>not</b> contain a default value for this property. You can see an
     * example of it being set in the examples source in
     * <code>com.ibm.oauth.examples.config.OAuthComponentConfigurationTestImpl</code>
     * 
     */
    public static final String OAUTH20_TOKEN_CACHE_CLASSNAME = "oauth20.token.cache.classname";

    /**
     * Defines the maximum authorization grant lifetime for OAuth 2.0 flows in
     * which a refresh token is used. The value will determine the maximum
     * possible lifetime of any issued refresh or access token starting from
     * when the resource owner first grants authorization (i.e. when the first
     * refresh/access token is issued from either the authorization code flow or
     * the resource owner password credentials flow. This property is not
     * relevant for flows where no refresh token is used since the
     * {@link #OAUTH20_TOKEN_LIFETIME_SECONDS} property will determine the
     * lifetime of access tokens. This configuration property MUST be defined
     * and should be a single string representing the lifetime in seconds. The
     * value should be greater than the value for
     * {@link #OAUTH20_TOKEN_LIFETIME_SECONDS}. The
     * {@link SampleComponentConfiguration} class contains a default value for
     * this property.
     * 
     */
    public static final String OAUTH20_MAX_AUTHORIZATION_GRANT_LIFETIME_SECONDS = "oauth20.max.authorization.grant.lifetime.seconds";

    /**
     * Defines the lifetime of an authorization code. The value will determine
     * the lifetime of an issued authorization code for the authorization code
     * flow. The client must exchange the authorization code for an access token
     * before this lifetime expires. As authorization codes are typically sent
     * via browser redirect to the client which then immediately uses them, this
     * value is normally a small number of seconds. This property is not
     * relevant for flows other than the authorization code flow. This
     * configuration property MUST be defined and should be a single string
     * representing the lifetime in seconds. The
     * {@link SampleComponentConfiguration} class contains a default value for
     * this property.
     * 
     */
    public static final String OAUTH20_CODE_LIFETIME_SECONDS = "oauth20.code.lifetime.seconds";

    /**
     * Defines the length in characters of a generated authorization code. This
     * configuration property MUST be defined and should be a single string
     * representing the length. The {@link SampleComponentConfiguration} class
     * contains a default value for this property.
     * 
     */
    public static final String OAUTH20_CODE_LENGTH = "oauth20.code.length";

    /**
     * Defines the maximum lifetime of an issued access token. The value will
     * determine the maximum lifetime of an issued access token. The actual
     * lifetime of the access token may be smaller than this if the access token
     * is being issued as part of a refresh token flow and the remaining time
     * associated with maximum authorization grant lifetime is less than this
     * value. This configuration property MUST be defined and should be a single
     * string representing the lifetime in seconds. The
     * {@link SampleComponentConfiguration} class contains a default value for
     * this property.
     * 
     */
    public static final String OAUTH20_TOKEN_LIFETIME_SECONDS = "oauth20.token.lifetime.seconds";

    /**
     * Defines the length in characters of a generated access token. This
     * configuration property MUST be defined and should be a single string
     * representing the length. The {@link SampleComponentConfiguration} class
     * contains a default value for this property.
     * 
     */
    public static final String OAUTH20_ACCESS_TOKEN_LENGTH = "oauth20.access.token.length";

    /**
     * Defines a boolean to indicate whether or not refresh tokens should be
     * issued. This property is only relevant for flows which issue refresh
     * tokens (authorization code and resource owner password credentials). This
     * configuration property MUST be defined and should be a single string
     * representing "true" or "false". The {@link SampleComponentConfiguration}
     * class contains a default value for this property.
     * 
     */
    public static final String OAUTH20_ISSUE_REFRESH_TOKEN = "oauth20.issue.refresh.token";

    /**
     * Defines the length in characters of a generated refresh token. This
     * configuration property MUST be defined and should be a single string
     * representing the length. The {@link SampleComponentConfiguration} class
     * contains a default value for this property.
     * 
     */
    public static final String OAUTH20_REFRESH_TOKEN_LENGTH = "oauth20.refresh.token.length";

    /**
     * Defines the implementation class for an access token issuer. Currently
     * only one internal implementation is supported and this property should
     * always be set to the value of
     * {@link SampleComponentConfiguration#ACCESS_TOKENTYPEHANDLER_CLASSNAME}.
     * This configuration property MUST be defined and set as indicated. The
     * {@link SampleComponentConfiguration} class contains a default value for
     * this property.
     * 
     */
    public static final String OAUTH20_ACCESS_TOKENTYPEHANDLER_CLASSNAME = "oauth20.access.tokentypehandler.classname";

    /**
     * Defines a list of implementation classes for custom mediators that
     * implement the
     * {@link com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator} interface
     * to execute during OAuth flows. This configuration property is optional
     * and when provided should be a list of string class names.
     * 
     */
    public static final String OAUTH20_MEDIATOR_CLASSNAMES = "oauth20.mediator.classnames";

    /**
     * Defines a boolean to indicate whether or not public clients may access
     * the token endpoint. This property is only relevant for the authorization
     * code flow and the resource owner password credentials flow (although it
     * doesn't make a lot of sense to use pulic clients in the resource owner
     * password credentials flow). The token endpoint is not used in the
     * implicit grant flow and despite the fact that all implicit grant clients
     * are behaving as public clients this parameter does <b>not</b> affect
     * whether or not a client may use the implicit grant flow. The ability for
     * any client to use the implicit grant flow is solely controlled by the
     * configuration property {@link #OAUTH20_GRANT_TYPES_ALLOWED}. The
     * client_credentials flow may not be used by a public client. This
     * configuration property MUST be defined and should be a single string
     * representing "true" or "false". The {@link SampleComponentConfiguration}
     * class contains a default value for this property.
     * 
     */
    public static final String OAUTH20_ALLOW_PUBLIC_CLIENTS = "oauth20.allow.public.clients";

    /**
     * Defines an implementation class for an audit handler that implements the
     * {@link com.ibm.oauth.core.api.audit.OAuthAuditHandler} interface to be
     * called during OAuth flows. This configuration property is optional and
     * when provided should be string class names. The component supports these
     * two pre-defined audit handlers, or you can write your own:
     * <ul>
     * <li>com.ibm.oauth.core.api.audit.SimpleFileOAuthAuditHandler -
     * {@link com.ibm.oauth.core.api.audit.SimpleFileOAuthAuditHandler}</li>
     * <li>com.ibm.oauth.core.api.audit.XMLFileOAuthAuditHandler -
     * {@link com.ibm.oauth.core.api.audit.XMLFileOAuthAuditHandler}</li>
     * </ul>
     * 
     */
    public static final String OAUTH20_AUDITHANDLER_CLASSNAME = "oauth20.audithandler.classname";

    /**
     * Defines a list of the flows which may be used by clients for this
     * component instance. This selectively allows you to enable any or all of
     * the different flow types by including one or more of these values for the
     * property:
     * <ul>
     * <li>{@link #OAUTH20_GRANT_TYPE_AUTH_CODE}</li>
     * <li>{@link #OAUTH20_GRANT_TYPE_IMPLICIT}</li>
     * <li>{@link #OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS}</li>
     * <li>{@link #OAUTH20_GRANT_TYPE_OWNER_PASSWORD}</li>
     * <li>{@link #OAUTH20_GRANT_TYPE_REFRESH_TOKEN}</li>
     * </ul>
     * 
     */
    public static final String OAUTH20_GRANT_TYPES_ALLOWED = "oauth20.grant.types.allowed";

    /**
     * Constant to be used as a value for the
     * {@link #OAUTH20_GRANT_TYPES_ALLOWED} property to enable clients to use
     * the authorization code flow.
     */
    public static final String OAUTH20_GRANT_TYPE_AUTH_CODE = OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE;

    /**
     * Constant to be used as a value for the
     * {@link #OAUTH20_GRANT_TYPES_ALLOWED} property to enable clients to use
     * the implicit grant flow.
     */
    public static final String OAUTH20_GRANT_TYPE_IMPLICIT = OAuth20Constants.GRANT_TYPE_IMPLICIT;

    /**
     * Constant to be used as a value for the
     * {@link #OAUTH20_GRANT_TYPES_ALLOWED} property to enable clients to use
     * the client credentials flow.
     */
    public static final String OAUTH20_GRANT_TYPE_CLIENT_CREDENTIALS = OAuth20Constants.GRANT_TYPE_CLIENT_CREDENTIALS;

    /**
     * Constant to be used as a value for the
     * {@link #OAUTH20_GRANT_TYPES_ALLOWED} property to enable clients to use
     * the resource owner password credentials flow.
     */
    public static final String OAUTH20_GRANT_TYPE_OWNER_PASSWORD = OAuth20Constants.GRANT_TYPE_PASSWORD;

    /**
     * Constant to be used as a value for the
     * {@link #OAUTH20_GRANT_TYPES_ALLOWED} property to enable clients to the
     * token endpoint to exchange refresh tokens for a new access token and
     * refresh token.
     */
    public static final String OAUTH20_GRANT_TYPE_REFRESH_TOKEN = OAuth20Constants.GRANT_TYPE_REFRESH_TOKEN;
}
