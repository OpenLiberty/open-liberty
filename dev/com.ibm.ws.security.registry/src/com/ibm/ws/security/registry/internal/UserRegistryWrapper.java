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
package com.ibm.ws.security.registry.internal;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.NotImplementedException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;

/**
 *
 */
public class UserRegistryWrapper implements com.ibm.websphere.security.UserRegistry {
    private final com.ibm.ws.security.registry.UserRegistry wrappedUr;

    public UserRegistryWrapper(com.ibm.ws.security.registry.UserRegistry wrappedUr) {
        this.wrappedUr = wrappedUr;
    }

    /**
     * {@inheritDoc}<p>
     * This call is not intended for use by external callers and will
     * therefore do nothing.
     */
    @Override
    public void initialize(Properties props) throws CustomRegistryException, RemoteException {}

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(IllegalArgumentException.class)
    public String checkPassword(String userSecurityName, @Sensitive String password) throws PasswordCheckFailedException, CustomRegistryException, RemoteException {
        try {
            String ret = wrappedUr.checkPassword(userSecurityName, password);
            if (ret == null) {
                throw new PasswordCheckFailedException("Unable to authenticate given specified username and password combination");
            } else {
                return ret;
            }
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new PasswordCheckFailedException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.CertificateMapFailedException.class)
    public String mapCertificate(X509Certificate[] cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.mapCertificate(cert[0]);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.CertificateMapNotSupportedException e) {
            throw new CertificateMapNotSupportedException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.CertificateMapFailedException e) {
            throw new CertificateMapFailedException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() throws CustomRegistryException, RemoteException {
        return wrappedUr.getRealm();
    }

    /** {@inheritDoc} */
    @Override
    public Result getUsers(String pattern, int limit) throws CustomRegistryException, RemoteException {
        try {
            SearchResult ret = wrappedUr.getUsers(pattern, limit);

            Result result = new Result();
            result.setList(ret.getList());
            if (ret.hasMore()) {
                result.setHasMore();
            }
            return result;
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getUserDisplayName(userSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getUniqueUserId(userSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getUserSecurityName(uniqueUserId);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(IllegalArgumentException.class)
    public boolean isValidUser(String userSecurityName) throws CustomRegistryException, RemoteException {
        try {
            return wrappedUr.isValidUser(userSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Result getGroups(String pattern, int limit) throws CustomRegistryException, RemoteException {
        try {
            SearchResult ret = wrappedUr.getGroups(pattern, limit);

            Result result = new Result();
            result.setList(ret.getList());
            if (ret.hasMore()) {
                result.setHasMore();
            }
            return result;
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getGroupDisplayName(groupSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getUniqueGroupId(groupSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public List<String> getUniqueGroupIds(String uniqueUserId) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getUniqueGroupIdsForUser(uniqueUserId);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getGroupSecurityName(uniqueGroupId);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(IllegalArgumentException.class)
    public boolean isValidGroup(String groupSecurityName) throws CustomRegistryException, RemoteException {
        try {
            return wrappedUr.isValidGroup(groupSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.ws.security.registry.EntryNotFoundException.class)
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            return wrappedUr.getGroupsForUser(userSecurityName);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc} <p>
     *
     * @throws com.ibm.ws.security.registry.NotImplementedException
     */
    @Override
    public Result getUsersForGroup(String groupSecurityName, int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException {
        try {
            SearchResult ret = wrappedUr.getUsersForGroup(groupSecurityName, limit);
            Result result = new Result();
            if (ret != null) {
                result.setList(ret.getList());
                if (ret.hasMore()) {
                    result.setHasMore();
                }
            }
            return result;

        } catch (com.ibm.ws.security.registry.NotImplementedException e) {
            throw new NotImplementedException("The getUsersForGroup API is not implemented");
        } catch (com.ibm.ws.security.registry.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (com.ibm.ws.security.registry.CustomRegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        } catch (RegistryException e) {
            throw new CustomRegistryException(e.getMessage(), e);
        }

    }

    /**
     * {@inheritDoc} <p>
     * This call is not intended for use by external callers and will
     * therefore a NotImplementedException will be thrown.
     */
    @Override
    public WSCredential createCredential(String userSecurityName) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException {
        throw new NotImplementedException("The createCredential method is not available");
    }
}
