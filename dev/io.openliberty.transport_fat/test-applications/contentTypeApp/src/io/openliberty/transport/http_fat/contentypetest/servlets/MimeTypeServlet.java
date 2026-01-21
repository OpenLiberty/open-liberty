/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.transport.http_fat.contentypetest.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

/**
 * Servlet to provide information about MIME types
 */
@WebServlet("/MimeTypeServlet")
public class MimeTypeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger Log = Logger.getLogger(MimeTypeServlet.class.getName());

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        String[] extensions = {
                "html", "css", "js", "ico", "xml", "json"
        };

        for (String ext : extensions) {
            String filename = "test." + ext;
            String mimeType = getServletContext().getMimeType(filename);
            Log.info("getMimeType(\"" + filename + "\") = " + mimeType);
            out.println(ext + ": " + mimeType);
        }

        String faviconMimeType = getServletContext().getMimeType("favicon.ico");
        Log.info("getMimeType(\"favicon.ico\") = " + faviconMimeType);
        out.println("favicon.ico: " + faviconMimeType);
    }
}
