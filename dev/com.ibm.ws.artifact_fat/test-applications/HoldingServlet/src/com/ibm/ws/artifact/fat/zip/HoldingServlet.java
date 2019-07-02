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
package com.ibm.ws.artifact.fat.zip;

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
public class HoldingServlet extends HttpServlet {
    public static final String CLASS_NAME = "HoldingServlet";


    public HoldingServlet() {
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

    //sccl.loadClass("CLASSNAME"); => work area cache increase WEB-INF/lib/TestJar.jar
    //sccl.getResource("testfile.txt") => does nothing
    //sc.getResourceAsStream("WEB-INF/lib/TestJar.jar"); => opens apps/*.war
    //testFile.openStream(); => opens work area cache


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        String methodName = "doGet";
        ServletContext sc = request.getServletContext();
        ClassLoader sccl = sc.getClassLoader();
        PrintWriter responseWriter = response.getWriter();
        String responseValue;

        URL testFile;
        InputStream in;
        InputStream in2;
        try{
            testFile = sccl.getResource("testfile.txt");
            in = sc.getResourceAsStream("WEB-INF/lib/TestJar.jar");
            in2 = testFile.openStream();
        }catch(Exception e){
            testFile = null;
            in = null;
            in2 = null;
        }

        String query = request.getQueryString();
        int waitMillis = -1;
        //if a hold has been requested
        if(query.contains("hold")){
            try{
                waitMillis = 1000 * Integer.parseInt(query.split("=")[1]);
                Thread.sleep(waitMillis);
                responseValue = "Waited for " + waitMillis + " ms";
            }catch(Exception e){
                responseValue = "Failed to wait for " + waitMillis + " ms";
            }
        }
        else{
            responseValue = "No hold specified";
        }
        
        if(in != null){
            in.close();
        }
        if(in2 != null){
            in2.close();
        }
        /*
        responseWriter.println(testClass);
        responseWriter.println(testFile);
        responseWriter.println(in);
        responseWriter.println(in2);
        responseWriter.println("-----------------------");
        responseWriter.println(request.getQueryString());
        */
        responseWriter.println(responseValue);
        responseWriter.flush();
        responseWriter.close();

        
    }
}
