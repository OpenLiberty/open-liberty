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
package oidc.authAppInjection.servlets;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/AuthenticationApp")
@ApplicationScoped
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerURI}", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         jwksReadTimeoutExpression = "60000",
                                         redirectToOriginalResourceExpression = "${openIdConfig.redirectToOriginalResourceExpression}",
                                         useSessionExpression = "${openIdConfig.useSessionExpression}",
                                         prompt = PromptType.LOGIN,
                                         logout = @LogoutDefinition(accessTokenExpiryExpression = "${openIdConfig.accessTokenExpiryExpression}",
                                                                    identityTokenExpiryExpression = "${openIdConfig.identityTokenExpiryExpression}"),
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub",
                                                                              callerGroupsClaim = "groupIds"))

public class AuthenticationAppServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private SecurityContext securityContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String useNewAuth = request.getParameter("useNewAuth");

        if (counter.incrementAndGet() == 1 || request.getUserPrincipal() == null) {
            System.out.println("Starting Authenticating");
            if (useNewAuth == null) {
                System.out.println("New Auth will not be set in AuthenticationParameters");
                securityContext.authenticate(request, response, AuthenticationParameters.withParams());
            } else {
                if (useNewAuth.equals("false")) {
                    System.out.println("New Auth will be set to \"false\" in AuthenticationParameters");
                    securityContext.authenticate(request, response, AuthenticationParameters.withParams().newAuthentication(false));
                } else {
                    System.out.println("New Auth will be set to \"true\" in AuthenticationParameters");
                    securityContext.authenticate(request, response, AuthenticationParameters.withParams().newAuthentication(true));
                }
            }
            System.out.println("Completed Authenticating");
        } else {
            System.out.println("Already Authenticated");
        }

        ServletOutputStream outputStream = response.getOutputStream();
        recordAppInfo(request, response, outputStream);

    }

}
