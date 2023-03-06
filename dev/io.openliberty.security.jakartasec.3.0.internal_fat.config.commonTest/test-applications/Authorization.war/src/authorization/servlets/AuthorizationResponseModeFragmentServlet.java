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
package authorization.servlets;

import java.io.IOException;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AuthorizationResponseModeFragmentServlet")
public class AuthorizationResponseModeFragmentServlet extends AuthorizationServlet {

    private static final long serialVersionUID = 230335973928990220L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String responseMode = request.getParameter(OpenIdConstant.RESPONSE_MODE);
        if (!Constants.FRAGMENT_RESPONSE_MODE.equals(responseMode)) {
            throw new Error("response_mode does not equal fragment");
        }

        String redirectUri = request.getParameter(OpenIdConstant.REDIRECT_URI);
        String state = request.getParameter(OpenIdConstant.STATE);

        String redirect = redirectUri + "#code=abc123&state=" + state;
        response.sendRedirect(redirect);
    }
}
