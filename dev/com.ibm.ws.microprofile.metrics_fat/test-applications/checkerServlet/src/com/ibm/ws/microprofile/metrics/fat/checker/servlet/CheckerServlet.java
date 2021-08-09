/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.fat.checker.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.metrics.classloader.utility.ClassLoaderUtils;

/**
 * Servlet implementation class CheckerServlet
 */
@WebServlet("/checkServlet")
public class CheckerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        ClassLoaderUtils classLoaderUtils = ClassLoaderUtils.getInstance();
        Runtime r = Runtime.getRuntime();
        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println(classLoaderUtils.resolveClassLoaderState());
        pw.flush();
    }

}
