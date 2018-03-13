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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class
 */
@WebServlet("/H2PushPromise")
public class H2PushPromise extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static int numberOfRequests = 1;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (1 == numberOfRequests) {
            // This should be the initial request
            // The response to this request should have rel=preload, which will kick
            // off the push-promise sequence
            numberOfRequests++;

            long time = System.currentTimeMillis();
            response.setDateHeader("Date", time);
            response.addHeader("Link", "</H2TestModule/H2PushPromise>; rel=preload;");

            //response.getWriter().write("DataFromPushPromiseServlet");
            response.getWriter().flush();
            response.getWriter().close();

        } else {
            // This should be the pushed request

            long time = System.currentTimeMillis();
            response.setDateHeader("Date", time);

            response.getWriter().write("DataFromPushPromiseServlet");
            response.getWriter().flush();
            response.getWriter().close();
            numberOfRequests = 1;
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
