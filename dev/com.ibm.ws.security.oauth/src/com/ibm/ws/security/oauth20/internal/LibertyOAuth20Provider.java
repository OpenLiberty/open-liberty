/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.audit.OAuthAuditHandler;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.config.OAuthComponentConfigurationConstants;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.api.oauth20.OAuth20Component;
import com.ibm.oauth.core.api.oauth20.mediator.OAuth20Mediator;
import com.ibm.oauth.core.internal.oauth20.OAuth20ComponentImpl;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigurationImpl;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.mediator.impl.OAuth20MediatorDefaultImpl;
import com.ibm.oauth.core.internal.oauth20.responsetype.OAuth20ResponseTypeHandlerFactory;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OAuth20ProviderConfiguration;
import com.ibm.ws.security.oauth20.api.OauthConsentStore;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuthProviderException;
import com.ibm.ws.security.oauth20.filter.OAuthResourceProtectionFilter;
import com.ibm.ws.security.oauth20.impl.OAuth20ComponentConfigurationImpl;
import com.ibm.ws.security.oauth20.plugins.BaseCache;
import com.ibm.ws.security.oauth20.plugins.BaseTokenHandler;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientValidator;
import com.ibm.ws.security.oauth20.plugins.custom.OauthClientStore;
import com.ibm.ws.security.oauth20.plugins.custom.OauthConsentStoreImpl;
import com.ibm.ws.security.oauth20.plugins.custom.OauthTokenStore;
import com.ibm.ws.security.oauth20.plugins.db.CachedDBOidcClientProvider;
import com.ibm.ws.security.oauth20.plugins.db.CachedDBOidcTokenStore;
import com.ibm.ws.security.oauth20.plugins.db.DBConsentCache;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OAuth20Parameter;
import com.ibm.ws.security.oauth20.util.OAuth20ProviderUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServer;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.security.oauth20.internal.config.OAuthEndpointSettings;

@Component(configurationPid = "com.ibm.ws.security.oauth20.provider", configurationPolicy = ConfigurationPolicy.REQUIRE, service = { OAuth20Provider.class, ConfigurationListener.class, ServerQuiesceListener.class }, immediate = false, property = { "service.vendor=IBM", "dataSourceFactory.target=(id=unbound)" })
public class LibertyOAuth20Provider implements OAuth20Provider, ConfigurationListener, ServerQuiesceListener {

