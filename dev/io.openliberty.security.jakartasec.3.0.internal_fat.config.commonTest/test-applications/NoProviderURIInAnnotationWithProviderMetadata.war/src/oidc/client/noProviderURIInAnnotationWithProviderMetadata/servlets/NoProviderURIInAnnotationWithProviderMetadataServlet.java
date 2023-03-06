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
package oidc.client.noProviderURIInAnnotationWithProviderMetadata.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdProviderMetadata;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

// TODO replace hard coded host/port info once EL is supported in providerMetadata
@WebServlet("/NoProviderURIInAnnotationWithProviderMetadataServlet")
@OpenIdAuthenticationMechanismDefinition(clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groupIds"),
                                         redirectURI = "${providerBean.clientSecureRoot}/NoProviderURIInAnnotationWithProviderMetadata/Callback",
                                         jwksReadTimeout = Constants.OVERRIDE_DEFAULT_JWKS_READ_TIMEOUT,
                                         providerMetadata = @OpenIdProviderMetadata(authorizationEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/authorize",
                                                                                    tokenEndpoint = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/token",
                                                                                    issuer = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1",
                                                                                    idTokenSigningAlgorithmsSupported = "RS256",
                                                                                    jwksURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1/jwk",
                                                                                    responseTypeSupported = "code,id_token,token id_token",
                                                                                    subjectTypeSupported = "public",
                                                                                    userinfoEndpoint = ""))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class NoProviderURIInAnnotationWithProviderMetadataServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
