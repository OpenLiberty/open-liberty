/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.fat.TestTokenEndpoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/***********************
 *
 * Copied from the com.ibm.ws.security.fat.common.jwt project. It was copied to some of the classes that the original used.
 * This copy is a simplified version - if the more complex version is needed, we'll need to investigate some issues with the transformer
 */
public class TokenEndpointServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
//    private String token = null;
//    String access_token = null;
//    String id_token = null;
//    int expires_in = 7199;
//    String token_type = "Bearer";
//    String scope = "openid profile";
//    String refresh_token = "21MhoIC95diaQo9tb5UpFBDFlHh45NixhcKkCwRipszH6WIzKz";
    String access_token = null;
    String id_token = null;
    int expires_in = 9999;
    String token_type = null;
    String scope = null;
    String refresh_token = null;

    public TokenEndpointServlet() {
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

        initValues();
        Map<String, String[]> parms = req.getParameterMap();
        parms.entrySet().iterator();
        Iterator<Entry<String, String[]>> itr = req.getParameterMap().entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String[]> entry = itr.next();
            System.out.println("Parm: " + entry.getKey() + " with Value: " + req.getParameter(entry.getKey()));
        }

        try {
            access_token = req.getParameter("access_token");
            id_token = req.getParameter("id_token");
            String expires = req.getParameter("expires_in");
            if (expires != null) {
                expires_in = Integer.parseInt(expires);
            }
            token_type = req.getParameter("token_type");
            scope = req.getParameter("scope");
            refresh_token = req.getParameter("refresh_token");
        } catch (Exception e) {
            writer.println(e);
            throw new ServletException(e.toString());
        }
        // save the token value passed for the next get call
        System.out.println("Saving access_token: " + access_token);
        System.out.println("Saving id_token: " + id_token);
        System.out.println("Saving expires_in: " + expires_in);
        System.out.println("Saving token_type: " + token_type);
        System.out.println("Saving scope: " + scope);
        System.out.println("Saving refresh_token: " + refresh_token);

        writer.flush();
        writer.close();
    }

    protected void initValues() {
        access_token = null;
        id_token = null;
        expires_in = 9999;
        token_type = null;
        scope = null;
        refresh_token = null;

    }

    /**
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleReturnTokenRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        System.out.println("Token Endpoint Returning building json response");

        JsonObjectBuilder theResponse = Json.createObjectBuilder();
        if (access_token != null) {
            theResponse.add("access_token", access_token);
        }
        if (token_type != null) {
            theResponse.add("token_type", token_type);
        }
        if (expires_in != 9999) {
            theResponse.add("expires_in", expires_in);
        }
        if (scope != null) {
            theResponse.add("scope", scope);
        }
        if (refresh_token != null) {
            theResponse.add("refresh_token", refresh_token);
        }
        if (id_token != null) {
            theResponse.add("id_token", id_token);
        }

        PrintWriter writer = resp.getWriter();
        writer.println(theResponse.build());
        writer.flush();
        writer.close();

    }

}
