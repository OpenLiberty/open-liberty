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
package oidc.simple.client.withEL.servlets;

import java.io.IOException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.SimpleServlet;

@WebServlet("/OidcAnnotatedServletWithEL")
@OpenIdAuthenticationMechanismDefinition(
                                         providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1",
                                         clientId = "${openIdConfig.clientId}",
                                         clientSecret = "${openIdConfig.clientSecret}",
//                                         redirectURI = "${openIdConfig.redirectURI}",
                                         redirectURI = "${providerBean.clientSecureRoot}/SimplestAnnotatedWithEL/Callback", // update when baseURL or EL within an EL works
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}"),
                                         providerMetadata = @OpenIdProviderMetadata(
                                                                                    authorizationEndpoint = "https://localhost:8920/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "https://localhost:8920/oidc/endpoint/OP1/token"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class OidcAnnotatedServletWitEL extends SimpleServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void recordHelloWorld(ServletOutputStream output) throws IOException {

        super.recordHelloWorld(output);
        System.out.println("Hello world from OidcAnnotatedServletWitEL");
        output.println("Hello world from OidcAnnotatedServletWitEL!");

    }

}
