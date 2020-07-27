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

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.constants.TraceConstants;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.common.jwk.internal.JwkConstants;
import com.ibm.ws.security.common.random.RandomUtils;

/**
 * TODO: This should replace com.ibm.ws.security.openidconnect.jwk.JWKImpl
 */
public class JWKImpl implements JWK {

    private static final TraceComponent tc = Tr.register(JWKImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected String kid = null;
    protected String x5t = null;
    protected String alg = null;
    protected String use = null;
    protected String kty = null;
    protected int size = 2048;
    protected PublicKey pubKey = null;
    protected PrivateKey priKey = null;
    protected byte[] sharedKey = null;
    protected JSONObject keyObject = new JSONObject();
    protected long created = (new Date()).getTime();

    protected int kidLength = 20;

    public JWKImpl(int size, String alg, String use, String type) {
        generateCommon();
        this.alg = alg;
        this.use = use;
        this.kty = type;
    }

    public JWKImpl(JSONObject keyObject) {
        this.keyObject = keyObject;
    }

    public void generateKey() {

    }

    public void generateCommon() {
        this.kid = RandomUtils.getRandomAlphaNumeric(kidLength);
        this.created = (new Date()).getTime();

    }

    public void parse() {
        parse(this.keyObject);
    }

    protected void parse(JSONObject keyObject) {

        JSONObject jkid = (JSONObject) keyObject.get("kid");
        JSONObject jx5t = (JSONObject) keyObject.get("x5t");
        JSONObject jalg = (JSONObject) keyObject.get("alg");
        JSONObject jkty = (JSONObject) keyObject.get("kty");
        JSONObject juse = (JSONObject) keyObject.get("use");

        if (jkid != null) {
            kid = jkid.toString();
        }
        if (jx5t != null) {
            x5t = jx5t.toString();
        }
        if (jalg != null) {
            alg = jalg.toString();
        }
        if (juse != null) {
            use = juse.toString();
        }
        if (jkty != null) {
            kty = jkty.toString();
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "KeyType=" + kty);
            Tr.debug(tc, "Algorithm=" + alg);
            Tr.debug(tc, "KeyID=" + kid);
            Tr.debug(tc, "KeyThumprint=" + x5t);
        }
        if ("sig".equals(use)) {
            buildPublicKey(keyObject, kty);
        }
    }

    protected void buildPublicKey(JSONObject keyObject, String kty) {

        if ("RSA".equals(kty)) {
            buildRSAPublicKey(keyObject);
        }
    }

    protected void buildRSAPublicKey(JSONObject keyObject) {
        JSONObject jModule = (JSONObject) keyObject.get("n");
        JSONObject jExponent = (JSONObject) keyObject.get("e");
        String sModule = null;
        String sExponent = null;
        if (jModule != null && jExponent != null) {
            sModule = jModule.toString();
            sExponent = jExponent.toString();
            byte[] modulusBytes = Base64.decodeBase64(sModule);
            byte[] exponentBytes = Base64.decodeBase64(sExponent);

            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(exponentBytes);
            try {
                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                KeyFactory factory = KeyFactory.getInstance("RSA");
                pubKey = factory.generatePublic(spec);
            } catch (InvalidKeySpecException ikse) {
                //do nothing
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid Key=" + ikse);
                }

            } catch (NoSuchAlgorithmException nsae) {
                //do nothing
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invald Algorithm=" + nsae);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyID() {
        return this.kid;
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyX5t() {
        return this.x5t;
    }

    /** {@inheritDoc} */
    @Override
    public String getAlgorithm() {
        return this.alg;
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyUse() {
        return this.use;
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyType() {
        return this.kty;
    }

    /** {@inheritDoc} */
    @Override
    public PublicKey getPublicKey() {
        return this.pubKey;
    }

    /** {@inheritDoc} */
    @Override
    public PrivateKey getPrivateKey() {
        return this.priKey;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getSharedKey() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public long getCreated() {
        return this.created;
    }

    public void setKidLength(int length) {
        kidLength = length;
    }

    public int getKidLength() {
        return kidLength;
    }

    public void toJsonObject() {
        this.keyObject.put(JwkConstants.kid, this.kid);
        this.keyObject.put(JwkConstants.use, JwkConstants.sig);
    }

    public JSONObject getJsonObject() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JSON Web Key:", this.keyObject);
        }
        return this.keyObject;
    }

}
