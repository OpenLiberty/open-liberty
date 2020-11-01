/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.jose4j.base64url.SimplePEMEncoder;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Convenience class to build jwt tokens to test consumption of them by WebSphere.
 * Uses a constant public and private key, so the public key only has to
 * be added to WebSphere's trust store once.
 *
 * Another way to get these tokens would have been to define a bunch of jwtbuilders
 * on the OP and call their "token" endpoints.
 *
 * To add the public key to a websphere trust store, proceed as follows:
 * copy the output of getPrivateKeyPem() to privatekey.pem.
 * openssl req -x509 -key privateKey.pem -nodes -days 3650 -newkey rsa:2048 -out temp.pem
 * openssl x509 -outform der -in temp.pem -out temp.der
 * then use ikeyman or keytool to add signer temp.der to key.jks, perhaps like this:
 * keytool -importcert \
 * -file <certificate to trust> \
 * -alias <alias for the certificate> \
 * -keystore <name of the trustore> \
 * -storepass <password for the truststore> \
 * -storetype jks
 *
 * @author bruce
 *
 */
public class JWTTokenBuilder {

    protected static Class<?> thisClass = JWTTokenBuilder.class;
    JwtClaims _claims = null;
    JsonWebSignature _jws = null;
    JsonWebEncryption _jwe = null;
    RsaJsonWebKey _rsajwk = null;
    String _jwt = null;
//    private final Key _signingKey = null;
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";
    private static final String BEGIN_PRIV_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIV_KEY = "-----END PRIVATE KEY-----";
//    private static final String pubKey = "-----BEGIN PUBLIC KEY-----\n" +
//                                         "<value>\n" +
//                                         "-----END PUBLIC KEY-----";

//    private static final String privKey = "-----BEGIN PRIVATE KEY-----\n" +
//                                          "<value>\n" +
//                                          "-----END PRIVATE KEY-----";

