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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.onlyProvider.servlets;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.security.DeclareRoles;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseServlet;

@WebServlet("/OnlyProviderInAnnotationServlet")
@OpenIdAuthenticationMechanismDefinition(
                                         providerURI = "${providerBean.providerSecureRoot}/oidc/endpoint/OP1", jwksReadTimeout = Constants.OVERRIDE_DEFAULT_JWKS_READ_TIMEOUT)
// have to include the jwksReadTimeout value to avoid random timeouts - it should not affect what the test is trying to verify
@DeclareRoles("all")
@ServletSecurity(@HttpConstraint(rolesAllowed = "all"))
public class OnlyProviderInAnnotationServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

}
