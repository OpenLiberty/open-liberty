/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package readlistener;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet({"/TestServlet"})
public class TestServlet extends HttpServlet {
    private static final String CLASS_NAME = TestServlet.class.getName();

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getHeader("Upgrade") != null) {
            LOG("doPost, Upgrade request. Responding with 101 Switching Protocols. ");
            response.setStatus(101);
            response.setHeader("Upgrade", "YES");
            response.setHeader("Connection", "Upgrade");

            LOG("doPost, register a HttpUpgradeHandler class");
            request.upgrade(TestHttpUpgradeHandler.class);
        } else {
            LOG("doPost, not an upgrade request. Done");
            response.getOutputStream().println("No [Upgrade] header.");
            response.getOutputStream().println("End of Test");
        } 
    }

    private static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
