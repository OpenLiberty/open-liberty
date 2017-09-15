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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.ExternalUserRegistryWrapper;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * Wraps the custom user registry from traditional WAS to be used inside the Liberty runtime.
 */
public class CustomUserRegistryWrapper implements UserRegistry, ExternalUserRegistryWrapper {

    private static final TraceComponent tc = Tr.register(CustomUserRegistryWrapper.class);
    private static final String DEFAULT_REALM = "Default Realm";
    private static final String CUSTOM_REALM = "customRealm";
    private final com.ibm.websphere.security.UserRegistry customUserRegistry;

    public CustomUserRegistryWrapper(com.ibm.websphere.security.UserRegistry customUserRegistry) {
        this.customUserRegistry = customUserRegistry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.registry.UserRegistry#getExternalUserRegistry()
     */
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.registry.internal.ExternalUserRegistryWrapper#getExternalUserRegistry()
     */
    @Override
    public com.ibm.websphere.security.UserRegistry getExternalUserRegistry() {
        return customUserRegistry;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(PasswordCheckFailedException.class)
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {
        try {
            return customUserRegistry.checkPassword(userSecurityName, password);
        } catch (PasswordCheckFailedException e) {
            return null;
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getGroupDisplayName(groupSecurityName);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getGroupSecurityName(uniqueGroupId);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        try {
            Result result = customUserRegistry.getGroups(pattern, limit);
            return new SearchResult(result.getList(), result.hasMore());
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getGroupsForUser(userSecurityName);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        try {
            String realmName = customUserRegistry.getRealm();
            if (realmName == null || realmName.trim().isEmpty()) {
                realmName = CUSTOM_REALM;
            }
            return realmName;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception caught on getRealm", customUserRegistry, e);
            }
        }
        return DEFAULT_REALM;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getUniqueGroupId(groupSecurityName);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getUniqueGroupIds(uniqueUserId);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getUniqueUserId(userSecurityName);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getUserDisplayName(userSecurityName);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.EntryNotFoundException.class)
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        try {
            return customUserRegistry.getUserSecurityName(uniqueUserId);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        try {
            Result result = customUserRegistry.getUsers(pattern, limit);
            return new SearchResult(result.getList(), result.hasMore());
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        try {
            return customUserRegistry.isValidGroup(groupSecurityName);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        try {
            return customUserRegistry.isValidUser(userSecurityName);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(com.ibm.websphere.security.CertificateMapFailedException.class)
    public String mapCertificate(X509Certificate cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        try {
            return customUserRegistry.mapCertificate(new X509Certificate[] { cert });
        } catch (com.ibm.websphere.security.CertificateMapNotSupportedException e) {
            throw new CertificateMapNotSupportedException(e.getMessage());
        } catch (com.ibm.websphere.security.CertificateMapFailedException e) {
            throw new CertificateMapFailedException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RegistryException(e.getMessage(), e);
        }
    }

    /** (@inheritDoc) */

    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        Result result = null;
        try {
            result = customUserRegistry.getUsersForGroup(groupSecurityName, limit);
        } catch (com.ibm.websphere.security.EntryNotFoundException e) {
            throw new EntryNotFoundException(e.getMessage(), e);
        } catch (RemoteException e) {
            throw new RemoteException(e.getMessage(), e);
        } catch (com.ibm.websphere.security.NotImplementedException e) {
            throw new com.ibm.ws.security.registry.NotImplementedException(e.getMessage(), e);
        } catch (com.ibm.websphere.security.CustomRegistryException e) {
            throw new com.ibm.ws.security.registry.CustomRegistryException(e.getMessage(), e);

        }
        return new SearchResult(result.getList(), result.hasMore());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return CustomUserRegistryFactory.KEY_TYPE_CUSTOM;
    }

}
