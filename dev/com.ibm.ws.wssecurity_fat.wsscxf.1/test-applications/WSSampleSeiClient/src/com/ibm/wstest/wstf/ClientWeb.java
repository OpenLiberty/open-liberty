/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wstest.wstf;

import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ClientServlet
 *
 * Just setup the default string and count values
 */
public class ClientWeb extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String INDEX_JSP_LOCATION = "/clientweb.jsp";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ClientWeb() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * processRequest Reads the posted parameters and calls the service
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Set up the default values to use
        InetAddress addr = InetAddress.getLocalHost();
        String contextString = getServletContext().getContextPath();

        String uriString = req.getScheme() + "://" + addr.getCanonicalHostName() + ":" + req.getServerPort();
        req.setAttribute("hostbase", uriString);
        req.setAttribute("msgcount", "1");
        req.setAttribute("context", contextString);
        req.setAttribute("scenario", contextString.substring(1));

        getServletContext().getRequestDispatcher(INDEX_JSP_LOCATION).forward(req, resp);
    }

}
