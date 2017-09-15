/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStorePermission;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.security.registry.RegistryHelper;

@Default
@ApplicationScoped
public class DummyLdapIdentityStore implements IdentityStore {

    private static final TraceComponent tc = Tr.register(DummyLdapIdentityStore.class);

    public DummyLdapIdentityStore() {}

    public CredentialValidationResult validate(@Sensitive BasicAuthenticationCredential cred) {
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
        if (cred != null) {
            result = validate(cred.getCaller(), cred.getPasswordAsString());
        }
        return result;
    }

    public CredentialValidationResult validate(@Sensitive UsernamePasswordCredential cred) {
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
        if (cred != null) {
            result = validate(cred.getCaller(), cred.getPasswordAsString());
        }
        return result;
    }

    @FFDCIgnore({ com.ibm.websphere.security.PasswordCheckFailedException.class })
    public CredentialValidationResult validate(String user, @Sensitive String password) {
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
        UserRegistry registry = getUserRegistry();
        if (registry != null) {
            try {
                String userSecurityName = registry.checkPassword(user, password);
                List<String> groupNames = registry.getGroupsForUser(userSecurityName);
                Set<String> uniqueGroups = new HashSet<String>();
                if (groupNames != null) {
                    for (String group : groupNames) {
                        uniqueGroups.add(registry.getUniqueGroupId(group));
                    }
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "validated successfully. username : " + user + ", callerUniqueId : " + userSecurityName);
                }
                result = new CredentialValidationResult(this.toString(), user, userSecurityName, userSecurityName, uniqueGroups);
            } catch (PasswordCheckFailedException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkUserPassword - password is not valid.");
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkUserPassword - registry exception: " + e);
                }
            }
        }
        return result;
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new IdentityStorePermission("getGroups"));
        }
        Set<String> groups = new HashSet<String>();
        groups.add("ldapGroup1");
        groups.add(validationResult.getCallerPrincipal().getName() + "Group");
        return groups;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Set<IdentityStore.ValidationType> validationTypes() {
//        return EnumSet.of(IdentityStore.ValidationType.VALIDATE);
//        return EnumSet.of(IdentityStore.ValidationType.PROVIDE_GROUPS);
        return IdentityStore.DEFAULT_VALIDATION_TYPES; // Contains VALIDATE and PROVIDE_GROUPS
    }

    UserRegistry getUserRegistry() {
        UserRegistry registry = null;
        try {
            registry = RegistryHelper.getUserRegistry(null);
        } catch (WSSecurityException e) {
            // TODO message i guess?
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error getting the user registry", e);
        }
        return registry;
    }
}
