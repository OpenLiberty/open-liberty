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
package oidc.authAppNoInjection.servlets;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.simple.servlets.SimpleServlet;

@WebServlet("/AuthenticationApp")
@ApplicationScoped
@OpenIdAuthenticationMechanismDefinition(providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         jwksReadTimeoutExpression = "60000",
                                         redirectToOriginalResourceExpression = "${openIdConfig.redirectToOriginalResourceExpression}",
                                         useSessionExpression = "${openIdConfig.useSessionExpression}",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub",
                                                                              callerGroupsClaim = "groupIds"))

public class AuthenticationAppServlet extends SimpleServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private SecurityContext securityContext;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String useNewAuth = request.getParameter("useNewAuth");

        // TODO - need to modify this next line to handle cases where we call the app a second time (from the test) with a different user - when we're testing that we do actually honor the newAuthentiction setting
        // TODO - pass another parm like user1 - the test case will login using user1 on the first pass, then use user2/testuser on the second request and newAuth set to true should force another login
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
        recordAppInfo(request, outputStream);

//        ServletOutputStream ps = response.getOutputStream();
//
//        ServletLogger.printLine(ps, "Class: " + this.getClass().getName());
//        ServletLogger.printLine(ps, "Super Class: " + this.getClass().getSuperclass().getName());
//
//        RequestLogger requestLogger = new RequestLogger(request, "Auth " + ServletMessageConstants.SERVLET + ServletMessageConstants.REQUEST);
//        requestLogger.printRequest(ps);
//
//        OpenIdContextLogger contextLogger = new OpenIdContextLogger(request, response, "Auth " + ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT, context);
//        contextLogger.logContext(ps);
//
//        WSSubjectLogger subjectLogger = new WSSubjectLogger(request, "Auth " + ServletMessageConstants.SERVLET + ServletMessageConstants.WSSUBJECT);
//        subjectLogger.printProgrammaticApiValues(ps);

//        securityContext.authenticate(request, response, AuthenticationParameters.withParams().newAuthentication(true));

//        ServletOutputStream outputStream = response.getOutputStream();

//        ServletLogger.printLine(outputStream, "Security context principal After authentication: " + securityContext.getCallerPrincipal());
//        ServletLogger.printLine(outputStream, "Completed Authenticating");

//        }

//        recordAppInfo(request, response, outputStream);
//
//        ServletLogger.printLine(outputStream, "Completed calling recordAppInfo");

//        super.doGet(request, response);

//        if (request.getParameter(Constants.LOGOUT) != null) {
//            request.logout();
//        }

    }

}
