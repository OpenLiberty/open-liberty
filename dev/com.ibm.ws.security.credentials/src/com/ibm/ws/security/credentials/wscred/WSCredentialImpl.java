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
package com.ibm.ws.security.credentials.wscred;

import java.io.UnsupportedEncodingException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.security.auth.AuthPermission;
import javax.security.auth.login.CredentialExpiredException;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.credentials.ExpirableCredential;

public class WSCredentialImpl implements WSCredential, ExpirableCredential {

    private static final long serialVersionUID = 7097141765627715179L;
    private static final String REALM_SEPARATOR = "/";
    private static final Permission APP_READ_PERMISSION = new AuthPermission("wssecurity.applicationReadCredential");
    private static final Permission APP_UPDATE_PERMISSION = new AuthPermission("wssecurity.applicationUpdateCredential");
    private static final Permission READ_PERMISSION = new AuthPermission("wssecurity.readCredential");
    private static final Permission UPDATE_PERMISSION = new AuthPermission("wssecurity.updateCredential");
    private static final Permission CREATE_PERMISSION = new AuthPermission("wssecurity.createCredential");
    private static final String LTPA_AUTHMECH_OID = "1.3.18.0.2.30.2";
    private static final String DEFAULT_AUTHMECH_OID = LTPA_AUTHMECH_OID;
    private static byte[] emptyByteArray = new byte[0];
    private final String realmName;
    private final String securityName;
    private final String realmSecurityName;
    private final String uniqueSecurityName;
    private final String realmUniqueSecurityName;
    private final String primaryGroupId;
    private final String accessId;
    private final ArrayList<String> groupIds;
    private long expiration = 0;
    private final boolean unauthenticated;
    private final boolean isBasicAuthCred;
    private final boolean forwardable;
    private final String authMechOID;
    private final Hashtable<String, Object> hashTable;
    private byte[] credentialToken = null;

