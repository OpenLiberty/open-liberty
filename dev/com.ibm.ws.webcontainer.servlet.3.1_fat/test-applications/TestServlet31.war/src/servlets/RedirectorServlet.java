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

@WebServlet(urlPatterns = "/Redirector/*")
public class RedirectorServlet extends HttpServlet {

    /**  */
    /*
     * @test_Strategy: Create a Servlet AsyncTestServlet which supports async;
     * Client send a request to AsyncTestServlet at "/AsyncTestServlet?testname=forwardTest";
     * getRequestDispatcher("/AsyncTestServlet?testname=forwardDummy").forward(request, response);
     * In forwardDummy:
     * AsyncContext ac = request.startAsync();
     * ac.dispatch();
     * verifies that it dispatches to "/AsyncTestServlet?testname=forwardTest".
     */

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(RedirectorServlet.class.getName());

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

        String test = req.getParameter("Test");
        LOG.info("RedirectorServlet : Entering. Test = " + test);

        StringBuffer redirectURL = null;
        if (test.equals("relative")) {
            redirectURL = new StringBuffer("Redirected?Test=relative");
        } else if (test.equals("relativeWithPathInfo")) {
        	/*
        	 * Defect 137702
        	 * Tests that servlet 3.1 keeps the path info of the request URI regardless
        	 * of whether the custom property 'com.ibm.ws.webcontainer.redirectWithPathInfo'
        	 * is set or not.
        	 */
            redirectURL = new StringBuffer("../../Redirected?Test=relativeWithPathInfo");
        } else if (test.equals("absolute")) {
            redirectURL = new StringBuffer(req.getContextPath());
            redirectURL.append("/Redirected?Test=absolute");
        } else if (test.equals("network")) {
            redirectURL = new StringBuffer("//");
            redirectURL.append(req.getServerName());
            redirectURL.append(":");
            redirectURL.append(req.getServerPort());
            redirectURL.append(req.getContextPath());
            redirectURL.append("/Redirected?Test=network");
        } else {
            PrintWriter pw = res.getWriter();
            pw.print("RedirectorServlet : **** ERROR, invalid test parameter: " + test + "***");
        }

        try {
            LOG.info("RedirectorServlet : redirect to " + redirectURL);
            res.sendRedirect(redirectURL.toString());
        } catch (IOException exc) {
            PrintWriter pw = res.getWriter();
            pw.print("RedirectorServlet : **** ERROR, IOException on redirect for test parameter: " + test + "***");
        }
    }
}
