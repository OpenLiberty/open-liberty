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
package oidc.client.withELAltOPAndRole.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/OidcAnnotatedServletWithEL")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP2", clientId = "${openIdConfig.clientId}",
                                         clientSecret = "${openIdConfig.clientSecret}",
                                         redirectURI = "${baseURL}/Callback",
                                         jwksReadTimeoutExpression = "${openIdConfig.jwksReadTimeoutExpression}",
                                         redirectToOriginalResourceExpression = "${openIdConfig.redirectToOriginalResourceExpression}",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         providerMetadata = @OpenIdProviderMetadata(authorizationEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP2/authorize",
                                                                                    tokenEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP2/token"))
@DeclareRoles("group4")
@ServletSecurity(@HttpConstraint(rolesAllowed = "group4"))
public class OidcAnnotatedServletWithEL extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
