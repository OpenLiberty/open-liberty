/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.identitystore;

import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.oidcclientcore.client.Client;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 *
 */
public class OidcIdentityStore implements IdentityStore {

    public OidcIdentityStore() {

    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        // Use OidcTokensCredential to validate
        if (!(credential instanceof OidcTokensCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        } else if (credential.isValid()) {
            if (((OidcTokensCredential) credential).getTokenResponse() != null && ((OidcTokensCredential) credential).getClient() != null) {
                Client client = ((OidcTokensCredential) credential).getClient();
                try {

                    client.validate(((OidcTokensCredential) credential).getTokenResponse());
                    // TODO : have the client validation result to have access to the caller and group etc.. ?
                    // String caller =
                    // Set <String> groups =
                    // return (new CredentialValidationResult(id, caller, null, caller, groups));
                } catch (Exception e) {
                    return CredentialValidationResult.INVALID_RESULT;
                }
            }
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

}