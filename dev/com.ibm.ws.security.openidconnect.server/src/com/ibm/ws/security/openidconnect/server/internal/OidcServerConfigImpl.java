/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import java.io.IOException;
import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Pattern;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.common.jwk.impl.JWKProvider;
import com.ibm.ws.security.openidconnect.common.ConfigUtils;
import com.ibm.ws.security.openidconnect.server.ServerConstants;
import com.ibm.ws.security.openidconnect.server.plugins.OIDCProvidersConfig;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.openidconnect.server.config.OidcEndpointSettings;

/**
 * Process the OpenID Connect OP entry in the server.xml file
 */
public class OidcServerConfigImpl implements OidcServerConfig {

    //OAUTH keys
    public static final String KEY_HTTPS_REQUIRED = "httpsRequired";

    //OIDC Configuration Keys
    private static final TraceComponent tc = Tr.register(OidcServerConfigImpl.class);
    public static final String CFG_KEY_ID = "id";
    public static final String CFG_KEY_OAUTH_PROVIDER_REF = "oauthProviderRef";
    public static final String CFG_KEY_UNIQUE_USER_IDENTIFIER = "uniqueUserIdentifier";
    public static final String CFG_KEY_ISSUER_IDENTIFIER = "issuerIdentifier";
    public static final String CFG_KEY_AUDIENCE = "audience";
    public static final String CFG_KEY_USER_IDENTITY = "userIdentity";
    public static final String CFG_KEY_GROUP_IDENTIFIER = "groupIdentifier";
    public static final String CFG_KEY_DEFAULT_SCOPE = "defaultScope";
    public static final String CFG_KEY_EXTERNAL_CLAIM_NAMES = OAuth20Constants.EXTERNAL_CLAIM_NAMES;
    public static final String CFG_KEY_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String CFG_KEY_CUSTOM_CLAIMS_ENABLED = "customClaimsEnabled";
    public static final String CFG_KEY_CUSTOM_CLAIMS = "customClaims";
    public static final String CFG_KEY_JTI_CLAIM_ENABLED = "jtiClaimEnabled";
    public static final String CFG_KEY_KEYSTORE_REF = "keyStoreRef";
    public static final String CFG_KEYSTORE_REF_DEFAULT = "opKeyStore";
    public static final String CFG_KEY_KEY_ALIAS_NAME = "keyAliasName";
    public static final String CFG_KEY_TRUSTSTORE_REF = "trustStoreRef";
    public static final String CFG_KEY_SESSION_MANAGED = "sessionManaged";
    public static final String CFG_KEY_ID_TOKEN_LIFETIME = "idTokenLifetime";
    public static final String CFG_KEY_CHECK_SESSION_IFRAME_ENDPOINT_URL = "checkSessionIframeEndpointUrl";
    public static final String CFG_KEY_PROTECTED_ENDPOINTS = "protectedEndpoints";
    public static final String CFG_KEY_CACHE_IDTOKEN = "idTokenCacheEnabled";
    // OIDC Discovery Configuration Metadata
    public static final String CFG_KEY_RESPONSE_TYPES_SUPPORTED = "responseTypesSupported";
    public static final String CFG_KEY_SUBJECT_TYPES_SUPPORTED = "subjectTypesSupported";
    public static final String CFG_KEY_ID_TOKEN_SIGNING_ALG_VAL_SUPPORTED = "idTokenSigningAlgValuesSupported";
    public static final String CFG_KEY_SCOPES_SUPPORTED = "scopesSupported";
    public static final String CFG_KEY_CLAIMS_SUPPORTED = "claimsSupported";
    public static final String CFG_KEY_RESPONSE_MODES_SUPPORTED = "responseModesSupported";
    public static final String CFG_KEY_GRANT_TYPES_SUPPORTED = "grantTypesSupported";
    public static final String CFG_KEY_TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "tokenEndpointAuthMethodsSupported";
    public static final String CFG_KEY_DISPLAY_VALUES_SUPPORTED = "displayValuesSupported";
    public static final String CFG_KEY_CLAIM_TYPES_SUPPORTED = "claimTypesSupported";
    public static final String CFG_KEY_CLAIMS_PARAMETERS_SUPPORTED = "claimsParameterSupported";
    public static final String CFG_KEY_REQUEST_PARAMETERS_SUPPORTED = "requestParameterSupported";
    public static final String CFG_KEY_REQUEST_URI_PARAMETER_SUPPORTED = "requestUriParameterSupported";
    public static final String CFG_KEY_REQUIRE_REQUEST_URI_REGISTRATION = "requireRequestUriRegistration";
    public static final String CFG_KEY_BACKING_IDP_URI_PREFIX = "backingIdpUriPrefix";
    public static final String CFG_KEY_AUTH_PROXY_ENDPOINT_URL = "authProxyEndpointUrl";
    public static final String CFG_KEY_REQUIRE_OPENID_SCOPE_FOR_USERINFO = "requireOpenidScopeForUserInfo";
    public static final String CFG_KEY_OIDC_ENDPOINT = "oidcEndpoint";

