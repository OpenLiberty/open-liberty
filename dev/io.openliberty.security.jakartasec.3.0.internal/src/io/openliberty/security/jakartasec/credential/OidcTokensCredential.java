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
package io.openliberty.security.jakartasec.credential;

import io.openliberty.security.oidcclientcore.client.Client;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.security.enterprise.credential.Credential;

/**
 *
 */
public class OidcTokensCredential implements Credential {
    
    TokenResponse tokenResponse;
    Client client;
    OidcClientConfig oidcClientConfig;
    
    public OidcTokensCredential(TokenResponse tokenResponse, Client client) {
        this.tokenResponse = tokenResponse;
        this.client = client;
    }
    
    public TokenResponse getTokenResponse() {
        return this.tokenResponse;
    }
    
    public Client getClient() {
        return this.client;
    }

}
