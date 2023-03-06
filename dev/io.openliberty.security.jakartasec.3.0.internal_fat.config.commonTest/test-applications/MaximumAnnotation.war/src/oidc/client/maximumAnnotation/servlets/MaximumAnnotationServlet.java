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
package oidc.client.maximumAnnotation.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

// TODO - Make sure all possible attrs using EL values
@WebServlet("/MaximumAnnotationServlet")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1",
                                         clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groupIds"),
                                         logout = @LogoutDefinition(),
                                         redirectURI = "${providerBean.clientSecureRoot}/MaximumAnnotation/Callback",
                                         redirectToOriginalResource = false,
                                         redirectToOriginalResourceExpression = "",
                                         scope = { Constants.OPENID_SCOPE, Constants.EMAIL_SCOPE, Constants.PROFILE_SCOPE },
                                         scopeExpression = "",
                                         responseType = Constants.CODE_FLOW,
                                         responseMode = "", prompt = {},
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
                                         jwksReadTimeout = Constants.OVERRIDE_DEFAULT_JWKS_READ_TIMEOUT,
                                         tokenAutoRefresh = false,
                                         tokenAutoRefreshExpression = "",
                                         tokenMinValidity = Constants.DEFAULT_TOKEN_MIN_VALIDITY,
                                         tokenMinValidityExpression = "",
                                         providerMetadata = @OpenIdProviderMetadata(authorizationEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/token"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class MaximumAnnotationServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
