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
package web.jar.common.identitystores;

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
public class CommonIdentityStore implements IdentityStore {
    
    private static final String sourceClass = CommonIdentityStore.class.getName();
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
        if (usrPwdCredential.getCaller().startsWith("commonuser") && "s3cur1ty".equals(usrPwdCredential.getPasswordAsString())) {
            String securityName = usrPwdCredential.getCaller().toLowerCase();
            CallerPrincipal callerPrincipal = new CallerPrincipal(securityName);
            Set<String> groups = new HashSet<String>();
            groups.add("commonGroup1");
            groups.add("commonGroup2");
            result = new CredentialValidationResult("CommonIdentityStore",  callerPrincipal, securityName+"_DN", securityName+"_UID", groups);
        }
        logger.exiting(sourceClass, "validate", result);
        return result;
    }

}
