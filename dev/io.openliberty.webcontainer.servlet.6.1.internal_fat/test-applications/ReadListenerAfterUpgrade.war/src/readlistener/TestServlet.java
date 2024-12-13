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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet({"/TestServlet"})
public class TestServlet extends HttpServlet {
    private static final String CLASS_NAME = TestServlet.class.getName();

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        if (request.getHeader("Upgrade") != null) {
            LOG("doPost , prepare respone with 100 Switching Protocol");
            response.setStatus(101);
            response.setHeader("Upgrade", "YES");
            response.setHeader("Connection", "Upgrade");
            
            LOG(" setting up server side HttpUpgradeHandler");
            request.upgrade(TestHttpUpgradeHandler.class);
        } else {
            LOG("doPost, not an upgrade request. Done");
            response.getWriter().println("No upgrade");
            response.getWriter().println("End of Test");
        } 
    }

    private static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}