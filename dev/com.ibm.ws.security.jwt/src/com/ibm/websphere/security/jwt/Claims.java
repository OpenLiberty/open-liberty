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

package com.ibm.websphere.security.jwt;

import java.util.List;
import java.util.Map;

/**
 * The {@code Claims} interface represents JSON Web Token (JWT) payload claims and offers convenient get methods for some of the
 * well known JWT claims such as "iss", "exp", and "iat".
 * 
 * @author IBM
 * @version 1.0
 * 
 * @since 1.0
 * 
 * @ibm-api
 */
public interface Claims extends Map<String, Object> {
    /**
     * The ISSUER is used to represent the "iss" claim
     */
    public static final String ISSUER = "iss";
    /**
     * The SUBJECT is used to represent the "sub" claim
     */
    public static final String SUBJECT = "sub";
    /**
     * The AUDIENCE is used to represent the "aud" claim
     */
    public static final String AUDIENCE = "aud";
    /**
     * The EXPIRATION is used to represent the "exp" claim
     */
    public static final String EXPIRATION = "exp";
    /**
     * The NOT_BEFORE is used to represent the "nbf" claim
     */
    public static final String NOT_BEFORE = "nbf";
    /**
     * The ISSUED_AT is used to represent the "iat" claim
     */
    public static final String ISSUED_AT = "iat";
    /**
     * The ID is used to represent the "jti" claim
     */
    public static final String ID = "jti";
    /**
     * The AZP is used to represent the "azp" claim
     */
    public static final String AZP = "azp";
    /**
     * The TOKEN_TYPE is used to represent the "token_type" claim
     */
    public static final String TOKEN_TYPE = "token_type";

    /**
     * 
     * @return The "iss" claim
     */

    String getIssuer();

    /**
     * 
     * @return The "sub" claim
     */

    String getSubject();

    /**
     * 
     * @return The "aud" claim
     */
    List<String> getAudience();

    /**
     * 
     * @return The "exp" claim
     */
    long getExpiration();

    /**
     * 
     * @return The "nbf" claim
     */
    long getNotBefore();

    /**
     * 
     * @return The "iat" claim
     */
    long getIssuedAt();

    /**
     * 
     * @return The "jti" claim
     */
    String getJwtId();

    /**
     * 
     * @return The "azp" claim
     */
    String getAuthorizedParty();

    /**
     * 
     * @param claimName
     *            claim name
     * @param requiredType
     *            This is the required type of the claim value
     * @return The claim value that matches the requiredType
     */

    <T> T getClaim(String claimName, Class<T> requiredType);

    /**
     * 
     * @return All the claims
     */
    Map<String, Object> getAllClaims();

    /**
     * 
     * @return All the claims in JSON string format
     */
    String toJsonString(); // Return payload in the Json String format.

}
