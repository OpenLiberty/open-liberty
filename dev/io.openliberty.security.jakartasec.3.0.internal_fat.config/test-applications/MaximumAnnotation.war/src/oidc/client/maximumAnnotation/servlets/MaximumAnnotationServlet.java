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
package oidc.client.maximumAnnotation.servlets;

import java.io.IOException;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.SimpleServlet;

// TODO - Make sure all possible attrs using EL values
@WebServlet("/MaximumAnnotationServlet")
@OpenIdAuthenticationMechanismDefinition(
                                         providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1",
                                         clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groups"),
                                         logout = @LogoutDefinition(),
                                         redirectURI = "${providerBean.clientSecureRoot}/MaximumAnnotation/Callback",
                                         redirectToOriginalResource = false,
                                         redirectToOriginalResourceExpression = "",
                                         scope = { Constants.OPENID_SCOPE, Constants.EMAIL_SCOPE, Constants.PROFILE_SCOPE },
                                         scopeExpression = "",
                                         responseType = Constants.CODE_FLOW,
                                         responseMode = "",
                                         prompt = {},
                                         promptExpression = "",
                                         display = DisplayType.PAGE,
                                         displayExpression = "",
                                         useNonce = true,
                                         useNonceExpression = "",
                                         useSession = true,
                                         useSessionExpression = "",
                                         extraParameters = {},
                                         extraParametersExpression = "",
                                         jwksConnectTimeout = Constants.DEFAULT_JWKS_CONN_TIMEOUT,
                                         jwksReadTimeoutExpression = "",
                                         tokenAutoRefresh = false,
                                         tokenAutoRefreshExpression = "",
                                         tokenMinValidity = Constants.TOKEN_MIN_VALIDITY,
                                         tokenMinValidityExpression = "",
                                         providerMetadata = @OpenIdProviderMetadata(
                                                                                    authorizationEndpoint = "https://localhost:8920/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "https://localhost:8920/oidc/endpoint/OP1/token"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class MaximumAnnotationServlet extends SimpleServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void recordHelloWorld(ServletOutputStream output) throws IOException {

        super.recordHelloWorld(output);
        System.out.println("Hello world from MaximumAnnotationServlet");
        output.println("Hello world from MaximumAnnotationServlet!");

    }
}
