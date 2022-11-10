/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.basicLogout.servlets;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

// Let discovery fill in the redirectURI in the LogoutDefinition
@WebServlet("/BasicLogoutServlet")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerURI}", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         redirectURI = "${baseURL}/Callback",
                                         tokenMinValidity = 0, // tokens used with these tests have very short lifetimes so the test cases don't have to sleep too long - keep this small to allow such short lifetimes
                                         promptExpression = "${openIdConfig.promptExpression}",
                                         logout = @LogoutDefinition(notifyProviderExpression = "${openIdConfig.notifyProviderExpression}",
                                                                    redirectURI = "${openIdConfig.logoutRedirectURI}",
                                                                    accessTokenExpiryExpression = "${openIdConfig.accessTokenExpiryExpression}",
                                                                    identityTokenExpiryExpression = "${openIdConfig.identityTokenExpiryExpression}"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class BasicLogoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
