/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.common.jwk.impl;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
import com.ibm.ws.security.common.jwk.constants.TraceConstants;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

/**
 * TODO: This should replace com.ibm.ws.security.openidconnect.server.internal.JWKProvider
 */
public class JWKProvider {
    private static final TraceComponent tc = Tr.register(JWKProvider.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected List<JWK> jwks = Collections.synchronizedList(new ArrayList<JWK>());

    public static final String RSA = "RSA";
    public static final String RS256 = "RS256";
    public static final String HS256 = "HS256";

    private static final int DEFAULT_KEY_SIZE = 2048;
    private static final long DEFAULT_ROTATION_TIME = 12 * 60 * 60 * 1000; //12 hours
    private static final int DEFAULT_MAX_KEYS = 2;

    protected String alg = null;
    protected String use = null;
    protected int size = DEFAULT_KEY_SIZE;
    protected Timer timer;
    protected long rotationTimeInMilliseconds = DEFAULT_ROTATION_TIME;
    protected int maxKeys = DEFAULT_MAX_KEYS;

    protected PublicKey publicKey = null;
    protected PrivateKey privateKey = null;

    protected String publicKeyKid = null;

    protected JWKProvider() {
        this(DEFAULT_KEY_SIZE, RS256, DEFAULT_ROTATION_TIME);
    }

    public JWKProvider(int keySize, String alg, long rotationTimeMs) {
        this(keySize, alg, rotationTimeMs, DEFAULT_MAX_KEYS);
    }

    public JWKProvider(int keySize, String alg, long rotationTimeMs, int maxKeys) {
        if (keySize < 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Specified key size " + keySize + " < 0. Setting key size to the default (" + DEFAULT_KEY_SIZE + ") instead");
            }
            keySize = DEFAULT_KEY_SIZE;
        }
        this.size = keySize;
        this.alg = alg;
        if (rotationTimeMs < 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Specified rotation time " + rotationTimeMs + " < 0. Setting rotation time to the default (" + DEFAULT_ROTATION_TIME + " ms) instead");
            }
            rotationTimeMs = DEFAULT_ROTATION_TIME;
        }
        this.rotationTimeInMilliseconds = rotationTimeMs;
        if (maxKeys < 1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Specified max keys " + maxKeys + " < 1. Setting max keys to the default (" + DEFAULT_MAX_KEYS + ") instead");
            }
            maxKeys = DEFAULT_MAX_KEYS;
        }
        this.maxKeys = maxKeys;

        // A rotation time of 0ms means do not rotate
        if (rotationTimeInMilliseconds != 0) {
            scheduleRotationTask();
        }
    }

    public JWKProvider(int keySize, String alg, long rotationTimeMs, PublicKey publicKey, PrivateKey privateKey) {
        if (keySize < 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Specified key size " + keySize + " < 0. Setting key size to the default (" + DEFAULT_KEY_SIZE + ") instead");
            }
            keySize = DEFAULT_KEY_SIZE;
        }
        this.size = keySize;
        this.alg = alg;
        if (rotationTimeMs < 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Specified rotation time " + rotationTimeMs + " < 0. Setting rotation time to the default (" + DEFAULT_ROTATION_TIME + " ms) instead");
            }
            rotationTimeMs = DEFAULT_ROTATION_TIME;
        }
        this.rotationTimeInMilliseconds = rotationTimeMs;
        this.maxKeys = 1; // There is only one key when a keystore is used for the JWKs

        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.publicKeyKid = buildKidFromPublicKey(this.publicKey);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "kid = " + this.publicKeyKid);
        }
    }

    private String buildKidFromPublicKey(PublicKey cert) {
        JwkKidBuilder kidbuilder = new JwkKidBuilder();
        return kidbuilder.buildKeyId(cert);
    }

    public JSONWebKey getJWK() {
        JWK jwk = null;
        if (jwks.isEmpty()) {
            generateJWKs();
        }
        jwk = jwks.get(jwks.size() - 1);
        return jwk;
    }

    protected void generateJWKs() {
        JWK jwk = null;
        if (jwks.isEmpty()) {
            jwk = generateJWK(alg, size);
            jwks.add(jwk);
        }
    }

    protected JWK generateJWK(String alg, int size) {
        JWK jwk = null;
        if (isValidJwkAlgorithm(alg)) {
            if (publicKey != null && privateKey != null) {
                jwk = generateJwkForValidAlgorithmWithExistingKeys(alg, size, publicKey, privateKey);
            } else {
                jwk = generateJwkForValidAlgorithm(alg, size);
            }
        }
        return jwk;
    }

    boolean isValidJwkAlgorithm(String alg) {
        return KeyAlgorithmChecker.isRSAlgorithm(alg) || KeyAlgorithmChecker.isESAlgorithm(alg);
    }

    JWK generateJwkForValidAlgorithmWithExistingKeys(String alg, int size, PublicKey publicKey, PrivateKey privateKey) {
        JWK jwk = null;
        if (KeyAlgorithmChecker.isRSAlgorithm(alg)) {
            jwk = generateRsaJwkWithExistingKeys(alg, publicKey, privateKey);
        } else if (KeyAlgorithmChecker.isESAlgorithm(alg)) {
            jwk = generateEcJwkWithExistingKeys(alg, publicKey, privateKey);
        }
        if (jwk != null) {
            jwk.generateKey();
        }
        return jwk;
    }

    JWK generateRsaJwkWithExistingKeys(String alg, PublicKey publicKey, PrivateKey privateKey) {
        return Jose4jRsaJWK.getInstance(alg, use, publicKey, privateKey, publicKeyKid);
    }

    JWK generateEcJwkWithExistingKeys(String alg, PublicKey publicKey, PrivateKey privateKey) {
        Jose4jEllipticCurveJWK jwk = null;
        if (publicKey instanceof ECPublicKey) {
            jwk = Jose4jEllipticCurveJWK.getInstance((ECPublicKey) publicKey, alg, null);
            jwk.setPrivateKey(privateKey);
            jwk.setKeyId(publicKeyKid);
        }
        return jwk;
    }

    JWK generateJwkForValidAlgorithm(String alg, int size) {
        JWK jwk = null;
        if (KeyAlgorithmChecker.isRSAlgorithm(alg)) {
            jwk = generateRsaJWK(alg, size);
        } else if (KeyAlgorithmChecker.isESAlgorithm(alg)) {
            jwk = generateEcJwk(alg);
        }
        return jwk;
    }

    protected JWK generateRsaJWK(String alg, int size) {
        JWK jwk = Jose4jRsaJWK.getInstance(size, alg, null, RSA);
        jwk.generateKey();

        return jwk;
    }

    protected JWK generateEcJwk(String alg) {
        JWK jwk = Jose4jEllipticCurveJWK.getInstance(alg, null);
        jwk.generateKey();
        return jwk;
    }

    public String getJwkSetString() {
        if (jwks.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generate initial JWK");
            }
            generateJWKs();
        }
        JWKS obj = new JWKS();

        // convert java object to JSON format, and returned as JSON formatted string
        return obj.toString();
    }

    protected void scheduleRotationTask() {
        RotationTask rotationTask = new RotationTask();
        timer = new Timer(true);

        timer.schedule(rotationTask, rotationTimeInMilliseconds, rotationTimeInMilliseconds);
    }

    protected void rotateKeys() {
        JWK newJwk = generateJWK(alg, size);
        jwks.add(newJwk);
        if (jwks.size() > maxKeys) {
            jwks.remove(0);
        }
    }

    protected class RotationTask extends TimerTask {
        /** {@inheritDoc} */
        @Override
        public void run() {
            rotateKeys();
        }
    }

    protected class JWKS {
        protected JWKS() {
            Iterator<JWK> it = jwks.iterator();

            while (it.hasNext()) {
                JWK jwk = it.next();
                //keys.add(( new Gson().toJson(jwk.getJsonObject())));
                JSONObject jsonKey = jwk != null ? jwk.getJsonObject() : (JSONObject) null;
                keys.add(jsonKey);
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{\"keys\":[");
            sb.append(getKeysString());
            sb.append("]}");
            return sb.toString();
        }

        private String getKeysString() {
            if (keys == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            Iterator<JSONObject> iter = keys.iterator();
            while (iter.hasNext()) {
                JSONObject entry = iter.next();
                if (entry == null) {
                    sb.append("null");
                } else {
                    sb.append(entry.toString());
                }
                if (iter.hasNext()) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        private List<JSONObject> keys = new ArrayList<JSONObject>();

    }
}
