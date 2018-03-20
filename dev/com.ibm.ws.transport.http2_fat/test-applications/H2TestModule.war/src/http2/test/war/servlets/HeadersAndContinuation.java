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
 * HTTP/2 test servlet. Generates a response header that exceeds the maximum frame size, which should force the server to
 * send out a HEADER frame followed by a CONTINUATION frame containing the excess header block data
 *
 * Per 6.5.2, the SETTINGS_MAX_FRAME_SIZE initial AND minumum value is 16384.
 */
@WebServlet("/HeadersAndContinuation")
public class HeadersAndContinuation extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        char c = 'a';
        // make sure we exceed 16384 bytes
        StringBuilder sb = new StringBuilder(30000);
        for (int i = 0; i < 29000; i++) {
            sb.append(c);
        }
        response.addHeader("excessHeader", sb.toString());
        response.setDateHeader("Date", 0);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
