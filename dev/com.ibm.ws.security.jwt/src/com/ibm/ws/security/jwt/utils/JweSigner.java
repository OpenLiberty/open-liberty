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

import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateException;

import org.jose4j.jwe.JsonWebEncryption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.impl.JwkKidBuilder;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.internal.JwtTokenException;

public class JweSigner {

    private static final TraceComponent tc = Tr.register(JweSigner.class);

    @FFDCIgnore({ Exception.class })
    public static String getSignedJwt(String jws, JwtData jwtData) throws Exception {
        JweSigner signer = new JweSigner();
        JwtConfig jwtConfig = jwtData.getConfig();
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            signer.setJweKeyData(jwe, jwtConfig, jwtData);
            signer.setJweHeaders(jwe, jwtConfig);
            jwe.setPayload(jws);
            return signer.getJwtString(jwe);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_BUILDING_SIGNED_JWE", new Object[] { jwtConfig.getId(), e });
            throw new Exception(errorMsg, e);
        }
    }

    void setJweKeyData(JsonWebEncryption jwe, JwtConfig jwtConfig, JwtData jwtData) throws KeyStoreException, CertificateException, InvalidTokenException {
        PublicKey keyManagementKey = getPublicKeyManagementKey(jwtConfig);
        if (keyManagementKey == null) {
            String errorMsg = Tr.formatMessage(tc, "KEY_MANAGEMENT_KEY_NOT_FOUND", new Object[] { jwtConfig.getId(), jwtConfig.getKeyManagementKeyAlias(), jwtConfig.getTrustStoreRef() });
            throw new KeyStoreException(errorMsg);
        }
        jwe.setKey(keyManagementKey);
        setJweKidHeader(jwe, keyManagementKey);
    }

    PublicKey getPublicKeyManagementKey(JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        String keyAlias = jwtConfig.getKeyManagementKeyAlias();
        String trustStoreRef = jwtConfig.getTrustStoreRef();
        return JwtUtils.getPublicKey(keyAlias, trustStoreRef);
    }

    void setJweKidHeader(JsonWebEncryption jwe, PublicKey keyManagementKey) {
        JwkKidBuilder kidbuilder = new JwkKidBuilder();
        String keyId = kidbuilder.buildKeyId(keyManagementKey);
        if (keyId != null) {
            jwe.setKeyIdHeaderValue(keyId);
        }
    }

    void setJweHeaders(JsonWebEncryption jwe, JwtConfig jwtConfig) {
        jwe.setAlgorithmHeaderValue(jwtConfig.getKeyManagementKeyAlgorithm());
        jwe.setEncryptionMethodHeaderParameter(jwtConfig.getContentEncryptionAlgorithm());
        jwe.setHeader("typ", "JWT");
        jwe.setHeader("cty", "jwt");
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
