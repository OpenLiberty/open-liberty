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
package com.ibm.ws.microprofile.appConfig.test.utils;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public abstract class AbstractTestServlet extends HttpServlet {

    private final String packageName;
    private final String[] tests;

    public AbstractTestServlet(String packageName, String... tests) {
        super();
        this.packageName = packageName;
        this.tests = tests;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String test = request.getParameter("test");

        if (test == null || "".equals(test)) {
            resp.setContentType("text/html");
            pw.println("<html><body><p>");
            pw.println("Test name is null, URL is: " + request.getRequestURL());
            pw.println("</p>");
            pw.println("<h3>Available Tests</h3><p><ul>");
            for (String available : tests) {
                pw.println("<li><a href='" + request.getRequestURL() + "?test=" + available + "'>" + available + "</a></li>");
            }
            pw.println("</ul></p></body></html>");
        } else {
            resp.setContentType("text/plain");
            String testClassName = packageName + "." + test;
            try {
                @SuppressWarnings("unchecked")
                Class<AppConfigTestApp> testClass = (Class<AppConfigTestApp>) Class.forName(testClassName);
                AppConfigTestApp t = testClass.newInstance();
                String result = t.runTest(request);
                pw.println("Test: " + testClass.getName());
                pw.println(result);
            } catch (Throwable t) {
                t.printStackTrace();
                StringBuilder builder = new StringBuilder("Test FAILED: ");
                builder.append(testClassName);
                builder.append("\n");
                builder.append(t);
                builder.append("\n at \n");
                StackTraceElement[] stack = t.getStackTrace();
                for (int i = 0; i < 5 && i < stack.length; i++) {
                    builder.append(stack[i]);
                    builder.append("\n");
                }
                pw.println(builder.toString());
            } finally {
                System.setProperty(AppConfigTestApp.DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "");
            }
        }
    }
}