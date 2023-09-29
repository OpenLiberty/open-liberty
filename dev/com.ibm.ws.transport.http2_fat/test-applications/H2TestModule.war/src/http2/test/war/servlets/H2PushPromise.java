/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 * Servlet implementation class
 */
@WebServlet("/H2PushPromise")
public class H2PushPromise extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PrintWriter pw = response.getWriter();

        String test = null;
        test = request.getParameter("test");

        if ((test == null) || (test == (new String("")))) {
            // No special parm, this is the pushed request

            long time = System.currentTimeMillis();
            response.setDateHeader("Date", time);

            pw.println("Response to push");
            pw.close();

        } else if (test.toLowerCase().equals(new String("preload"))) {

            // Test the header link rel=preload path
            pw.println("push_promise link header rel=preload");

            long time = System.currentTimeMillis();
            try {
                Thread.sleep(250);
            } catch (Exception x) {
            }
            response.setDateHeader("Date", time);
            response.addHeader("Link", "</H2TestModule/H2PushPromise>; rel=preload;");
            response.addHeader("Link", "</H2TestModule/H2PushPromise>; rel=preload;");
            pw.close();

        } else if ((test.toLowerCase().equals(new String("pushbuilder")))) {

            // Test the pushbuilder path
            pw.println("push_promise PushBuilder");

            Enumeration<String> reqHeaderNames = request.getHeaderNames();
            while (reqHeaderNames.hasMoreElements()) {
                String name = reqHeaderNames.nextElement();
                pw.println("Req Header : " + name + ":" + request.getHeader(name));
            }
            try {
                Thread.sleep(250);
            } catch (Exception x) {
            }
            PushBuilder pb = request.newPushBuilder();
            if (pb != null) {
                pb.path("/H2TestModule/H2PushPromise");
                pb.queryString("test=queryString");
                try {
                    pb.push();
                    pw.println("PASS : pb.push() did not throw an ISE");
                } catch (IllegalStateException exc) {
                    pw.println("FAIL : pb.push() threw an ISE : " + exc.getMessage());
                }
            }
            pw.close();
        } else if ((test.toLowerCase().equals(new String("delay")))) {

            try {
                Thread.sleep(5000);
            } catch (Exception x) {
            }

        }

        try {
            Thread.yield();
        } catch (Exception x) {
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     *
     * If this method is not implemented this happens:
     *
     * HTTP/1.1 405 Method Not Allowed<CR>
     * <LF>X-Powered-By: Servlet/3.1<CR>
     * <LF>Content-Type: text/html;charset=ISO-8859-1<CR>
     * <LF>$WSEP: <CR>
     * <LF>Content-Language: en-US<CR>
     * <LF>Transfer-Encoding: chunked<CR>
     * <LF>Connection: Close<CR>
     * <LF>Date: Mon, 17 Apr 2017 20:44:36 GMT<CR>
     * <LF><CR>
     * <LF>3a<CR>
     * <LF>Error 405: HTTP method POST is not supported by this URL<CR>
     * <LF><CR>
     * <LF>
     *
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
