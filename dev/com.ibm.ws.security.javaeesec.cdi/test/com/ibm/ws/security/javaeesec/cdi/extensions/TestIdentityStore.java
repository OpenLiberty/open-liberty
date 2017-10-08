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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;

import com.ibm.websphere.ras.annotation.Sensitive;

@Default
@ApplicationScoped
public class TestIdentityStore implements IdentityStore {

    public TestIdentityStore() {}

    public CredentialValidationResult validate(@Sensitive BasicAuthenticationCredential cred) {
        return CredentialValidationResult.INVALID_RESULT;
    }

    public CredentialValidationResult validate(@Sensitive UsernamePasswordCredential cred) {
        return CredentialValidationResult.INVALID_RESULT;
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        return null;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Set<IdentityStore.ValidationType> validationTypes() {
        return IdentityStore.DEFAULT_VALIDATION_TYPES; // Contains VALIDATE and PROVIDE_GROUPS
    }
}