    private static final TraceComponent tc = Tr.register(LibertyOAuth20Provider.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    // DS constants and fields
    private static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private volatile ConfigurationAdmin configAdmin = null;
    private static final String KEY_CLASSLOADING_SVC = "classLoadingSvc";
    private volatile ClassLoadingService classLoadingSvc = null;
    @SuppressWarnings("unused")
    private static final String KEY_OIDC_IDTOKEN_HANDLER = "IDTokenHandler";
    private static volatile OAuth20TokenTypeHandler oidcIDTokenHandler = null;
    @SuppressWarnings("unused")
    private static final String KEY_OIDC_GRANT_TYPE_HANDLER_FACTORY = "OAuth20GrantTypeHandlerFactory";
    private static volatile OAuth20GrantTypeHandlerFactory oidcGrantTypeHandlerFactory = null;
    @SuppressWarnings("unused")
    private static final String KEY_OIDC_RESPONSE_TYPE_HANDLER_FACTORY = "OAuth20ResponseTypeHandlerFactory";
    private static volatile OAuth20ResponseTypeHandlerFactory oidcResponseTypeHandlerFactory = null;
    private static final String KEY_OAUTH_SHARED_LIB = "sharedLib";
    private volatile Library sharedLib = null;
    private static final String KEY_OIDC_SERVER = "oidcServer";
    private static final String KEY_DATA_SOURCE_FACTORY = "dataSourceFactory";
    private ResourceFactory dataSourceFactory = null;
    private static final String KEY_RESOURCE_CONFIG_FACTORY = "resourceConfigFactory";
    private ResourceConfigFactory resourceConfigFactory = null;
    private static final String KEY_JDBC_DATASOURCEREF = "dataSourceRef";
    private static final String KEY_EXECUTOR_SVC = "executorService";

    // The oauthProvider's attributes sorted as they appear in metatype.xml
    protected static final String KEY_PROVIDER_ID = "id";
    protected static final String KEY_MAX_AUTHGRANT_LT_SECS = "authorizationGrantLifetime";
    protected static final String KEY_CODE_LT_SECS = "authorizationCodeLifetime";
    protected static final String KEY_CODE_LEN = "authorizationCodeLength";
    protected static final String KEY_TOK_LT_SECS = "accessTokenLifetime";
    protected static final String KEY_ACCESS_TOK_LEN = "accessTokenLength";
    protected static final String KEY_ISSUE_REFRESH_TOK = "issueRefreshToken";
    protected static final String KEY_REFRESH_TOK_LEN = "refreshTokenLength";
    protected static final String KEY_MED_CLASS_NAMES = "mediatorClassname";
    protected static final String KEY_ALLOW_PUBLIC_CLIENTS = "allowPublicClients";
    protected static final String KEY_GRANT_TYPE = "grantType";
    protected static final String KEY_AUTHZ_FORM_TEMP = "authorizationFormTemplate";
    protected static final String KEY_AUTHZ_ERR_TEMP = "authorizationErrorTemplate";
    protected static final String KEY_AUTHZ_LOGIN_URL = "customLoginURL";
    protected static final String KEY_AUTO_AUTHZ_PARAM = "autoAuthorizeParam";
    protected static final String KEY_AUTO_AUTHORIZE = "autoAuthorize";
    protected static final String KEY_AUTO_AUTHZ_CLIENT = "autoAuthorizeClient";
    protected static final String KEY_CL_URI_SUBS = "clientURISubstitutions";
    protected static final String KEY_TOK_USER_CLIENT_LIMIT = "userClientTokenLimit";
    // TODO: These 4 props are TAI parms in tWAS. Determine if these can be removed.
    protected static final String KEY_FILTER = "filter";
    protected static final String KEY_CHARACTER_ENCODING = "characterEncoding";
    protected static final String KEY_OUATH_ONLY = "oauthOnly";
    protected static final String KEY_INCLUDE_TOKEN = "includeTokenInSubject";
    // End of TAI parms
    protected static final String KEY_CONSENT_CACHE_ENTRY_LIFETIME = "consentCacheEntryLifetime";
    protected static final String KEY_CONSENT_CACHE_SIZE = "consentCacheSize";
    protected static final String KEY_HTTPS_REQUIRED = "httpsRequired";
    protected static final String KEY_CERT_AUTHENTICATION = "certAuthentication";
    protected static final String KEY_ALLOW_CERT_AUTHENTICATION = "allowCertAuthentication";
    protected static final String KEY_ALLOW_SPNEGO_AUTHENTICATION = "allowSpnegoAuthentication";
    protected static final String KEY_CLIENT_ADMIN = "clientAdmin";

    protected static final String KEY_JWT_MAX_JTI_CACHE_SIZE = "maxJtiCacheSize";
    protected static final String KEY_JWT_SKEW = "clockSkew";
    protected static final String KEY_JWT_TOKEN_MAX_LIFETIME = "tokenMaxLifetime";
    protected static final String KEY_JWT_IAT_REQUIRED = "iatRequired";
    protected static final String KEY_SKIP_USER_VALIDATION = "skipResourceOwnerValidation";

    protected static final String KEY_JWT_ACCESS_TOKEN = "jwtAccessToken";
    static final String KEY_STORE_ACCESSTOKEN_ENCODING = "accessTokenEncoding";
    protected static final String KEY_PASSWORD_GRANT_REQUIRES_APP_PASSWORD = "passwordGrantRequiresAppPassword";
    protected static final String KEY_APP_PASSWORD_LIFETIME = "appPasswordLifetime";
    protected static final String KEY_APP_TOKEN_LIFETIME = "appTokenLifetime";
    protected static final String KEY_APP_TOKEN_OR_PASSWORD_LIMIT = "appTokenOrPasswordLimit";
    protected static final String KEY_INTERNAL_CLIENT_ID = "internalClientId";
    protected static final String KEY_INTERNAL_CLIENT_SECRET = "internalClientSecret";
    protected static final String KEY_TOKEN_FORMAT = "tokenFormat";
    protected static final String KEY_logoutRedirectURL = "logoutRedirectURL";
    protected static final String KEY_CACHE_ACCESSTOKEN = "accessTokenCacheEnabled";
    protected static final String KEY_REVOKE_ACCESSTOK_W_REFRESHTOK = "revokeAccessTokensWithRefreshTokens";
    public static final String KEY_TRACK_OAUTH_CLIENTS = "trackOAuthClients";
    public static final String KEY_OAUTH_ENDPOINT = "oauthEndpoint";

    // TODO: Rational Jazz props. Determine if these can be move to OIDC config.
    protected static final String KEY_COVERAGE_MAP_SESSION_MAX_AGE = "coverageMapSessionMaxAge";
    // End of Rational Jazz props

    // TODO: Other attributes. Determine how to organize these
    static final String KEY_JDBC_TOK_TABLE = "tokenTable";
    static final String KEY_JDBC_CLEANUP_INT = "cleanupExpiredTokenInterval";
    static final String KEY_JDBC_LIM_REF_TOK = "limitRefreshToken";
    static final String KEY_JDBC_PASSWORD = "password";
    static final String KEY_JDBC_USER = "user";
    static final String KEY_JDBC_SCHEMA = "schema";

    static final String KEY_CUSTOM_CLEANUP_INT = "cleanupExpiredInterval";

    static final String KEY_TOK_STORE_SIZE = "tokenStoreSize";

    public static final String KEY_CLIENT_ID = "name";
    public static final String KEY_CLIENT_COMPONENT = "component";
    public static final String KEY_CLIENT_SECRET = "secret";
    public static final String KEY_CLIENT_DISPLAYNAME = "displayname";
    public static final String KEY_CLIENT_REDIRECT = "redirect";
    public static final String KEY_CLIENT_ENABLED = "enabled";
    public static final String KEY_CLIENT_TOKEN_EP_AUTH_METHOD = "tokenEndpointAuthMethod";
    public static final String KEY_CLIENT_SCOPE = "scope";
    public static final String KEY_CLIENT_GRANT_TYPES = "grantTypes";
    public static final String KEY_CLIENT_RESPONSE_TYPES = "responseTypes";
    public static final String KEY_CLIENT_APP_TYPE = "applicationType";
    public static final String KEY_CLIENT_SUBJECT_TYPE = "subjectType";
    public static final String KEY_CLIENT_POST_LOGOUT_REDIRECT_URIS = "postLogoutRedirectUris";
    public static final String KEY_CLIENT_PREAUTHORIZED_SCOPE = "preAuthorizedScope";
    public static final String KEY_CLIENT_INTROSPECT_TOKENS = "introspectTokens";
    public static final String KEY_CLIENT_TRUSTED_URI_PREFIXES = "trustedUriPrefixes";
    public static final String KEY_CLIENT_RESOURCE_IDS = "resourceIds";
    public static final String KEY_CLIENT_FUNCTIONAL_USER_ID = "functionalUserId";
    public static final String KEY_CLIENT_FUNCTIONAL_USER_GROUPIDS = "functionalUserGroupIds";
    public static final String KEY_CLIENT_allowRegexpRedirects = "allowRegexpRedirects";
    public static final String KEY_CLIENT_APP_PASSWORD_ALLOWED = "appPasswordAllowed";
    public static final String KEY_CLIENT_APP_TOKEN_ALLOWED = "appTokenAllowed";
    public static final String KEY_CLIENT_SECRET_ENCODING = "clientSecretEncoding";

    public static final String KEY_CLIENT_PROOF_KEY_FOR_CODE_EXCHANGE = "proofKeyForCodeExchange";
    public static final String KEY_CLIENT_PUBLIC_CLIENT = "publicClient";

    public static final String KEY_ROPC_PREFER_USERSECURITYNAME = "ropcPreferUserSecurityName";

    private volatile SecurityService securityService;

    // Stores
    private static final String VALUE_DB_CLIENT_TABLE = ".OAUTH20CLIENTCONFIG";
    private static final String VALUE_DB_TOKEN_TABLE = ".OAUTH20CACHE";
    private static final String CONSENT_CACHE_DB_TABLE = ".OAUTH20CONSENTCACHE";

    private Map<String, Object> properties;
    private OAuthResourceProtectionFilter resourceProtectionFilter;
    private ClassLoader pluginClassLoader;
    // For Tivoli's Core Classes
    private boolean needToCreateCoreClasses = true;
    private OAuth20Component component;
    private OAuth20ProviderConfiguration providerConfig;
    private OidcOAuth20ClientProvider clientProvider;
    private OAuth20EnhancedTokenCache tokenCache;
    private ExecutorService executorService;

    // Use locks instead of synchronize blocks to ensure concurrent access while reading and lock during modification only
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private final ReadLock readLock = reentrantReadWriteLock.readLock();

    private final CommonConfigUtils configUtils = new CommonConfigUtils();

    // Fields sorted by the metatype.xml attributes
    private String providerId;
    private long authorizationGrantLifetime;
    private long authorizationCodeLifetime;
    private int authorizationCodeLength;
    private long accessTokenLifetime;
    private int accessTokenLength;
    private boolean issueRefreshToken;
    private int refreshTokenLength;
    private String mediatorClassname;
    private boolean allowPublicClients;
    private String[] grantTypesAllowed;
    private String authorizationFormTemplate;
    private String authorizationErrorTemplate;
    private String customLoginURL;
    private String autoAuthorizeParam;
    private boolean autoAuthorize;
    private String[] autoAuthorizeClients;
    private String clientURISubstitutions;
    private long clientTokenCacheSize;
    private String filter;
    private String characterEncoding;
    private boolean oauthOnly;
    private boolean includeTokenInSubject;
    private long consentCacheEntryLifetime;
    private long consentCacheSize;
    private boolean httpsRequired;
    private boolean certAuthentication;
    private boolean allowCertAuthentication = true;
    private boolean allowSpnegoAuthentication = false;
    private String clientAdmin;
    private long jwtMaxJtiCacheSize;
    private long jwtSkew;
    private long jwtTokenMaxLifetime;
    private boolean jwtIatRequired;
    private boolean jwtAccessToken = false;
    private long coverageMapSessionMaxAge;
    private boolean skipUserValidation;
    private String accessTokenEncoding = OAuth20Constants.PLAIN_ENCODING;
    private String schemaName;
    private String logoutRedirectURL;
    private boolean passwordGrantRequiresAppPassword = false;
    private long appPasswordLifetime = 0;
    private long appTokenLifetime = 0;
    private long appTokenOrPasswordLimit = 0;
    private String internalClientId;
    @Sensitive
    private String internalClientSecret;
    private String clientSecretEncoding;

    // Store and custom related fields
    private boolean isCustomStore;
    private boolean isLocalStore;
    private boolean isDatabaseStore;
    private boolean checkForSharedLib;
    private boolean checkForDataSource;
    private final Set<String> pids = new HashSet<String>();
    private Long cleanupInterval;
    private Boolean limitRefreshToken;
    private String[] providerRewrites;
    private Object[] credentials;
    private Long tokenStoreSize;
    private boolean isValid = false;
    private boolean cacheAccessToken = true;

    private List<OidcBaseClient> clientsList = null;
    // TODO: Remove after getting rid of OAuth20Parameter list
    private ArrayList<OAuth20Parameter> parameters = null;

    private Set<String> finalGrantTypesAllowedSet;
    private OAuth20TokenTypeHandler tokenTypeHandler;
    private String idTokenTypeHandlerClassname;
    private OAuth20TokenTypeHandler idTokenTypeHandler;
    private String grantTypeHandlerFactoryClassname;
    private OAuth20GrantTypeHandlerFactory grantTypeHandlerFactory;
    private String responseTypeHandlerFactoryClassname;
    private OAuth20ResponseTypeHandlerFactory responseTypeHandlerFactory;
    private OAuthAuditHandler auditHandler;
    private OAuth20Mediator mediator;

    private OauthConsentStore consentCache;
    private byte[] authorizationFormTemplateContent = null;
    private boolean mpJwt = false; // micropfile format Jwt token
    private String tokenFormat;
    private boolean revokeAccessTokensWithRefreshTokens = true;
    private boolean ropcPreferUserSecurityName = false;
    private boolean trackOAuthClients = false;
    private OAuthEndpointSettings oauthEndpointSettings;

    // DS related methods

    Pattern patternOauthOidc = null;

    private String bundleLocation;
    static final String OIDC_CTX = "/oidc/";
    static final String OAUTH2_CTX = "/oauth2/";

    private final AtomicReference<OAuthStore> iOAuthStoreRef = new AtomicReference<OAuthStore>();

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        writeLock.lock();
        try {
            this.properties = properties;
            this.bundleLocation = cc.getBundleContext().getBundle().getLocation();
            setupFields();
            processProviderConfig();
            validateConfig();
            loadAuthFormTemplate(cc);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "activated provider: " + providerId);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * if the default authform is used, load it to memory, else leave it null.
     * @param cc
     */
    private void loadAuthFormTemplate(ComponentContext cc) {
        if (authorizationFormTemplate == null || authorizationFormTemplate.compareTo(OIDCConstants.DEFAULT_TEMPLATE_HTML) != 0) {
            authorizationFormTemplateContent = null;
            return;
        }
        try {
            if (cc.getBundleContext() == null || cc.getBundleContext().getBundle() == null) {
                return;
            }
            URL u = cc.getBundleContext().getBundle().getResource(OIDCConstants.DEFAULT_TEMPLATE_HTML);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (u == null) {
                return;
            }
            InputStream is = u.openConnection().getInputStream();
            byte[] buf = new byte[1024 * 4];
            int count = -1;
            while ((count = is.read(buf)) != -1) {
                baos.write(buf, 0, count);
            }
            is.close();
            baos.close();
            authorizationFormTemplateContent = baos.toByteArray();

        } catch (IOException e) {
            // emit ffdc
        }
    }

    private void setupFields() {
        providerId = (String) properties.get(KEY_PROVIDER_ID);
        authorizationGrantLifetime = (Long) properties.get(KEY_MAX_AUTHGRANT_LT_SECS);
        authorizationCodeLifetime = (Long) properties.get(KEY_CODE_LT_SECS);
        authorizationCodeLength = getProperty(KEY_CODE_LEN, 30, 2048);
        accessTokenLifetime = (Long) properties.get(KEY_TOK_LT_SECS);
        accessTokenLength = getProperty(KEY_ACCESS_TOK_LEN, 40, 2048);
        issueRefreshToken = (Boolean) properties.get(KEY_ISSUE_REFRESH_TOK);
        refreshTokenLength = getProperty(KEY_REFRESH_TOK_LEN, 50, 2048);
        // mediatorClassname
        allowPublicClients = (Boolean) properties.get(KEY_ALLOW_PUBLIC_CLIENTS);
        grantTypesAllowed = (String[]) properties.get(KEY_GRANT_TYPE); // TODO: Review naming since there are several of these grantTypeXX attributes
        authorizationFormTemplate = (String) properties.get(KEY_AUTHZ_FORM_TEMP);
        if (authorizationFormTemplate == null || authorizationFormTemplate.trim().isEmpty()) {
            authorizationFormTemplate = OIDCConstants.DEFAULT_TEMPLATE_HTML;
        }
        authorizationErrorTemplate = (String) properties.get(KEY_AUTHZ_ERR_TEMP);
        customLoginURL = (String) properties.get(KEY_AUTHZ_LOGIN_URL);
        if (customLoginURL == null || "".equals(customLoginURL.trim())) {
            customLoginURL = OAuth20Constants.DEFAULT_AUTHZ_LOGIN_URL;
        }
        handlePatterns();
        autoAuthorizeParam = (String) properties.get(KEY_AUTO_AUTHZ_PARAM);
        autoAuthorize = (Boolean) properties.get(KEY_AUTO_AUTHORIZE);
        autoAuthorizeClients = (String[]) properties.get(KEY_AUTO_AUTHZ_CLIENT);
        clientURISubstitutions = (String) properties.get(KEY_CL_URI_SUBS);
        clientTokenCacheSize = 0;
        if (properties.containsKey(KEY_TOK_USER_CLIENT_LIMIT)) {
            // TODO: Determine if this needs to be an int since the OAuth20EndpointServlet is comparing against
            // an integer value. We would have a cache defect if a value greater than the int max is specified.
            clientTokenCacheSize = (Long) properties.get(KEY_TOK_USER_CLIENT_LIMIT);
        }
        filter = (String) properties.get(KEY_FILTER);
        setResourceProtectionFilter(filter);
        characterEncoding = (String) properties.get(KEY_CHARACTER_ENCODING);
        oauthOnly = (Boolean) properties.get(KEY_OUATH_ONLY);
        includeTokenInSubject = (Boolean) properties.get(KEY_INCLUDE_TOKEN);
        consentCacheEntryLifetime = (Long) properties.get(KEY_CONSENT_CACHE_ENTRY_LIFETIME);
        consentCacheSize = (Long) properties.get(KEY_CONSENT_CACHE_SIZE);
        httpsRequired = (Boolean) properties.get(KEY_HTTPS_REQUIRED);
        certAuthentication = (Boolean) properties.get(KEY_CERT_AUTHENTICATION);
        allowCertAuthentication = configUtils.getBooleanConfigAttribute(properties, KEY_ALLOW_CERT_AUTHENTICATION, allowCertAuthentication);
        allowSpnegoAuthentication = configUtils.getBooleanConfigAttribute(properties, KEY_ALLOW_SPNEGO_AUTHENTICATION, allowSpnegoAuthentication);
        clientAdmin = (String) properties.get(KEY_CLIENT_ADMIN);
        coverageMapSessionMaxAge = (Long) properties.get(KEY_COVERAGE_MAP_SESSION_MAX_AGE);
        skipUserValidation = (Boolean) properties.get(KEY_SKIP_USER_VALIDATION);
        accessTokenEncoding = getAccessTokenEncodingFromConfig();
        schemaName = (String) properties.get(KEY_JDBC_SCHEMA);
        String[] buffer = (String[]) properties.get(KEY_TOKEN_FORMAT);
        tokenFormat = buffer == null ? "unspecified" : buffer[0].toLowerCase();
        jwtAccessToken = (Boolean) properties.get(KEY_JWT_ACCESS_TOKEN);
        logoutRedirectURL = (String) properties.get(KEY_logoutRedirectURL);
        cacheAccessToken = (Boolean) properties.get(KEY_CACHE_ACCESSTOKEN);
        revokeAccessTokensWithRefreshTokens = (Boolean) properties.get(KEY_REVOKE_ACCESSTOK_W_REFRESHTOK);
        passwordGrantRequiresAppPassword = configUtils.getBooleanConfigAttribute(properties, KEY_PASSWORD_GRANT_REQUIRES_APP_PASSWORD, passwordGrantRequiresAppPassword);
        appPasswordLifetime = (Long) properties.get(KEY_APP_PASSWORD_LIFETIME);
        appTokenLifetime = (Long) properties.get(KEY_APP_TOKEN_LIFETIME);
        appTokenOrPasswordLimit = (Long) properties.get(KEY_APP_TOKEN_OR_PASSWORD_LIMIT);
        clientSecretEncoding = getClientSecretEncodingFromConfig();
        ropcPreferUserSecurityName = (Boolean) properties.get(KEY_ROPC_PREFER_USERSECURITYNAME);
        trackOAuthClients = (Boolean) properties.get(KEY_TRACK_OAUTH_CLIENTS);
        oauthEndpointSettings = populateOAuthEndpointSettings(properties, KEY_OAUTH_ENDPOINT);

        setUpInternalClient();
        // tolerate old jwtAccessToken attrib but if tokenFormat attrib is specified,
        // give priority to it.
        if (tokenFormat.contains("unspecified")) { // default
            mpJwt = false;
            // jwtAccessToken can stay as it was
        } else if (tokenFormat.contains("opaque")) {
            mpJwt = false;
            jwtAccessToken = false; // override
        } else if (tokenFormat.contains("mpjwt")) {
            mpJwt = true;
            jwtAccessToken = true;
        } else if (tokenFormat.contains("jwt")) { // jwt
            mpJwt = false;
            jwtAccessToken = true;
        } else { // invalid option, shouldn't happen, treat as default
            mpJwt = false;
        }
    }

    // disallow hashing for < java8
    private String getAccessTokenEncodingFromConfig() {
        String configValue = (String) properties.get(KEY_STORE_ACCESSTOKEN_ENCODING);
        if (configValue != null && configValue.compareTo(OAuth20Constants.PLAIN_ENCODING) != 0 &&
                (OAuth20Constants.JAVA_VERSION_7 || OAuth20Constants.JAVA_VERSION_6)) {
            Tr.warning(tc, "JAVA8_REQUIRED_FOR_AT_HASHING", new Object[] { providerId, KEY_STORE_ACCESSTOKEN_ENCODING, configValue, KEY_STORE_ACCESSTOKEN_ENCODING });
            configValue = OAuth20Constants.PLAIN_ENCODING;
        }
        return configValue;
    }

    // disallow hashing for < java8
    private String getClientSecretEncodingFromConfig() {
        String configValue = (String) properties.get(KEY_CLIENT_SECRET_ENCODING);
        if (configValue != null && configValue.compareTo(OAuth20Constants.XOR) != 0 &&
                (OAuth20Constants.JAVA_VERSION_7 || OAuth20Constants.JAVA_VERSION_6)) {
            Tr.warning(tc, "JAVA8_REQUIRED_FOR_AT_HASHING", new Object[] { providerId, KEY_CLIENT_SECRET_ENCODING, configValue, KEY_CLIENT_SECRET_ENCODING });
            configValue = OAuth20Constants.XOR;
        }
        return configValue;
    }

    private OAuthEndpointSettings populateOAuthEndpointSettings(Map<String, Object> configProps, String endpointSettingsElementName) {
        OAuthEndpointSettings endpointSettings = null;
        String[] endpointSettingsElementPids = configUtils.getStringArrayConfigAttribute(configProps, endpointSettingsElementName);
        if (endpointSettingsElementPids != null && endpointSettingsElementPids.length > 0) {
            endpointSettings = populateOAuthEndpointSettings(endpointSettingsElementPids);
        }
        return endpointSettings;
    }

    private OAuthEndpointSettings populateOAuthEndpointSettings(String[] endpointSettingsElementPids) {
        OAuthEndpointSettings endpointSettings = new OAuthEndpointSettings();
        for (String elementPid : endpointSettingsElementPids) {
            Configuration config = getConfigurationFromConfigAdmin(elementPid);
            endpointSettings.addOAuthEndpointSettings(config);
        }
        return endpointSettings;
    }

    Configuration getConfigurationFromConfigAdmin(String elementPid) {
        Configuration config = null;
        try {
            config = configAdmin.getConfiguration(elementPid, "");
        } catch (IOException e) {
        }
        return config;
    }

    void setUpInternalClient() {
        setUpInternalClientId();
        setUpInternalClientSecret();
    }

    private void setUpInternalClientId() {
        // TODO
        internalClientId = configUtils.getConfigAttribute(properties, KEY_INTERNAL_CLIENT_ID);
    }

    private void setUpInternalClientSecret() {
        // TODO
        Object o = properties.get(KEY_INTERNAL_CLIENT_SECRET);
        if (o != null) {
            if (o instanceof SerializableProtectedString) {
                internalClientSecret = new String(((SerializableProtectedString) o).getChars());
            } else {
                internalClientSecret = (String) o;
            }
        } else {
            internalClientSecret = null;
        }
    }

    int iSubOidcOauth = 0;
    String[] subOidcOauth = new String[4];
    int iHttps = 0;
    String[] https = new String[3];
    int iIndepends = 0;
    String[] independs = new String[3];

    /**
     *
     */
    void handlePatterns() {
        // TODO handle the character which has special meanings in regexp
        // init patterns
        iHttps = 0;
        iSubOidcOauth = 0;
        iIndepends = 0;

        handlePattern(authorizationFormTemplate);
        handlePattern(authorizationErrorTemplate);
        handlePattern(customLoginURL);
        subOidcOauth[iSubOidcOauth++] = "scripts/oauthForm.js";
        String patterns = null;
        // pattern1
        StringBuffer patternOidcOauth = new StringBuffer("(oidc|oauth2)/(");
        for (int iI = 0; iI < iSubOidcOauth; iI++) {
            if (iI > 0)
                patternOidcOauth.append("|");
            patternOidcOauth.append(subOidcOauth[iI]);
        }
        patternOidcOauth.append(")");

        if (iIndepends > 0) {
            StringBuffer patternIndepends = new StringBuffer("");
            for (int iI = 0; iI < iIndepends; iI++) {
                if (iI > 0)
                    patternIndepends.append("|");
                patternIndepends.append(independs[iI]);
            }
            patterns = "(/" + patternOidcOauth.toString() +
                    ")|(" + patternIndepends.toString() + ")";
        } else {
            patterns = "/" + patternOidcOauth.toString();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Misc URI patterns: '" + patterns + "'");
        }
        patternOauthOidc = Pattern.compile(patterns);
    }

    /**
     * @param authorizationErrorTemplate2
     */
    void handlePattern(String pattern) {
        if (pattern == null || pattern.isEmpty())
            return;
        if (pattern.startsWith("http")) {
            https[iHttps++] = pattern;
        } else {
            if (pattern.startsWith("/")) {
                independs[iIndepends++] = pattern;
            } else {
                subOidcOauth[iSubOidcOauth++] = pattern;
            }
        }
    }

    /**
     * @param keyCode
     * @param iMin
     * @param iMax
     * @return
     */
    protected int getProperty(String keyCode, int iMin, int iMax) {
        long result = (Long) properties.get(keyCode);
        if (result < iMin) {
            Tr.info(tc, "OAUTH_LENGTH_TOO_SMALL_AND_CHANGED", result, iMin);
            result = iMin;
        }
        if (result > iMax) {
            Tr.info(tc, "OAUTH_LENGTH_TOO_LARGE_AND_CHANGED", result, iMax);
            result = iMax;
        }
        return (int) result;
    }

    @Modified
    protected void modify(ComponentContext cc, Map<String, Object> properties) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "modifying provider: " + providerId);
            }
            // remove the provider's clients
            removeClients();
            // remove this provider from the factory map
            invalidateProvider();
            // read in the provider config again
            this.properties = properties;
            setupFields();
            processProviderConfig();
            validateConfig();
            resetCoreClassesObjects();
            loadAuthFormTemplate(cc);
        } finally {
            writeLock.unlock();
        }
    }

    private void resetCoreClassesObjects() {
        needToCreateCoreClasses = true;
        createCoreClasses();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, Map<String, Object> properties) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "deactivating provider: " + providerId);
            }
            // remove the provider's clients
            removeClients();
            // remove this provider from the factory map
            invalidateProvider();
            providerId = null;
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_CONFIGURATION_ADMIN, service = ConfigurationAdmin.class, policy = ReferencePolicy.DYNAMIC)
    protected void setConfigurationAdmin(ConfigurationAdmin admin) {
        writeLock.lock();
        try {
            configAdmin = admin;
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin admin) {
        writeLock.lock();
        try {
            configAdmin = null;
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_CLASSLOADING_SVC, service = ClassLoadingService.class)
    protected void setClassLoadingSvc(ClassLoadingService cls) {
        writeLock.lock();
        try {
            classLoadingSvc = cls;
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetClassLoadingSvc(ClassLoadingService cls) {
        writeLock.lock();
        try {
            classLoadingSvc = null;
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_OAUTH_SHARED_LIB, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setSharedLib(Library lib) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setSharedLib for provider: " + providerId);
            }
            sharedLib = lib;
            // if the provider config was already read, then either the
            // shared lib activated after the provider, or the shared lib was
            // modified. Either way, regenerate the provider
            if (providerId != null) {
                // remove this provider from the factory map
                invalidateProvider();
                checkForSharedLib = true;
                validateConfig();
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetSharedLib(Library lib) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unsetSharedLib for provider: " + providerId);
            }
            sharedLib = null;
            // The shared lib was deactivated. If the provider config was
            // already read then regenerate the provider
            if (providerId != null) {
                // remove this provider from the factory map
                invalidateProvider();
                checkForSharedLib = true;
                validateConfig();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_DATA_SOURCE_FACTORY, service = ResourceFactory.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setDataSourceFactory(ResourceFactory factory) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setDataSource for provider: " + providerId);
            }
            dataSourceFactory = factory;
            // Issue msg if provider is all set now
            if (providerId != null) {
                // remove the provider's clients
                removeClients();
                // remove this provider from the factory map
                invalidateProvider();
                checkForSharedLib = true;
                checkForDataSource = true;
                validateConfig();
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetDataSourceFactory(ResourceFactory factory) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unsetDataSource for provider: " + providerId);
            }
            if (factory == dataSourceFactory) {
                dataSourceFactory = null;
                // Issue msg if provider is all set now
                if (providerId != null) {
                    // remove the provider's clients
                    removeClients();
                    // remove this provider from the factory map
                    invalidateProvider();
                    checkForDataSource = true;
                    validateConfig();
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "unsetDataSource - wrong ResourceFactory for provider: " + providerId);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_RESOURCE_CONFIG_FACTORY, service = ResourceConfigFactory.class)
    protected void setResourceConfigFactory(ResourceConfigFactory svc) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setResourceConfigFactory for provider: " + providerId);
            }
            resourceConfigFactory = svc;
            // Issue msg if provider is all set now
            if (providerId != null) {
                // remove the provider's clients
                removeClients();
                // remove this provider from the factory map
                invalidateProvider();
                checkForDataSource = true;
                validateConfig();
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetResourceConfigFactory(ResourceConfigFactory svc) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unsetResourceConfigFactory for provider: " + providerId);
            }
            if (svc == resourceConfigFactory) {
                resourceConfigFactory = null;
                // Issue msg if provider is all set now
                if (providerId != null) {
                    // remove the provider's clients
                    removeClients();
                    // remove this provider from the factory map
                    invalidateProvider();
                    checkForDataSource = true;
                    validateConfig();
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "unsetResourceConfigFactory - wrong ResourceConfigFactory for provider: " + providerId);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_OIDC_SERVER, service = OidcServer.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setOidcServer(ServiceReference<OidcServer> oidcServerRef) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setOidcServer for provider: " + providerId);
            }

            // TODO: Move these to fields in OIDC specific implementation
            grantTypeHandlerFactoryClassname = OIDCConstants.DEFAULT_OIDC_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME;
            responseTypeHandlerFactoryClassname = OIDCConstants.DEFAULT_OIDC10_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME;
            idTokenTypeHandlerClassname = OIDCConstants.DEFAULT_ID_TOKEN_HANDLER_CLASS;
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetOidcServer(ServiceReference<OidcServer> oidcServerRef) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unsetOidcServer for provider: " + providerId);
            }

            grantTypeHandlerFactoryClassname = null;
            responseTypeHandlerFactoryClassname = null;
            idTokenTypeHandlerClassname = null;
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setSecurityService(SecurityService securityService) {
        writeLock.lock();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setSecurityService: " + providerId + ":" + securityService);
            }
            if (providerId != null) {
                ConfigUtils.addSecurityService(providerId, securityService);
            }
            this.securityService = securityService;
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetSecurityService(SecurityService securityService) {
        writeLock.lock();
        try {
            if (this.securityService == securityService) {
                this.securityService = null;
                ConfigUtils.removeSecurityService(providerId);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Reference(name = KEY_EXECUTOR_SVC, service = ExecutorService.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setExecutorService(ExecutorService executorService) {
        writeLock.lock();
        try {
            this.executorService = executorService;
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetExecutorService(ExecutorService executorService) {
        writeLock.lock();
        try {
            if (this.executorService == executorService) {
                this.executorService = null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SecurityService getSecurityService() {
        return securityService;
    }

    /** {@inheritDoc} */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        writeLock.lock();
        try {
            if (event.getType() == ConfigurationEvent.CM_UPDATED && pids.contains(event.getPid())) {
                processProviderConfig();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Process the oauthProvider element
     *
     * @param providerName
     * @param configAdmin
     * @param properties
     */
    private void processProviderConfig() {
        writeLock.lock();
        try {
            clientsList = ConfigUtils.getClients();
            pids.clear();
            // TODO: Remove usage of OAuth20Parameters and use fields and getter methods instead.
            parameters = new ArrayList<OAuth20Parameter>();
            Object mediatorClassnameArray = properties.get(KEY_MED_CLASS_NAMES);
            if (mediatorClassnameArray != null) {
                String[] mediatorClassNames = (String[]) mediatorClassnameArray;
                mediatorClassname = mediatorClassNames[0];
                if (mediatorClassNames.length > 1 || !mediatorClassname.equals(ConfigUtils.BUILTIN_SAMPLE_MEDIATOR_CLASS)) {
                    checkForSharedLib = true;
                }
            } else {
                mediatorClassname = null;
            }
            loadProviderParams();
        } finally {
            writeLock.unlock();
        }
    }

    private void validateConfig() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "validateConfig entry \n  checkForDataSource: " + checkForDataSource
                    + "\n  checkForSharedLib: " + checkForSharedLib
                    + "\n  dataSourceFactory: " + dataSourceFactory
                    + "\n  resourceConfigFactory: " + resourceConfigFactory
                    + "\n  mediatorClassname: " + mediatorClassname
                    + "\n  sharedLib: " + sharedLib
                    + "\n  isValid: " + isValid);
        }
        if (checkForDataSource) {
            if (!isDatabaseStore) {
                checkForDataSource = false;
            } else if (dataSourceFactory != null &&
                    resourceConfigFactory != null) {
                checkForDataSource = false;
            }
        }
        if (checkForSharedLib) {
            if (mediatorClassname == null) {
                checkForSharedLib = false;
                setSharedLibClassLoader();
            } else if (sharedLib != null) {
                checkForSharedLib = false;
                setSharedLibClassLoader();
            }
        }
        if (!checkForDataSource && !checkForSharedLib) {
            if (!isValid) {
                Tr.info(tc, "OAUTH_PROVIDER_CONFIG_PROCESSED", new Object[] { providerId });
            }
            isValid = true;
        } else { // provider config has issues
            if (isValid) { // if it was ok before then issue message
                Tr.info(tc, "OAUTH_PROVIDER_CONFIG_INVALID", new Object[] { providerId });
            }
            isValid = false;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "validateConfig exit \n  checkForDataSource: " + checkForDataSource
                    + "\n  checkForSharedLib: " + checkForSharedLib
                    + "\n  dataSourceFactory: " + dataSourceFactory
                    + "\n  resourceConfigFactory: " + resourceConfigFactory
                    + "\n  mediatorClassname: " + mediatorClassname
                    + "\n  sharedLib: " + sharedLib
                    + "\n  isValid: " + isValid);
        }
    }

    /**
     * Process the customized Factories for Grant Type Handler
     * The Factories handle new customize grant types or override existing ones
     */
    private void processJwtGrantTypeConfig() {
        String[] jwtGrantTypes = (String[]) properties.get("jwtGrantType");
        Configuration config = null;

        // it's supposed to be only one jwtGrantType per OAuthProvider
        if (jwtGrantTypes != null && jwtGrantTypes.length > 0) {
            String jwtGrantType = jwtGrantTypes[0];
            try {
                config = configAdmin.getConfiguration(jwtGrantType, bundleLocation);
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid Jwt Grant Type:", jwtGrantType);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Config of jwtGrantType", config);
        }
        if (config != null) {
            Dictionary<String, Object> props = config.getProperties();
            if (props != null) {
                jwtMaxJtiCacheSize = (Long) props.get(KEY_JWT_MAX_JTI_CACHE_SIZE);
                jwtSkew = (Long) props.get(KEY_JWT_SKEW);
                jwtTokenMaxLifetime = (Long) props.get(KEY_JWT_TOKEN_MAX_LIFETIME);
                jwtIatRequired = (Boolean) props.get(KEY_JWT_IAT_REQUIRED);
            } else {
                resetJwtProps();
            }
        } else {
            resetJwtProps();
        }
    }

    private void resetJwtProps() {
        // reset the values to default
        jwtMaxJtiCacheSize = 10000; // default value
        jwtSkew = 300;
        jwtTokenMaxLifetime = 7200;
        jwtIatRequired = false;
    }

    /**
     * Process the localStore, databaseStore, and customStore elements
     */
    private void processClientConfig() {
        String customStore = (String) properties.get("customStore.0.storeId");
        String[] localStores = (String[]) properties.get("localStore");
        isCustomStore = customStore != null;
        isLocalStore = !isCustomStore && localStores != null && localStores.length > 0;

        if (isCustomStore) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Using customStore in provider " + providerId);
            }
            List<Map<String, Object>> listOfPropMaps = Nester.nest("customStore", properties);
            if (!listOfPropMaps.isEmpty()) {
                Map<String, Object> customStoreProps = listOfPropMaps.get(0);
                if (customStoreProps != null) {
                    cleanupInterval = (Long) customStoreProps.get(KEY_CUSTOM_CLEANUP_INT);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, KEY_CUSTOM_CLEANUP_INT, new Object[] { cleanupInterval });
                    }
                    if (cleanupInterval != null && (cleanupInterval.longValue() < 0 || cleanupInterval.longValue() > Integer.MAX_VALUE)) {
                        // note, cleanupInterval is treated as int value later, so check both negative number and number doesn't exceed the max number of int.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "The " + KEY_CUSTOM_CLEANUP_INT + " spedified by the customStore in the " + providerId + " is invalid. The acceptable value is 0 to 2,147,483,647.");
                        }
                        Tr.error(tc, "OAUTH_PROVIDER_CUSTOMSTORE_INVALID_ATTRIBUTE", new Object[] { providerId, KEY_CUSTOM_CLEANUP_INT });
                    }
                    if (cleanupInterval == null) {
                        cleanupInterval = new Long(3600);
                    }
                }
            }
        } else if (isLocalStore) {
            processLocalStoreConfig(localStores[0]);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No localStore in provider " + providerId);
            }
            List<Map<String, Object>> listOfPropMaps = Nester.nest("databaseStore", properties);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "listOfPropMaps: " + listOfPropMaps);
            }
            if (!listOfPropMaps.isEmpty()) {
                isDatabaseStore = true;
                checkForDataSource = true;
                processDatabaseStoreConfig(listOfPropMaps);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No databaseStore in the provider " + providerId);
                }
            }
        }
    }

    /**
     * Read in the clients under the localStore element
     *
     * @param localStore
     */
    private void processLocalStoreConfig(String localStore) {
        pids.add(localStore);
        Configuration config = null;
        try {
            config = configAdmin.getConfiguration(localStore, bundleLocation);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid oauth localStore configuration", localStore);
            }
            return;
        }
        Dictionary<String, Object> localStoreProps = config.getProperties();
        tokenStoreSize = (Long) localStoreProps.get(KEY_TOK_STORE_SIZE);
        if (tokenStoreSize == null)
            tokenStoreSize = new Long(2000);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "tokenStoreSize " + tokenStoreSize);
        }

        // TODO: Client URI substitutions is currently a String in metatype, maybe it should be a String[]
        providerRewrites = clientURISubstitutions != null ? new String[] { clientURISubstitutions } : null;

        String[] clients = (String[]) localStoreProps.get("client");
        if (clients == null || clients.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No oauth clients were defined in the provider. ");
            }
        } else {

            for (String clientPid : clients) {
                pids.add(clientPid);
                Configuration clientConfig = null;
                try {
                    clientConfig = configAdmin.getConfiguration(clientPid, bundleLocation);
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid oauth client configuration", clientPid);
                    }
                    continue;
                }
                if (clientConfig == null || clientConfig.getProperties() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "NULL oauth client configuration", clientPid);
                    }
                    continue;
                }

                OidcBaseClient newClient = getClientFromLocalStore(clientConfig.getProperties());

                // decode client secret
                newClient.setClientSecret(PasswordUtil.passwordDecode(newClient.getClientSecret()));

                // Set default values for omitted, then try and validate local store client, but on error report to tracing and continue
                OidcBaseClient validatedClient = newClient;

                try {
                    validatedClient = OidcBaseClientValidator.getInstance(validatedClient).validateCreateUpdate();
                } catch (OidcServerException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        String errorPrefix = "ClientId: " + (OidcOAuth20Util.isNullEmpty(validatedClient.getClientId()) ? "Unknown" : validatedClient.getClientId()) + ", Provider: " + providerId + ", ";
                        Tr.debug(tc, errorPrefix + e.getErrorDescription(), clientPid);
                    }
                }

                validatedClient.setEnabled(newClient.isEnabled());
                validatedClient.setComponentId(providerId);
                clientsList.add(validatedClient);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added client: " + validatedClient.getClientId() + " for provider: " + providerId);
                }
            }
        }
    }

    private OidcBaseClient getClientFromLocalStore(Dictionary<String, Object> props) {
        String secret;
        Object o = props.get(KEY_CLIENT_SECRET);
        if (o != null) {
            if (o instanceof SerializableProtectedString) {
                secret = new String(((SerializableProtectedString) o).getChars());
            } else {
                secret = (String) o;
            }
        } else {
            secret = null;
        }

        OidcBaseClient newClient = new OidcBaseClient((String) props.get(KEY_CLIENT_ID),
                secret,
                OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_REDIRECT)),
                (String) props.get(KEY_CLIENT_DISPLAYNAME),
                providerId,
                ((Boolean) props.get(KEY_CLIENT_ENABLED)).booleanValue());

        newClient.setTokenEndpointAuthMethod((String) props.get(KEY_CLIENT_TOKEN_EP_AUTH_METHOD));
        newClient.setScope((String) props.get(KEY_CLIENT_SCOPE));
        newClient.setGrantTypes(OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_GRANT_TYPES)));
        newClient.setResponseTypes(OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_RESPONSE_TYPES)));
        newClient.setApplicationType((String) props.get(KEY_CLIENT_APP_TYPE));
        newClient.setSubjectType((String) props.get(KEY_CLIENT_SUBJECT_TYPE));
        newClient.setPostLogoutRedirectUris(OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_POST_LOGOUT_REDIRECT_URIS)));
        newClient.setPreAuthorizedScope((String) props.get(KEY_CLIENT_PREAUTHORIZED_SCOPE));
        newClient.setIntrospectTokens(((Boolean) props.get(KEY_CLIENT_INTROSPECT_TOKENS)).booleanValue());
        newClient.setAllowRegexpRedirects(((Boolean) props.get(KEY_CLIENT_allowRegexpRedirects)).booleanValue());
        newClient.setTrustedUriPrefixes(OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_TRUSTED_URI_PREFIXES)));
        newClient.setResourceIds(OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_RESOURCE_IDS)));
        newClient.setFunctionalUserId((String) props.get(KEY_CLIENT_FUNCTIONAL_USER_ID));
        newClient.setFunctionalUserGroupIds(OidcOAuth20Util.initJsonArray((String[]) props.get(KEY_CLIENT_FUNCTIONAL_USER_GROUPIDS)));
        newClient.setAppPasswordAllowed(((Boolean) props.get(KEY_CLIENT_APP_PASSWORD_ALLOWED)).booleanValue());
        newClient.setAppTokenAllowed(((Boolean) props.get(KEY_CLIENT_APP_TOKEN_ALLOWED)).booleanValue());
        newClient.setProofKeyForCodeExchange(((Boolean) props.get(KEY_CLIENT_PROOF_KEY_FOR_CODE_EXCHANGE)).booleanValue());
        boolean publicClient = false;
        if (props.get(KEY_CLIENT_PUBLIC_CLIENT) != null) {
            publicClient = ((Boolean) props.get(KEY_CLIENT_PUBLIC_CLIENT)).booleanValue();
        }
        newClient.setPublicClient(publicClient);
        // newClient.setAppPasswordLifetime(((Long) props.get(KEY_CLIENT_APP_PASSWORD_LIFETIME)).longValue());
        // newClient.setAppTokenLifetime(((Long) props.get(KEY_CLIENT_APP_TOKEN_LIFETIME)).longValue());
        // newClient.setAppTokenOrPasswordLimit(((Long) props.get(KEY_APP_TOKEN_OR_PASSWORD_LIMIT)).longValue());

        return newClient;
    }

    /**
     * Read in the config databaseStore element
     *
     * @param databaseStore
     */
    private void processDatabaseStoreConfig(List<Map<String, Object>> listOfPropMaps) {
        // Just get the first one (there should only ever be one)
        Map<String, Object> databaseStoreProps = listOfPropMaps.get(0);
        if (databaseStoreProps != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "databaseStore elementProps for " + providerId + ": " + databaseStoreProps);
            }
            cleanupInterval = (Long) databaseStoreProps.get(KEY_JDBC_CLEANUP_INT);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, KEY_JDBC_CLEANUP_INT, new Object[] { cleanupInterval });
            }
            if (cleanupInterval != null && (cleanupInterval.longValue() < 0 || cleanupInterval.longValue() > Integer.MAX_VALUE)) {
                // note, cleanupInterval is treated as int value later, so check both negative number and number doesn't exceed the max number of int.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The " + KEY_JDBC_CLEANUP_INT + " spedified by the databaseStore in the " + providerId + " is invalid. The acceptable value is 0 to 2,147,483,647.");
                }
                Tr.error(tc, "OAUTH_PROVIDER_DATABASESTORE_INVALID_ATTRIBUTE", new Object[] { providerId, KEY_JDBC_CLEANUP_INT });
            }
            if (cleanupInterval == null) {
                cleanupInterval = new Long(3600);
            }

            limitRefreshToken = (Boolean) databaseStoreProps.get(KEY_JDBC_LIM_REF_TOK);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "limitRefreshToken", new Object[] { limitRefreshToken });
            }
            if (limitRefreshToken == null) {
                limitRefreshToken = Boolean.valueOf(true);
            }

            SerializableProtectedString password = null;
            Object o = databaseStoreProps.get(KEY_JDBC_PASSWORD);
            if (o != null) {
                if (o instanceof SerializableProtectedString) {
                    password = (SerializableProtectedString) o;
                } else {
                    password = new SerializableProtectedString(((String) o).toCharArray());
                }
            }
            String user = (String) databaseStoreProps.get(KEY_JDBC_USER);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "user password", new Object[] { user, password });
            }

            if (user != null || password != null) {
                credentials = new Object[] { user, password };
            }

            schemaName = (String) databaseStoreProps.get(KEY_JDBC_SCHEMA);

            providerRewrites = clientURISubstitutions != null ? new String[] { clientURISubstitutions } : null;

            String[] dataSourceRef = (String[]) databaseStoreProps.get(KEY_JDBC_DATASOURCEREF);
            if (dataSourceRef == null || dataSourceRef.length == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "dataSourceRef in " + providerId + " is null.");
                }
                Tr.error(tc, "OAUTH_PROVIDER_DATABASESTORE_INVALID_DATASOURCEREF", new Object[] { providerId });
            }
        }
    }

    private void loadProviderParams() {
        processJwtGrantTypeConfig();
        processClientConfig();
    }

    private void setSharedLibClassLoader() {
        if (sharedLib != null) {
            pluginClassLoader = classLoadingSvc.getSharedLibraryClassLoader(sharedLib);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cl: " + pluginClassLoader);
            }
            if (mediatorClassname != null) {
                Tr.info(tc, "OAUTH_PROVIDER_CONFIG_MEDIATOR_LIBRARYREF_ACTIVE", new Object[] { providerId, mediatorClassname });
            }
        } else {
            pluginClassLoader = null;
            if (mediatorClassname != null) {
                Tr.info(tc, "OAUTH_PROVIDER_CONFIG_NO_LIBRARYREF", new Object[] { providerId });
            }
        }
    }

    /**
     * Remove the server.xml defined clients for this provider
     */
    private void removeClients() {
        if (isLocalStore) {
            OidcBaseClientProvider clientProvider = (OidcBaseClientProvider) getClientProvider();
            if (clientProvider != null) {
                Collection<OidcBaseClient> clients = null;
                try {
                    clients = clientProvider.getAll();
                } catch (OidcServerException e) {
                    // TODO: Decide what to do about this exception
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception getting all clients from localstore while trying to removing clients:" + e);
                    }
                }

                try {
                    for (OidcBaseClient client : clients) {
                        clientProvider.deleteOverride(client.getClientId());
                    }
                } catch (OidcServerException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, e.getErrorDescription());
                    }
                }
            }
            ConfigUtils.deleteClients(providerId);
        }
    }

    private void invalidateProvider() {
        if (providerId != null) {
            pluginClassLoader = null;
            if (consentCache != null) {
                consentCache.stopCleanupThread();
            }
            if (tokenCache != null) {
                tokenCache.stopCleanupThread();
            }
            isValid = false;
        }
    }

    // ---------------------- OAuth20Provider methods ----------------------

    /** {@inheritDoc} */
    @Override
    public OAuth20Component getComponent() {
        readLock.lock();
        try {
            return component;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getID() {
        readLock.lock();
        try {
            return providerId;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OidcOAuth20ClientProvider getClientProvider() {
        readLock.lock();
        try {
            return clientProvider;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20EnhancedTokenCache getTokenCache() {
        readLock.lock();
        try {
            return tokenCache;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OauthConsentStore getConsentCache() {
        readLock.lock();
        try {
            return consentCache;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void createCoreClasses() {
        if (!needToCreateCoreClasses) {
            return;
        }
        writeLock.lock();
        try {
            if (isValid && needToCreateCoreClasses) {
                // TODO: Make creation order explicit.
                // Need to create component configuration, then the client provider and token cache before creating the OAuth20Component
                createComponentConfiguration();
                createInitializedStores(providerConfig);
                createCommonComponentRuntime(providerConfig);
                needToCreateCoreClasses = false;
            }
        } catch (OAuthProviderException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the OAuth20 common component configuration: " + e);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void createComponentConfiguration() {
        providerConfig = new OAuth20ComponentConfigurationImpl(providerId, parameters, pluginClassLoader);
    }

    private void createCommonComponentRuntime(OAuthComponentConfiguration providerConfig) throws OAuthProviderException {
        try {
            // The handlers and factories need to be created before the OAuth20ComponentImpl
            createTokenTypeHandler();
            createIDTokenTypeHandler();
            createGrantTypeHandlerFactory();
            createResponseTypeHandlerFactory();
            createMediators();
            processGrantTypes();
            component = new OAuth20ComponentImpl(this, providerConfig, this);
        } catch (OAuthException e) {
            throw new OAuthProviderException(e);
        }
    }

    private void createInitializedStores(OAuthComponentConfiguration providerConfig) throws OAuthProviderException {
        // Null out the old stores.
        clientProvider = null;
        tokenCache = null;
        consentCache = null;

        if (isCustomStore) {
            createCustomStores();
        } else if (isLocalStore) {
            createLocalStores();
        } else {
            createDatabaseStores();
        }

        // Initialize the stores.
        if (clientProvider != null) {
            clientProvider.initialize();
        }
        if (tokenCache != null) {
            tokenCache.initialize();
        }
        if (consentCache != null) {
            consentCache.initialize();
        }
    }

    private void createCustomStores() {
        OAuthStore oauthStore = iOAuthStoreRef.get();
        if (oauthStore != null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Custom store is enabled: " + oauthStore.getClass().getName());
            }

            clientProvider = new OauthClientStore(providerId, oauthStore, clientSecretEncoding);
            tokenCache = new OauthTokenStore(providerId, oauthStore, cleanupInterval * 1000, getAccessTokenEncoding(), getAccessTokenLength());
            consentCache = new OauthConsentStoreImpl(providerId, oauthStore, cleanupInterval * 1000);
        } else {
            /*
             * Wait to create the stores until the custom store service has been
             * registered.
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The custom store service reference has not been registered with this OAuth provider.");
            }
        }
    }

    private void createLocalStores() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Local store is enabled.");
        }
        clientProvider = new OidcBaseClientProvider(providerId, providerRewrites);
        tokenCache = new BaseCache(tokenStoreSize.intValue(), getAccessTokenEncoding(), getAccessTokenLength());
    }

    private void createDatabaseStores() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Database store is enabled.");
        }
        try {
            ResourceConfig resourceConfig = resourceConfigFactory.createResourceConfig(DataSource.class.getName());
            resourceConfig.setResAuthType(ResourceConfig.AUTH_CONTAINER);
            if (dataSourceFactory == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "dataSourceFactory in " + providerId + " is null.");
                }
                Tr.error(tc, "OAUTH_PROVIDER_DATABASESTORE_INVALID_DATASOURCEFACTORY", new Object[] { providerId });
                throw new OAuthProviderException("OAUTH_PROVIDER_DATABASESTORE_INVALID_DATASOURCEFACTORY");
            }
            DataSource dataSource = (DataSource) dataSourceFactory.createResource(resourceConfig);
            clientProvider = new CachedDBOidcClientProvider(providerId, dataSource, getSchemaName() + VALUE_DB_CLIENT_TABLE, credentials, null, providerRewrites, clientSecretEncoding);
            tokenCache = new CachedDBOidcTokenStore(providerId, executorService, dataSource, getSchemaName() + VALUE_DB_TOKEN_TABLE, credentials, null, cleanupInterval.intValue(), 250, limitRefreshToken, getAccessTokenEncoding(), getAccessTokenLength());
            // create another db cache instance to use it for consent cache
            consentCache = new DBConsentCache(providerId, executorService, dataSource, getSchemaName() + CONSENT_CACHE_DB_TABLE, credentials, null, cleanupInterval.intValue(), 250);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error accessing database store", e);
            }
        }
    }

    private void setResourceProtectionFilter(String filter) {
        if (filter != null) {
            resourceProtectionFilter = new OAuthResourceProtectionFilter(filter, false);
        } else {
            resourceProtectionFilter = new OAuthResourceProtectionFilter(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRequestAccepted(HttpServletRequest request) {
        // readLock.lock();
        try {
            return getResourceProtectionFilter().isAccepted(request);
        } finally {
            // readLock.unlock();
        }
    }

    private OAuthResourceProtectionFilter getResourceProtectionFilter() {
        return resourceProtectionFilter;
    }

    /** {@inheritDoc} */
    @Override
    public OAuthResult processResourceRequest(HttpServletRequest request) {
        readLock.lock();
        try {
            return component.processResourceRequest(request);
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuthResult processAuthorization(HttpServletRequest request, HttpServletResponse response, AttributeList options) {
        readLock.lock();
        try {
            return component.processAuthorization(request, response, options);
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuthResult processTokenRequest(String authenticatedClient, HttpServletRequest request, HttpServletResponse response) {
        readLock.lock();
        try {
            return component.processTokenRequest(authenticatedClient, request, response);
        } finally {
            readLock.unlock();
        }
    }

    // -------------- Getters sorted by metatype.xml attributes --------------

    @Override
    public long getAuthorizationGrantLifetime() {
        readLock.lock();
        try {
            return authorizationGrantLifetime;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getAuthorizationCodeLifetime() {
        readLock.lock();
        try {
            return authorizationCodeLifetime;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getAuthorizationCodeLength() {
        readLock.lock();
        try {
            return authorizationCodeLength;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getAccessTokenLifetime() {
        readLock.lock();
        try {
            return accessTokenLifetime;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getAccessTokenLength() {
        readLock.lock();
        try {
            return accessTokenLength;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isIssueRefreshToken() {
        readLock.lock();
        try {
            return issueRefreshToken;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getRefreshTokenLength() {
        readLock.lock();
        try {
            return refreshTokenLength;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getMediatorClassname() {
        readLock.lock();
        try {
            return mediatorClassname;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isAllowPublicClients() {
        readLock.lock();
        try {
            return allowPublicClients;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getGrantTypesAllowed() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            if (grantTypesAllowed != null) {
                return grantTypesAllowed.clone();
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getAuthorizationFormTemplate() {
        readLock.lock();
        try {
            return authorizationFormTemplate;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getAuthorizationErrorTemplate() {
        readLock.lock();
        try {
            return authorizationErrorTemplate;
        } finally {
            readLock.unlock();
        }
    }

    // @Override
    public String getSchemaName() {
        readLock.lock();
        try {
            return schemaName;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getCustomLoginURL() {
        readLock.lock();
        try {
            return customLoginURL;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getAutoAuthorizeParam() {
        readLock.lock();
        try {
            return autoAuthorizeParam;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isAutoAuthorize() {
        readLock.lock();
        try {
            return autoAuthorize;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getAutoAuthorizeClients() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            if (autoAuthorizeClients != null) {
                return autoAuthorizeClients.clone();
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getClientURISubstitutions() {
        readLock.lock();
        try {
            return clientURISubstitutions;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getClientTokenCacheSize() {
        readLock.lock();
        try {
            return clientTokenCacheSize;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getFilter() {
        readLock.lock();
        try {
            return filter;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getCharacterEncoding() {
        readLock.lock();
        try {
            return characterEncoding;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isOauthOnly() {
        readLock.lock();
        try {
            return oauthOnly;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isIncludeTokenInSubject() {
        readLock.lock();
        try {
            return includeTokenInSubject;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getConsentCacheEntryLifetime() {
        readLock.lock();
        try {
            return consentCacheEntryLifetime;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getConsentCacheSize() {
        readLock.lock();
        try {
            return consentCacheSize;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isHttpsRequired() {
        readLock.lock();
        try {
            return httpsRequired;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isCertAuthentication() {
        readLock.lock();
        try {
            return certAuthentication;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isAllowCertAuthentication() {
        readLock.lock();
        try {
            return allowCertAuthentication;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isAllowSpnegoAuthentication() {
        readLock.lock();
        try {
            return allowSpnegoAuthentication;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isSkipUserValidation() {
        readLock.lock();
        try {
            return skipUserValidation;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getClientAdmin() {
        readLock.lock();
        try {
            return clientAdmin;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getJwtMaxJtiCacheSize() {
        readLock.lock();
        try {
            return jwtMaxJtiCacheSize;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getJwtClockSkew() {
        readLock.lock();
        try {
            return jwtSkew;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getJwtTokenMaxLifetime() {
        readLock.lock();
        try {
            return jwtTokenMaxLifetime;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean getJwtIatRequired() {
        readLock.lock();
        try {
            return jwtIatRequired;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getCoverageMapSessionMaxAge() {
        readLock.lock();
        try {
            return coverageMapSessionMaxAge;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxAuthGrantLifetimeSeconds() {
        readLock.lock();
        try {
            return (int) authorizationGrantLifetime;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getCodeLifetimeSeconds() {
        readLock.lock();
        try {
            return (int) authorizationCodeLifetime;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getCodeLength() {
        readLock.lock();
        try {
            return authorizationCodeLength;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getTokenLifetimeSeconds() {
        readLock.lock();
        try {
            return (int) accessTokenLifetime;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20TokenTypeHandler getTokenTypeHandler() {
        readLock.lock();
        try {
            return tokenTypeHandler;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20TokenTypeHandler getIDTokenTypeHandler() {
        readLock.lock();
        try {
            // This will be null for OAuth20.
            return idTokenTypeHandler;
        } finally {
            readLock.unlock();
        }

    }

    /** {@inheritDoc} */
    @Override
    public OAuth20GrantTypeHandlerFactory getGrantTypeHandlerFactory() {
        readLock.lock();
        try {
            // This will be null for OAuth20.
            return grantTypeHandlerFactory;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20ResponseTypeHandlerFactory getResponseTypeHandlerFactory() {
        readLock.lock();
        try {
            // This will be null for OAuth20.
            return responseTypeHandlerFactory;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20Mediator getMediators() {
        readLock.lock();
        try {
            // TODO: There is only one mediator used from the server.xml.
            return mediator;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGrantTypeAllowed(String grantType) {
        readLock.lock();
        try {
            return finalGrantTypesAllowedSet.contains(grantType);
        } finally {
            readLock.unlock();
        }
    }

    private void createTokenTypeHandler() {
        tokenTypeHandler = new BaseTokenHandler();
    }

    private void createIDTokenTypeHandler() throws OAuthException {
        if (idTokenTypeHandlerClassname != null) {
            if (OIDCConstants.DEFAULT_ID_TOKEN_HANDLER_CLASS.equals(idTokenTypeHandlerClassname) &&
                    oidcIDTokenHandler != null) {
                idTokenTypeHandler = oidcIDTokenHandler;
            } else {
                idTokenTypeHandler = (OAuth20TokenTypeHandler) createInstance(idTokenTypeHandlerClassname,
                        OAuth20ConfigurationImpl.OAUTH20_ID_TOKENTYPEHANDLER_CLASSNAME,
                        OAuth20TokenTypeHandler.class);
            }
        }
    }

    private void createGrantTypeHandlerFactory() throws OAuthException {
        if (grantTypeHandlerFactoryClassname != null) {
            if (OIDCConstants.DEFAULT_OIDC_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME.equals(grantTypeHandlerFactoryClassname) &&
                    oidcGrantTypeHandlerFactory != null) {
                grantTypeHandlerFactory = oidcGrantTypeHandlerFactory;
            } else {
                grantTypeHandlerFactory = (OAuth20GrantTypeHandlerFactory) createInstance(grantTypeHandlerFactoryClassname,
                        OAuth20ConfigurationImpl.OAUTH20_GRANT_TYPE_HANDLER_FACTORY_CLASSNAME,
                        OAuth20GrantTypeHandlerFactory.class);
            }
        }
    }

    private void createResponseTypeHandlerFactory() throws OAuthException {
        if (responseTypeHandlerFactoryClassname != null) {
            if (OIDCConstants.DEFAULT_OIDC10_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME.equals(responseTypeHandlerFactoryClassname) &&
                    oidcResponseTypeHandlerFactory != null) {
                responseTypeHandlerFactory = oidcResponseTypeHandlerFactory;
            } else {
                responseTypeHandlerFactory = (OAuth20ResponseTypeHandlerFactory) createInstance(responseTypeHandlerFactoryClassname,
                        OAuth20ConfigurationImpl.OAUTH20_RESPONSE_TYPE_HANDLER_FACTORY_CLASSNAME,
                        OAuth20ResponseTypeHandlerFactory.class);
            }
        }
    }

    private void createMediators() throws OAuthException {
        // TODO: Investigate why only one mediator was being used from server.xml.
        if (mediatorClassname == null) {
            // TODO: Why is a default mediator needed?
            mediator = new OAuth20MediatorDefaultImpl();
        } else {
            mediator = (OAuth20Mediator) createInstance(mediatorClassname,
                    OAuthComponentConfigurationConstants.OAUTH20_MEDIATOR_CLASSNAMES,
                    OAuth20Mediator.class);
        }
    }

    @SuppressWarnings("rawtypes")
    private Object createInstance(String className, String configConstant, Class interfaceClass) throws OAuthException {
        ClassLoader cl = null;
        if (!ConfigUtils.isBuiltinClass(className)) {
            cl = pluginClassLoader;
        } else {
            cl = OAuth20ProviderUtils.class.getClassLoader();
        }
        return OAuth20ProviderUtils.processClass(className, configConstant, interfaceClass, cl);
    }

    protected void processGrantTypes() throws OAuthException {
        if (grantTypesAllowed == null || grantTypesAllowed.length == 0) {
            // No grant types specified, invalid configuration
            throw new OAuthConfigurationException("security.oauth.error.config.notspecified.exception", OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, "null", null);
        }

        finalGrantTypesAllowedSet = new HashSet<String>();
        for (String name : grantTypesAllowed) {
            // If one of the grant_type elements has a list of grant types, we have to split it and verify each of them
            String[] grantTypesList = name.split(",");
            for (String grant : grantTypesList) {
                if (!OAuth20Constants.ALL_GRANT_TYPES_SET.contains(grant)) {
                    // Unrecognized parameter for grant types
                    throw new OAuthConfigurationException("security.oauth.error.invalidconfig.exception", OAuthComponentConfigurationConstants.OAUTH20_GRANT_TYPES_ALLOWED, name, null);
                }
                finalGrantTypesAllowedSet.add(grant);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuthAuditHandler getAuditHandler() {
        readLock.lock();
        try {
            // Null for OAuth20 in Liberty since the audit handler class name is not in metatype.
            return auditHandler;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getInstanceId() {
        readLock.lock();
        try {
            return providerId;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20Component getOAuth20Component() {
        readLock.lock();
        try {
            return component;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        return isValid;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLocalStoreUsed() {
        return isLocalStore;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMiscUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Matcher m = null;
        synchronized (patternOauthOidc) {
            m = patternOauthOidc.matcher(uri);
        }
        if (m.matches()) {
            return true;
        }

        if (iHttps > 0) {
            String url = request.getRequestURL().toString();
            for (int iI = 0; iI < iHttps; iI++) {
                if (url.equals(https[iI])) {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * This is set from com.ibm.ws.security.oauth20.util.ConfigUtil
     * to prevent the Oauth provider waiting for openidconnect server feature to start
     * This won't be activated until the OAuthProvider do so. See
     * createIDTokenTypeHandler();
     * This is a singleton instance.
     * Do not use it for multiple instance
     */
    public static void setOidcIDTokenTypeHandler(OAuth20TokenTypeHandler handler) {
        oidcIDTokenHandler = handler;
    }

    /*
     * This is set from com.ibm.ws.security.oauth20.util.ConfigUtil
     * to prevent the Oauth provider waiting for openidconnect server feature to start
     * This won't be activated until the OAuthProvider do so. See
     * createGrantTypeHandlerFactory();
     * This is a singleton instance.
     * Do not use it for multiple instance
     */
    public static void setOidcGrantTypeHandlerFactory(OAuth20GrantTypeHandlerFactory handler) {
        oidcGrantTypeHandlerFactory = handler;
    }

    /*
     * This is set from com.ibm.ws.security.oauth20.util.ConfigUtil
     * to prevent the Oauth provider waiting for openidconnect server feature to start
     * This won't be activated until the OAuthProvider do so. See
     * createResponseTypeHandlerFactory();
     * This is a singleton instance.
     * Do not use it for multiple instance
     */
    public static void setOidcResponseTypeHandlerFactory(OAuth20ResponseTypeHandlerFactory handler) {
        oidcResponseTypeHandlerFactory = handler;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isJwtAccessToken() {
        return this.jwtAccessToken;
    }

    @Override
    public byte[] getDefaultAuthorizationFormTemplateContent() {
        return authorizationFormTemplateContent == null ? null : authorizationFormTemplateContent.clone();
    }

    @Override
    public boolean isMpJwt() {
        return mpJwt;
    }

    @Override
    public String getLogoutRedirectURL() {
        return logoutRedirectURL;
    }

    @Override
    public boolean cacheAccessToken() {
        return this.cacheAccessToken;
    }

    @Override
    public boolean getRevokeAccessTokensWithRefreshTokens() {
        return this.revokeAccessTokensWithRefreshTokens;
    }

    @Override
    public boolean isPasswordGrantRequiresAppPassword() {
        readLock.lock();
        try {
            return passwordGrantRequiresAppPassword;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getAppPasswordLifetime() {
        return appPasswordLifetime;
    }

    @Override
    public long getAppTokenLifetime() {
        return appTokenLifetime;
    }

    @Override
    public long getAppTokenOrPasswordLimit() {
        return appTokenOrPasswordLimit;
    }

    /** {@inheritDoc} */
    @Override
    public void serverStopping() {
        invalidateProvider();
    }

    @Override
    public String getInternalClientId() {
        return internalClientId;
    }

    @Override
    @Sensitive
    public String getInternalClientSecret() {
        return internalClientSecret;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, target = "(id=unbound)")
    protected void setCustomStore(OAuthStore store) throws OAuthProviderException {
        iOAuthStoreRef.set(store);

        /*
         * Initialize the custom stores if configuration indicates we are configured to run
         * with a custom store.
         */
        writeLock.lock();
        try {
            if (isCustomStore) {
                createInitializedStores(providerConfig);
            }
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetCustomStore(OAuthStore store) throws OAuthProviderException {
        boolean cleared = iOAuthStoreRef.compareAndSet(store, null);

        /*
         * If we cleared the reference, we no longer have a reference set.
         * We should rebuild the stores in the case the user has
         * changed from a custom store to either a local or a DB store.
         *
         * If we didn't clear the reference, then there is nothing to do.
         */
        if (cleared) {
            writeLock.lock();
            try {
                createInitializedStores(providerConfig);
            } finally {
                writeLock.unlock();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getAccessTokenEncoding() {
        return this.accessTokenEncoding;
    }

    @Override
    public boolean isROPCPreferUserSecurityName() {
        return this.ropcPreferUserSecurityName;
    }

    @Override
    public boolean isTrackOAuthClients() {
        return trackOAuthClients;
    }

    @Override
    public OAuthEndpointSettings getOAuthEndpointSettings() {
        return oauthEndpointSettings;
    }

}
