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
package oidc.client.noProviderURIInAnnotationWithProviderMetadata.servlets;

import java.io.IOException;

import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;
import oidc.client.base.utils.ServletLogger;

// TODO replace hard coded host/port info once EL is supported in providerMetadata
@WebServlet("/NoProviderURIInAnnotationWithProviderMetadataServlet")
@OpenIdAuthenticationMechanismDefinition(clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groupIds"),
                                         redirectURI = "${providerBean.clientSecureRoot}/NoProviderURIInAnnotationWithProviderMetadata/Callback",
                                         providerMetadata = @OpenIdProviderMetadata(authorizationEndpoint = "https://localhost:8920/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "https://localhost:8920/oidc/endpoint/OP1/token",
                                                                                    issuer = "https://localhost:8920/oidc/endpoint/OP1",
                                                                                    idTokenSigningAlgorithmsSupported = "RS256",
                                                                                    jwksURI = "",
                                                                                    responseTypeSupported = "code,id_token,token id_token",
                                                                                    subjectTypeSupported = "public",
                                                                                    userinfoEndpoint = ""))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class NoProviderURIInAnnotationWithProviderMetadataServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void recordHelloWorld(ServletOutputStream outputStream) throws IOException {

        super.recordHelloWorld(outputStream);
        ServletLogger.printLine(outputStream, "Hello world from NoProviderURIInAnnotationWithProviderMetadataServlet");

    }
}