    public JWTTokenBuilder() {
        _claims = new JwtClaims();
        _jws = new JsonWebSignature();
        _jwe = new JsonWebEncryption();

        try {
            _rsajwk = RsaJwkGenerator.generateJwk(2048); // this generates new pub and private key pair but we will replace them.
            _rsajwk.setKeyId("keyid");
            _jws.setKeyIdHeaderValue(_rsajwk.getKeyId());
//            _jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
//            // The JWT is signed using the private key
//            _jws.setKey(this.fromPemEncoded(privKey)); // replace the private key so we can use same public key every time.
//            //_jws.setHeader("typ","JWT");  // not sure if we should do this or let twas figure it out.
//            System.out.println("jws key: " + _jws.getKey());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

//    private static String pemEncode(Key publicKey) {
//        byte[] encoded = publicKey.getEncoded(); // X509 SPKI
//        return BEGIN_PUBLIC_KEY + "\r\n" + SimplePEMEncoder.encode(encoded) + "\r\n" + END_PUBLIC_KEY;
//    }

//    public String getPublicKeyPem() {
//        //return RsaKeyUtil.pemEncode(_rsajwk.getPublicKey());
//        return pubKey;
//    }

//    public String getPrivateKeyPem() {
//        //byte[] encoded = _rsajwk.getPrivateKey().getEncoded(); // X509 SPKI
//        //return BEGIN_PUBLIC_KEY + "\r\n" + SimplePEMEncoder.encode(encoded) + END_PUBLIC_KEY;
//        return privKey;
//    }

    public String readKeyFromFile(String theFile) throws Exception {
        return new String(Files.readAllBytes(Paths.get(theFile)));
    }

    private PrivateKey fromPemEncoded(String pem) throws JoseException, InvalidKeySpecException, NoSuchAlgorithmException {

        String thisMethod = "fromPemEncoded";
        int beginIndex = pem.indexOf(BEGIN_PRIV_KEY) + BEGIN_PRIV_KEY.length();
        int endIndex = pem.indexOf(END_PRIV_KEY);
        String base64 = pem.substring(beginIndex, endIndex).trim();
        Log.info(thisClass, thisMethod, "base64: " + base64 + " end");
        byte[] decode = SimplePEMEncoder.decode(base64);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public JWTTokenBuilder setIssuer(String in) {
        _claims.setIssuer(in);
        return this;
    }

    public JWTTokenBuilder setAudience(String in) {
        _claims.setAudience(in);
        return this;
    }

    public JWTTokenBuilder setAudience(String... in) {
        _claims.setAudience(in);
        return this;
    }

    public JWTTokenBuilder setExpirationTimeMinutesIntheFuture(int in) {
        _claims.setExpirationTimeMinutesInTheFuture(in);
        return this;
    }

    public JWTTokenBuilder setExpirationTimeSecondsFromNow(int in) {
        NumericDate exp = NumericDate.now();
        exp.addSeconds(in);
        _claims.setExpirationTime(exp);
        return this;
    }

    public JWTTokenBuilder setExpirationTime(NumericDate exp) {
        _claims.setExpirationTime(exp);
        return this;
    }

    public JWTTokenBuilder setGeneratedJwtId() {
        _claims.setGeneratedJwtId();
        return this;
    }

    public JWTTokenBuilder setJwtId(String in) {
        _claims.setJwtId(in);
        return this;
    }

    public JWTTokenBuilder setIssuedAtToNow() {
        _claims.setIssuedAtToNow();
        return this;
    }

    public JWTTokenBuilder setIssuedAt(NumericDate in) {
        _claims.setIssuedAt(in);
        return this;
    }

    public JWTTokenBuilder setNotBeforeMinutesInThePast(int in) {
        _claims.setNotBeforeMinutesInThePast(in);
        return this;
    }

    public JWTTokenBuilder setNotBefore(NumericDate in) {
        _claims.setNotBefore(in);
        return this;
    }

    public JWTTokenBuilder setSubject(String in) {

        _claims.setSubject(in);
        return this;
    }

    public JWTTokenBuilder setScope(String in) {

        _claims.setClaim(PayloadConstants.SCOPE, in);
        return this;
    }

    public JWTTokenBuilder setRealmName(String in) {

        _claims.setClaim(PayloadConstants.REALM_NAME, in);
        return this;
    }

    public JWTTokenBuilder setTokenType(String in) {

        _claims.setClaim(PayloadConstants.TOKEN_TYPE, in);
        return this;
    }
//    public JWTTokenBuilder setScope(String... in) {
//
//        _claims.setStringListClaim(ClaimConstants.SCOPE, in);
//        return this;
//    }

    public JWTTokenBuilder setClaim(String name, Object val) {
        _claims.setClaim(name, val);
        return this;
    }

    public JWTTokenBuilder unsetClaim(String name) {
        _claims.unsetClaim(name);
        return this;
    }

    // todo: groups?

    public JWTTokenBuilder setKey(Key key) {
        _jws.setKey(key);
        return this;
    }

    public JWTTokenBuilder setKeyIdHeaderValue(String id) {
        _jws.setKeyIdHeaderValue(id);
        return this;
    }

    public JWTTokenBuilder setHSAKey(String keyId) {
        try {
            _jws.setKey(new HmacKey(keyId.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            _jws.setKey(null);
        }
        return this;
    }

//    public JWTTokenBuilder setRSAKey() {
//        try {
//            _jws.setKey(this.fromPemEncoded(privKey));
//        } catch (Exception e) {
//            e.printStackTrace(System.out);
//            _jws.setKey(null);
//        }
//        return this;
//    }

    public JWTTokenBuilder setRSAKey(String keyFile) {

        String thisMethod = "setRSAKey";
        try {
            if (keyFile == null) {
//                return setRSAKey();
                throw new IOException("Can not load a key file that was not specified...");
            }
            String key = readKeyFromFile(keyFile);
            Log.info(thisClass, thisMethod, "Read from file: " + keyFile + " key is: " + key);
            _jws.setKey(this.fromPemEncoded(key));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            _jws.setKey(null);
        }
        return this;
    }

    public JWTTokenBuilder setAlorithmHeaderValue(String alg) {
        _jws.setAlgorithmHeaderValue(alg);
        return this;
    }

    public JWTTokenBuilder setKeyId(String kid) {
        _jws.setKeyIdHeaderValue(kid);
        return this;
    }

    public JWTTokenBuilder setKeyManagementKey(Key key) {
        _jwe.setKey(key);
        return this;
    }

    public JWTTokenBuilder setContentEncryptionAlg(String alg) {
        _jwe.setEncryptionMethodHeaderParameter(alg);
        return this;
    }

    public JWTTokenBuilder setKeyManagementKeyAlg(String alg) {
        _jwe.setAlgorithmHeaderValue(alg);
        return this;
    }

    public JWTTokenBuilder setPayload(String payload) {
        _jwe.setPayload(payload);
        return this;
    }
    
    // does not currently support building JWE's 
    // The tests have been using the built in builder (with encryptWith)
    // to generate encrypted tokens
    // Use apps such as JwtBuilderSetApisClient and JwtBuilderServlet
    public String build() {
        String thisMethod = "build";

        try {
            if (_claims.getIssuedAt() == null) {
                _claims.setIssuedAtToNow();
            }
        } catch (MalformedClaimException e1) {
            e1.printStackTrace(System.out);
        }
        try {
            _jws.setPayload(_claims.toJson());
            Log.info(thisClass, thisMethod, "after setPayload");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
        // key may already have been set with setkey
        // kidheadervalue may have already been set
        // algoheadervalue may have already been set
        try {
            Log.info(thisClass, thisMethod, "jwt: " + _jwt);
            Log.info(thisClass, thisMethod, "jws: " + _jws);
            Log.info(thisClass, thisMethod, "jws: " + _jws.getKey());
//            _jws.setKey(this.fromPemEncoded(privKey));
            _jwt = _jws.getCompactSerialization();
            Log.info(thisClass, thisMethod, "after compact");
            return _jwt;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }

    }

    // builds a JWE with a simple payload
    // caller needs to have already used the set methods to
    // set:  keyManagementKeyAlgorithm, keyManagementKeyAlias,
    // contentEncryptionAlgorithm and payload 
    public String buildAlternateJWE() {
        String thisMethod = "build";

        try {
            _jwe.setHeader("typ", "JOSE");
            _jwe.setHeader("cty", "jwt");
            _jwt = _jwe.getCompactSerialization();
            Log.info(thisClass, thisMethod, "after compact");
            return _jwt;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }

    }

    public String getJsonClaims() {
        return _claims.toJson();
    }

    public String getJwt() {
        return _jwt;
    }

    public JwtClaims getRawClaims() {
        return _claims;
    }
}
