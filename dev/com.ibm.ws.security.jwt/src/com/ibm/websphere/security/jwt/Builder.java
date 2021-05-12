/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.jwt;

import java.security.Key;
import java.util.List;
import java.util.Map;

public interface Builder {

    public abstract Builder create() throws InvalidBuilderException;

    public abstract Builder create(String builderConfigId) throws InvalidBuilderException;

    public abstract Builder issuer(String issuerUrl) throws InvalidClaimException;

    // to whom the token is intended to be sent
    public abstract Builder audience(List<String> newaudiences)
            throws InvalidClaimException;

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    public abstract Builder expirationTime(long exp) throws InvalidClaimException;

    // generate a unique identifier for the token. The default is "false"
    public abstract Builder jwtId(boolean create);

    // time in milliseconds since January 1, 1970, 00:00:00 GMT
    public abstract Builder notBefore(long time_from) throws InvalidClaimException;

    // the subject/principal is whom the token is about
    public abstract Builder subject(String username) throws InvalidClaimException;

    // private key for token signing
    public abstract Builder signWith(String algorithm, Key key) throws KeyException;

    // shared key for signing
    public abstract Builder signWith(String algorithm, String key)
            throws KeyException;

    // Sets token encryption information for creating JSON Web Encryption tokens.
    public abstract Builder encryptWith(String keyManagementAlg, Key keyManagementKey, String contentEncryptionAlg) throws KeyException;

    // add claims with the given name and value
    public abstract Builder claim(String name, Object value)
            throws InvalidClaimException;

    // Add given claims
    public abstract Builder claim(Map<String, Object> map)
            throws InvalidClaimException;

    // fetch claim from configured user registry
    public abstract Builder fetch(String name) throws InvalidClaimException;

    // remove claims associated with the name
    public abstract Builder remove(String name) throws InvalidClaimException;

    // fetch claim from json or jwt. The jwt can be base 64 encoded or
    // decoded.
    public abstract Builder claimFrom(String jsonOrJwt, String claim)
            throws InvalidClaimException, InvalidTokenException;

    // fetch all claim from json or jwt. The jwt can be base 64 encoded or
    // decoded
    public abstract Builder claimFrom(String jsonOrJwt) throws InvalidTokenException;

    // fetch claim from jwt.
    public abstract Builder claimFrom(JwtToken jwt, String claimName)
            throws InvalidClaimException, InvalidTokenException;

    // fetch all claim from jwt.
    public abstract Builder claimFrom(JwtToken jwt) throws InvalidTokenException;

    // build JWT
    public abstract JwtToken buildJwt() throws JwtException, InvalidBuilderException;

}