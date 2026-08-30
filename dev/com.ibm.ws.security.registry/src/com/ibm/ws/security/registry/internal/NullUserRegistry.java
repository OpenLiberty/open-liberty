/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 *
 */

@Component(service = UserRegistry.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                        "name=NullUserRegistry",
                        "service.ranking:Integer=-99",
                        "com.ibm.ws.security.registry.type=NullUserRegistry",
                        "config.id=NullUserRegistry",
                        "id=NullUserRegistry" })
public class NullUserRegistry implements UserRegistry {
    private static final TraceComponent tc = Tr.register(NullUserRegistry.class);
    static final String NULL_USER_REGISTRY_TYPE = "NullUserRegistry";
    static final String NULL_USER_REGISTRY_REALM_NAME = "defaultRealm";

    @Activate
    protected void activate() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "NullUserRegistry activated.");
        }
    }

    @Deactivate
    protected void deactivate() {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return NULL_USER_REGISTRY_TYPE;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getRealm()
     */
    @Override
    public String getRealm() {
        return NULL_USER_REGISTRY_REALM_NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#checkPassword(java.lang.String, java.lang.String)
     */
    @Override
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#mapCertificate(java.security.cert.X509Certificate[])
     */
    @Override
    public String mapCertificate(X509Certificate[] chain) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#isValidUser(java.lang.String)
     */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        throwsRegistryException();
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsers(java.lang.String, int)
     */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUserDisplayName(java.lang.String)
     */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUniqueUserId(java.lang.String)
     */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUserSecurityName(java.lang.String)
     */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUsersForGroup(java.lang.String, int)
     */
    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#isValidGroup(java.lang.String)
     */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        throwsRegistryException();
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroups(java.lang.String, int)
     */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroupDisplayName(java.lang.String)
     */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUniqueGroupId(java.lang.String)
     */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroupSecurityName(java.lang.String)
     */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getUniqueGroupIdsForUser(java.lang.String)
     */
    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getGroupsForUser(java.lang.String)
     */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        throwsRegistryException();
        return null;
    }

    private void throwsRegistryException() throws RegistryException {
        throw new RegistryException("NullUserRegistry will not authenticate any user. Authentication is always rejected.");
    }
} 