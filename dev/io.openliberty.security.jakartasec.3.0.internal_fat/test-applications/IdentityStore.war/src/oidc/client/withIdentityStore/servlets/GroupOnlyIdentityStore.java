/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.withIdentityStore.servlets;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import jakarta.enterprise.context.Dependent;
import jakarta.security.enterprise.credential.BasicAuthenticationCredential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

@Named("GroupOnlyIdentityStore")
@Dependent
@ApplicationScoped
public class GroupOnlyIdentityStore implements IdentityStore {

    private static String className = GroupOnlyIdentityStore.class.getName();
    private static Logger log = Logger.getLogger(GroupOnlyIdentityStore.class.getName());

    public CredentialValidationResult validate(BasicAuthenticationCredential basicAuthenticationCredential) {
        log.info(className + " - Basic validate " + basicAuthenticationCredential.toString());
        throw new UnsupportedOperationException("validate method is not implemented.");
    }

    public CredentialValidationResult validate(UsernamePasswordCredential usernamePasswordCredential) {
        log.info(className + " - Username validate " + usernamePasswordCredential.toString());
        throw new UnsupportedOperationException("validate method is not implemented.");
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        // do not check IdentityStorePermission, because this class itself is not granted by default.
        log.info(className + " - getCallerGroups");
        // if user id contains "user", add groups."
        if (validationResult.getStatus() == CredentialValidationResult.Status.VALID) {
            if (validationResult.getCallerPrincipal().getName().contains("user")) {
                Set<String> groups = new HashSet<String>();
                groups.add("group1");
                groups.add("idStoreGroup");
                return groups;
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("The status of CredentialValidationResult is not valid.");
        }
    }

    @Override
    public int priority() {
        log.info(className + " - priority");
        return 200;
    }

    @Override
    public Set<IdentityStore.ValidationType> validationTypes() {
        log.info(className + " - validationTypes");
        return EnumSet.of(IdentityStore.ValidationType.PROVIDE_GROUPS);
    }
}