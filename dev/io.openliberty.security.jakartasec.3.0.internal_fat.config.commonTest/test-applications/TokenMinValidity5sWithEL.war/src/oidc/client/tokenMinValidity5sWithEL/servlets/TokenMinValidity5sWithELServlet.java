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
package oidc.client.tokenMinValidity5sWithEL.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/TokenMinValidity5sWithELServlet")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1",
                                         clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groupIds"),
                                         redirectURI = "${baseURL}/Callback",
                                         jwksReadTimeoutExpression = "${openIdConfig.jwksReadTimeoutExpression}",
                                         tokenMinValidity = 5 * 1000,
                                         tokenMinValidityExpression = "${openIdConfig.tokenMinValidityExpression}",
                                         prompt = PromptType.LOGIN,
                                         logout = @LogoutDefinition(accessTokenExpiry = true, identityTokenExpiry = true))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class TokenMinValidity5sWithELServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

}
