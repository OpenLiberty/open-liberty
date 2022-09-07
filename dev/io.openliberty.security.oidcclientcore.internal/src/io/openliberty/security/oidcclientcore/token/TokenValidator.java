/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.util.List;


import org.jose4j.jwt.NumericDate;

import com.ibm.websphere.ras.Tr;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;

/**
 *
 */
public class TokenValidator {

    private String issuer;
    private String subject = null;
    private List<String> audiences;
    private String azp = null;
    private NumericDate iat;
    private NumericDate exp;
    private NumericDate notBefore = null;
    private OidcClientConfig oidcConfig;
    
    private long clockSkewInSeconds = 120; // default value in seconds
    

    /**
     * @param clientConfig
     */
    public TokenValidator(OidcClientConfig clientConfig) {
        this.oidcConfig = clientConfig;
    }

    /**
     * @param issuer
     * @return
     */
    public TokenValidator issuer(String issuer) {
        this.issuer= issuer;
        return this;
    }

    /**
     * @param subject
     */
    public TokenValidator subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * @param audience
     */
    public TokenValidator audiences(List<String> audience) {
        this.audiences = audiences;
        return this;
    }

    /**
     * @param claimValue
     */
    public TokenValidator azp(String azp) {
        this.azp = azp;
        return this;     
    }

    /**
     * @param issuedAt
     */
    public TokenValidator iat(NumericDate iat) {
        this.iat = iat;
        return this;
    }

    /**
     * @param expirationTime
     * @return 
     */
    public TokenValidator exp(NumericDate exp) {
        this.exp = exp;
        return this;
    }

    /**
     * @param notBefore
     * @return 
     */
    public TokenValidator nbf(NumericDate notBefore) {
        this.notBefore = notBefore;
        return this;
    }

    /**
     * 
     */
    public void validate() throws TokenValidationException {
        validateIssuer();
        validateSubject();
        validateAudiences();
        validateAZP();
        validateExpiration();
        validateIssuedAt();
        validateNotBefore();
        
    }

    /**
     * 
     */
    protected void validateNotBefore() throws TokenValidationException {
        if (this.notBefore != null) {
            long now = System.currentTimeMillis();
            if (now + (this.clockSkewInSeconds * 1000) < this.iat.getValueInMillis()) {
                throw new TokenValidationException(oidcConfig.getClientId(), "nbf claim must be in past");
            }
        } 
    }

    /**
     * 
     */
    protected void validateIssuedAt() throws TokenValidationException {
        long now = System.currentTimeMillis();
        if (now + (this.clockSkewInSeconds * 1000) < this.iat.getValueInMillis()) {
            throw new TokenValidationException(oidcConfig.getClientId(), "iat claim must be in past");
        }
    }

    /**
     * @throws TokenValidationException 
     * 
     */
    protected void validateExpiration() throws TokenValidationException {
        long now = System.currentTimeMillis();
        if (now - (this.clockSkewInSeconds * 1000) > this.exp.getValueInMillis()) {
            throw new TokenValidationException(oidcConfig.getClientId(), "exp claim must be in future");
        }        
    }

    /**
     * 
     */
    protected void validateAZP() throws TokenValidationException {
        if (this.azp != null) {
            if (!(oidcConfig.getClientId().equalsIgnoreCase(this.azp))) {
                throw new TokenValidationException(oidcConfig.getClientId(), "azp is [ " + this.azp + " ], expecting  [ " + oidcConfig.getClientId() + " ]");
            }
        } 
    }

    /**
     * @throws TokenValidationException 
     * 
     */
    protected void validateAudiences() throws TokenValidationException {
        if (this.audiences != null && !(this.audiences.isEmpty())) {
            if (this.audiences.size() == 1) {
                // validate aud claim against client id
                if (!(oidcConfig.getClientId().equalsIgnoreCase(this.audiences.get(0)))) {
                    throw new TokenValidationException(oidcConfig.getClientId(), "audience is [ " + this.audiences.get(0) + " ], expecting [ " + oidcConfig.getClientId() + " ]");
                }
            } else if (this.azp == null) {
                // if more than one audience, then azp claim is a must     
                throw new TokenValidationException(oidcConfig.getClientId(), "multiple audiences present [ " + this.audiences(audiences).toString() + " ], but required azp claim is missing");
            }
        }  
    }

    /**
     * @throws TokenValidationException 
     * 
     */
    protected void validateSubject() throws TokenValidationException {
        if (this.subject != null) {
            if (this.subject.isEmpty()) {
                throw new TokenValidationException(oidcConfig.getClientId(), "subject claim is present but empty");
            }
        }      
    }

    /**
     * @throws TokenValidationException 
     * 
     */
    protected void validateIssuer() throws TokenValidationException {
        if (!(oidcConfig.getProviderMetadata().getIssuer().equalsIgnoreCase(this.issuer))) {
            
            throw new TokenValidationException(oidcConfig.getClientId(), "issuer is [ " + this.issuer + " ], expecting  [ " + oidcConfig.getProviderMetadata().getIssuer() + " ]");
        }
        
    }

}
