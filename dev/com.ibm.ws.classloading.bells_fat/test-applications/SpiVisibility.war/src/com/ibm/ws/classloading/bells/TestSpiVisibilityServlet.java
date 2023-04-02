/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.bells;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/TestServlet")
public class TestSpiVisibilityServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Attempt to load a class using the application class loader
        String className = req.getParameter("className");
        String loadOp = req.getParameter("loadOp");
        System.out.println("TestSpiVisibilityServlet.doGet: loading class " + className + ", using " + loadOp);
        Class<?> clazz = null;
        try {
            if (loadOp == null || "loadClass".equals(loadOp)) {
                clazz = this.getClass().getClassLoader().loadClass(className);
            }
            else if ("forName".equals(loadOp)) {
                clazz = Class.forName(className, true, this.getClass().getClassLoader());
            }
            else {
                throw new IllegalArgumentException("Invalid loadOp: " + loadOp);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
        resp.getOutputStream().println("TestSpiVisibilityServlet: class " + className + (clazz==null ? " is not" : " is") + " visible to the application classloader");
    }
}
