/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * The UserRegistryProxy is an internal class which allows the runtime to
 * transparently invoke UserRegistry APIs against multiple implementations.
 *
 * This is effectively a pass-through implementation, allowing the delegates
 * to enforce the API constraints laid out by the UserRegistry interface.
 *
 * TODO: What does it mean for userA defined in delegate1 and userB defined
 * in delegate2 to be different?
 */
public class UserRegistryProxy implements UserRegistry {
    private static final TraceComponent tc = Tr.register(UserRegistryProxy.class);
    private final String realm;
    private final List<UserRegistry> delegates;

    /**
     * Constructs a UserRegistryProxy which will delegate its calls out
     * to the UserRegistry instances in the delegates list. Also takes a
     * realm name as there may be no meaningful way to merge realm names
     * of the delegates.
     *
     * @param realm The realm name for the proxy.
     * @param delegates List of 2 or more UserRegistry instances.
     */
    UserRegistryProxy(String realm, List<UserRegistry> delegates) {
        this.realm = realm;
        this.delegates = delegates;
        if (delegates.size() <= 1) {
            throw new IllegalArgumentException("Using the UserRegistryProxy on a set of 1 or less delegates is pointless and/or meaningless");
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        // TODO: what about delegate1.getRealm()+delegate2.getRelam() ?
        return realm;
    }

    /** {@inheritDoc} */
    @Override
    public String checkPassword(String userSecurityName, @Sensitive String password) throws RegistryException {
        for (UserRegistry registry : delegates) {
            String ret = registry.checkPassword(userSecurityName, password);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String mapCertificate(X509Certificate cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.mapCertificate(cert);
            } catch (CertificateMapNotSupportedException cmnse) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "CertificateMapNotSupportedException caught on mapCertificate", registry, cert, cmnse);
                }
            } catch (CertificateMapFailedException cmfe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "CertificateMapFailedException caught on mapCertificate", registry, cert, cmfe);
                }
            }
        }
        throw new CertificateMapFailedException("Unable to map certificate: " + cert);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        for (UserRegistry registry : delegates) {
            if (registry.isValidUser(userSecurityName)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        SearchResult ret = new SearchResult();
        for (UserRegistry registry : delegates) {
            SearchResult result = registry.getUsers(pattern, limit);
            ret = mergeSearchResults(ret, result);
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getUserDisplayName(userSecurityName);
            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getUserDisplayName", registry, userSecurityName, enf);
                }
            }
        }
        throw new EntryNotFoundException(userSecurityName + " does not exist");
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getUniqueUserId(userSecurityName);
            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getUniqueUserId", registry, userSecurityName, enf);
                }
            }
        }
        throw new EntryNotFoundException(userSecurityName + " does not exist");
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getUserSecurityName(uniqueUserId);
            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getUserSecurityName", registry, uniqueUserId, enf);
                }
            }
        }
        throw new EntryNotFoundException(uniqueUserId + " does not exist");
    }

    /**
     * (@inheritedDoc)
     *
     * @throws RegistryException
     */

    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getUsersForGroup(groupSecurityName, limit);
            } catch (NotImplementedException nie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NotImplementedException caught on getUsersForGroup", registry, groupSecurityName, limit);
                }
                throw new NotImplementedException("getUsersForGroup not implemented");

            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getUsersForGroup", registry, groupSecurityName, limit);
                }

            } catch (CustomRegistryException re) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "CustomRegistryException caught on getUsersForGroup", registry, groupSecurityName, limit);
                }
            }
            throw new EntryNotFoundException(groupSecurityName + " does not exist.");
        }

        return null;

    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        for (UserRegistry registry : delegates) {
            if (registry.isValidGroup(groupSecurityName)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        SearchResult ret = new SearchResult();
        for (UserRegistry registry : delegates) {
            SearchResult result = registry.getGroups(pattern, limit);
            ret = mergeSearchResults(ret, result);
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getGroupDisplayName(groupSecurityName);
            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getGroupDisplayName", registry, groupSecurityName, enf);
                }
            }
        }
        throw new EntryNotFoundException(groupSecurityName + " does not exist");
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getUniqueGroupId(groupSecurityName);
            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getUniqueGroupId", registry, groupSecurityName, enf);
                }
            }
        }
        throw new EntryNotFoundException(groupSecurityName + " does not exist");
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        for (UserRegistry registry : delegates) {
            try {
                return registry.getGroupSecurityName(uniqueGroupId);
            } catch (EntryNotFoundException enf) {
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getGroupSecurityName", registry, uniqueGroupId, enf);
                }
            }
        }
        throw new EntryNotFoundException(uniqueGroupId + " does not exist");
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        int attempts = 0;
        int exceptions = 0;
        List<String> ret = new ArrayList<String>();
        for (UserRegistry registry : delegates) {
            attempts++;
            try {
                List<String> result = registry.getUniqueGroupIdsForUser(uniqueUserId);
                ret.addAll(result);
            } catch (EntryNotFoundException enf) {
                exceptions++;
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getUniqueGroupIdsForUser", registry, uniqueUserId, enf);
                }
            }
        }
        if (attempts == exceptions) {
            throw new EntryNotFoundException(uniqueUserId + " does not exist");
        } else {
            return ret;
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(EntryNotFoundException.class)
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        int attempts = 0;
        int exceptions = 0;
        List<String> ret = new ArrayList<String>();
        for (UserRegistry registry : delegates) {
            attempts++;
            try {
                List<String> result = registry.getGroupsForUser(userSecurityName);
                ret.addAll(result);
            } catch (EntryNotFoundException enf) {
                exceptions++;
                // We ignore this and keep trying. If we can't find we'll throw below
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EntryNotFoundException caught on getGroupsForUser", registry, userSecurityName, enf);
                }
            }
        }
        if (attempts == exceptions) {
            throw new EntryNotFoundException(userSecurityName + " does not exist");
        } else {
            return ret;
        }
    }

    /**
     * Merges the <code>source</code> SearchResult into the <code>dest</code> SearchResult.
     * The merge of the List is a union of both lists.
     * The merge of hasMore is via logical OR.
     *
     * @param inDest SearchResult object
     * @param source SearchResult object
     * @return a merged SearchResult.
     */
    private SearchResult mergeSearchResults(SearchResult inDest, SearchResult source) {
        List<String> list = inDest.getList();
        list.addAll(source.getList());
        boolean hasMore = inDest.hasMore() || source.hasMore();
        return new SearchResult(list, hasMore);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        return UserRegistryServiceImpl.UNKNOWN_TYPE;
    }

}
