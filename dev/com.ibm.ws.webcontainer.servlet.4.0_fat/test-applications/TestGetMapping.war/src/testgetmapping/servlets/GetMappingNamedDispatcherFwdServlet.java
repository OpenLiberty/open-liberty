/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testgetmapping.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This test will test the functionality of the HttpServletRequest.getMapping()
 * API. In this case, the ServletContext.getNamedDispatcher() method is
 * going to be used to get the RequestDispatcher and then perform the forward.
 *
 * Multiple requests will be driven to the same servlet using different url
 * patterns to ensure that the correct ServletMapping values are returned:
 *
 */
@WebServlet(urlPatterns = { "/pathNamedDispatcherFwdMatch" }, name = "GetMappingNamedDispatcherFwdServlet")
public class GetMappingNamedDispatcherFwdServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String servletName = "GetMappingTestServlet";

        System.out.println(">>>>>>>> Enter GetMappingNamedDispatcherServlet : dispatch to : " + servletName);
        RequestDispatcher rd = request.getServletContext().getNamedDispatcher(servletName);
        rd.forward(request, response);
        System.out.println("<<<<<<<< Exit GetMappingNamedDispatcherServlet");

    }

}
