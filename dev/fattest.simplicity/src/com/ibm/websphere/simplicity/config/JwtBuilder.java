/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>jwtBuilder</li>
 * </ul>
 */
public class JwtBuilder extends ConfigElement {

    private String audiences;
    private String claims;
    private String contentEncryptionAlgorithm;
    private String expiresInSeconds;
    private String expiry;
    private String issuer;
    private Boolean jti;
    private Boolean jwkEnabled;
    private String keyAlias;
    private String keyManagementKeyAlgorithm;
    private String keyManagementKeyAlias;
    private String keyStoreRef;
    private String nbfOffset;
    private String scope;
    private String sharedKey;
    private String signatureAlgorithm;
    private String trustStoreRef;

    /**
     * @return the audiences
     */
    public String getAudiences() {
        return audiences;
    }

    /**
     * @param audiences the audiences to set
     */
    @XmlAttribute(name = "audiences")
    public void setAudiences(String audiences) {
        this.audiences = audiences;
    }

    /**
     * @return the claims
     */
    public String getClaims() {
        return claims;
    }

    /**
     * @param claims the claims to set
     */
    @XmlAttribute(name = "claims")
    public void setClaims(String claims) {
        this.claims = claims;
    }

    /**
     * @return the contentEncryptionAlgorithm
     */
    public String getContentEncryptionAlgorithm() {
        return contentEncryptionAlgorithm;
    }

    /**
     * @param contentEncryptionAlgorithm the contentEncryptionAlgorithm to set
     */
    @XmlAttribute(name = "contentEncryptionAlgorithm")
    public void setContentEncryptionAlgorithm(String contentEncryptionAlgorithm) {
        this.contentEncryptionAlgorithm = contentEncryptionAlgorithm;
    }

    /**
     * @return the expiresInSeconds
     */
    public String getExpiresInSeconds() {
        return expiresInSeconds;
    }

