/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.principals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.security.Principal;

public class WSPrincipal implements Principal, Serializable {
    private static final long serialVersionUID = -5854954199707595239L;

    /**
     * Names of serializable fields.
     */
    private static final String AUTH_METHOD = "authMethod", ACCESS_ID = "accessId", SECURITY_NAME = "securityName";

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                                new ObjectStreamField(AUTH_METHOD, String.class),
                                                                                                new ObjectStreamField(ACCESS_ID, String.class),
                                                                                                new ObjectStreamField(SECURITY_NAME, String.class)
    };

    // TODO: should this be an enum? can we really enumerate all of the different methods
    //       we'll want to use in the future? Note - switching to enum would break serialization compatibility if not done carefully
    public static final String AUTH_METHOD_PASSWORD = "password";
    public static final String AUTH_METHOD_CERTIFICATE = "certificate";
    public static final String AUTH_METHOD_TOKEN = "token";
    public static final String AUTH_METHOD_JWT_SSO_TOKEN = "jwtSSOToken";
    public static final String AUTH_METHOD_IDENTITY_ASSERTION = "idAssert";
    public static final String AUTH_METHOD_HASH_TABLE = "hashtable";
    public static final String AUTH_METHOD_BASIC = "basic";

    private transient String securityName;
    private transient String accessId;
    private transient String authMethod;
    private transient int hash;

    /**
     * A WSPrincipal's name is the securityName of the authenticated
     * identity.
     *
     * @param securityName A String representing the user's name. Must not be <code>null</code> or empty.
     * @param accessId A String representing the user's accessId.
     * @see com.ibm.ws.security.AccessIdUtil#createAccessId(String, String, String)
     */
    public WSPrincipal(String securityName, String accessId, String authMethod) {
        if (securityName == null || securityName.isEmpty() ||
            authMethod == null || authMethod.isEmpty()) {
            throw new IllegalArgumentException("Sanity check, null / empty values are invalid here");
        }
        this.securityName = securityName;
        this.accessId = accessId;
        this.authMethod = authMethod;
        this.hash = (toString() + accessId).hashCode();
    }

    /**
     * {@inheritDoc} Returns the securityName for this Principal.
     */
    @Override
    public String getName() {
        return securityName;
    }

    /**
     * Return the accessId
     */
    public String getAccessId() {
        return accessId;
    }

    /**
     * Return the authentication method used:
     * password, certificate, token, identity assertion, etc
     */
    public String getAuthenticationMethod() {
        return authMethod;
    }

    /**
     * {@inheritDoc} Two WSPrincipal are considered equal if their
     * accessIds are the same.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WSPrincipal)) {
            return false;
        }
        WSPrincipal that = (WSPrincipal) obj;
        boolean securityNamesAreEqual = false;
        boolean accessIdsAreEqual = false;
        if (this.securityName.equals(that.securityName)) {
            securityNamesAreEqual = true;
        }
        if ((this.accessId != null && this.accessId.equals(that.accessId)) ||
            this.accessId == null && that.accessId == null) {
            accessIdsAreEqual = true;
        }
        return (securityNamesAreEqual && accessIdsAreEqual);
    }

    /**
     * {@inheritDoc} Returns a hash of the toString value.
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * Deserialize security context.
     *
     * @param in The stream from which this object is read.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        authMethod = (String) fields.get(AUTH_METHOD, null);
        accessId = (String) fields.get(ACCESS_ID, null);
        securityName = (String) fields.get(SECURITY_NAME, null);
        hash = (toString() + accessId).hashCode();
    }

    /**
     * {@inheritDoc} Returns the a String of the format WSPrincipal:accessId.
     */
    @Override
    public String toString() {
        return "WSPrincipal:" + securityName;
    }

    /**
     * Serialize security context.
     *
     * @param out The stream to which this object is serialized.
     *
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put(AUTH_METHOD, authMethod);
        fields.put(ACCESS_ID, accessId);
        fields.put(SECURITY_NAME, securityName);
        out.writeFields();
    }
}