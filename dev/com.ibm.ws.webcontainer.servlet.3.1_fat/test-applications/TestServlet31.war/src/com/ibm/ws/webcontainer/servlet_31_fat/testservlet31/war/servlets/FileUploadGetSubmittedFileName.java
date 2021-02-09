/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

//import myValues.FileUploadValues;

@WebServlet("/FileUploadGetSubmittedFileName")
@MultipartConfig(fileSizeThreshold = 50, location = "", maxFileSize = 5000, maxRequestSize = 1000)
public class FileUploadGetSubmittedFileName extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */

    public FileUploadGetSubmittedFileName() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Using getParts()

        try {

            ServletOutputStream sos1 = response.getOutputStream();
            Collection<Part> myParts = request.getParts();

            String ss;
            ss = "--------  Using getParts() --------";
            sos1.println(ss);
            sos1.println(" ");

            ss = "Got One";;
            for (Iterator<Part> myIt = myParts.iterator(); myIt.hasNext();) {
                Part p = myIt.next();

                sos1.println(ss);
                ss = "ContentType = " + p.getContentType();
                sos1.println(ss);
                ss = "Name	    = " + p.getName();
                sos1.println(ss);
                ss = "Size." + p.getName() + " = " + p.getSize();
                sos1.println(ss);

                // getting getParameter from jsp
                ss = "Parameter.Value " + p.getName() + " = " + request.getParameter(p.getName());
                sos1.println(ss);

                ss = "Part.getSubmittedFileName = " + p.getSubmittedFileName();
                sos1.println(ss);

                ss = "~~~~~~~~~~~~~~~~~";
                sos1.println(ss);
            }

            // Using getPart()
            sos1.println(" ");
            ss = "--------  Using getPart() --------";
            sos1.println(ss);
            sos1.println(" ");
            sos1.println(" ");

            Part part = null;

            try {
                part = request.getPart("files");
            } catch (Exception e) {
                System.out.println(" Part is null, exception:" + e);
            }

            if (part != null) {
                ss = "Part        = " + part;
                sos1.println(ss);
                ss = "Content 	= " + part.getContentType();
                sos1.println(ss);
                ss = "Header  	= " + part.getHeader("files");
                sos1.println(ss);
                ss = "HeaderNames = " + part.getHeaderNames();
                sos1.println(ss);

                Collection<String> headercollection = part.getHeaderNames();
                for (Iterator<String> It = headercollection.iterator(); It.hasNext();) {

                    String header = It.next();
                    ss = "Header  	= " + part.getHeader(header);
                    sos1.println(ss);
                    ss = "Headers  	= " + part.getHeaders(header);
                    sos1.println(ss);
                }

                ss = "InputStream = " + part.getInputStream();
                sos1.println(ss);
                ss = "Name        = " + part.getName();
                sos1.println(ss);
                ss = "Size        = " + part.getSize();
                sos1.println(ss);

                sos1.println(" ");
                ss = "--------  Printing File Content --------";
                sos1.println(ss);

                BufferedReader in = new BufferedReader(new InputStreamReader(part.getInputStream()));
                String line = null;

                while ((line = in.readLine()) != null) {
                    sos1.println(line);
                }

            }

        }

        catch (Exception e) {
            ServletOutputStream sos1 = response.getOutputStream();
            String ss = "--------  Exception Output --------";
            sos1.println(ss);
            sos1.println(e.getMessage());
        }

    }

}