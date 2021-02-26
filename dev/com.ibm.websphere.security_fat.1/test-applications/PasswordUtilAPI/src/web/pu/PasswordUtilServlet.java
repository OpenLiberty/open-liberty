/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.pu;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.crypto.PasswordUtil;

/**
 * Test Servlet to access security public APIs (packages websphere or wsspi)
 * current UserRegistry on each request.
 */
public class PasswordUtilServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    PrintWriter pw = null;

    /**
     * {@inheritDoc} GET handles method requests and calls the requested public API
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String className = req.getParameter("class");
        if (className.equals("PasswordUtil")) {
            testPasswordUtil(req, pw);
        } else {
            pw.println("Usage: PasswordUtilServlet?class=CLASSNAME&method=METHODNAME&options...");
        }
        pw.flush();
        pw.close();
    }

    /**
     * invokes PasswordUtil API, ClassNotFoundException will be thrown if PasswordUtilities-1.0 is not on the feature list.
     *
     * @param req
     * @param pw
     */
    private void testPasswordUtil(HttpServletRequest req, PrintWriter pw) {
        String method = req.getParameter("method");
        String input = req.getParameter("input");
        try {
            if (method.equals("encode")) {
                String output = PasswordUtil.encode(input);
                pw.println("encode output is: " + output);
            } else if (method.equals("decode")) {
                String output = PasswordUtil.decode(input);
                pw.println("decode output is: " + output);
            }
        } catch (Throwable e) {
            if (e instanceof NoClassDefFoundError) {
                pw.println("NoClassDefFoundError: " + e.getMessage());
            } else {
                pw.println("Unexpected Exception during processing: " + e.getMessage());
                e.printStackTrace(pw);
            }
        }
    }

    /**
     * {@inheritDoc} POST does nothing for this servlet.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
    }
}
