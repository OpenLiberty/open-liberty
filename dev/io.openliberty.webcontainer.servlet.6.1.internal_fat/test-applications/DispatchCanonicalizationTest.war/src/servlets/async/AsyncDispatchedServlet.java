/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package servlets.async;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/AsyncDispatched/*", asyncSupported = true)
public class AsyncDispatchedServlet extends HttpServlet {
    private static final String CLASS_NAME = AsyncDispatchedServlet.class.getName();
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG("doGet ENTRY");

        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
        } catch (IOException e1) {
        }

        writer.println("<br>****** AsyncDispatchedServlet runs ***");
        writer.println("<br>****** AsyncDispatchedServlet flushing and completing async ***");

        writer.flush();

        req.getAsyncContext().complete();
        
        writer.println("<br>****** AsyncDispatchedServlet completed. PASS_2 ***");
        writer.flush();
        LOG("Completed async and EXIT");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
