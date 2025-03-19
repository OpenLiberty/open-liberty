/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.PH62271;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
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

/**
 * This servlet tests whether the absolute or relative path is used
 * within the server to save the file uploaded depending on the
 * allowAbsolutePathForPartWriting parameter.
 */
@WebServlet("/PH62271Servlet")
@MultipartConfig(fileSizeThreshold = 50, location = "", maxFileSize = 5000, maxRequestSize = 1000)
public class PH62271Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public PH62271Servlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            Part part = request.getPart("files");

            String ss;
            ss = "--------  Using getPart(\"files\") --------";
            outputStream.println(ss);
            outputStream.println(" ");

            if (part != null){
                String path = request.getParameterMap().get("location")[0];
                outputStream.println("Server Path: " + path);
                String final_path = path + "/uploads/" + part.getSubmittedFileName();
                final_path = final_path.replace('/', File.separator.charAt(0)).replace('\\', File.separator.charAt(0));
                outputStream.println("Final Write Path: " + final_path);

                part.write(final_path); // KEY PART OF TEST -- writes to absolute location based on allowAbsoluteFileNameForPartWrite
            } else {
                outputStream.println("Part was null");
            }
        } catch (Exception e) {
            ServletOutputStream outputStream = response.getOutputStream();
            String ss = "--------  Exception Output --------";
            outputStream.println(ss);
            outputStream.println(e.getMessage());
        }
    }

}
