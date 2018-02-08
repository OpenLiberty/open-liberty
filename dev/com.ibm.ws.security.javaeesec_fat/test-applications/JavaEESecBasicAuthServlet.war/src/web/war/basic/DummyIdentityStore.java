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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStorePermission;

@Named("web.DummyIdentityStore")
@ApplicationScoped
public class DummyIdentityStore implements IdentityStore {

    private static final String sourceClass = DummyIdentityStore.class.getName();
    private final Logger logger = Logger.getLogger(sourceClass);

    public CredentialValidationResult validate(BasicAuthenticationCredential basicAuthenticationCredential) {
        return validate(new UsernamePasswordCredential(basicAuthenticationCredential.getCaller(), basicAuthenticationCredential.getPasswordAsString()));
    }

    public CredentialValidationResult validate(UsernamePasswordCredential usernamePasswordCredential) {

        logger.entering(sourceClass, "validate", usernamePasswordCredential);
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;

        // FOR TESTING ONLY!!! NEVER DO THIS FROM A REAL IDENTITY STORE
        if (usernamePasswordCredential.getCaller().startsWith("jaspiuser1") && "s3cur1ty".equals(usernamePasswordCredential.getPasswordAsString())) {
            String securityName = usernamePasswordCredential.getCaller().toLowerCase();
            CallerPrincipal callerPrincipal = new CallerPrincipal(securityName);
            Set<String> groups = new HashSet<String>();
            groups.add("group1");
            result = new CredentialValidationResult("DummyIdentityStore", callerPrincipal, securityName + "_DN", securityName + "_UID", groups);
        }
        logger.exiting(sourceClass, "validate", result);
        return result;
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