    public static final String CFG_KEY_JWK_ENABLED = "jwkEnabled";
    public static final String CFG_KEY_JWK_ROTATION = "jwkRotationTime";
    public static final String CFG_KEY_JWK_SIGNING_KEY_SIZE = "jwkSigningKeySize";
    public static final String CFG_KEY_SSO_COOKIE_NAME = "allowDefaultSsoCookieName";
    // End of OIDC Discovery Configuration Metadata

    public static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);
    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);
    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);
    private ConfigUtils configUtils;
    private final CommonConfigUtils commonConfigUtils = new CommonConfigUtils();

    private String providerId;
    private String oauthProviderRef;
    private String userIdentifier;
    private String uniqueUserIdentifier;
    private String issuerIdentifier;
    private String audience;
    private String userIdentity;
    private String groupIdentifier;
    private String defaultScope;
    private String externalClaimNames;
    private Properties scopeToClaimMap;
    private Properties claimToUserRegistryMap;
    private String signatureAlgorithm;
    private boolean customClaimsEnabled;
    private boolean cacheIDToken;
    static final Set<String> defaultCustomClaims = new HashSet<String>();
    static {
        defaultCustomClaims.add("realmName");
        defaultCustomClaims.add("uniqueSecurityName");
        defaultCustomClaims.add("groupIds");
    };
    private Set<String> customClaims;
    private boolean jtiClaimEnabled;
    private boolean sessionManaged;
    private String keyStoreRef;
    private String keyAliasName;
    private String trustStoreRef;
    private long idTokenLifetime;
    private String checkSessionIframeEndpointUrl;
    private String protectedEndpoints;
    Pattern patternProtectedEndpoints;
    Pattern patternOidcEndpoints;
    Pattern patternNonOidcEndpoints;
    // OIDC Discovery Configuration Metadata
    private Properties discovery;
    private String[] responseTypesSupported;
    private String[] subjectTypesSupported;
    private String idTokenSigningAlgValuesSupported;
    private String[] scopesSupported;
    private String[] claimsSupported;
    private String[] responseModesSupported;
    private String[] grantTypesSupported;
    private String[] tokenEndpointAuthMethodsSupported;
    private String[] displayValuesSupported;
    private String[] claimTypesSupported;
    private boolean claimsParameterSupported; // TODO Consider renaming from "Supported" to "Allowed" to avoid confusion with previous "Supported" atts that mean a list
    private boolean requestParameterSupported;
    private boolean requestUriParameterSupported;
    private boolean requireRequestUriRegistration;
    private String backingIdpUriPrefix;
    private String authProxyEndpointUrl;
    private boolean jwkEnabled = false;
    private JWKProvider jwkProvider;
    private long jwkRotationTime = 0l;
    private int jwkSigningKeySize = 0;
    private boolean allowLtpaToken2Name = false;
    private boolean requireOpenidScopeForUserInfo = true;
    // End of OIDC Discovery Configuration Metadata
    private OidcEndpointSettings oidcEndpointSettings;

    // Use locks instead of synchronize blocks to ensure concurrent access while reading and lock during modification only
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();;
    private final WriteLock writeLock = reentrantReadWriteLock.writeLock();
    private final ReadLock readLock = reentrantReadWriteLock.readLock();

    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        writeLock.lock();
        try {
            configAdminRef.setReference(ref);
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        writeLock.lock();
        try {
            configAdminRef.unsetReference(ref);
        } finally {
            writeLock.unlock();
        }
    }

    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        writeLock.lock();
        try {
            keyStoreServiceRef.setReference(ref);
        } finally {
            writeLock.unlock();
        }
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        writeLock.lock();
        try {
            keyStoreServiceRef.unsetReference(ref);
        } finally {
            writeLock.unlock();
        }
    }

    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
    }

    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        writeLock.lock();
        try {
            configAdminRef.activate(cc);
            configUtils = new ConfigUtils(configAdminRef);
            keyStoreServiceRef.activate(cc);
            sslSupportRef.activate(cc);
            processConfigProps(props);
            Tr.info(tc, "OIDC_SERVER_CONFIG_PROCESSED", providerId);
        } finally {
            writeLock.unlock();
        }
    }

    protected synchronized void modify(Map<String, Object> props) {
        writeLock.lock();
        try {
            processConfigProps(props);
            Tr.info(tc, "OIDC_SERVER_CONFIG_MODIFIED", providerId);
        } finally {
            writeLock.unlock();
        }
    }

    protected synchronized void deactivate(ComponentContext cc) {
        writeLock.lock();
        try {
            configAdminRef.deactivate(cc);
            keyStoreServiceRef.deactivate(cc);
            sslSupportRef.deactivate(cc);
            OIDCProvidersConfig.removeOidcServerConfig(providerId);
        } finally {
            writeLock.unlock();
        }
    }

    // Ensure we use https scheme if OP has httpsRequired set to true
    private String processIssuerIdentifier(String issuer) {
        if (issuer == null || issuer.isEmpty())
            return issuer;

        String HTTPS_SCHEME = "https:";
        boolean httpsRequired = false;

        ConfigurationAdmin configAdmin = configAdminRef.getService();
        try {
            Configuration config = configAdmin.getConfiguration(oauthProviderRef, null);
            Dictionary<String, Object> properties = config.getProperties();
            httpsRequired = (Boolean) properties.get(KEY_HTTPS_REQUIRED);

        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid oauthProviderRef configuration", e.getMessage());
            }
            return null;
        }

        // Let runtime reset issuerIdentifier to default
        if (httpsRequired && !issuer.contains(HTTPS_SCHEME)) {
            Tr.warning(tc, "OIDC_SERVER_ISSUER_IDENTIFIER_NOT_HTTPS", new Object[] { issuer });
            issuer = null;
        }
        return issuer;
    }

    private void processConfigProps(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;

        providerId = trimIt((String) props.get(CFG_KEY_ID));
        oauthProviderRef = trimIt((String) props.get(CFG_KEY_OAUTH_PROVIDER_REF));
        userIdentifier = trimIt((String) props.get(CFG_KEY_UNIQUE_USER_IDENTIFIER));
        uniqueUserIdentifier = trimIt((String) props.get(CFG_KEY_UNIQUE_USER_IDENTIFIER));
        issuerIdentifier = processIssuerIdentifier(trimIt((String) props.get(CFG_KEY_ISSUER_IDENTIFIER)));
        audience = trimIt((String) props.get(CFG_KEY_AUDIENCE));
        userIdentity = trimIt((String) props.get(CFG_KEY_USER_IDENTITY));
        groupIdentifier = trimIt((String) props.get(CFG_KEY_GROUP_IDENTIFIER));
        defaultScope = trimIt((String) props.get(CFG_KEY_DEFAULT_SCOPE));
        externalClaimNames = trimIt((String) props.get(CFG_KEY_EXTERNAL_CLAIM_NAMES));
        signatureAlgorithm = trimIt((String) props.get(CFG_KEY_SIGNATURE_ALGORITHM));
        customClaimsEnabled = (Boolean) props.get(CFG_KEY_CUSTOM_CLAIMS_ENABLED);
        String[] aCustomClaims = (String[]) props.get(CFG_KEY_CUSTOM_CLAIMS);
        customClaims = newCustomClaims(aCustomClaims);
        jtiClaimEnabled = (Boolean) props.get(CFG_KEY_JTI_CLAIM_ENABLED);
        sessionManaged = (Boolean) props.get(CFG_KEY_SESSION_MANAGED);
        keyStoreRef = trimIt(fixUpKeyStoreRef((String) props.get(CFG_KEY_KEYSTORE_REF)));
        keyAliasName = trimIt((String) props.get(CFG_KEY_KEY_ALIAS_NAME));
        trustStoreRef = trimIt((String) props.get(CFG_KEY_TRUSTSTORE_REF));
        idTokenLifetime = (Long) props.get(CFG_KEY_ID_TOKEN_LIFETIME);
        checkSessionIframeEndpointUrl = trimIt((String) props.get(CFG_KEY_CHECK_SESSION_IFRAME_ENDPOINT_URL));
        cacheIDToken = (Boolean) props.get(CFG_KEY_CACHE_IDTOKEN);
        if (props.get(CFG_KEY_SSO_COOKIE_NAME) != null) {
            allowLtpaToken2Name = (Boolean) props.get(CFG_KEY_SSO_COOKIE_NAME);
        }
        String protectedEndpoints = trimIt((String) props.get(CFG_KEY_PROTECTED_ENDPOINTS));
        if (!protectedEndpoints.equals(this.protectedEndpoints)) {
            // hanldle new pattern
            this.protectedEndpoints = protectedEndpoints;
            patternProtectedEndpoints = handleNewPattern(protectedEndpoints);
        }
        patternOidcEndpoints = handleOidcPattern();
        patternNonOidcEndpoints = handleNonOidcPattern();

        scopeToClaimMap = configUtils.processFlatProps(props, ConfigUtils.CFG_KEY_SCOPE_TO_CLAIM_MAP);
        claimToUserRegistryMap = configUtils.processFlatProps(props, ConfigUtils.CFG_KEY_CLAIM_TO_UR_MAP);

        discovery = configUtils.processDiscoveryProps(props, ConfigUtils.CFG_KEY_DISCOVERY);
        processDiscoveryRefElement();

        if (props.containsKey(CFG_KEY_REQUIRE_OPENID_SCOPE_FOR_USERINFO)) {
            requireOpenidScopeForUserInfo = (Boolean) props.get(CFG_KEY_REQUIRE_OPENID_SCOPE_FOR_USERINFO);
        }

        jwkEnabled = (Boolean) props.get(CFG_KEY_JWK_ENABLED);
        jwkRotationTime = (Long) props.get(CFG_KEY_JWK_ROTATION);
        jwkRotationTime = jwkRotationTime * 60 * 1000;
        jwkSigningKeySize = ((Long) props.get(CFG_KEY_JWK_SIGNING_KEY_SIZE)).intValue();
        buildJwk();

        oidcEndpointSettings = populateOidcEndpointSettings(props, CFG_KEY_OIDC_ENDPOINT);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "providerId: " + providerId);
            Tr.debug(tc, "oauthProviderRef: " + oauthProviderRef);
            Tr.debug(tc, "userIdentifier: " + userIdentifier);
            Tr.debug(tc, "uniqueUserIdentifier: " + uniqueUserIdentifier);
            Tr.debug(tc, "issuerIdentifier: " + issuerIdentifier);
            Tr.debug(tc, "audience: " + audience);
            Tr.debug(tc, "userIdentity: " + userIdentity);
            Tr.debug(tc, "groupIdentifier: " + groupIdentifier);
            Tr.debug(tc, "customClaimsEnabled: " + customClaimsEnabled);
            Tr.debug(tc, "customClaims: " + customClaims);
            Tr.debug(tc, "jtiClaimEnabled: " + jtiClaimEnabled);
            Tr.debug(tc, "defaultScope: " + defaultScope);
            Tr.debug(tc, "externalClaimNames: " + externalClaimNames);
            Tr.debug(tc, "mapScopeToClaims: " + scopeToClaimMap);
            Tr.debug(tc, "claimToUserRegistryAttributeMappings: " + claimToUserRegistryMap);
            Tr.debug(tc, "signatureAlgorithm: " + signatureAlgorithm);
            Tr.debug(tc, "keyStoreRef: " + keyStoreRef);
            Tr.debug(tc, "keyAliasName: " + keyAliasName);
            Tr.debug(tc, "trustStoreRef: " + trustStoreRef);
            Tr.debug(tc, "sessionManaged: " + sessionManaged);
            Tr.debug(tc, "idTokenLifetime: " + idTokenLifetime);
            Tr.debug(tc, "checkSessionIframeEndpointUrl: " + checkSessionIframeEndpointUrl);
            Tr.debug(tc, "protectedEndpoints: " + protectedEndpoints);
            Tr.debug(tc, "jwkRotationTime: " + jwkRotationTime);
            Tr.debug(tc, "jwkEnabled: " + jwkEnabled);
            Tr.debug(tc, "allowLtpaToken2Name: " + this.allowLtpaToken2Name);
            Tr.debug(tc, "cacheIDToken: " + cacheIDToken);

            //TODO: Joe Add debug statements for Discovery Properties
        }
        OIDCProvidersConfig.putOidcServerConfig(providerId, this);
    }

    private OidcEndpointSettings populateOidcEndpointSettings(Map<String, Object> configProps, String endpointSettingsElementName) {
        OidcEndpointSettings endpointSettings = null;
        String[] endpointSettingsElementPids = commonConfigUtils.getStringArrayConfigAttribute(configProps, endpointSettingsElementName);
        if (endpointSettingsElementPids != null && endpointSettingsElementPids.length > 0) {
            endpointSettings = populateOidcEndpointSettings(endpointSettingsElementPids);
        }
        return endpointSettings;
    }

    private OidcEndpointSettings populateOidcEndpointSettings(String[] endpointSettingsElementPids) {
        OidcEndpointSettings endpointSettings = new OidcEndpointSettings();
        for (String elementPid : endpointSettingsElementPids) {
            Configuration config = getConfigurationFromConfigAdmin(elementPid);
            endpointSettings.addOidcEndpointSettings(config);
        }
        return endpointSettings;
    }

    Configuration getConfigurationFromConfigAdmin(String elementPid) {
        Configuration config = null;
        try {
            ConfigurationAdmin configAdmin = configAdminRef.getService();
            if (configAdmin != null) {
                config = configAdmin.getConfiguration(elementPid, "");
            }
        } catch (IOException e) {
        }
        return config;
    }

    /**
     * @return the customClaims which does not include the default "realmName uniqueSecurityName groupIds"
     */
    protected Set<String> newCustomClaims(String[] aCustomClaims) {
        Set<String> result = new HashSet<String>(); // (defaultCustomClaims);
        if (aCustomClaims != null) {
            for (String claim : aCustomClaims) {
                claim = claim.trim();
                if (!defaultCustomClaims.contains(claim)) {
                    result.add(claim);
                }
            }
        }
        return result;
    }

    private void processDiscoveryRefElement() {
        responseTypesSupported = (String[]) discovery.get(CFG_KEY_RESPONSE_TYPES_SUPPORTED);
        subjectTypesSupported = (String[]) discovery.get(CFG_KEY_SUBJECT_TYPES_SUPPORTED);

        //WI 150930: Purposely setting the idTokenSigningAlg value to the main signatureAlgo value
        //leaving idTokenSigningAlgValuesSupported, as it's own variable in case this changes in the future
        idTokenSigningAlgValuesSupported = signatureAlgorithm;

        scopesSupported = (String[]) discovery.get(CFG_KEY_SCOPES_SUPPORTED);
        claimsSupported = (String[]) discovery.get(CFG_KEY_CLAIMS_SUPPORTED);
        responseModesSupported = (String[]) discovery.get(CFG_KEY_RESPONSE_MODES_SUPPORTED);
        grantTypesSupported = (String[]) discovery.get(CFG_KEY_GRANT_TYPES_SUPPORTED);
        tokenEndpointAuthMethodsSupported = (String[]) discovery.get(CFG_KEY_TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED);
        displayValuesSupported = (String[]) discovery.get(CFG_KEY_DISPLAY_VALUES_SUPPORTED);
        claimTypesSupported = (String[]) discovery.get(CFG_KEY_CLAIM_TYPES_SUPPORTED);
        claimsParameterSupported = (Boolean) discovery.get(CFG_KEY_CLAIMS_PARAMETERS_SUPPORTED);
        requestParameterSupported = (Boolean) discovery.get(CFG_KEY_REQUEST_PARAMETERS_SUPPORTED);
        requestUriParameterSupported = (Boolean) discovery.get(CFG_KEY_REQUEST_URI_PARAMETER_SUPPORTED);
        requireRequestUriRegistration = (Boolean) discovery.get(CFG_KEY_REQUIRE_REQUEST_URI_REGISTRATION);
        backingIdpUriPrefix = trimIt((String) discovery.get(CFG_KEY_BACKING_IDP_URI_PREFIX));
        authProxyEndpointUrl = trimIt(discovery.getProperty(CFG_KEY_AUTH_PROXY_ENDPOINT_URL));
    }

    /** {@inheritDoc} */
    @Override
    public String getProviderId() {
        readLock.lock();
        try {
            return providerId;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getOauthProviderName() {
        readLock.lock();
        try {
            return getOauthProviderName(oauthProviderRef);
        } finally {
            readLock.unlock();
        }
    }

    private String getOauthProviderName(String oauthProviderRef) {
        Configuration config = null;
        ConfigurationAdmin configAdmin = configAdminRef.getService();
        try {
            config = configAdmin.getConfiguration(oauthProviderRef, null);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid oauthProviderRef configuration", e.getMessage());
            }
            return null;
        }
        Dictionary<String, Object> props = config.getProperties();
        if (props == null)
            return null;
        String id = (String) props.get(CFG_KEY_ID);
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String getOauthProviderPid() {
        readLock.lock();
        try {
            return oauthProviderRef;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserIdentifier() {
        readLock.lock();
        try {
            return userIdentifier;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getIssuerIdentifier() {
        readLock.lock();
        try {
            return issuerIdentifier;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupIdentifier() {
        readLock.lock();
        try {
            return groupIdentifier;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Determines if the custom claims should be added.
     */
    @Override
    public boolean isCustomClaimsEnabled() {
        readLock.lock();
        try {
            return customClaimsEnabled;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Determines if the jti claim should be added.
     */
    @Override
    public boolean isJTIClaimEnabled() {
        readLock.lock();
        try {
            return jtiClaimEnabled;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultScope() {
        readLock.lock();
        try {
            return defaultScope;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getExternalClaimNames() {
        readLock.lock();
        try {
            return externalClaimNames;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Properties getScopeToClaimMap() {
        readLock.lock();
        try {
            return scopeToClaimMap;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Properties getClaimToUserRegistryMap() {
        readLock.lock();
        try {
            return claimToUserRegistryMap;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getSignatureAlgorithm() {
        readLock.lock();
        try {
            return signatureAlgorithm;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserIdentifier() {
        readLock.lock();
        try {
            return uniqueUserIdentifier;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getAudience() {
        readLock.lock();
        try {
            return audience;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserIdentity() {
        readLock.lock();
        try {
            return userIdentity;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustStoreRef() {
        readLock.lock();
        try {
            return trustStoreRef;
        } finally {
            readLock.unlock();
        }
    }

    public SSLSupport getSSLSupportService() {
        // TODO Auto-generated method stub
        return (sslSupportRef.getService());
    }

    public String getDefaultKeyStoreName(String propKey) {
        String keyStoreName = null;
        // config does not specify keystore, so try to get one from servers
        // default ssl config.
        SSLSupport sslSupport = getSSLSupportService();
        JSSEHelper helper = null;
        if (sslSupport != null) {
            helper = sslSupport.getJSSEHelper();
        }
        Properties props = null;
        final JSSEHelper jsseHelper = helper;
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        if (jsseHelper != null) {
            try {
                // props = jsseHelper.getProperties("", null, null, true);
                props = (Properties) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        return jsseHelper.getProperties("", connectionInfo, null, true);
                    }
                });

            } catch (PrivilegedActionException pae) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception getting properties from jssehelper!!!");
                }
                // throw (SSLException) pae.getCause();
            }

            if (props != null) {
                keyStoreName = props.getProperty(propKey);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "KeyStore name from default ssl config = " + keyStoreName);
                }
            }
        }
        return keyStoreName;
    }

    // usability: don't use our long-established default value if it is highly likely to be wrong.
    // the new JwtData impl needs the keystore ref value to be correct, all the time.
    private String fixUpKeyStoreRef(String keyStoreRef) {
        if (!keyStoreRef.equals(CFG_KEYSTORE_REF_DEFAULT)) {
            return keyStoreRef; // if user changed default, take what they supplied.
        }
        if (!keyStoreExists(keyStoreRef) && onlyOneKeyStore()) {
            // if default is missing, use what's on the system, not the default in metatype.xml
            return getDefaultKeyStoreName("com.ibm.ssl.keyStoreName");
        }
        return keyStoreRef;
    }

    @FFDCIgnore(KeyStoreException.class)
    private boolean keyStoreExists(String keyStoreRef) {
        KeyStoreService keyStoreService = keyStoreServiceRef.getService();
        try {
            keyStoreService.getKeyStoreLocation(keyStoreRef);
        } catch (KeyStoreException k) {
            return false;
        }
        return true;
    }

    private boolean onlyOneKeyStore() {
        KeyStoreService keyStoreService = keyStoreServiceRef.getService();
        return keyStoreService.getKeyStoreCount() == 1;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    public PrivateKey getPrivateKey() throws KeyStoreException, CertificateException {
        readLock.lock();
        try {
            KeyStoreService keyStoreService = keyStoreServiceRef.getService();
            // TODO error handle keyStoreRef is null. Currently, it's handled by the keyStoreService
            if (keyStoreRef == null) {
                keyStoreRef = getDefaultKeyStoreName("com.ibm.ssl.keyStoreName");
            }
            if (keyAliasName != null) {
                return keyStoreService.getPrivateKeyFromKeyStore(keyStoreRef, keyAliasName, null);
            } else {
                // If this keyStore has one and only one private key, we will get it in case no keyAlias defined
                return keyStoreService.getPrivateKeyFromKeyStore(keyStoreRef);
            }
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    public PublicKey getPublicKey(String trustAliasName) throws KeyStoreException, CertificateException {
        readLock.lock();
        try {
            KeyStoreService keyStoreService = keyStoreServiceRef.getService();
            if (keyStoreService != null && signatureAlgorithm.equals(OAuth20Constants.SIGNATURE_ALGORITHM_RS256)) {
                if (trustStoreRef == null) {
                    trustStoreRef = getDefaultKeyStoreName("com.ibm.ssl.trustStoreName");
                }
                if (trustAliasName != null) {
                    return keyStoreService.getCertificateFromKeyStore(trustStoreRef, trustAliasName).getPublicKey();
                } else {
                    Collection<String> aliases = keyStoreService.getTrustedCertEntriesInKeyStore(trustStoreRef);
                    // check for NO aliases in the trust store
                    if (aliases == null || aliases.size() == 0) {
                        X509Certificate cert = keyStoreService.getX509CertificateFromKeyStore(trustStoreRef);
                        if (cert != null) {
                            return cert.getPublicKey();
                        }
                        //String errorMsg = Tr.formatMessage(tc, "JWT_SIGNER_CERT_NOT_AVAILABLE"); //TODO
                        return null;
                    }
                    // check for more than 1 alias in the trust store (with more than 1,
                    // we need to have trustAlias specified (this is part of the no
                    // trustAlias path))
                    if (aliases.size() > 1) {
                        //String errorMsg = Tr.formatMessage(tc, "JWT_SIGNER_CERT_AMBIGUOUS"); //TODO
                        return null;
                    }
                    // We now have 1 alias, we'll get the cert
                    String alias = aliases.iterator().next();
                    X509Certificate cert = keyStoreService.getX509CertificateFromKeyStore(trustStoreRef, alias);
                    // if NO cert, fail, otherwise get and return the public key
                    if (cert != null) {
                        return cert.getPublicKey();
                    } else {
                        //String errorMsg = Tr.formatMessage(tc, "JWT_SIGNER_CERT_NOT_AVAILABLE"); //TODO
                        return null;
                    }
                }

            } else {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @FFDCIgnore({ KeyStoreException.class, CertificateException.class })
    @Sensitive
    public PublicKey getX509PublicKey() {
        readLock.lock();
        PublicKey result = null;
        try {
            String myKeyStoreRef = keyStoreRef;
            KeyStoreService keyStoreService = keyStoreServiceRef.getService();
            if (keyStoreService != null) {

                // if keystore is the default "opKeyStore" from metatype, see if it's really there
                String[] ksAliases = keyStoreService.getAllKeyStoreAliases();
                if (keyStoreRef != null && keyStoreRef.equals("opKeyStore")) {
                    boolean keyStoreExists = false;
                    for (int i = 0; i < ksAliases.length; i++) {
                        if (ksAliases[i].equals(keyStoreRef)) {
                            keyStoreExists = true;
                            break;
                        }
                    }
                    if (!keyStoreExists) {
                        myKeyStoreRef = null;
                    }
                }
                // if only one keystore in server, try to use.
                if (myKeyStoreRef == null) {
                    if (ksAliases.length == 1) {
                        myKeyStoreRef = ksAliases[0];
                    }
                }

                if (myKeyStoreRef != null) {
                    X509Certificate x509 = null;
                    if (keyAliasName == null) {
                        x509 = keyStoreService.getX509CertificateFromKeyStore(myKeyStoreRef);
                    } else {
                        x509 = keyStoreService.getX509CertificateFromKeyStore(myKeyStoreRef, keyAliasName);
                    }
                    if (x509 != null) {
                        result = x509.getPublicKey();
                    }
                }
            }
        } catch (KeyStoreException ke) {
        } catch (CertificateException ce) {
        } finally {
            readLock.unlock();
        }
        return result;
    }

    /**
     * Determines if the session management is enabled.
     */
    @Override
    public boolean isSessionManaged() {
        readLock.lock();
        try {
            return sessionManaged;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long getIdTokenLifetime() {
        readLock.lock();
        try {
            return idTokenLifetime;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getCheckSessionIframeEndpointUrl() {
        readLock.lock();
        try {
            return checkSessionIframeEndpointUrl;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getResponseTypesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return responseTypesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getSubjectTypesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return subjectTypesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getIdTokenSigningAlgValuesSupported() {
        readLock.lock();
        try {
            return idTokenSigningAlgValuesSupported;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getScopesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return scopesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getClaimsSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return claimsSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getResponseModesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return responseModesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getGrantTypesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return grantTypesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getTokenEndpointAuthMethodsSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return tokenEndpointAuthMethodsSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getDisplayValuesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return displayValuesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getClaimTypesSupported() {
        readLock.lock();
        try {
            // Return a clone to avoid inadvertent or intentional modification
            return claimTypesSupported.clone();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isClaimsParameterSupported() {
        readLock.lock();
        try {
            return claimsParameterSupported;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isRequestParameterSupported() {
        readLock.lock();
        try {
            return requestParameterSupported;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isRequestUriParameterSupported() {
        readLock.lock();
        try {
            return requestUriParameterSupported;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isRequireRequestUriRegistration() {
        readLock.lock();
        try {
            return requireRequestUriRegistration;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getBackingIdpUriPrefix() {
        readLock.lock();
        try {
            return backingIdpUriPrefix;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getAuthProxyEndpointUrl() {
        readLock.lock();
        try {
            return authProxyEndpointUrl;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getKeyStoreRef() {
        readLock.lock();
        try {
            return keyStoreRef;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getKeyAliasName() {
        readLock.lock();
        try {
            return keyAliasName;
        } finally {
            readLock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Pattern getProtectedEndpointsPattern() {
        return patternProtectedEndpoints;
    }

    /** {@inheritDoc} */
    @Override
    public Pattern getEndpointsPattern() {
        return patternOidcEndpoints;
    }

    /** {@inheritDoc} */
    @Override
    public Pattern getNonEndpointsPattern() {
        return patternNonOidcEndpoints;
    }

    @Override
    public boolean isOpenidScopeRequiredForUserInfo() {
        return requireOpenidScopeForUserInfo;
    }

    /**
     * @param protectedEndpoints
     * @return
     */
    private Pattern handleNewPattern(String protectedEndpoints) {
        String pattern = "/oidc/(endpoint|providers)/" + providerId +
                         "/(";
        StringTokenizer st = new StringTokenizer(protectedEndpoints, " ");
        boolean bHasOne = false;
        String ep = null;
        while (st.hasMoreTokens()) {
            if (bHasOne) {
                pattern = pattern.concat("|");
            }
            ep = st.nextToken();
            if (OAuth20Constants.AUTHORIZE_URI.equals(ep)) {
                pattern = pattern.concat(ep);
            } else {
                // accept ep, ep/ ep/(anything)
                pattern = pattern.concat(ep + "|" + ep + "/.*");
            }
            bHasOne = true;
        }
        pattern = pattern.concat(")");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Pattern:" + pattern);
        }
        return Pattern.compile(pattern);
    }

    /**
     * @return
     */
    private Pattern handleOidcPattern() {
        String pattern = "/oidc/(endpoint|providers)/" + providerId + "/.*"; // later on, need to take off end_session check_session_iframe
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Pattern:" + pattern);
        }
        return Pattern.compile(pattern);
    }

    /**
     * @return
     */
    private Pattern handleNonOidcPattern() {
        String pattern = "/oidc/(endpoint|providers)/" + providerId + "/(end_session|check_session_iframe)"; // take off end_session check_session_iframe
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Non Pattern:" + pattern);
        }
        return Pattern.compile(pattern);
    }

    @Override
    public boolean isJwkEnabled() {
        return this.jwkEnabled;
    }

    private void buildJwk() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "buildJwk: jwkEnabled=" + jwkEnabled);

        if (!jwkEnabled) {
            jwkProvider = null;
            return;
        }

        jwkProvider = new JWKProvider(jwkSigningKeySize, signatureAlgorithm, jwkRotationTime);
    }

    @Override
    public String getJwkJsonString() {
        if (isJwkEnabled()) {
            return jwkProvider.getJwkSetString();
        }
        jwkProvider = getJwkProviderWithX509();
        if (jwkProvider != null) {
            return jwkProvider.getJwkSetString();
        }
        // CWWKS1640W
        Tr.warning(tc, "OIDC_SERVER_JWK_NOT_AVAILABLE", new Object[] {});
        return null;
    }

    @Override
    public JSONWebKey getJSONWebKey() {
        if (isJwkEnabled()) {
            return jwkProvider.getJWK();
        }
        jwkProvider = getJwkProviderWithX509();
        if (jwkProvider != null) {
            return jwkProvider.getJWK();
        }
        return null;
    }

    @FFDCIgnore({ KeyStoreException.class, CertificateException.class })
    private JWKProvider getJwkProviderWithX509() {
        JWKProvider jwkX509Provider = null;
        if (signatureAlgorithm.equals("RS256") && !ServerConstants.JAVA_VERSION_6) {
            PublicKey publicKey = null;
            PrivateKey privateKey = null;
            try {
                publicKey = getX509PublicKey();
                privateKey = getPrivateKey();
            } catch (KeyStoreException k) {
            } catch (CertificateException c) {
            }
            if (publicKey != null) {
                jwkX509Provider = new JWKProvider(jwkSigningKeySize, signatureAlgorithm, jwkRotationTime, publicKey, privateKey);
                //return jwkProvider.getJWK();
            }
        }
        return jwkX509Provider;
    }

    @Override
    public long getJwkRotationTime() {
        return this.jwkRotationTime;
    }

    @Override
    public int getJwkSigningKeySize() {
        return this.jwkSigningKeySize;
    }

    /**
     * @return the customClaims which does not include the default "realmName uniqueSecurityName groupIds"
     */
    @Override
    public Set<String> getCustomClaims() {
        return new HashSet<String>(this.customClaims);
    }

    @Override
    public boolean allowDefaultSsoCookieName() {
        return this.allowLtpaToken2Name;
    }

    @Trivial
    String trimIt(String str) {
        if (str == null)
            return null;
        String retVal = str.trim();

        if (retVal.isEmpty())
            retVal = null;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "trimIt(" + str + ") returns [" + retVal + "]");
        }
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean cacheIDToken() {
        // TODO Auto-generated method stub
        return this.cacheIDToken;
    }

    public OidcEndpointSettings getOidcEndpointSettings() {
        return oidcEndpointSettings;
    }

}
