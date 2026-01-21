/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.fileupload.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Simple Servlet for testing the httpOptions maxMessageSize.
 */
@MultipartConfig
@WebServlet("/FileUploadServlet")
public class FileUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Entered FileUploadServlet#doPost");
        PrintWriter writer = response.getWriter();
        writer.println("Response from FileUploadServlet!");

        Part filePart = request.getPart("file");
        String fileName = filePart.getSubmittedFileName();
        long fileSize = filePart.getSize();
                
        response.setContentType("text/plain");
        writer.println("File uploaded: " + fileName);
        writer.println("Size: " + fileSize);

        writer.flush();
        writer.close();
        System.out.println("Exited FileUploadServlet#doPost");
    }
}
