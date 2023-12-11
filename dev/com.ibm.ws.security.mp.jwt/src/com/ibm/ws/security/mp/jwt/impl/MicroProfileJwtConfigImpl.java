/*******************************************************************************
 * Copyright (c) 2017 - 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtService;
import com.ibm.ws.security.mp.jwt.SslRefInfo;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion;

/**
 *
 */
@Component(name = "com.ibm.ws.security.mp.jwt", configurationPid = "com.ibm.ws.security.mp.jwt", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = { MicroProfileJwtConfig.class, JwtConsumerConfig.class }, property = { "service.vendor=IBM", "type=microProfileJwtConfig" })
public class MicroProfileJwtConfigImpl implements MicroProfileJwtConfig {

    private static TraceComponent tc = Tr.register(MicroProfileJwtConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    protected final boolean IS_REQUIRED = true;
    protected final boolean IS_NOT_REQUIRED = false;

    protected static final String KEY_UNIQUE_ID = "id";
    protected String uniqueId = null;

    protected SSLContext sslContext = null;
    protected SSLSocketFactory sslSocketFactory = null;
    public static final String KEY_sslRef = "sslRef";
    protected String sslRef;
    protected SslRefInfo sslRefInfo = null;

    public static final String KEY_jwksUri = "jwksUri";
    protected String jwksUri = null;

    static final String KEY_MP_JWT_SERVICE = "microProfileJwtService";
    final AtomicServiceReference<MicroProfileJwtService> mpJwtServiceRef = new AtomicServiceReference<MicroProfileJwtService>(KEY_MP_JWT_SERVICE);

    ConsumerUtils consumerUtils = null; // lazy init
    JWKSet jwkSet = null;

    public static final String KEY_ISSUER = "issuer";
    String issuer = null;

    public static final String KEY_AUDIENCE = "audiences";
    String[] audience = null;

    boolean ignoreAudClaimIfNotConfigured = false;

    public static final String CFG_KEY_HOST_NAME_VERIFICATION_ENABLED = "hostNameVerificationEnabled";
    protected boolean hostNameVerificationEnabled = true;

    public static final String KEY_TRUSTED_ALIAS = "keyName";
    private String trustAliasName = null;

    public static final String KEY_userNameAttribute = "userNameAttribute";
    protected String userNameAttribute = null;

    public static final String KEY_groupNameAttribute = "groupNameAttribute";
    protected String groupNameAttribute = null;

    public static final String KEY_authorizationHeaderScheme = "authorizationHeaderScheme";
    protected String authorizationHeaderScheme = null;

    public static final String CFG_KEY_TOKEN_REUSE = "tokenReuse";
    protected boolean tokenReuse = true;

    public static final String CFG_KEY_CLOCK_SKEW = "clockSkew";
    private long clockSkewMilliSeconds;

    public static final String CFG_KEY_TOKEN_AGE = "tokenAge";
    private long tokenAgeMilliSeconds;

    public static final String CFG_KEY_DECRYPT_KEY_ALGORITHM = "keyManagementKeyAlgorithm";
    private String keyManagementKeyAlgorithm = null;

    public static final String CFG_KEY_IGNORE_APP_AUTH_METHOD = "ignoreApplicationAuthMethod";
    protected boolean ignoreApplicationAuthMethod = true;

    public static final String CFG_KEY_mapToUserRegistry = "mapToUserRegistry";
    protected boolean mapToUserRegistry = false;

    public static final String CFG_KEY_SIGALG = "signatureAlgorithm";

    String signatureAlgorithm = null;

    public static final String KEY_authFilterRef = "authFilterRef";
    protected String authFilterRef;

    public static final String CFG_KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS = "useSystemPropertiesForHttpClientConnections";
    private boolean useSystemPropertiesForHttpClientConnections = false;

    public static final String KEY_TOKEN_HEADER = "tokenHeader";
    protected String tokenHeader;

    public static final String KEY_COOKIE_NAME = "cookieName";
    protected String cookieName;

    public static final String KEY_KEY_MANAGEMENT_KEY_ALIAS = "keyManagementKeyAlias";
    protected String keyManagementKeyAlias;

    @com.ibm.websphere.ras.annotation.Sensitive
    private String sharedKey;

    protected CommonConfigUtils configUtils = new CommonConfigUtils();

    @Reference(service = MicroProfileJwtService.class, name = KEY_MP_JWT_SERVICE, cardinality = ReferenceCardinality.MANDATORY)
    protected void setMicroProfileJwtService(ServiceReference<MicroProfileJwtService> ref) {
        this.mpJwtServiceRef.setReference(ref);
    }

    protected void unsetMicroProfileJwtService(ServiceReference<MicroProfileJwtService> ref) {
        this.mpJwtServiceRef.unsetReference(ref);
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) throws MpJwtProcessingException {
        this.mpJwtServiceRef.activate(cc);
        uniqueId = (String) props.get(KEY_UNIQUE_ID);
        initProps(cc, props);
        Tr.info(tc, "MPJWT_CONFIG_PROCESSED", uniqueId);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) throws MpJwtProcessingException {
        initProps(cc, props);
        Tr.info(tc, "MPJWT_CONFIG_MODIFIED", uniqueId);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        this.mpJwtServiceRef.deactivate(cc);
        Tr.info(tc, "MPJWT_CONFIG_DEACTIVATED", uniqueId);
    }

    public void initProps(ComponentContext cc, Map<String, Object> props) throws MpJwtProcessingException {
        String methodName = "initProps";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, cc, props);
        }

