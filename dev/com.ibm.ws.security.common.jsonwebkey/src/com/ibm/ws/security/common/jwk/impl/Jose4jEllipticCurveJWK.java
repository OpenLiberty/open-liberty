/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.common.jwk.impl;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.lang.JoseException;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.constants.TraceConstants;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.common.jwk.internal.JwkConstants;
import com.ibm.ws.security.common.random.RandomUtils;

/**
 */
public class Jose4jEllipticCurveJWK extends EllipticCurveJsonWebKey implements JWK {
    /**  */
    private static final long serialVersionUID = 1787582L;

    private static final TraceComponent tc = Tr.register(Jose4jEllipticCurveJWK.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    protected long created = (new Date()).getTime();

    /**
     * generate a new EC type jwk with the specified parameters
     *
     * @param alg
     * @param use
     * @return
     */
    public static Jose4jEllipticCurveJWK getInstance(String alg, String use) {
        String kid = RandomUtils.getRandomAlphaNumeric(20);
        KeyPair keypair = getKeyPair(alg);
        if (keypair == null) {
            return null;
        }

        ECPublicKey pubKey = (ECPublicKey) keypair.getPublic();
        ECPrivateKey priKey = (ECPrivateKey) keypair.getPrivate();
        Jose4jEllipticCurveJWK jwk = new Jose4jEllipticCurveJWK(pubKey);

        jwk.setPrivateKey(priKey);
        jwk.setAlgorithm(alg);
        jwk.setKeyId(kid);
        jwk.setUse(use == null ? JwkConstants.sig : use);

        return jwk;
    }

    static KeyPair getKeyPair(String alg) {
        KeyPairGenerator keyGenerator = null;
        try {
            keyGenerator = KeyPairGenerator.getInstance("EC");
        } catch (GeneralSecurityException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "hit exception", e);
            }
            return null;
        }
        try {
            final String ecParameterSpecName = getECParameterSpecName(alg);
            if (ecParameterSpecName == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to find a matching ECGenParameterSpec name for algorithm [" + alg + "]");
                }
                return null;
            }
            keyGenerator.initialize(new ECGenParameterSpec(ecParameterSpecName));
        } catch (InvalidAlgorithmParameterException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to initialize the key generator: " + e);
            }
            return null;
        }
        return keyGenerator.generateKeyPair();
    }

    static String getECParameterSpecName(String alg) {
        if (alg.endsWith("256")) {
            return "secp256r1";
        } else if (alg.endsWith("384")) {
            return "secp384r1";
        } else if (alg.endsWith("512")) {
            return "secp521r1";
        }
        return null;
    }

    /**
     * re-generate the EC type jwk through the keyObject(JsonObject)
     *
     * @param keyObject
     * @return
     */
    public static Jose4jEllipticCurveJWK getInstance(JSONObject keyObject) {
        Map<String, Object> params = new HashMap<String, Object>();
        Set<Entry<String, Object>> entries = keyObject.entrySet();
        for (Entry<String, Object> entry : entries) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Entry Key:" + entry.getKey() + " value:" + entry.getValue().toString());
            }
            params.put(entry.getKey(), entry.getValue().toString());
        }

        Jose4jEllipticCurveJWK jwk = null;
        try {
            jwk = new Jose4jEllipticCurveJWK(params);
        } catch (JoseException e) {
            // TODO error handling
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "hit exception", e);
            }
        }
        return jwk;
    }

    /**
     * @param publicKey
     */
    public Jose4jEllipticCurveJWK(ECPublicKey publicKey) {
        super(publicKey);
    }

    /**
     * @param publicKey
     * @throws JoseException
     */
    public Jose4jEllipticCurveJWK(Map<String, Object> params) throws JoseException {
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
        //return getX509CertificateSha1Thumbprint(); // only x5t for now
        return null;
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

    //{
    //    "kty": "EC",
    //    "kid": "jwt_ec384_signer",
    //    "use": "sig",
    //    "x": "OhrGpYrRC_u6eDXUYosmfmM1sWF8zkDgifztbJ694bVu3nUV6xY7jWZhWhUk0Kfe",
    //    "y": "1zSvB8He-QmBhqXlQ1FYUQyrFTDjDfSFj52StSJNFXbH_B2v8frhmNVv5UOaLBAM",
    //    "crv": "P-384"
    //}

    /** {@inheritDoc} */
    @Override
    public JSONObject getJsonObject() {
        Map<String, Object> params = toParams(OutputControlLevel.INCLUDE_SYMMETRIC);
        JSONObject jsonObject = new JSONObject();

        addProp(jsonObject, params, JwkConstants.kid);
        addProp(jsonObject, params, JwkConstants.use);
        addProp(jsonObject, params, JwkConstants.alg);
        addProp(jsonObject, params, JwkConstants.kty);

        addProp(jsonObject, params, JwkConstants.x);
        addProp(jsonObject, params, JwkConstants.y);
        addProp(jsonObject, params, JwkConstants.crv);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JSON Web Key(EC):", jsonObject);
        }
        return jsonObject;
    }

    /**
     * @param jsonObject
     * @param params
     * @param kid
     */
    protected void addProp(JSONObject jsonObject, Map<String, Object> params, String key) {
        Object obj = params.get(key);
        if (obj != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key:" + key + " json-type:" + obj.toString());
            }
            if (obj instanceof String) {
                jsonObject.put(key, (String) obj);
            } else if (obj instanceof JSONObject) {
                jsonObject.put(key, ((JSONObject) obj).toString());
            } else {
                // TODO error type handling
                //
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error json type:" + obj.getClass().getName() + " value:" + obj.toString());
                }
                jsonObject.put(key, ((JSONObject) obj).toString());
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key:" + key + " return null");
            }
        }
    }

}
