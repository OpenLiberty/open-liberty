/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.common.jwk.impl;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.lang.JoseException;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.constants.TraceConstants;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.common.jwk.internal.JwkConstants;
import com.ibm.ws.security.common.random.RandomUtils;

/**
 * TODO: This should replace com.ibm.ws.security.openidconnect.jose4j.Jose4jRsaJWK
 *
 */
public class Jose4jRsaJWK extends RsaJsonWebKey implements JWK {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(Jose4jRsaJWK.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final int KID_LENGTH = 20;

    protected long created = (new Date()).getTime();

    /**
     * generate a new JWK with the specified parameters
     *
     * @param size
     * @param alg
     * @param use
     * @param type
     * @return
     */
    public static Jose4jRsaJWK getInstance(int size, String alg, String use, String type) {

        String kid = RandomUtils.getRandomAlphaNumeric(KID_LENGTH);
        KeyPairGenerator keyGenerator = null;
        try {
            keyGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            // This should not happen, since we hardcoded as "RSA"
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught unexpected exception: " + e.getLocalizedMessage(), e);
            }
            return null;
        }

        keyGenerator.initialize(size);
        KeyPair keypair = keyGenerator.generateKeyPair();

        RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();
        RSAPrivateKey priKey = (RSAPrivateKey) keypair.getPrivate();
        Jose4jRsaJWK jwk = new Jose4jRsaJWK(pubKey);

        jwk.setPrivateKey(priKey);
        jwk.setAlgorithm(alg);
        jwk.setKeyId(kid);
        jwk.setUse((use == null) ? JwkConstants.sig : use);

        return jwk;
    }

    /**
     * re-generate the jwk from the keyObject(JsonObject)
     *
     * @param keyObject
     * @return
     */
    public static Jose4jRsaJWK getInstance(JSONObject keyObject) {
        Map<String, Object> params = new HashMap<String, Object>();
        Set<Entry<String, Object>> entries = keyObject.entrySet();
        for (Entry<String, Object> entry : entries) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Entry key:" + entry.getKey() + " value:" + entry.getValue().toString());
            }
            params.put(entry.getKey(), entry.getValue());
        }

        Jose4jRsaJWK jwk = null;
        try {
            jwk = new Jose4jRsaJWK(params);
        } catch (JoseException e) {
            // TODO error handling
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught unexpected exception: " + e.getLocalizedMessage(), e);
            }
        }
        return jwk;
    }

    // alg, use, publicKey, privateKey
    public static Jose4jRsaJWK getInstance(String alg, String use, PublicKey publicKey, PrivateKey privateKey, String kid) {

        //String kid = RandomUtils.getRandomAlphaNumeric(KID_LENGTH);
        RSAPublicKey pubKey = (RSAPublicKey) publicKey;
        RSAPrivateKey priKey = (RSAPrivateKey) privateKey;
        Jose4jRsaJWK jwk = new Jose4jRsaJWK(pubKey);

        jwk.setPrivateKey(priKey);
        jwk.setAlgorithm(alg);
        jwk.setKeyId(kid);
        jwk.setUse(use == null ? JwkConstants.sig : use);

        return jwk;
    }

    /**
     * @param publicKey
     */
    public Jose4jRsaJWK(RSAPublicKey publicKey) {
        super(publicKey);
    }

    /**
     * @param publicKey
     * @throws JoseException
     */
    public Jose4jRsaJWK(Map<String, Object> params) throws JoseException {
        super(params);
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyID() {
        return getKeyId();
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyX5t() {
        return getX509CertificateSha1Thumbprint(); // only x5t for now
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyUse() {
        return getUse();
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getSharedKey() {
        return null; // no need in jose4j, neither
    }

    /** {@inheritDoc} */
    @Override
    public long getCreated() {
        return created;
    }

    /** {@inheritDoc} */
    @Override
    public void parse() {
        // doing nothing in jose4j
    }

    /** {@inheritDoc} */
    @Override
    public void generateKey() {
        // doing nothing in jose4j
    }

    /** {@inheritDoc} */
    @Override
    public JSONObject getJsonObject() {
        Map<String, Object> params = toParams(OutputControlLevel.INCLUDE_SYMMETRIC);
        JSONObject jsonObject = new JSONObject();

        addProp(jsonObject, params, JwkConstants.kid);
        addProp(jsonObject, params, JwkConstants.use);
        addProp(jsonObject, params, JwkConstants.alg);
        addProp(jsonObject, params, JwkConstants.kty);

        addProp(jsonObject, params, JwkConstants.n);
        addProp(jsonObject, params, JwkConstants.e);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JSON Web Key:", jsonObject);
        }
        return jsonObject;
    }

    //  {
    //    "kty": "RSA",
    //    "alg": "RS256",
    //    "use": "sig",
    //    "kid": "70aae097be0fa2a999852d57a480e4cad6bdb81c",
    //    "n": "1NdMI-DCDkINz0LrnugfpyubBKBniVtzl0RpY4eVq0vxZSZtwXFt1kX5XT6xyiIDAqq7vHzoaByp3gXXWoGEqFHPgk3ssX_goMP1xrj4eFZuyo73NA3gJmCjFKKmemsDhl-mCgp3f3noAPaguMZFQUyoK1UPFfOShTkAOQ6b87XdR_ylhHE2faJZK_L7H3nx-P_PSGC6VYKvlmjuB0qU64oILH2iL5svYq0yz3jxtB7JWknY3KlEHkgBnvcO64gxb4yOSYTqH5oi2za4DmatKPfYvbsJTzg0iGI98xSQkYWtGolwZgpSPdweX5PSTuIYmcRxMHHTlXIz27Tn-eL8XQ==",
    //    "e": "AQAB"
    //   }

    /**
     * @param jsonObject
     * @param params
     * @param kid
     */
    protected void addProp(JSONObject jsonObject, Map<String, Object> params, String key) {
        Object obj = params.get(key);
        if (obj == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key:" + key + " returned null");
            }
            return;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Key:" + key + " json-type:" + obj.toString());
        }
        if (obj instanceof String) {
            jsonObject.put(key, (String) obj);
        } else if (obj instanceof JSONObject) {
            jsonObject.put(key, ((JSONObject) obj).toString());
        } else {
            // TODO error type handling
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Error json type:" + obj.getClass().getName() + " value:" + obj.toString());
            }
            jsonObject.put(key, ((JSONObject) obj).toString());
        }
    }

}
