/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

@WebServlet("/SimplePushServlet")
public class SimplePushServlet extends HttpServlet {
    static final long serialVersionUID = 9999L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();
        PushBuilder pushBuilder = req.newPushBuilder();

        // push a file, push.txt, that contains a small amount of text.
        // the test will validate that the correct text is sent by the server.
        if (pushBuilder != null) {
            pushBuilder.path("files/push.txt").push();
        }
        pw.print("<!DOCTYPE html><html><body>");
        pw.println("This is a simple push servlet");
        pw.print("</body></html>");
    }
}
