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
package http2.test.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

/**
 *
 */
@WebServlet("/PushBuilderAPIServlet")
public class PushBuilderAPIServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    static int numberOfRequests = 1;

    public PushBuilderAPIServlet() {

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        PrintWriter pw = res.getWriter();
        if (1 == numberOfRequests) {

            numberOfRequests++;

            pw.println("PushBuilder API Tests");

            Enumeration<String> reqHeaderNames = req.getHeaderNames();
            while (reqHeaderNames.hasMoreElements()) {
                String name = reqHeaderNames.nextElement();
                pw.println("Req Header : " + name + ":" + req.getHeader(name));
            }

            PushBuilder pb = req.newPushBuilder();
            if (pb != null) {
                pb.path("/H2TestModule/PushBuilderAPIServlet");
                pb.queryString("test=queryString");
                try {
                    pb.push();
                    pw.println("PASS : pb.push() did not throw an ISE");
                } catch (IllegalStateException exc) {
                    pw.println("FAIL : pb.push() threw an ISE : " + exc.getMessage());
                }
            } else {
                numberOfRequests = 1;
            }

            long time = System.currentTimeMillis();
            res.setDateHeader("Date", time);

            res.getWriter().flush();
            res.getWriter().close();

        } else {
            // This should be the pushed request

            long time = System.currentTimeMillis();
            res.setDateHeader("Date", time);

            res.getWriter().write("DataFromPushPromiseServlet");
            res.getWriter().flush();
            res.getWriter().close();
            numberOfRequests = 1;
        }

    }

}
