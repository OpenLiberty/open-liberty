/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.backChannelLogoutTestApp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BackChannelLogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final String servletName = "BackChannelLogoutServlet";

    public BackChannelLogoutServlet() {
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleProcessLogout(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleProcessLogout(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleProcessLogout(req, resp);
    }

    /**
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleProcessLogout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String[] backChannelLogoutUris = null;
        PrintWriter writer = resp.getWriter();

        try {
            Map<String, String[]> parms = req.getParameterMap();
            parms.entrySet().iterator();
            Iterator<Entry<String, String[]>> itr = req.getParameterMap().entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, String[]> entry = itr.next();
                System.out.println("Parm: " + entry.getKey() + " with Value: " + req.getParameter(entry.getKey()));
            }

            String uriString = req.getParameter("uris");
            if (uriString == null) {
                writer.println("No uri found in the request");
                backChannelLogoutUris = null;
            } else {
                writer.println("Uris found: " + uriString);
                backChannelLogoutUris = uriString.split(",");
            }

            String token = req.getParameter("logoutToken");
            if (token == null) {
                writer.println("No token found in the request");
            } else {
                writer.println("Token found: " + token);
                dumpToken(token);
            }

            // save the list of back channel logout uri values passed for the next get call
            System.out.println("Saving backChannelLogoutUris: " + String.join(",", backChannelLogoutUris));

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }

        writer.flush();
        writer.close();
    }

    protected void dumpToken(String token) throws Exception {

        // Dump the contents of the token for tracing - is it signed? encrypted? Just json
    }

}
