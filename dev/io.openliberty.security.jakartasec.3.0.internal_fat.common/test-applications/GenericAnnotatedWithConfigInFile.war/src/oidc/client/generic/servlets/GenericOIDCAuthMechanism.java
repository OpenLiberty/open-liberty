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
package oidc.client.generic.servlets;

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

// TODO - needs updates once providerMetadata can handle EL
// use openIdConfig values instead of the provider bean value - this will allow the copies of this app to use their own config
// specified OPs
@WebServlet("/GenericOIDCAuthMechanism")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${openIdConfig.providerBase}",
                                         clientId = "${openIdConfig.clientId}", clientSecret = "${openIdConfig.clientSecret}", redirectURI = "${openIdConfig.redirectURI}",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         //                                         useSessionExpression = "${openIdConfig.useSessionExpression}",
                                         //                                         redirectToOriginalResource = false,
                                         //                                         redirectToOriginalResourceExpression = "${openIdConfig.redirectToOriginalResource}", // overrides specified value
                                         providerMetadata = @OpenIdProviderMetadata(authorizationEndpoint = "https://localhost:8920/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "https://localhost:8920/oidc/endpoint/OP1/token"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class GenericOIDCAuthMechanism extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void recordHelloWorld(ServletOutputStream outputStream) throws IOException {

        super.recordHelloWorld(outputStream);
        ServletLogger.printLine(outputStream, "Hello world from GenericOIDCAuthMechanism");

    }

}
