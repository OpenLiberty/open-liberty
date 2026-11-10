/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package jwtbuilderstartup;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtToken;

@WebServlet("/build")
public class BuildJwtServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JwtToken token = JwtBuilder.create("workloadIdentityBuilder").subject("jdoe").buildJwt();
            // testJwtBuilderCannotBuildDuringCheckpointButCanBuildAfterRestore expects the string to start with "workload-identity="
            resp.getWriter().println("workload-identity=" + token.getClaims().get("workload-identity"));
        } catch (Exception e) {
            resp.getWriter().println("Caught an exception: " + e);
        }
    }
}