    /**
     * @param expiresInSeconds the expiresInSeconds to set
     */
    @XmlAttribute(name = "expiresInSeconds")
    public void setExpiresInSeconds(String expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    /**
     * @return the expiry
     */
    public String getExpiry() {
        return expiry;
    }

    /**
     * @param expiry the expiry to set
     */
    @XmlAttribute(name = "expiry")
    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    /**
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * @param issuer the issuer to set
     */
    @XmlAttribute(name = "expiry")
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * @return the jti
     */
    public Boolean getJti() {
        return jti;
    }

    /**
     * @param jti the jti to set
     */
    @XmlAttribute(name = "jti")
    public void setJti(Boolean jti) {
        this.jti = jti;
    }

    /**
     * @return the jwkEnabled
     */
    public Boolean getJwkEnabled() {
        return jwkEnabled;
    }

    /**
     * @param jwkEnabled the jwkEnabled to set
     */
    @XmlAttribute(name = "jwkEnabled")
    public void setJwkEnabled(Boolean jwkEnabled) {
        this.jwkEnabled = jwkEnabled;
    }

    /**
     * @return the keyAlias
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * @param keyAlias the keyAlias to set
     */
    @XmlAttribute(name = "keyAlias")
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    /**
     * @return the keyManagementKeyAlgorithm
     */
    public String getKeyManagementKeyAlgorithm() {
        return keyManagementKeyAlgorithm;
    }

    /**
     * @param keyManagementKeyAlgorithm the keyManagementKeyAlgorithm to set
     */
    @XmlAttribute(name = "keyManagementKeyAlgorithm")
    public void setKeyManagementKeyAlgorithm(String keyManagementKeyAlgorithm) {
        this.keyManagementKeyAlgorithm = keyManagementKeyAlgorithm;
    }

    /**
     * @return the keyManagementKeyAlias
     */
    public String getKeyManagementKeyAlias() {
        return keyManagementKeyAlias;
    }

    /**
     * @param keyManagementKeyAlias the keyManagementKeyAlias to set
     */
    @XmlAttribute(name = "keyManagementKeyAlias")
    public void setKeyManagementKeyAlias(String keyManagementKeyAlias) {
        this.keyManagementKeyAlias = keyManagementKeyAlias;
    }

    /**
     * @return the keyStoreRef
     */
    public String getKeyStoreRef() {
        return keyStoreRef;
    }

    /**
     * @param keyStoreRef the keyStoreRef to set
     */
    @XmlAttribute(name = "keyManagementKeyAlias")
    public void setKeyStoreRef(String keyStoreRef) {
        this.keyStoreRef = keyStoreRef;
    }

    /**
     * @return the nbfOffset
     */
    public String getNbfOffset() {
        return nbfOffset;
    }

    /**
     * @param nbfOffset the nbfOffset to set
     */
    @XmlAttribute(name = "nbfOffset")
    public void setNbfOffset(String nbfOffset) {
        this.nbfOffset = nbfOffset;
    }

    /**
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     */
    @XmlAttribute(name = "scope")
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @return the sharedKey
     */
    public String getSharedKey() {
        return sharedKey;
    }

    /**
     * @param sharedKey the sharedKey to set
     */
    @XmlAttribute(name = "sharedKey")
    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    /**
     * @return the signatureAlgorithm
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * @param signatureAlgorithm the signatureAlgorithm to set
     */
    @XmlAttribute(name = "signatureAlgorithm")
    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /**
     * @return the trustStoreRef
     */
    public String getTrustStoreRef() {
        return trustStoreRef;
    }

    /**
     * @param trustStoreRef the trustStoreRef to set
     */
    @XmlAttribute(name = "trustStoreRef")
    public void setTrustStoreRef(String trustStoreRef) {
        this.trustStoreRef = trustStoreRef;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (audiences != null) {
            sb.append("audiences=\"").append(audiences).append("\" ");
        }
        if (claims != null) {
            sb.append("claims=\"").append(claims).append("\" ");
        }
        if (contentEncryptionAlgorithm != null) {
            sb.append("contentEncryptionAlgorithm=\"").append(contentEncryptionAlgorithm).append("\" ");
        }
        if (expiresInSeconds != null) {
            sb.append("expiresInSeconds=\"").append(expiresInSeconds).append("\" ");
        }
        if (expiry != null) {
            sb.append("expiry=\"").append(expiry).append("\" ");
        }
        if (issuer != null) {
            sb.append("issuer=\"").append(issuer).append("\" ");
        }
        if (jti != null) {
            sb.append("jti=\"").append(jti).append("\" ");
        }
        if (jwkEnabled != null) {
            sb.append("jwkEnabled=\"").append(jwkEnabled).append("\" ");
        }
        if (keyAlias != null) {
            sb.append("keyAlias=\"").append(keyAlias).append("\" ");
        }
        if (keyManagementKeyAlgorithm != null) {
            sb.append("keyManagementKeyAlgorithm=\"").append(keyManagementKeyAlgorithm).append("\" ");
        }
        if (keyManagementKeyAlias != null) {
            sb.append("keyManagementKeyAlias=\"").append(keyManagementKeyAlias).append("\" ");
        }
        if (keyStoreRef != null) {
            sb.append("keyStoreRef=\"").append(keyStoreRef).append("\" ");
        }
        if (nbfOffset != null) {
            sb.append("nbfOffset=\"").append(nbfOffset).append("\" ");
        }
        if (scope != null) {
            sb.append("scope=\"").append(scope).append("\" ");
        }
        if (sharedKey != null) {
            sb.append("sharedKey=\"").append(sharedKey).append("\" ");
        }
        if (signatureAlgorithm != null) {
            sb.append("signatureAlgorithm=\"").append(signatureAlgorithm).append("\" ");
        }
        if (trustStoreRef != null) {
            sb.append("trustStoreRef=\"").append(trustStoreRef).append("\" ");
        }

        sb.append("}");

        return sb.toString();
    }
}
