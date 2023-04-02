/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.tokenTest.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/TokenTestApp")
@ApplicationScoped
@OpenIdAuthenticationMechanismDefinition(providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP3", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         jwksReadTimeoutExpression = "60000",
                                         useNonceExpression = "${openIdConfig.useNonceExpression}",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub",
                                                                              callerGroupsClaim = "groupIds"),
                                         providerMetadata = @OpenIdProviderMetadata(tokenEndpoint = "${openIdConfig.tokenEndpoint}",
                                                                                    issuer = "${openIdConfig.issuer}",
                                                                                    idTokenSigningAlgorithmsSupported = "HS256"))

@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class TokenTestAppServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}