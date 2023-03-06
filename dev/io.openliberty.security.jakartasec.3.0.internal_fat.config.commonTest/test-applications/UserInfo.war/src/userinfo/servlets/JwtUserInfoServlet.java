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
package userinfo.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/JwtUserInfo")
public class JwtUserInfoServlet extends UserInfoServlet {

    private static final long serialVersionUID = -145839375682343L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        recordWhichApp();

        String jwtString = "..";
        try {
            String accessToken = getAccessToken(request);
            jwtString = createJwtResponse(accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        writeResponse(response, jwtString, "jwt");
    }

}
