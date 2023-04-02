/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package oidc.client.generic.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

// TODO - needs updates once providerMetadata can handle EL
// use openIdConfig values instead of the provider bean value - this will allow the copies of this app to use their own config
// specified OPs
@WebServlet("/GenericOIDCAuthMechanism")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerBase}",
                                         clientId = "${openIdConfig.clientId}", clientSecret = "${openIdConfig.clientSecret}", redirectURI = "${openIdConfig.redirectURI}",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         //                                         useSessionExpression = "${openIdConfig.useSessionExpression}",
                                         //                                         redirectToOriginalResource = false,
                                         //                                         redirectToOriginalResourceExpression = "${openIdConfig.redirectToOriginalResource}", // overrides specified value
                                         responseType = "${openIdConfig.responseType}",
                                         scopeExpression = "${openIdConfig.scopeExpression}",
                                         responseMode = "${openIdConfig.responseMode}",
                                         jwksReadTimeoutExpression = "${openIdConfig.jwksReadTimeoutExpression}",
                                         providerMetadata = @OpenIdProviderMetadata(authorizationEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/token",
                                                                                    userinfoEndpoint = "${openIdConfig.userinfoEndpoint}",
                                                                                    idTokenSigningAlgorithmsSupported = "${openIdConfig.idTokenSigningAlgorithmsSupported}"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class GenericOIDCAuthMechanism extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
