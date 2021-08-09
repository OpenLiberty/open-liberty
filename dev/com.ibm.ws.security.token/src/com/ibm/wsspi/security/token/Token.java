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
package com.ibm.wsspi.security.token;

/**
 * This interface should not be implemented directly but rather
 * one should implement either AuthenticationToken, AuthorizationToken,
 * SingleSignonToken or PropagationToken.
 * 
 * @author IBM Corporation
 * @version 5.1.1
 * @since 5.1.1
 * @ibm-spi
 */
public interface Token extends java.lang.Cloneable {

    /**
     * <p>
     * Called by the runtime to determine if a token is valid still in
     * terms of expiration, digital signature, etc. The implementation
     * determines what valid means. If this returns false to the
     * WebSphere runtime, an exception will be thrown (appropriate to
     * where the call was made, e.g., NO_PERMISSION, WSLoginFailedException,
     * etc.) and the request will be rejected.
     * </p>
     * 
     * @return boolean
     **/
    public boolean isValid();

    /**
     * <p>
     * This returns the expiration time in milli-seconds.
     * </p>
     * 
     * @return long
     **/
    public long getExpiration();

    /**
     * <p>
     * Returns if this token should be forwarded/propagated downstream.
     * Some token implementations may not be intended to be propagated
     * downstream, it's up to the implementation to determine this.
     * <p>
     * 
     * @return boolean
     **/
    public boolean isForwardable();

    /**
     * <p>
     * Gets the principal that this Token belongs to. If this is an
     * authorization token, this principal string must match the
     * authentication token principal string or the message will be
     * rejected. CSIv2 has stringent rules about validating authorization
     * tokens using either the Identity Token or Authentication Token
     * principal.
     * </p>
     * 
     * @return String
     **/
    public String getPrincipal();

    /**
     * Gets the bytes to be sent across the wire. The information in the byte[]
     * needs to be enough to recreate the Token object at the target server.
     * 
     * @return byte[]
     **/
    public byte[] getBytes();

    /**
     * <p>
     * Gets the name of the token, used to identify the byte[] in the protocol message.
     * </p>
     * 
     * @return String
     **/
    public String getName();

    /**
     * Gets the version of the token as an short. This is also used to identify the
     * byte[] in the protocol message.
     * 
     * @return short
     **/
    public short getVersion();

    /**
     * <p>
     * Returns a unique identifier of the token based upon information
     * that the provider considers to be unique. This will be used for
     * caching purposes and may be used in combination with other token
     * unique IDs that are part of the same Subject to form a Subject
     * unique identifier.
     * </p>
     * <p>
     * An implementation of this method should be careful to only change
     * the token uniqueness when required. Any login which generates a
     * new unique ID will create a Subject entry in the cache, which
     * will increase memory requirements.
     * </p>
     * <p>
     * This method should return null if the token does not need to
     * affect the cache uniqueness. Typically, if using only static
     * registry attributes, this should return null. However, if dynamic
     * attributes are used including strength of authentication, time
     * of day, etc. you may affect the cache uniqueness by returning a
     * non-null value that reflects how you want the cache key too look.
     * Typically, the token implementation will know what is most unique
     * about the dynamic data, however, an alternative is to return a
     * UUID. The values of getUniqueID() from all custom tokens present
     * in the Subject will be added together and used in the SSO token
     * for lookup. A one-way hash of this string will be created as the
     * unique ID.
     * 
     * When altering a token to contain a non-null value here, the token
     * must be added to the Subject prior to the commit phase or before
     * the wsMap module commit is called.
     * </p>
     * 
     * @return String
     **/
    public String getUniqueID();

    /**
     * <p>
     * When called, the token becomes irreversibly read-only. The
     * implementation needs to ensure any setter methods check that
     * this has been set.
     * </p>
     **/
    public void setReadOnly();

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
     * <p>
     * Gets the List of all attribute names present in the token.
     * </p>
     * 
     * @return java.util.Enumeration
     **/
    public java.util.Enumeration getAttributeNames();

    /**
     * Makes a deep copy of this token when necessary. This is typically
     * used for a custom propagation token when it is stored in a security
     * session. For each invocation to that session, we should use a copy
     * of the propagation token to go downstream instead of a reference to
     * the token.
     * 
     * @return Object
     */
    public Object clone();

}
