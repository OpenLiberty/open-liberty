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
package web.war.identitystores;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

@ApplicationScoped
public class Security10IdentityStore implements IdentityStore {
    
    private static final String sourceClass = Security10IdentityStore.class.getName();
    private Logger logger = Logger.getLogger(sourceClass);

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
        if ("jaspiuser1".equals(usrPwdCredential.getCaller()) && "s3cur1ty".equals(usrPwdCredential.getPasswordAsString())) {
            CallerPrincipal callerPrincipal = new CallerPrincipal("jaspiuser1");
            Set<String> groups = new HashSet<String>();
            groups.add("group1");
            groups.add("group2");
            result = new CredentialValidationResult(callerPrincipal, groups );
        }

        logger.exiting(sourceClass, "validate", result);
        return result;
    }

}
