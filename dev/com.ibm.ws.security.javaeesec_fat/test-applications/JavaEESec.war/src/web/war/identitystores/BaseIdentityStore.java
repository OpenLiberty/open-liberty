/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.identitystores;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

public class BaseIdentityStore implements IdentityStore {

    protected static String sourceClass = BaseIdentityStore.class.getName();
    private final Logger logger = Logger.getLogger(sourceClass);

    protected String expectedUser = "jaspiuser1";

    public CredentialValidationResult validate(BasicAuthenticationCredential basicAuthCredential) {
        logger.entering(sourceClass, "validate", basicAuthCredential);
        CredentialValidationResult result;
        // Validate BasicAuthenticationCredential, although the mechanism can validate BasicAuthenticationCredential.
        result = validate((UsernamePasswordCredential) basicAuthCredential);

        logger.exiting(sourceClass, "validate", result);
        return result;

    }

    public CredentialValidationResult validate(UsernamePasswordCredential usrPwdCredential) {
        logger.entering(sourceClass, "validate", usrPwdCredential);
        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;

        // FOR TESTING ONLY!!! NEVER DO THIS FROM A REAL IDENTITY STORE
        if (expectedUser.equals(usrPwdCredential.getCaller()) && "s3cur1ty".equals(usrPwdCredential.getPasswordAsString())) {
            CallerPrincipal callerPrincipal = new CallerPrincipal(expectedUser);
            Set<String> groups = new HashSet<String>();
            groups.add("group1");
            groups.add("group2");
            result = new CredentialValidationResult("BaseIdentityStore", callerPrincipal, null, expectedUser, groups);
        }

        logger.exiting(sourceClass, "validate", result);
        return result;
    }

}
