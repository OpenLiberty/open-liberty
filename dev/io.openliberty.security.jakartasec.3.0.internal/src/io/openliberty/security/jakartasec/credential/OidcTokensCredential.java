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

import io.openliberty.security.oidcclientcore.token.TokenResponse;
import jakarta.security.enterprise.credential.Credential;

/**
 *
 */
public class OidcTokensCredential implements Credential {
    
    TokenResponse tokenResponse;
    
    public OidcTokensCredential(TokenResponse tokenResponse) {
        this.tokenResponse = tokenResponse;
    }
    
    public TokenResponse getTokenResponse() {
        return this.tokenResponse;
    }

}
