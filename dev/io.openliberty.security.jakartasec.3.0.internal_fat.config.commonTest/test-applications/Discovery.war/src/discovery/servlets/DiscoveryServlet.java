/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package discovery.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/.well-known/openid-configuration")
public class DiscoveryServlet extends HttpServlet {

    private String discData = null;

    private static final long serialVersionUID = -217476984908088827L;

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("In Discovery to save token.");

        Map<String, String[]> parms = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parms.entrySet()) {
            System.out.println("Parm: " + entry.getKey() + " value: " + entry.getValue());
        }
        discData = request.getParameter("UpdatedDiscoveryData");

        System.out.println("Saving updated discovery data: " + discData);

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // ServletOutputStream ps = response.getOutputStream();

        System.out.println("In Discovery override");

        writeResponse(response, discData, "json");
    }

    void writeResponse(HttpServletResponse response, String returnString, String format) throws IOException {
        String cacheControlValue = response.getHeader("Cache-Control");
        if (cacheControlValue != null &&
            !cacheControlValue.isEmpty()) {
            cacheControlValue = cacheControlValue + ", " + "no-store";
        } else {
            cacheControlValue = "no-store";
        }
        response.setHeader("Cache-Control", cacheControlValue);
        response.setHeader("Pragma", "no-cache");

        response.setContentType("application/" + format);
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter pw;
        pw = response.getWriter();
        System.out.println("Discovery returning discovery (in format " + format + ") : " + returnString);
        pw.write(returnString);
        pw.flush();
    }

}
