/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(urlPatterns = "/simpleAsync", asyncSupported = true)
public class SimpleAsyncServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final AsyncContext asyncContext = request.startAsync(request, response);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ServletResponse response = asyncContext.getResponse();
                    response.setContentType("text/plain");
                    PrintWriter out = response.getWriter();
                    Thread.sleep(2000); // 2 second sleep
                    out.println("SimpleAsyncServlet");
                    out.flush();
                    asyncContext.complete();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        asyncContext.start(runnable);

    }
}
