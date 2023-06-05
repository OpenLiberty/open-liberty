/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package oidc.client.basicLogout.servlets;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.servlets.BaseServlet;
import oidc.client.base.utils.ServletLogger;

@WebServlet("/BasicLogoutServlet")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerURI}", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         redirectURI = "${baseURL}/Callback",
                                         jwksReadTimeoutExpression = "${openIdConfig.jwksReadTimeoutExpression}",
                                         tokenMinValidity = 0, // tokens used with these tests have very short lifetimes so the test cases don't have to sleep too long - keep this small to allow such short lifetimes
                                         promptExpression = "${openIdConfig.promptExpression}",
                                         logout = @LogoutDefinition(notifyProviderExpression = "${openIdConfig.notifyProviderExpression}",
                                                                    redirectURI = "${openIdConfig.logoutRedirectURI}",
                                                                    accessTokenExpiryExpression = "${openIdConfig.accessTokenExpiryExpression}",
                                                                    identityTokenExpiryExpression = "${openIdConfig.identityTokenExpiryExpression}"),
                                         providerMetadata = @OpenIdProviderMetadata(endSessionEndpoint = "${openIdConfig.endSessionEndpoint}"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class BasicLogoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();
        ServletLogger.printLine(outputStream, "Reached BasicLogoutServlet");
        ServletLogger.printLine(outputStream, "BasicLogoutServlet: Start");

        Enumeration<String> parmNames = request.getParameterNames();
        while (parmNames.hasMoreElements()) {
            String key = parmNames.asIterator().next();
            ServletLogger.printLine(outputStream, "BasicLogoutServlet - parmKey: " + key + " parmValue: " + request.getParameter(key));
        }

        request.logout();
        // In some cases the response is already committed before we return to the app, so, catch that specific exception on the print that follows
        try {
            ServletLogger.printLine(outputStream, "BasicLogoutServlet: End");
        } catch (IOException io) {
            System.out.println("Ignoring an IO exception due to the already closed stream when we try to log that the logout app has completed.");
        }

    }
}