        this.issuer = configUtils.getConfigAttribute(props, KEY_ISSUER);//configUtils.getRequiredConfigAttribute(props, KEY_ISSUER);

        this.audience = configUtils.getStringArrayConfigAttribute(props, KEY_AUDIENCE);
        this.jwksUri = configUtils.getConfigAttribute(props, KEY_jwksUri);

        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.groupNameAttribute = configUtils.getConfigAttribute(props, KEY_groupNameAttribute);

        this.authorizationHeaderScheme = configUtils.getConfigAttribute(props, KEY_authorizationHeaderScheme);
        this.clockSkewMilliSeconds = configUtils.getLongConfigAttribute(props, CFG_KEY_CLOCK_SKEW, clockSkewMilliSeconds);

        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.sslRefInfo = null; // lazy init

        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        //this.authFilter = null; // lazy init

        this.sslContext = null;
        this.trustAliasName = configUtils.getConfigAttribute(props, KEY_TRUSTED_ALIAS);
        this.hostNameVerificationEnabled = configUtils.getBooleanConfigAttribute(props, CFG_KEY_HOST_NAME_VERIFICATION_ENABLED, hostNameVerificationEnabled);
        this.tokenReuse = configUtils.getBooleanConfigAttribute(props, CFG_KEY_TOKEN_REUSE, tokenReuse);
        this.ignoreApplicationAuthMethod = configUtils.getBooleanConfigAttribute(props, CFG_KEY_IGNORE_APP_AUTH_METHOD, ignoreApplicationAuthMethod);
        this.useSystemPropertiesForHttpClientConnections = configUtils.getBooleanConfigAttribute(props, CFG_KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, useSystemPropertiesForHttpClientConnections);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, CFG_KEY_mapToUserRegistry, mapToUserRegistry);
        jwkSet = null; // the jwkEndpoint may have been changed during dynamic update
        consumerUtils = null; // the parameters in consumerUtils may have been changed during dynamic changing
        this.signatureAlgorithm = configUtils.getConfigAttribute(props, CFG_KEY_SIGALG);
        sharedKey = JwtUtils.processProtectedString(props, JwtUtils.CFG_KEY_SHARED_KEY);

        loadConfigValuesForHigherVersions(cc, props);

        debug();
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    void loadConfigValuesForHigherVersions(ComponentContext cc, Map<String, Object> props) {
        if (!isRuntimeVersionAtLeast(MpJwtRuntimeVersion.VERSION_1_2)) {
            return;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Loading additional properties for runtime version " + MpJwtRuntimeVersion.VERSION_1_2 + " and above");
        }
        tokenHeader = configUtils.getConfigAttribute(props, KEY_TOKEN_HEADER);
        cookieName = configUtils.getConfigAttribute(props, KEY_COOKIE_NAME);
        keyManagementKeyAlias = configUtils.getConfigAttribute(props, KEY_KEY_MANAGEMENT_KEY_ALIAS);
        // Ensure that for MP JWT 1.2 and above that "aud" claim is allowed in tokens even if audiences or
        // mp.jwt.verify.audiences are not configured
        ignoreAudClaimIfNotConfigured = true;

        if (!isRuntimeVersionAtLeast(MpJwtRuntimeVersion.VERSION_2_1)) {
            return;
        }
        this.tokenAgeMilliSeconds = configUtils.getLongConfigAttribute(props, CFG_KEY_TOKEN_AGE, tokenAgeMilliSeconds);
        this.keyManagementKeyAlgorithm = configUtils.getConfigAttribute(props, CFG_KEY_DECRYPT_KEY_ALGORITHM);

    }

    boolean isRuntimeVersionAtLeast(Version minimumVersionRequired) {
        MpJwtRuntimeVersion mpJwtRuntimeVersion = getMpJwtRuntimeVersion();
        if (mpJwtRuntimeVersion == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find runtime version");
            }
            return false;
        }
        Version runtimeVersion = mpJwtRuntimeVersion.getVersion();
        return (runtimeVersion.compareTo(minimumVersionRequired) >= 0);
    }

    MpJwtRuntimeVersion getMpJwtRuntimeVersion() {
        MicroProfileJwtService mpJwtService = mpJwtServiceRef.getService();
        if (mpJwtService == null) {
            return null;
        }
        return mpJwtService.getMpJwtRuntimeVersion();
    }

    protected void debug() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, KEY_ISSUER + ": " + issuer);
            //            Tr.debug(tc, KEY_SIGNATURE_ALGORITHM + ": " + signatureAlgorithm);
            Tr.debug(tc, CFG_KEY_HOST_NAME_VERIFICATION_ENABLED + ": " + hostNameVerificationEnabled);
            Tr.debug(tc, CFG_KEY_TOKEN_REUSE + ": " + tokenReuse);
            Tr.debug(tc, KEY_TRUSTED_ALIAS + ": " + trustAliasName);
            Tr.debug(tc, "jwksUri:" + jwksUri);
            Tr.debug(tc, "userNameAttribute:" + userNameAttribute);
            Tr.debug(tc, "groupNameAttribute:" + groupNameAttribute);
            Tr.debug(tc, "mapToUserRegistry:" + mapToUserRegistry);
            Tr.debug(tc, "authFilterRef = " + authFilterRef);
            Tr.debug(tc, "sslRef = " + sslRef);
            Tr.debug(tc, "sigAlg = " + signatureAlgorithm);
            Tr.debug(tc, "sharedKey" + sharedKey == null ? "null" : "*********");
            Tr.debug(tc, "useSystemPropertiesForHttpClientConnections = " + useSystemPropertiesForHttpClientConnections);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHostNameVerificationEnabled() {
        return this.hostNameVerificationEnabled;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return getUniqueId();
    }

    /** {@inheritDoc} */
    @Override
    public String getIssuer() {
        return issuer;
    }

    /** {@inheritDoc} */
    @Override
    public String getSharedKey() {
        return sharedKey;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAudiences() {
        if (audience != null) {
            List<String> audiences = new ArrayList<String>();
            for (String aud : audience) {
                audiences.add(aud);
            }
            return audiences;
        } else {
            return null;
        }
    }

    @Override
    public boolean ignoreAudClaimIfNotConfigured() {
        return ignoreAudClaimIfNotConfigured;
    }

    /** {@inheritDoc} */
    @Override
    public String getSignatureAlgorithm() {
        return this.signatureAlgorithm;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(MpJwtProcessingException.class)
    public String getTrustStoreRef() {
        if (this.sslRefInfo == null) {
            sslRefInfo = initializeSslRefInfo();
            if (sslRefInfo == null) {
                return null;
            }
        }
        try {
            return sslRefInfo.getTrustStoreName();
        } catch (MpJwtProcessingException e) {
            // We already logged the error
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(MpJwtProcessingException.class)
    public String getKeyStoreRef() {
        if (this.sslRefInfo == null) {
            sslRefInfo = initializeSslRefInfo();
            if (sslRefInfo == null) {
                return null;
            }
        }
        try {
            return sslRefInfo.getKeyStoreName();
        } catch (MpJwtProcessingException e) {
            // We already logged the error
        }
        return null;
    }

    SslRefInfo initializeSslRefInfo() {
        MicroProfileJwtService service = mpJwtServiceRef.getService();
        if (service == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "MP JWT service is not available");
            }
            return null;
        }
        return new SslRefInfoImpl(service.getSslSupport(), service.getKeyStoreServiceRef(), sslRef, trustAliasName);
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustedAlias() {
        return trustAliasName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getJwkEnabled() {
        return jwksUri != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwkEndpointUrl() {
        return jwksUri;
    }

    /** {@inheritDoc} */
    @Override
    public ConsumerUtils getConsumerUtils() {
        if (consumerUtils == null) { // lazy init
            MicroProfileJwtService service = mpJwtServiceRef.getService();
            if (service != null) {
                consumerUtils = new ConsumerUtils(service.getKeyStoreServiceRef());
            } else {
                Tr.warning(tc, "SERVICE_NOT_FOUND_JWT_CONSUMER_NOT_AVAILABLE", new Object[] { uniqueId });
            }
        }
        return consumerUtils;
    }

    /** {@inheritDoc} */
    @Override
    public JWKSet getJwkSet() {
        if (jwkSet == null) { // lazy init
            jwkSet = new JWKSet();
        }
        return jwkSet;
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public String getSslRef() {
        return this.sslRef;
    }

    //@Override
    public HashMap<String, PublicKey> getPublicKeys() throws MpJwtProcessingException {
        String methodName = "getPublicKeys";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.sslRefInfo == null) {
            MicroProfileJwtService service = mpJwtServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "MP JWT service is not available");
                }
                if (tc.isDebugEnabled()) {
                    Tr.exit(tc, methodName, null);
                }
                return null;
            }
            sslRefInfo = new SslRefInfoImpl(service.getSslSupport(), service.getKeyStoreServiceRef(), sslRef, trustAliasName);
        }
        HashMap<String, PublicKey> keys = sslRefInfo.getPublicKeys();
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, keys);
        }
        return keys;
    }

    //@Override
    public SSLContext getSSLContext() throws MpJwtProcessingException {
        String methodName = "getSSLContext";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.sslContext == null) {
            MicroProfileJwtService service = mpJwtServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "MP JWT service is not available");
                }
                if (tc.isDebugEnabled()) {
                    Tr.exit(tc, methodName, null);
                }
                return null;
            }
            SSLSupport sslSupport = service.getSslSupport();
            if (sslSupport == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL support could not be found for microprofile jwt service");
                }
                if (tc.isDebugEnabled()) {
                    Tr.exit(tc, methodName, null);
                }
                return null;
            }
            try {
                JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
                if (jsseHelper != null) {
                    sslContext = jsseHelper.getSSLContext(sslRef, null, null, true);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "sslContext (" + sslRef + ") get: " + sslContext);
                        // Properties sslProps =
                        // jsseHelper.getProperties(sslRef);
                    }
                }
            } catch (Exception e) {
                String msg = Tr.formatMessage(tc, "FAILED_TO_GET_SSL_CONTEXT", new Object[] { uniqueId, e.getLocalizedMessage() });
                throw new MpJwtProcessingException(msg, e);
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, this.sslContext);
        }
        return this.sslContext;
    }

    //@Override
    public SSLSocketFactory getSSLSocketFactory() throws MpJwtProcessingException {
        String methodName = "getSSLSocketFactory";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.sslContext == null) {
            MicroProfileJwtService service = mpJwtServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Social login service is not available");
                }
                if (tc.isDebugEnabled()) {
                    Tr.exit(tc, methodName, null);
                }
                return null;
            }
            SSLSupport sslSupport = service.getSslSupport();
            if (sslSupport == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL support could not be found for microprofile jwt service");
                }
                if (tc.isDebugEnabled()) {
                    Tr.exit(tc, methodName, null);
                }
                return null;
            }
            try {
                sslSocketFactory = sslSupport.getSSLSocketFactory(sslRef);
                JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslSocketFactory (" + sslRef + ") get: " + sslSocketFactory);
                }
            } catch (Exception e) {
                String msg = Tr.formatMessage(tc, "FAILED_TO_GET_SSL_CONTEXT", new Object[] { uniqueId, e.getLocalizedMessage() });
                throw new MpJwtProcessingException(msg, e);
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, this.sslSocketFactory);
        }
        return this.sslSocketFactory;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserNameAttribute() {
        return this.userNameAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupNameAttribute() {
        return this.groupNameAttribute;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthorizationHeaderScheme() {
        return this.authorizationHeaderScheme;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidationRequired() {
        // TODO Auto-generated method stub
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public long getClockSkew() {
        return clockSkewMilliSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public long getTokenAge() {
        return tokenAgeMilliSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyManagementKeyAlgorithm() {
        return this.keyManagementKeyAlgorithm;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getTokenReuse() {
        return this.tokenReuse;
    }

    /** {@inheritDoc} */
    @Override
    public boolean ignoreApplicationAuthMethod() {
        // TODO Auto-generated method stub
        return this.ignoreApplicationAuthMethod;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getMapToUserRegistry() {
        // TODO Auto-generated method stub
        return this.mapToUserRegistry;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthFilterRef() {
        // TODO Auto-generated method stub
        return this.authFilterRef;
    }

    @Override
    public boolean getUseSystemPropertiesForHttpClientConnections() {
        return this.useSystemPropertiesForHttpClientConnections;
    }

    @Override
    public String getTokenHeader() {
        return tokenHeader;
    }

    @Override
    public String getCookieName() {
        return cookieName;
    }

    @Override
    public List<String> getAMRClaim() {
        return null;
    }

    @Override
    public String getKeyManagementKeyAlias() {
        return keyManagementKeyAlias;
    }

    @Override
    public Key getJweDecryptionKey() throws GeneralSecurityException {
        String keyAlias = getKeyManagementKeyAlias();
        if (keyAlias != null) {
            String keyStoreRef = getKeyStoreRef();
            return JwtUtils.getPrivateKey(keyAlias, keyStoreRef);
        }
        return null;
    }

}
