/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package removeheaders.servlets;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test removing sensitive headers:
 *
 * Authorization, Cookie, X-Forwarded, Forwarded, Proxy-Authorization
 *
 * from response for doTrace requests.
 *
 * This is a standin servlet which is needed for the endpoint.  
 * The response for TRACE requests are from the default jakarta.servlet.http.HttpServlet.doTrace()
 *
 * Use with <webContainer com.ibm.ws.webcontainer.DefaultTraceRequestBehavior="true"/> in order for
 * the default doTrace() to pickup
 */
@WebServlet(urlPatterns = {"/TestRemoveHeaders/*"}, name = "RemoveSensitiveHeadersServlet")
public class RemoveSensitiveHeadersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = RemoveSensitiveHeadersServlet.class.getName();

    public RemoveSensitiveHeadersServlet() {
        super();
    }
}
