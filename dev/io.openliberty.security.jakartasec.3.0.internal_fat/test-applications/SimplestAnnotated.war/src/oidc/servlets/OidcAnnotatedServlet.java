/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.servlets;

import java.io.IOException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// TODO: Use a bean to get providerURI since port is dynamic
@WebServlet("/OidcAnnotatedServlet")
@OpenIdAuthenticationMechanismDefinition(
                                         providerURI = "https://localhost:9443/oidc/endpoint/OP",
                                         clientId = "client_id",
                                         clientSecret = "client_secret",
                                         redirectURI = "${baseURL}/Callback")
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class OidcAnnotatedServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public OidcAnnotatedServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: Extend a base servlet for diagnostics
        ServletOutputStream sos = response.getOutputStream();

        sos.println("Hello world!");
    }
}
