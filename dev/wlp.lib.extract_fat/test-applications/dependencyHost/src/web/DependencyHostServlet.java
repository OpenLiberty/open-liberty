/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that can return a variety of HTTP response codes and redirects.
 * <p>
 * Used to test that the sample extractor can cope with various server responses.
 */
@SuppressWarnings("serial")
public class DependencyHostServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        boolean validRequest = true;
        if (path != null) {
            if (path.equals("/good")) {
                sendGoodFile(response);
            } else if (path.equals("/notfound")) {
                sendErrorCode(response, HttpServletResponse.SC_NOT_FOUND);
            } else if (path.equals("/badrequest")) {
                sendErrorCode(response, HttpServletResponse.SC_BAD_REQUEST);
            } else if (path.equals("/forbidden")) {
                sendErrorCode(response, HttpServletResponse.SC_FORBIDDEN);
            } else if (path.equals("/serverError")) {
                sendErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else if (path.equals("/redirect302")) {
                sendRedirect(response, request, 302, "good");
            } else if (path.equals("/redirect301")) {
                sendRedirect(response, request, 301, "good");
            } else if (path.equals("/redirect303")) {
                sendRedirect(response, request, 303, "good");
            } else if (path.equals("/redirect307")) {
                sendRedirect(response, request, 307, "good");
            } else if (path.equals("/redirectnotfound")) {
                sendRedirect(response, request, 302, "notfound");
            } else if (path.equals("/protocolchange")) {
                sendAbsoluteRedirect(response, 302, "https://example.com");
            } else {
                validRequest = false;
            }
        } else {
            validRequest = false;
        }

        if (validRequest) {
            log("Request OK: " + path);
        } else {
            log("Bad request: " + path);
            sendErrorCode(response, HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void sendGoodFile(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        sendText(response, "DependencyContent");
    }

    private void sendErrorCode(HttpServletResponse response, int code) throws IOException {
        response.setStatus(code);
        sendText(response, "ErrorContent");
    }

    private void sendRedirect(HttpServletResponse response, HttpServletRequest request, int code, String location) throws IOException {
        response.setStatus(code);
        StringBuffer locationUrl = request.getRequestURL();
        locationUrl.delete(locationUrl.lastIndexOf("/"), locationUrl.length());
        locationUrl.append("/").append(location);
        response.setHeader("Location", locationUrl.toString());
        sendText(response, "Redirecting...");
    }

    private void sendAbsoluteRedirect(HttpServletResponse response, int code, String location) throws IOException {
        response.setStatus(code);
        response.setHeader("Location", location);
        sendText(response, "Redirecting...");
    }

    private void sendText(HttpServletResponse response, String text) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        Writer writer = response.getWriter();
        writer.append(text);
        writer.append("\r\n");
        writer.close();
    }
}
