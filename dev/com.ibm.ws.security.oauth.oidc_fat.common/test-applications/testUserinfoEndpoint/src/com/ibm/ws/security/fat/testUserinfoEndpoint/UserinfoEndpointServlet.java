/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.security.fat.testUserinfoEndpoint;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserinfoEndpointServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String servletName = "UserinfoEndpointServlet";
    private final String jwtContentType = "application/jwt";
    private String token = null;
    private String contentType = jwtContentType;

    public UserinfoEndpointServlet() {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleSaveTokenRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleReturnTokenRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleReturnTokenRequest(req, resp);
    }

    /**
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleSaveTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer = resp.getWriter();

        token = req.getParameter("userinfoToken");
        String contentTypeSetting = req.getParameter("contentType");
        if (contentTypeSetting == null || contentTypeSetting == "") { // content type not passed, use default of jwt
            contentType = jwtContentType;
        } else {
            contentType = contentTypeSetting;
        }

        System.out.println("Userinfo Endpoint Saving token: " + token);
        writer.println("token saved: " + token);

        writer.flush();
        writer.close();

    }

    /**
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleReturnTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Userinfo Endpoint Returning token: " + token + "\n with contentType: " + contentType);

        resp.setContentType(contentType);
        PrintWriter writer = resp.getWriter();

        writer.println(token);
        writer.flush();
        writer.close();

    }

}
