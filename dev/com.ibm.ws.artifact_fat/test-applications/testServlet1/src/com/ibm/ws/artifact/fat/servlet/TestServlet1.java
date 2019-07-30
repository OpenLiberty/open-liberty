/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;


@WebServlet("/")
public class TestServlet1 extends HttpServlet {
    public static final String CLASS_NAME = "TestServlet1";


    public TestServlet1() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException{
        
            String methodName = "doPost";

            PrintWriter responseWriter = response.getWriter();
            responseWriter.println(String.format("%s.%s",CLASS_NAME,methodName));
            responseWriter.flush();
            responseWriter.close();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String methodName = "doGet";
        
        PrintWriter responseWriter = response.getWriter();
        responseWriter.println(String.format("%s.%s",CLASS_NAME,methodName));
        responseWriter.flush();
        responseWriter.close();

    }
}
