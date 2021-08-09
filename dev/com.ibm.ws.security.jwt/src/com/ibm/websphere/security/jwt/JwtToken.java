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

/**
 * The {@code JwtToken} represents JSON Web Token (JWT) and consists of a payload which is represented by {@code Claims} and
 * header and signature. This interface has three methods to return these.
 * 
 * @author IBM Corporation
 * 
 * @version 1.0
 * 
 * @since 1.0
 * 
 * @ibm-api
 * 
 */
public interface JwtToken {
    /**
     * 
     * @return The {@code JwtToken} claims or payload
     */
    Claims getClaims();

    /**
     * 
     * @param name
     *            This is the header name
     * @return The {@code JwtToken} header value corresponding to the given header name
     */
    String getHeader(String name); // return the value of given JWT header name

    /**
     * 
     * @return The {@code JwtToken} as a string consisting of base64 encoded header, payload, signature separated by period ('.')
     *         characters.
     */
    String compact(); // return JWT, a string consisting of three dot ('.')
                      // separated base64url-encoded part

}
