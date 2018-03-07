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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

/**
 * Servlet implementation class
 */
@WebServlet("/PushPromiseClient")
public class PushPromiseClient extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static int numberOfRequests = 1;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        PushBuilder pushBuilder = request.newPushBuilder();
        pushBuilder.path("images/test.jpg").addHeader("content-type", "image/jpg").push();
        pushBuilder.path("images/test2.jpg").addHeader("content-type", "image/jpg").push();
        pushBuilder.path("images/test3.jpg").addHeader("content-type", "image/jpg").push();
        pushBuilder.path("images/test4.jpg").addHeader("content-type", "image/jpg").push();
        pushBuilder.path("images/test5.jpg").addHeader("content-type", "image/jpg").push();
        pushBuilder.path("images/test32325.jpg").addHeader("content-type", "image/jpg").push();

        try (PrintWriter respWriter = response.getWriter();) {
            respWriter.write("<html>"
                             + "<body>"
                             + "<center>"
                             + "<h1>From Servlet PushBuilderDriverImages</h1>"
                             + "</center>"
                             + "<hr>"
                             + "<img src='images/test.jpg' height='50' width='50' />"
                             + "<img src='images/test2.jpg' height='125' width='125' />"
                             + "<img src='images/test3.jpg' height='250' width='250' />"
                             + "<img src='images/test4.jpg' height='375' width='375' />"
                             + "<img src='images/test5.jpg' height='600' width='600' />"
                             + "<hr>"
                             + "</body>"
                             + "</html>");
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
