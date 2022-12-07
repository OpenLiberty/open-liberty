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
package oidc.client.noProviderURIInAnnotationWithoutProviderMetadata.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/NoProviderURIInAnnotationWithoutProviderMetadataServlet")
@OpenIdAuthenticationMechanismDefinition(
                                         clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         redirectURI = "${providerBean.clientSecureRoot}/NoProviderURIInAnnotationWithoutProviderMetadata/Callback",
                                         jwksReadTimeout = Constants.OVERRIDE_DEFAULT_JWKS_CONN_TIMEOUT,
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "sub", callerGroupsClaim = "groupIds"))
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class NoProviderURIInAnnotationWithoutProviderMetadataServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
