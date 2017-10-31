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
package testservlet40.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

/**
 *
 */
@WebServlet("/PushBuilderAPIServlet")
public class PushBuilderAPIServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    public PushBuilderAPIServlet() {

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        PrintWriter pw = res.getWriter();

        pw.println("PushBuilder Test");

        PushBuilder pb = req.newPushBuilder();

        if (pb == null) {
            pw.println("PASS : req.newPushBuilder() returned null");
        } else {
            pw.println("FAIL : req.newPushBuilder() returned a non-null value");
        }

    }

}
