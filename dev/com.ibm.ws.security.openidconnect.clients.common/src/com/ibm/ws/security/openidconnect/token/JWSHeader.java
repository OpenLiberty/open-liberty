/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import java.util.List;

public class JWSHeader extends JWTHeader {
    private static final long serialVersionUID = 4531593432619777120L;

    public JWSHeader() {}

    /**
     * 
     * Header as specified in <a
     * href="http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-11#section-4.1">Reserved
     * Header Parameter Names</a>.
     */
    /**
     * Algorithm header parameter that identifies the cryptographic algorithm used to secure the JWS
     * or {@code null} for none.
     */
    //("alg")
    private String algorithm;

    /**
     * JSON Web Key URL header parameter that is an absolute URL that refers to a resource for a set
     * of JSON-encoded public keys, one of which corresponds to the key that was used to digitally
     * sign the JWS or {@code null} for none.
     */
    //("jku")
    private String jwkUrl;

    /**
     * JSON Web Key header parameter that is a public key that corresponds to the key used to
     * digitally sign the JWS or {@code null} for none.
     */
    //("jwk")
    private String jwk;

    /**
     * Key ID header parameter that is a hint indicating which specific key owned by the signer
     * should be used to validate the digital signature or {@code null} for none.
     */
    //("kid")
    private String keyId;

    /**
     * X.509 URL header parameter that is an absolute URL that refers to a resource for the X.509
     * public key certificate or certificate chain corresponding to the key used to digitally sign
     * the JWS or {@code null} for none.
     */
    //("x5u")
    private String x509Url;

    /**
     * X.509 certificate thumbprint header parameter that provides a base64url encoded SHA-1
     * thumbprint (a.k.a. digest) of the DER encoding of an X.509 certificate that can be used to
     * match the certificate or {@code null} for none.
     */
    //("x5t")
    private String x509Thumbprint;

    /**
     * X.509 certificate chain header parameter contains the X.509 public key certificate or
     * certificate chain corresponding to the key used to digitally sign the JWS or {@code null} for
     * none.
     */
    //("x5c")
    private String x509Certificate;

    /**
     * Array listing the header parameter names that define extensions that are used in the JWS
     * header that MUST be understood and processed or {@code null} for none.
     */
    //("crit")
    private List<String> critical;

    @Override
    public JWSHeader setType(String type) {
        super.setType(type);
        return this;
    }

    /**
     * Returns the algorithm header parameter that identifies the cryptographic algorithm used to
     * secure the JWS or {@code null} for none.
     */
    public final String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the algorithm header parameter that identifies the cryptographic algorithm used to
     * secure the JWS or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        this.put(HeaderConstants.ALGORITHM, algorithm);
        return this;
    }

    /**
     * Returns the JSON Web Key URL header parameter that is an absolute URL that refers to a
     * resource for a set of JSON-encoded public keys, one of which corresponds to the key that was
     * used to digitally sign the JWS or {@code null} for none.
     */
    public final String getJwkUrl() {
        return jwkUrl;
    }

    /**
     * Sets the JSON Web Key URL header parameter that is an absolute URL that refers to a resource
     * for a set of JSON-encoded public keys, one of which corresponds to the key that was used to
     * digitally sign the JWS or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setJwkUrl(String jwkUrl) {
        this.jwkUrl = jwkUrl;
        this.put(HeaderConstants.JWK_URL, jwkUrl);
        return this;
    }

    /**
     * Returns the JSON Web Key header parameter that is a public key that corresponds to the key
     * used to digitally sign the JWS or {@code null} for none.
     */
    public final String getJwk() {
        return jwk;
    }

    /**
     * Sets the JSON Web Key header parameter that is a public key that corresponds to the key used
     * to digitally sign the JWS or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setJwk(String jwk) {
        this.jwk = jwk;
        this.put(HeaderConstants.JWK, jwk);
        return this;
    }

    /**
     * Returns the key ID header parameter that is a hint indicating which specific key owned by the
     * signer should be used to validate the digital signature or {@code null} for none.
     */
    public final String getKeyId() {
        return keyId;
    }

    /**
     * Sets the key ID header parameter that is a hint indicating which specific key owned by the
     * signer should be used to validate the digital signature or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setKeyId(String keyId) {
        this.keyId = keyId;
        this.put(HeaderConstants.KEY_ID, keyId);
        return this;
    }

    /**
     * Returns the X.509 URL header parameter that is an absolute URL that refers to a resource for
     * the X.509 public key certificate or certificate chain corresponding to the key used to
     * digitally sign the JWS or {@code null} for none.
     */
    public final String getX509Url() {
        return x509Url;
    }

    /**
     * Sets the X.509 URL header parameter that is an absolute URL that refers to a resource for the
     * X.509 public key certificate or certificate chain corresponding to the key used to digitally
     * sign the JWS or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setX509Url(String x509Url) {
        this.x509Url = x509Url;
        this.put(HeaderConstants.X509_URL, x509Url);
        return this;
    }

    /**
     * Returns the X.509 certificate thumbprint header parameter that provides a base64url encoded
     * SHA-1 thumbprint (a.k.a. digest) of the DER encoding of an X.509 certificate that can be used
     * to match the certificate or {@code null} for none.
     */
    public final String getX509Thumbprint() {
        return x509Thumbprint;
    }

    /**
     * Sets the X.509 certificate thumbprint header parameter that provides a base64url encoded
     * SHA-1 thumbprint (a.k.a. digest) of the DER encoding of an X.509 certificate that can be used
     * to match the certificate or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setX509Thumbprint(String x509Thumbprint) {
        this.x509Thumbprint = x509Thumbprint;
        this.put(HeaderConstants.X509_TP, x509Thumbprint);
        return this;
    }

    /**
     * Returns the X.509 certificate chain header parameter contains the X.509 public key
     * certificate or certificate chain corresponding to the key used to digitally sign the JWS or {@code null} for none.
     */
    public final String getX509Certificate() {
        return x509Certificate;
    }

    /**
     * Sets the X.509 certificate chain header parameter contains the X.509 public key certificate
     * or certificate chain corresponding to the key used to digitally sign the JWS or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader setX509Certificate(String x509Certificate) {
        this.x509Certificate = x509Certificate;
        this.put(HeaderConstants.X509_CERT, x509Certificate);
        return this;
    }

    /**
     * Returns the array listing the header parameter names that define extensions that are used in
     * the JWS header that MUST be understood and processed or {@code null} for none.
     * 
     * 
     */
    public final List<String> getCritical() {
        return critical;
    }

    /**
     * Sets the array listing the header parameter names that define extensions that are used in the
     * JWS header that MUST be understood and processed or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     * 
     * 
     */
    public JWSHeader setCritical(List<String> critical) {
        this.critical = critical;
        this.put(HeaderConstants.CRITICAL, critical);
        return this;
    }

    @Override
    public JWSHeader clone() throws CloneNotSupportedException {
        return (JWSHeader) super.clone();
    }

    @Override
    public JWSHeader getHeader() {
        return (JWSHeader) super.getHeader();
    }
}
