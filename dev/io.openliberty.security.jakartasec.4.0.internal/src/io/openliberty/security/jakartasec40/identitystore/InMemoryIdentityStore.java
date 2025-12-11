/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.identitystore;

import java.util.Collections;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

import io.openliberty.security.jakartasec40.identitystore.InMemoryIdentityStoreDefinitionWrapper.CredentialValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.security.enterprise.CallerPrincipal;
import jakarta.security.enterprise.credential.CallerOnlyCredential;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;

/**
 * Liberty's InMemory {@link IdentityStore} implementation.
 */
@Default
@ApplicationScoped
@Component(service = {},
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class InMemoryIdentityStore implements IdentityStore {

    private static final TraceComponent tc = Tr.register(InMemoryIdentityStore.class);

    /** The definitions for this IdentityStore. */
    private final InMemoryIdentityStoreDefinitionWrapper inMemoryIdentityStoreDefinitionWrapper;

    /**
     * Construct a new {@link InMemoryIdentityStore} instance using the specified definitions
     * from the annotation.
     *
     * @param inMemoryIdentityStoreDefinition The definitions to use to configure the {@link IdentityStore}.
     */

    public InMemoryIdentityStore(InMemoryIdentityStoreDefinition inMemoryIdentityStoreDefinition) {
        inMemoryIdentityStoreDefinitionWrapper = new InMemoryIdentityStoreDefinitionWrapper(inMemoryIdentityStoreDefinition);
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        if ((validationResult == null) || (!validationTypes().contains(ValidationType.PROVIDE_GROUPS))) {
            return Collections.emptySet();
        }

        CallerPrincipal callerPrincipal = validationResult.getCallerPrincipal();
        String caller = (callerPrincipal != null) ? callerPrincipal.getName() : null;

        if (caller == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A null caller was passed into getCallerGroups. No groups returned." + validationResult);
            }
            return Collections.emptySet();
        }
        CredentialValue credentialValue = inMemoryIdentityStoreDefinitionWrapper.getCredential(caller);
        if (credentialValue != null) {
            // found a caller, so set groups
            return Set.of(credentialValue.getGroups());
        }

        return Collections.emptySet();
    }

    @Override
    public int priority() {
        return inMemoryIdentityStoreDefinitionWrapper.getPriority();
    }

    @Override
    @Sensitive
    public CredentialValidationResult validate(Credential credential) {

        // first check the annotation allows for validation
        if (!validationTypes().contains(ValidationType.VALIDATE)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        // now extract client caller and password details
        String caller = null;
        ProtectedString password = null;
        if (credential instanceof UsernamePasswordCredential) {
            caller = ((UsernamePasswordCredential) credential).getCaller();
            password = new ProtectedString(((UsernamePasswordCredential) credential).getPassword().getValue());
        } else {
            Tr.error(tc, "JAKARTASEC_ERROR_WRONG_CRED");
            if (credential instanceof CallerOnlyCredential) {
                return CredentialValidationResult.INVALID_RESULT;
            } else {
                return CredentialValidationResult.NOT_VALIDATED_RESULT;
            }
        }

        if (caller == null) { // should be prevented when UsernamePasswordCredential is created.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A null caller was passed in.");
            }
            return CredentialValidationResult.INVALID_RESULT;
        }

        // finally, check password and if valid, then return success with groups
        if (isPasswordValid(caller, password)) {
            Set<String> groups = getCallerGroups(new CredentialValidationResult(null, caller, null, caller, null));
            return new CredentialValidationResult(null, caller, null, caller, groups);
        } else {
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    /**
     * Validate a client password given the caller name. The caller's existing
     * credentials (from the annotation) are fetched and compared with the passed
     * client password.
     *
     * @param caller   is the caller from the client request
     * @param password is the password from the client request
     * @return true if the caller exists as an annotation and the client password
     *         matches the annotation password. False else.
     */
    private boolean isPasswordValid(String caller, @Sensitive ProtectedString password) {

        CredentialValue credentialValue = inMemoryIdentityStoreDefinitionWrapper.getCredential(caller);
        if (credentialValue == null) {
            // caller not in annotation values
            return false;
        }

        // caller found, so validate its password against internal ones from annotation
        return credentialValue.validate(password);
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return inMemoryIdentityStoreDefinitionWrapper.getUseFor();
    }
}