    /**
     * Constructor used by the registry code to create the original WSCredential.
     * This constructor is used by IMS to reduce overhead when compared with using a Hashtable
     * in the Subject's public credentials.
     * 
     * This is an IBM private-in-use API.
     * 
     * DO NOT change the signature or the implementation of this ctor.
     * 
     * @ibm-private-in-use
     * @param realmName
     * @param securityName
     * @param uniqueSecurityName
     * @param unauthenticatedUserid
     * @param primaryUniqueGroupAccessId
     * @param accessId
     * @param roles ignored for now
     * @param uniqueGroupAccessIds
     */
    public WSCredentialImpl(String realmName,
                            String securityName,
                            String uniqueSecurityName,
                            String unauthenticatedUserid,
                            String primaryUniqueGroupAccessId,
                            String accessId,
                            List<String> roles,
                            List<String> uniqueGroupAccessIds) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(CREATE_PERMISSION);
        }
        this.realmName = realmName;
        this.securityName = securityName;
        this.realmSecurityName = realmName + REALM_SEPARATOR + securityName;
        this.uniqueSecurityName = uniqueSecurityName;
        this.realmUniqueSecurityName = realmName + REALM_SEPARATOR + uniqueSecurityName;
        this.primaryGroupId = primaryUniqueGroupAccessId;
        this.accessId = accessId;
        this.groupIds = (uniqueGroupAccessIds != null) ? createListCopy(uniqueGroupAccessIds) : new ArrayList<String>();
        this.unauthenticated = unauthenticatedUserid != null && unauthenticatedUserid.equalsIgnoreCase(securityName);
        this.hashTable = new Hashtable<String, Object>(32);
        isBasicAuthCred = false;
        forwardable = true;
        authMechOID = DEFAULT_AUTHMECH_OID;
    }

    /**
     * Constructor used to create a basic auth credential.
     */
    public WSCredentialImpl(String realmName, String securityName, @Sensitive String password) {
        this.realmName = realmName;
        this.securityName = securityName;
        credentialToken = getConvertedBytes(password);
        isBasicAuthCred = true;
        authMechOID = "oid:2.23.130.1.1.1";
        forwardable = true;

        // These are nulled out to avoid inadvertent authorization.
        uniqueSecurityName = null;
        realmSecurityName = null;
        realmUniqueSecurityName = null;
        accessId = null;
        primaryGroupId = null;
        groupIds = null;

        unauthenticated = false;
        hashTable = new Hashtable<String, Object>(32);
    }

    @FFDCIgnore(UnsupportedEncodingException.class)
    @Sensitive
    private static byte[] getConvertedBytes(@Sensitive String in_String) {
        byte[] convertedBytes = null;
        if (in_String == null) {
            return null;
        }

        if (in_String.length() == 0) {
            return emptyByteArray;
        }

        try {
            convertedBytes = in_String.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 should always be supported.
        }
        return convertedBytes;
    }

    /**
     * @param groupIds
     * @return
     */
    private ArrayList<String> createListCopy(List<String> groupIds) {
        ArrayList<String> list = new ArrayList<String>();
        for (String id : groupIds) {
            list.add(id);
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmName() throws CredentialDestroyedException, CredentialExpiredException {
        return realmName;
    }

    /** {@inheritDoc} */
    @Override
    public String getSecurityName() throws CredentialDestroyedException, CredentialExpiredException {
        return securityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmSecurityName() throws CredentialDestroyedException, CredentialExpiredException {
        return realmSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueSecurityName() throws CredentialDestroyedException, CredentialExpiredException {
        return uniqueSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealmUniqueSecurityName() throws CredentialDestroyedException, CredentialExpiredException {
        return realmUniqueSecurityName;
    }

    /** {@inheritDoc} */
    @Override
    public long getExpiration() throws CredentialDestroyedException, CredentialExpiredException {
        return expiration;
    }

    /** {@inheritDoc} */
    @Override
    public String getPrimaryGroupId() throws CredentialDestroyedException, CredentialExpiredException {
        return primaryGroupId;
    }

    /** {@inheritDoc} */
    @Override
    public String getAccessId() throws CredentialDestroyedException, CredentialExpiredException {
        return accessId;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public ArrayList getGroupIds() throws CredentialDestroyedException, CredentialExpiredException {
        return groupIds;
    }

    /** {@inheritDoc} */
    @Override
    public Object get(String key) throws CredentialDestroyedException, CredentialExpiredException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(APP_READ_PERMISSION);
        }
        if (key.startsWith("wssecurity")) {
            if (sm != null) {
                sm.checkPermission(READ_PERMISSION);
            }
        }
        return hashTable.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public Object set(String key, Object value) throws CredentialDestroyedException, CredentialExpiredException {
        SecurityManager sm = System.getSecurityManager();
        if (key.startsWith("wssecurity")) {
            if (sm != null) {
                sm.checkPermission(UPDATE_PERMISSION);
            }
        }
        if (hashTable.get(key) != null) {
            if (sm != null) {
                sm.checkPermission(APP_UPDATE_PERMISSION);
            }
        }
        return hashTable.put(key, value);
    }

    /**
     * Sets the expiration in milliseconds.
     * 
     * @param expiration credential expiration in milliseconds.
     */
    @Override
    public void setExpiration(long expirationInMilliseconds) {
        this.expiration = expirationInMilliseconds;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUnauthenticated() {
        return unauthenticated;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return super.toString() +
               ",realmName=" + realmName +
               ",securityName=" + securityName +
               ",realmSecurityName=" + realmSecurityName +
               ",uniqueSecurityName=" + uniqueSecurityName +
               ",primaryGroupId=" + primaryGroupId +
               ",accessId=" + accessId +
               ",groupIds=" + groupIds;
    }

    /** {@inheritDoc} */
    @Override
    public String getOID() throws CredentialDestroyedException, CredentialExpiredException {
        return authMechOID;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBasicAuth() {
        return isBasicAuthCred;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isForwardable() throws CredentialDestroyedException, CredentialExpiredException {
        return forwardable;
    }

    /** {@inheritDoc} */
    @Sensitive
    @Override
    public byte[] getCredentialToken() throws CredentialDestroyedException, CredentialExpiredException {
        if (credentialToken != null) {
            return credentialToken.clone();
        } else {
            return null;
        }
    }

}
