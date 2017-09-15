/*******************************************************************************
 * Copyright (c) 2012, 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.urbridge.util;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.security.registry.CustomRegistryException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.NotImplementedException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

public class DummyRegistry implements UserRegistry {

    @Override
    public String checkPassword(String userSecurityName, String password) throws RegistryException {
        if ("testUser".equals(userSecurityName)) {
            if ("password".equals(password)) {
                return "testUser";
            } else {
                throw new RegistryException("Incorrect Password");
            }
        } else {
            throw new RegistryException("Unknown user");
        }
    }

    @Override
    public String getRealm() {
        return "dummyRealm";
    }

    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        SearchResult result = null;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher("testUser");
        if (m.matches()) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("testUser");
            result = new SearchResult(list, true);
        } else {
            ArrayList<String> list = new ArrayList<String>();
            result = new SearchResult(list, false);
        }
        return result;
    }

    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException {
        return null;
    }

    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException {
        return null;
    }

    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException {
        return null;
    }

    @Override
    public boolean isValidUser(String userSecurityName) {
        return false;
    }

    @Override
    public SearchResult getGroups(String pattern, int limit) {
        SearchResult result = null;
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher("testGroup");
        if (m.matches()) {
            ArrayList<String> list = new ArrayList<String>();
            list.add("testUser");
            result = new SearchResult(list, true);
        } else {
            ArrayList<String> list = new ArrayList<String>();
            result = new SearchResult(list, false);
        }
        return result;
    }

    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException {
        return null;
    }

    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException {
        return null;
    }

    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException {
        return null;
    }

    @Override
    public boolean isValidGroup(String groupSecurityName) {
        return false;
    }

    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException {
        return null;
    }

    @Override
    public SearchResult getUsersForGroup(String groupSecurityName,
                                         int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException, RegistryException {
        return null;
    }

    @Override
    public String mapCertificate(X509Certificate cert) throws com.ibm.ws.security.registry.CertificateMapNotSupportedException, com.ibm.ws.security.registry.CertificateMapFailedException, RegistryException {
        return null;
    }

    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws com.ibm.ws.security.registry.EntryNotFoundException, RegistryException {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }
}
