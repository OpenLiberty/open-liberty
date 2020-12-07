/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.resolver.sample;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.wsspi.security.saml2.UserCredentialResolver;
import com.ibm.wsspi.security.saml2.UserIdentityException;

public class SamlCredentialResolver implements UserCredentialResolver {
    private final Properties properties = new Properties();
    static final String client_id = "client_id";
    static String resolverId = null;
    String userName = null;

    public SamlCredentialResolver(Dictionary<String, Object> serviceProps) {
        saveDictionary(serviceProps);
    }

    /** {@inheritDoc} */
    @Override
    public String mapSAMLAssertionToUser(Saml20Token token) throws UserIdentityException {
        String idpUserId = token.getSAMLNameID();
        if (token.getServiceProviderID().startsWith("sp")) {
            String oldUserId = idpUserId;
            String strNewUser = properties.getProperty(idpUserId);
            // allow null and empty for testing purposes    if (strNewUser != null && !strNewUser.isEmpty()) {
            if (strNewUser != null) {
                if (strNewUser.equals("null")) {
                    idpUserId = null;
                } else {
                    idpUserId = strNewUser;
                }
            } else {
                throw new UserIdentityException("No Mapping User to " + oldUserId);
            }
            System.out.println("mapSAMLAssertionToUser() oldId:" + oldUserId + "  newUserID:" + idpUserId);
        }

        if (idpUserId == null) {
            System.out.println("mapSAMLAssertionToUser() is returning a null value");
        }
        userName = idpUserId;
        return idpUserId;
    }

    void saveDictionary(Dictionary<String, ?> original) {
        Enumeration<String> keys = original.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            properties.put(key, original.get(key));
        }
        resolverId = (String) original.get("id");
    }

    /** {@inheritDoc} */
    @Override
    public List<String> mapSAMLAssertionToGroups(Saml20Token token) throws UserIdentityException {
        System.out.println("mapSAMLAssertionToGroups() 'Employee', '" + resolverId + "_group" + "'");
        List<String> groups = new ArrayList<String>();
        groups.add("Employee");
        groups.add(resolverId + "_group");
        return groups;
    }

    /** {@inheritDoc} */
    @Override
    public String mapSAMLAssertionToUserUniqueID(Saml20Token token) throws UserIdentityException {
        System.out.println("mapSAMLAssertionToUserUniqueID() '" + "user:" + resolverId + "_realm/" + userName + "'");
        return "user:" + resolverId + "_realm/" + userName;
    }

    /** {@inheritDoc} */
    @Override
    public String mapSAMLAssertionToRealm(Saml20Token token) throws UserIdentityException {
        System.out.println("mapSAMLAssertionToRealm() '" + resolverId + "_realm'");
        return resolverId + "_realm";
    }
}
