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

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;

/**
 * Liberty's database {@link IdentityStore} implementation.
 */
@Default
@ApplicationScoped
public class DatabaseIdentityStore implements IdentityStore {

    /** The definitions for this IdentityStore. */
    private final DatabaseIdentityStoreDefinitionWrapper idStoreDefinition;

    /**
     * Construct a new {@link DatabaseIdentityStore} instance using the specified definitions.
     *
     * @param idStoreDefinition The definitions to use to configure the {@link IdentityStore}.
     */
    public DatabaseIdentityStore(DatabaseIdentityStoreDefinition idStoreDefinition) {
        this.idStoreDefinition = new DatabaseIdentityStoreDefinitionWrapper(idStoreDefinition);
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        // TODO Add group lookup.
        throw new UnsupportedOperationException(getClass().getName() + " does not yet implement the 'getCallerGroups(CredentialValidationResult)' method.");
    }

    @Override
    public int priority() {
        return idStoreDefinition.getPriority();
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        /*
         * Only support UserPasswordCredential.
         */
        if (!(credential instanceof UsernamePasswordCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        // TODO Add validation.
        throw new UnsupportedOperationException(getClass().getName() + " does not yet implement the 'validate(Credential)' method.");
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return idStoreDefinition.getUseFor();
    }
}
