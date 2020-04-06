/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/Redirected")
public class RedirectedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RedirectedServlet.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

        String test = req.getParameter("Test");
        LOG.info("RedirectedServlet : Entering. Test = " + test);

        PrintWriter pw = res.getWriter();
        if (test.equals("relative")) {
            pw.println("*** Redirected Servlet : called by relative URI. ***");
        } else if (test.equals("relativeWithPathInfo")) {
            /*
             * Defect 137702
             * Tests that servlet 3.1 keeps the path info of the request URI regardless
             * of whether the custom property 'com.ibm.ws.webcontainer.redirectWithPathInfo'
             * is set or not.
             */
            pw.println("*** Redirected Servlet: called by relative URI with PathInfo. ***");
        } else if (test.equals("absolute")) {
            pw.println("*** Redirected Servlet : called by absolute URI. ***");
        } else if (test.equals("network")) {
            pw.println("*** Redirected Servlet : called by network URI. ***");
        } else {
            pw.print("*** Redirected Servlet : ERROR, invalid test parameter: " + test + "***");
        }
    }
}
