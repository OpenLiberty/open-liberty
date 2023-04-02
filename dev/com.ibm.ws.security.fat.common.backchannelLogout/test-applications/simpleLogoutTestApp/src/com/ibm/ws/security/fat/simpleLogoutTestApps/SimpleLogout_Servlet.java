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
package com.ibm.ws.security.fat.simpleLogoutTestApps;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleLogout_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    PrintWriter writer = null;
    private String servletName = "SimpleLogout_Servlet";

    public SimpleLogout_Servlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Get");
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Post");
        handleRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String opLogoutUri = req.getParameter("opLogoutUri");
        String idTokenHint = req.getParameter("id_token_hint");

        try {
            System.out.println("Test application class " + servletName + " is logging out");

            req.logout();

            // invoke logout or end_session endpoint on the OP if caller requests
            if (opLogoutUri == null) {
                System.out.println("NOT Invoking provider logout or end_session endpoint on the OP.");
            } else {
                System.out.println("Invoking provider logout or end_session endpoint on the OP: " + opLogoutUri);

                StringBuffer sb = new StringBuffer("");
                sb.append(opLogoutUri);
                if (idTokenHint != null) {
                    System.out.println("Including the id_token_hint: " + idTokenHint);
                    sb.append("?id_token_hint=" + idTokenHint);
                }
                String url = sb.toString();
                String urlEncodedReq = resp.encodeRedirectURL(url);
                resp.sendRedirect(urlEncodedReq);
            }
            System.out.println("Test Application class " + servletName + " logged out\n");

        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

    }

}
