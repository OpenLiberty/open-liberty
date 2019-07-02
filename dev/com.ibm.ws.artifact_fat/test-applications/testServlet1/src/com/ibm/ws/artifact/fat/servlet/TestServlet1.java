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
            responseWriter.println(String.format("%s.%s: Enter",CLASS_NAME, methodName));
            responseWriter.flush();
            responseWriter.close();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String methodName = "doGet";
        ServletContext sc = request.getServletContext();
        ClassLoader sccl = sc.getClassLoader();

        URL testFile;
        Class testClass;
        InputStream in;
        InputStream in2;
        try{
            testClass = sccl.loadClass("TestClass");
            testFile = sccl.getResource("testfile.txt");
            in = null;//sc.getResourceAsStream("WEB-INF/lib/TestJar.jar");
            in2 = null; //testFile.openStream();
        }catch(Exception e){

            testClass = null;
            testFile = null;
            in = null;
            in2 = null;
            
        }
        
        PrintWriter responseWriter = response.getWriter();
        responseWriter.println(testClass);
        responseWriter.println(testFile);
        responseWriter.println(in);
        responseWriter.println(in2);
        responseWriter.println("-----------------------");
        responseWriter.println(request.getQueryString());
        
        responseWriter.flush();
        responseWriter.close();



        if(in != null){
            //in.close();
        }
        if(in2 != null){
            //in2.close();
        }
        
    }
}
