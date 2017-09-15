/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.quickstart.internal;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.LDAPUtils;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * UserRegistry implementation for the quick start security registry.
 */
class QuickStartSecurityRegistry implements UserRegistry {
    static final String REALM_NAME = "QuickStartSecurityRealm";
    private volatile String user;
    @Sensitive
    private volatile ProtectedString password;

    /**
     * Create QuickStartSecurityRegistry instance.
     *
     * @param user
     * @param password
     * @throws IllegalArgumentException
     */
    QuickStartSecurityRegistry(String user, ProtectedString password) {
        update(user, password);
    }

    /**
     * @param user
     * @param password
     */
    void update(String user, ProtectedString password) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (user.trim().isEmpty()) {
            throw new IllegalArgumentException("user must not be empty");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (new String(password.getChars()).trim().isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        this.user = user;
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        return REALM_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }
        if (password == null) {
            throw new IllegalArgumentException("password is null");
        }
        if (password.isEmpty()) {
            throw new IllegalArgumentException("password is an empty String");
        }
        String savedPassword = new String(this.password.getChars());
        if (this.user.equals(userSecurityName) && savedPassword.equals(password)) {
            return user;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String mapCertificate(X509Certificate cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        if (cert == null) {
            throw new IllegalArgumentException("cert is null");
        }
        // getSubjectDN is denigrated
        String dn = cert.getSubjectX500Principal().getName();
        String name = LDAPUtils.getCNFromDN(dn);
        if (name == null || !isValidUser(name)) {
            throw new CertificateMapFailedException("DN: " + dn + " does not map to a valid registry user");
        }
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }
        return user.equals(userSecurityName);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern is null");
        }
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern is an empty String");
        }
        if (limit >= 0 && user.matches(pattern)) {
            List<String> list = new ArrayList<String>();
            list.add(user);
            return new SearchResult(list, false);
        } else {
            return new SearchResult();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }
        if (user.equals(userSecurityName)) {
            return user;
        } else {
            throw new EntryNotFoundException(userSecurityName + " does not exist");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("userSecurityName is an empty String");
        }
        if (user.equals(userSecurityName)) {
            return user;
        } else {
            throw new EntryNotFoundException(userSecurityName + " does not exist");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (uniqueUserId == null) {
            throw new IllegalArgumentException("uniqueUserId is null");
        }
        if (uniqueUserId.isEmpty()) {
            throw new IllegalArgumentException("uniqueUserId is an empty String");
        }
        if (user.equals(uniqueUserId)) {
            return user;
        } else {
            throw new EntryNotFoundException(uniqueUserId + " does not exist");
        }
    }

    /** {@inheritDoc}. This registry does not support groups. */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern is null");
        }
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern is an empty String");
        }
        return new SearchResult();
    }

    /** {@inheritDoc}. This registry does not support groups. */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }
        throw new EntryNotFoundException(REALM_NAME + " does not support groups");
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }
        throw new EntryNotFoundException(REALM_NAME + " does not support groups");
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        if (uniqueGroupId == null) {
            throw new IllegalArgumentException("uniqueGroupId is null");
        }
        if (uniqueGroupId.isEmpty()) {
            throw new IllegalArgumentException("uniqueGroupId is an empty String");
        }
        throw new EntryNotFoundException(REALM_NAME + " does not support groups");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        if (uniqueUserId == null) {
            throw new IllegalArgumentException("uniqueUserId is null");
        }
        if (uniqueUserId.isEmpty()) {
            throw new IllegalArgumentException("uniqueUserId is an empty String");
        }
        if (user.equals(uniqueUserId)) {
            return new ArrayList<String>();
        } else {
            throw new EntryNotFoundException(REALM_NAME + " does not support groups");
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        if (userSecurityName == null) {
            throw new IllegalArgumentException("userSecurityName is null");
        }
        if (userSecurityName.isEmpty()) {
            throw new IllegalArgumentException("uniqueGroupId is an empty String");
        }
        if (user.equals(userSecurityName)) {
            return new ArrayList<String>();
        } else {
            throw new EntryNotFoundException(REALM_NAME + " does not support groups");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsersForGroup(java.lang.String, int)
     */
    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        if (groupSecurityName == null) {
            throw new IllegalArgumentException("groupSecurityName is null");
        }
        if (groupSecurityName.isEmpty()) {
            throw new IllegalArgumentException("groupSecurityName is an empty String");
        }
        return new SearchResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return QuickStartSecurity.QUICK_START_SECURITY_REGISTRY_TYPE;
    }

}
