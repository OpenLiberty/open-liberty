/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.multipart;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;

/**
 * Registers the OpenID Connect {@code HttpAuthenticationMechanism} (the HAM from issue
 * 35585). The provider information is explicit and the OpenID Provider does not need to be
 * running: every route in this application is unprotected, so the mechanism never starts a
 * login flow. It is still invoked for every request, which is what makes it read the
 * request body before Jakarta REST has a chance to.
 */
@OpenIdAuthenticationMechanismDefinition(
        providerURI = "https://localhost:8920/oidc/endpoint/OP1",
        clientId = "client01",
        clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
        redirectURI = "${baseURL}/Callback",
        claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groupIds"),
        providerMetadata = @OpenIdProviderMetadata(
                authorizationEndpoint = "https://localhost:8920/oidc/endpoint/OP1/authorize",
                tokenEndpoint = "https://localhost:8920/oidc/endpoint/OP1/token"))
@ApplicationScoped
public class MultipartOidcConfig {
}
