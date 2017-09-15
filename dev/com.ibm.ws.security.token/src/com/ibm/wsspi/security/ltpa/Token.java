/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.ltpa;

/**
 * <p>
 * This interface is implemented by a provider to define the behavior of
 * the LTPA token. The TokenFactory implementation should handle loading
 * the Token implementation.
 * </p>
 * 
 * @ibm-spi
 */

public interface Token extends java.lang.Cloneable {

    /**
     * Validates the token including expiration, signature, etc.
     * 
     * @param com.ibm.ws.security.ltpa.Token token
     * @return boolean
     * @throws com.ibm.websphere.security.auth.InvalidTokenException
     * @throws com.ibm.websphere.security.auth.TokenExpiredException
     */
    public boolean isValid()
                    throws com.ibm.websphere.security.auth.InvalidTokenException,
                    com.ibm.websphere.security.auth.TokenExpiredException;

    /**
     * Gets the encrypted bytes for inclusion in the WSCredential or SSO cookie.
     * 
     * @return byte[]
     * @throws com.ibm.websphere.security.auth.InvalidTokenException
     * @throws com.ibm.websphere.security.auth.TokenExpiredException
     */
    public byte[] getBytes()
                    throws com.ibm.websphere.security.auth.InvalidTokenException,
                    com.ibm.websphere.security.auth.TokenExpiredException;

    /**
     * Gets the expiration as a long.
     * 
     * @return long
     */
    public long getExpiration();

    /**
     * Gets the version of the token as an short.
     * 
     * @return short
     */
    public short getVersion();

    /**
     * <p>
     * Gets the attribute value based on the named value. A string array
     * is returned containing all values of the attribute previously set.
     * </p>
     * 
     * @param String key
     * @return String[]
     **/
    public String[] getAttributes(String key);

    /**
     * <p>
     * Adds the attribute name/value pair to a String[] list of values for
     * the specified key. Once an attribute is set, it cannot only be
     * appended to but not overwritten. Returns the previous value(s)
     * set for key, not including the current value being set, or null
     * if not previously set.
     * </p>
     * 
     * @param String key
     * @param String value
     * @return String[]
     **/
    public String[] addAttribute(String key, String value);

    /**
     * Gets the attribute names
     * 
     * @return java.lang.Enumeration
     */
    public java.util.Enumeration getAttributeNames();

    /**
     * Makes a deep copy of this token when necessary
     * 
     * @return Object
     */
    public Object clone();

}
