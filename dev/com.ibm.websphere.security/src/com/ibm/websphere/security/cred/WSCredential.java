/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.cred;

import java.util.ArrayList;

import javax.security.auth.login.CredentialExpiredException;

import com.ibm.websphere.security.auth.CredentialDestroyedException;

/**
 * <p>Interface that defines a Credential used represent an authenticated principal to WebSphere.</p>
 * <p>Authentication mechanisms are expected to implement this interface.</p>
 * <p>Several of the method return types in this interface are array types.
 * If implementors internally store instance data as arrays for these methods, they should
 * return a deep copy of the array so that modifying the return result does not also modify
 * the internally stored array.
 * <p>Once a credential has been created, it is typically immutable except for
 * expiration time.
 * 
 * <p>
 * If a credential is expired, any method access generates a
 * <code>CredentialExpiredException.</code> The <code>refresh()</code> method
 * of the <code>Refreshable</code> interface is not implemented. A new login
 * must be performed.
 * </p>
 * 
 * <p>
 * If a credential is destroyed, any method access generates a
 * <code>CredentialDestroyedException.</code> A destroyed credential can not be
 * used.
 * </p>
 * 
 * @ibm-api
 * @author IBM Corporation
 * @version 1.0
 * @see javax.security.auth.Destroyable
 * @see javax.security.auth.Refreshable
 * @since 1.0
 * @ibm-spi
 */

public interface WSCredential extends java.io.Serializable {

    /**
     * <p>
     * Return the realm name. The format of the realm name
     * depends on the authentication targets, for example:
     * <ul>
     * <li><b>LTPA:</b> Returns the domain name of LTPA</li>
     * <li><b>Kerberos:</b> Returns the realm name of Kerberos</li>
     * </ul>
     * </p>
     * 
     * <p>
     * If there is no realm name, <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return The realm name, a string, or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getRealmName() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns the user principal name. If there is no principal name,
     * <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return The user principal name, a string, or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getSecurityName() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns the realm and the user principal name, the default implementation format is <Q>realm/user principal name</Q>.
     * If there is no valid value, <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return The realm and user principal name, a string, or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getRealmSecurityName() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns the unique user name as it applies to the configured user registry. For LDAP,
     * this would might be the DistinguishedName. For LocalOS, this might return
     * the unique name from the local registry. For Custom, this will be whatever the
     * custom registry getUniqueUserId() API returns.
     * </p>
     * 
     * @return The user unique name, a string, or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getUniqueSecurityName() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns the realm and the unique user name, the default implementation format is <Q>realm/unique user name</Q>.
     * If there is no valid value, <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return The realm and unique user name, a string, or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getRealmUniqueSecurityName() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns a long value that indicates when a credential will expire.
     * The authentication mechanism determines if and when a credential expires typically when
     * the credential was issued. The unit of measure is also determined by the actual
     * authentication mechanism.
     * <p>
     * If there is no expiration time, <code><b>0</b></code> is returned.
     * </p>
     * 
     * @return long.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public long getExpiration() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns a string value that indicates the primary group the authenticated principal is a member of.
     * 
     * <p>
     * If there is no primary group ID, <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return String or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getPrimaryGroupId() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns a string value that represents the access-Id of the principal.
     * An access-Id is used to uniquely identity the principal in a user registry and
     * is typically used during authorization checks.
     * <p>
     * If there is no access-Id <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return String or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getAccessId() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns a ArrayList which indicates the groups the authenticated principal is a member of.
     * 
     * <p>
     * If there are no groups, an empty List is returned.
     * </p>
     * 
     * @return ArrayList
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    @SuppressWarnings("unchecked")
    public ArrayList getGroupIds() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Allows user to get an Object based on a key. It is similar to a hash table.
     * </p>
     * 
     * @param key A String value, <Q>wssecurity.*</Q> is keys reserved for WebSphere internal usage
     * @return return null if no object associated with the key
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public Object get(String key) throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Allows user to set an Object based on a key. It is similar to a hash table. Please do not use
     * key values begin with <Q>wssecurity.*</Q>, the <Q>wssecurity</Q> is the namespace reserved by WebSphere
     * internal usage.
     * </p>
     * 
     * @param key A String value, <Q>wssecurity.*</Q> is keys reserved for WebSphere internal usage
     * @param value Object to be set to associate with the key
     * @return if there is already an object associated with the key prior to the set,
     *         then the object is returned, else null is returned
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public Object set(String key, Object value) throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Return true if the credential is an Unauthenticated Credential.
     * </p>
     * 
     * @return Return true if the credential is an Unauthenticated Credential.
     */
    public boolean isUnauthenticated();

    /**
     * <p>
     * Returns the credential token.
     * </p>
     * 
     * <p>
     * The Credential Token should be treated as an opaque object. It should be a deep copy of
     * any byte array that an actual WSCredential implementation may use to store the token
     * internally.
     * </p>
     * 
     * <p>
     * If there is no credential token, <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return The Credential Token of a credential, a byte array or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public byte[] getCredentialToken() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Returns the OID that identifies the authentication mechanism, for example:
     * </p>
     * <p>
     * The OID is an object identifier in string format, e.g. 111.222.33 for instance.
     * </p>
     * 
     * 
     * <p>
     * If there is no OID, <code><b>null</b></code> is returned.
     * </p>
     * 
     * @return The OID of a credential or null.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public String getOID() throws CredentialDestroyedException, CredentialExpiredException;

    /**
     * <p>
     * Determines if the credential is a BasicAuth credential or not. If a BasicAuth credential,
     * it will contain data to authenticate a user, but cannot represent an authenticated user.
     * If not a BasicAuth credential, it can be used for authorization decisions.
     * </p>
     */
    public boolean isBasicAuth();

    /**
     * <p>
     * Returns a boolean value that indicates if the credential is forwardable.
     * A forwardable credential can be propagated to other servers as part of a delegated
     * remote method invocation. The authentication mechanism determines forwardability.
     * 
     * @return boolean.
     * @exception CredentialDestroyedException
     *                Thrown if credential is destroyed.
     * @exception CredentialExpiredException
     *                Thrown if credential is expired.
     */
    public boolean isForwardable() throws CredentialDestroyedException, CredentialExpiredException;
}
