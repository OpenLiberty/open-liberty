/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

        String className = req.getParameter("className");
        ClassLoader appCl = this.getClass().getClassLoader();
        Class<?> clazz = null;
        try {
            clazz = appCl.loadClass(className);
        } catch (Exception e) {
            //
        }
        resp.getOutputStream().println("TestSpiVisibilityServlet: class " + className + (clazz==null ? " is not" : " is") + " visible to the application classloader");
    }
}
