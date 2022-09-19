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

import java.util.Collections;
import java.util.Set;

import io.openliberty.security.jakartasec.credential.OidcTokensCredential;
import io.openliberty.security.oidcclientcore.client.ClaimsMappingConfig;
import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
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
            OidcTokensCredential castCredential = (OidcTokensCredential) credential;
            TokenResponse tokenResponse = castCredential.getTokenResponse();
            Client client = castCredential.getClient();
            if (tokenResponse != null && client != null) {
                try {
                    client.validate(tokenResponse);
                    return createSuccessfulCredentialValidationResult(client.getOidcClientConfig(), tokenResponse);
                } catch (Exception e) {
                    return CredentialValidationResult.INVALID_RESULT;
                }
            }
        }
        return CredentialValidationResult.INVALID_RESULT;
    }

    CredentialValidationResult createSuccessfulCredentialValidationResult(OidcClientConfig clientConfig, TokenResponse tokenResponse) {
        String storeId = clientConfig.getClientId();
        String caller = getCallerName(clientConfig, tokenResponse);
        Set<String> groups = getCallerGroups(clientConfig, tokenResponse);
        return new CredentialValidationResult(storeId, caller, null, caller, groups);
    }

    String getCallerName(OidcClientConfig clientConfig, TokenResponse tokenResponse) {
        String callerNameClaim = getCallerNameClaim(clientConfig);
        if (callerNameClaim == null || callerNameClaim.isEmpty()) {
            return null;
        }
        // TODO
        return null;
    }

    Set<String> getCallerGroups(OidcClientConfig clientConfig, TokenResponse tokenResponse) {
        String callerGroupClaim = getCallerGroupsClaim(clientConfig);
        if (callerGroupClaim == null || callerGroupClaim.isEmpty()) {
            return null;
        }
        // TODO
        return Collections.emptySet();
    }

    String getCallerNameClaim(OidcClientConfig clientConfig) {
        ClaimsMappingConfig claimsMappingConfig = clientConfig.getClaimsMappingConfig();
        if (claimsMappingConfig != null) {
            return claimsMappingConfig.getCallerNameClaim();
        }
        return null;
    }

    String getCallerGroupsClaim(OidcClientConfig clientConfig) {
        ClaimsMappingConfig claimsMappingConfig = clientConfig.getClaimsMappingConfig();
        if (claimsMappingConfig != null) {
            return claimsMappingConfig.getCallerGroupsClaim();
        }
        return null;
    }

}