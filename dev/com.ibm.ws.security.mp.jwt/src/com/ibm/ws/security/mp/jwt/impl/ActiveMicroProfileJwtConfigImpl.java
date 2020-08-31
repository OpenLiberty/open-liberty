/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.TraceConstants;

public class ActiveMicroProfileJwtConfigImpl implements MicroProfileJwtConfig {

    private static TraceComponent tc = Tr.register(ActiveMicroProfileJwtConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public final static String ISSUER = "mp.jwt.verify.issuer";
    public final static String PUBLIC_KEY = "mp.jwt.verify.publickey";
    public final static String KEY_LOCATION = "mp.jwt.verify.publickey.location";

    // Properties added by MP JWT 1.2 specification
    public final static String PUBLIC_KEY_ALG = "mp.jwt.verify.publickey.algorithm";
    public final static String DECRYPT_KEY_LOCATION = "mp.jwt.decrypt.key.location";
    public final static String VERIFY_AUDIENCES = "mp.jwt.verify.audiences";
    public final static String TOKEN_HEADER = "mp.jwt.token.header";
    public final static String TOKEN_COOKIE = "mp.jwt.token.cookie";

    private final MicroProfileJwtConfig config;
    private Map<String, String> mpConfigProps = null;

    public ActiveMicroProfileJwtConfigImpl(MicroProfileJwtConfig config, Map<String, String> mpConfigProps) {
        this.config = config;
        if (mpConfigProps != null) {
            this.mpConfigProps = new HashMap<String, String>(mpConfigProps);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return config.getId();
    }

    /** {@inheritDoc} */
    @Override
    public String getIssuer() {
        String issuer = config.getIssuer();
        if (issuer == null) {
            issuer = mpConfigProps.get(ISSUER);
        }

        return issuer;
    }

    /** {@inheritDoc} */
    @Override
    public String getSharedKey() {
        return config.getSharedKey();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAudiences() {
        return config.getAudiences();
    }

    /** {@inheritDoc} */
    @Override
    public String getSignatureAlgorithm() {
        String signatureAlgorithm = config.getSignatureAlgorithm();
        if (signatureAlgorithm != null) {
            // Server configuration takes precedence over MP Config property values
            return signatureAlgorithm;
        }
        return getSignatureAlgorithmFromMpConfigProps();
    }

    private String getSignatureAlgorithmFromMpConfigProps() {
        String defaultAlg = "RS256";
        String publicKeyAlgMpConfigProp = getMpConfigProperty(PUBLIC_KEY_ALG);
        if (publicKeyAlgMpConfigProp == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Didn't find " + PUBLIC_KEY_ALG + " property in MP Config props; defaulting to " + defaultAlg);
            }
            return defaultAlg;
        }
        if (!isSupportedSignatureAlgorithm(publicKeyAlgMpConfigProp)) {
            Tr.warning(tc, "MP_CONFIG_PUBLIC_KEY_ALG_NOT_SUPPORTED", new Object[] { publicKeyAlgMpConfigProp, defaultAlg });
            return defaultAlg;
        }
        return publicKeyAlgMpConfigProp;
    }

    private String getMpConfigProperty(String propName) {
        if (propName == null) {
            return null;
        }
        if (mpConfigProps.containsKey(propName)) {
            return String.valueOf(mpConfigProps.get(propName));
        }
        return null;
    }

    private boolean isSupportedSignatureAlgorithm(String sigAlg) {
        if (sigAlg == null) {
            return false;
        }
        return sigAlg.matches("[RHE]S(256|384|512)");
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustStoreRef() {
        return config.getTrustStoreRef();
    }

    /** {@inheritDoc} */
    @Override
    public String getTrustedAlias() {
        return config.getTrustedAlias();
    }

    /** {@inheritDoc} */
    @Override
    public long getClockSkew() {
        return config.getClockSkew();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getJwkEnabled() {
        return config.getJwkEnabled() || (config.getTrustedAlias() == null && isPublicKeyPropsPresent());
    }

    private boolean isPublicKeyPropsPresent() {
        return mpConfigProps.get(PUBLIC_KEY) != null || mpConfigProps.get(KEY_LOCATION) != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getJwkEndpointUrl() {
        return config.getJwkEndpointUrl();
    }

    @Override
    public Key getJwksKey(String kid) throws Exception {
        JwKRetriever jwkRetriever = null;
        if (mpConfigProps != null) {
            String publickey = mpConfigProps.get(PUBLIC_KEY);
            String keyLocation = mpConfigProps.get(KEY_LOCATION);
            if (publickey != null || keyLocation != null) {
                jwkRetriever = new JwKRetriever(getId(), getSslRef(), getJwkEndpointUrl(),
                        getJwkSet(), JwtUtils.getSSLSupportService(), isHostNameVerificationEnabled(),
                        null, null, getSignatureAlgorithm(), publickey, keyLocation);
            }
        }
        if (jwkRetriever == null) {
            jwkRetriever = new JwKRetriever(getId(), getSslRef(), getJwkEndpointUrl(),
                    getJwkSet(), JwtUtils.getSSLSupportService(), isHostNameVerificationEnabled(), null,
                    null, getSignatureAlgorithm());
        }
        Key signingKey = jwkRetriever.getPublicKeyFromJwk(kid, null,
                config.getUseSystemPropertiesForHttpClientConnections()); // only kid or x5t will work but not both
        return signingKey;
    }

    /** {@inheritDoc} */
    @Override
    public ConsumerUtils getConsumerUtils() {
        return config.getConsumerUtils();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidationRequired() {
        return config.isValidationRequired();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHostNameVerificationEnabled() {
        return config.isHostNameVerificationEnabled();
    }

    /** {@inheritDoc} */
    @Override
    public String getSslRef() {
        return config.getSslRef();
    }

    /** {@inheritDoc} */
    @Override
    public JWKSet getJwkSet() {
        return config.getJwkSet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getTokenReuse() {
        return config.getTokenReuse();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getUseSystemPropertiesForHttpClientConnections() {
        return config.getUseSystemPropertiesForHttpClientConnections();
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueId() {
        return config.getUniqueId();
    }

    /** {@inheritDoc} */
    @Override
    public String getUserNameAttribute() {
        return config.getUserNameAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupNameAttribute() {
        return config.getGroupNameAttribute();
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthorizationHeaderScheme() {
        return config.getAuthorizationHeaderScheme();
    }

    /** {@inheritDoc} */
    @Override
    public boolean ignoreApplicationAuthMethod() {
        return config.ignoreApplicationAuthMethod();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getMapToUserRegistry() {
        return config.getMapToUserRegistry();
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthFilterRef() {
        return config.getAuthFilterRef();
    }

    @SuppressWarnings("restriction")
    public JwtToken createJwt(String token) throws Exception {
        return config.getConsumerUtils().parseJwt(token, this);
    }

}
