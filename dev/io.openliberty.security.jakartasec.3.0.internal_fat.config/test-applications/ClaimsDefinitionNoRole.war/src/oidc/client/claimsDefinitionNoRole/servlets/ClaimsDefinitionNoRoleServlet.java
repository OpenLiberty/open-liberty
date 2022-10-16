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
package oidc.client.claimsDefinitionNoRole.servlets;

import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/ClaimsDefinitionNoRoleServlet")
@OpenIdAuthenticationMechanismDefinition(providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1", clientId = "client_1",
                                         clientSecret = "mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger",
                                         claimsDefinition = @ClaimsDefinition(callerNameClaim = "${openIdConfig.callerNameClaim}",
                                                                              callerGroupsClaim = "${openIdConfig.callerGroupsClaim}"),
                                         redirectURI = "${baseURL}/Callback")

public class ClaimsDefinitionNoRoleServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
