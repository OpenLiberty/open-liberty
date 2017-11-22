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
package web.war.identitystores.custom.grouponly;

import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStorePermission;

import com.ibm.websphere.simplicity.log.Log;

@Named("GroupOnlyIdentityStore200")
@ApplicationScoped
public class GroupOnlyIdentityStore200 implements IdentityStore {

    private static Logger log = Logger.getLogger(GroupOnlyIdentityStore200.class.getName());

    public CredentialValidationResult validate(BasicAuthenticationCredential basicAuthenticationCredential) {
        log.info("validate");
        throw new UnsupportedOperationException("validate method is not implemented.");
    }

    public CredentialValidationResult validate(UsernamePasswordCredential usernamePasswordCredential) {
        log.info("validate");
        throw new UnsupportedOperationException("validate method is not implemented.");
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new IdentityStorePermission("getGroups"));
        }
        // if user id contains "user", add groups."
        if (validationResult.getStatus() == CredentialValidationResult.Status.VALID) {
            if (validationResult.getCallerPrincipal().getName().contains("user")) {
                Set<String> groups = new HashSet<String>();
                groups.add("grantedgroup");
                groups.add("grantedgroup2");
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
        return 200;
    }

    @Override
    public Set<IdentityStore.ValidationType> validationTypes() {
        return EnumSet.of(IdentityStore.ValidationType.PROVIDE_GROUPS);
    }
}
