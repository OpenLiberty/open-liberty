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
package web.war.basic;

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

@Named("web.DummyIdentityStore")
@ApplicationScoped
public class DummyIdentityStore implements IdentityStore {

    private static Logger log = Logger.getLogger(DummyIdentityStore.class.getName());

    public CredentialValidationResult validate(BasicAuthenticationCredential basicAuthenticationCredential) {
        return validate(new UsernamePasswordCredential(basicAuthenticationCredential.getCaller(), basicAuthenticationCredential.getPasswordAsString()));
    }

    public CredentialValidationResult validate(UsernamePasswordCredential usernamePasswordCredential) {
        log.info("validate");
        Set<String> groups = new HashSet<String>();
        groups.add("group1");
        CredentialValidationResult result = new CredentialValidationResult("DummyIdentityStore", "jaspiuser1", "jaspiuser1", "jaspiuser1", groups);
        return result;
//        return CredentialValidationResult.INVALID_RESULT;
//        return CredentialValidationResult.NOT_VALIDATED_RESULT;
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new IdentityStorePermission("getGroups"));
        }
        Set<String> groups = new HashSet<String>();
        groups.add("group1");
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
}
