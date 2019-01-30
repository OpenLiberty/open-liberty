/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.webcontainer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

@Path("environment/webcontainer/context/")
public class WebContainerContextInjectionResource {

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    @Context
    private ServletConfig servletConfig;

    @Context
    private ServletContext servletContext;

    @GET
    public String getHTTPRequestPathInfo() {
        return httpServletRequest.getPathInfo();
    }

    @POST
    public String getHTTPResponse() {
        httpServletResponse.addHeader("responseheadername", "responseheadervalue");
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);

        try {
            PrintWriter pw =
                            new PrintWriter(new OutputStreamWriter(httpServletResponse.getOutputStream()));
            /*
             * PrintWriter does not automatically flush so going to flush pw
             * manually. Reminder, cannot just flush HttpServletResponse
             * OutputStream either since decorated class has no idea about
             * PrintWriter.
             */
            pw.write("Hello World");
            pw.flush();
            /*
             * this should always be committed now
             */
            //if (httpServletResponse.isCommitted()) {
            pw.write(" -- I was committted");
            //}
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not output the servlet response.");
        }

        return "Shouldn't see me";
    }

    @GET
    @Path("servletcontext")
    public void getServletContext() throws IOException, ServletException {
        httpServletRequest.setAttribute("wink", "testing 1-2-3");
        servletContext.getRequestDispatcher("/servlets-test.jsp").include(httpServletRequest,
                                                                          httpServletResponse);
        httpServletRequest.removeAttribute("wink");

        // need to flush buffer so the response is committed
        httpServletResponse.flushBuffer();
    }

    @GET
    @Path("servletconfig")
    public String getServletConfig() {
        return servletConfig.getServletName();
    }
}
