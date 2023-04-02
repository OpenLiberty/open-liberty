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
package oidc.client.basicRefresh.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.RequestScoped;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

// Let discovery fill in the redirectURI in the LogoutDefinition
@WebServlet("/BasicRefreshServlet")
@RequestScoped
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerURI}", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         redirectURI = "${baseURL}/Callback",
                                         jwksReadTimeoutExpression = "${openIdConfig.jwksReadTimeoutExpression}",
                                         tokenMinValidity = 0, // tokens used with these tests have very short lifetimes so the test cases don't have to sleep too long - keep this small to allow such short lifetimes
                                         promptExpression = "${openIdConfig.promptExpression}",
                                         tokenAutoRefreshExpression = "${openIdConfig.tokenAutoRefreshExpression}",
                                         logout = @LogoutDefinition(notifyProviderExpression = "${openIdConfig.notifyProviderExpression}",
                                                                    redirectURI = "${openIdConfig.logoutRedirectURI}",
                                                                    accessTokenExpiryExpression = "${openIdConfig.accessTokenExpiryExpression}",
                                                                    identityTokenExpiryExpression = "${openIdConfig.identityTokenExpiryExpression}"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class BasicRefreshServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
