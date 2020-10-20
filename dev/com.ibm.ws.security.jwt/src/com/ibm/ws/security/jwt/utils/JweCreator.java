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
package com.ibm.ws.security.jwt.utils;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.impl.JwkKidBuilder;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.internal.BuilderImpl;
import com.ibm.ws.security.jwt.internal.JwtTokenException;

public class JweCreator {

    private static final TraceComponent tc = Tr.register(JweCreator.class);

    @FFDCIgnore({ Exception.class })
    public static String createJweString(String jws, JwtData jwtData) throws Exception {
        JweCreator signer = new JweCreator();
        JwtConfig jwtConfig = jwtData.getConfig();
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            BuilderImpl builder = jwtData.getBuilder();
            signer.setJweKeyData(jwe, builder, jwtConfig);
            signer.setJweHeaders(jwe, builder, jwtConfig);
            jwe.setPayload(jws);
            return signer.getJwtString(jwe);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_BUILDING_SIGNED_JWE", new Object[] { jwtConfig.getId(), e });
            throw new Exception(errorMsg, e);
        }
    }

    void setJweKeyData(JsonWebEncryption jwe, BuilderImpl builder, JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        Key keyManagementKey = getKeyManagementKey(builder, jwtConfig);
        if (keyManagementKey == null) {
            String errorMsg = Tr.formatMessage(tc, "KEY_MANAGEMENT_KEY_NOT_FOUND", new Object[] { jwtConfig.getId(), jwtConfig.getKeyManagementKeyAlias(), jwtConfig.getTrustStoreRef() });
            throw new KeyStoreException(errorMsg);
        }
        jwe.setKey(keyManagementKey);
        setJweKidHeader(jwe, keyManagementKey);
    }

    Key getKeyManagementKey(BuilderImpl builder, JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        Key keyManagementKey = builder.getKeyManagementKey();
        if (keyManagementKey == null) {
            keyManagementKey = getKeyManagementKeyFromTrustStore(jwtConfig);
        }
        return keyManagementKey;
    }

    Key getKeyManagementKeyFromTrustStore(JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        String keyAlias = jwtConfig.getKeyManagementKeyAlias();
        String trustStoreRef = jwtConfig.getTrustStoreRef();
        return JwtUtils.getPublicKey(keyAlias, trustStoreRef);
    }

    void setJweKidHeader(JsonWebEncryption jwe, Key keyManagementKey) {
        JwkKidBuilder kidbuilder = new JwkKidBuilder();
        String keyId = kidbuilder.buildKeyId(keyManagementKey);
        if (keyId != null) {
            jwe.setKeyIdHeaderValue(keyId);
        }
    }

    void setJweHeaders(JsonWebEncryption jwe, BuilderImpl builder, JwtConfig jwtConfig) {
        jwe.setAlgorithmHeaderValue(getKeyManagementKeyAlgorithm(builder, jwtConfig));
        jwe.setEncryptionMethodHeaderParameter(getContentEncryptionAlgorithm(builder, jwtConfig));
        jwe.setHeader("typ", "JOSE");
        jwe.setHeader("cty", "jwt");
    }

    String getKeyManagementKeyAlgorithm(BuilderImpl builder, JwtConfig jwtConfig) {
        String keyManagementAlg = builder.getKeyManagementAlg();
        if (keyManagementAlg == null) {
            keyManagementAlg = getKeyManagementKeyAlgFromConfig(jwtConfig);
        }
        return keyManagementAlg;
    }

    String getKeyManagementKeyAlgFromConfig(JwtConfig jwtConfig) {
        String configuredKeyManagementAlg = jwtConfig.getKeyManagementKeyAlgorithm();
        if (configuredKeyManagementAlg == null) {
            configuredKeyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key management algorithm not specified in server config. Defaulting to [" + configuredKeyManagementAlg + "]");
            }
        }
        return configuredKeyManagementAlg;
    }

    String getContentEncryptionAlgorithm(BuilderImpl builder, JwtConfig jwtConfig) {
        String contentEncryptionAlg = builder.getContentEncryptionAlg();
        if (contentEncryptionAlg == null) {
            contentEncryptionAlg = getContentEncryptionAlgorithmFromConfig(jwtConfig);
        }
        return contentEncryptionAlg;
    }

    String getContentEncryptionAlgorithmFromConfig(JwtConfig jwtConfig) {
        String configuredContentEncryptionAlg = jwtConfig.getContentEncryptionAlgorithm();
        if (configuredContentEncryptionAlg == null) {
            configuredContentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Content encryption algorithm not specified in server config. Defaulting to [" + configuredContentEncryptionAlg + "]");
            }
        }
        return configuredContentEncryptionAlg;
    }

    String getJwtString(JsonWebEncryption jwe) throws JwtTokenException {
        String jwt = null;
        try {
            jwt = jwe.getCompactSerialization();
        } catch (Exception e) {
            throw new JwtTokenException(e.getLocalizedMessage(), e);
        }
        return jwt;
    }

}
