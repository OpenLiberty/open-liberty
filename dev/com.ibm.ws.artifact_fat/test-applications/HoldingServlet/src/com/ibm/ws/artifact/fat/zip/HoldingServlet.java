/*******************************************************************************
 * Copyright (c) 2019,2023 IBM Corporation and others.
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

        try ( PrintWriter responseWriter = response.getWriter(); ) {
            responseWriter.println(CLASS_NAME + "." + methodName + ": Enter");
            responseWriter.flush();
        }
    }

    //
    
    protected URL fileResource;
    protected InputStream fileInputStream;

    protected InputStream jarInputStream;
    
    protected void openResources(ServletContext sc) throws IOException {
        fileResource = sc.getClassLoader().getResource("testfile.txt"); // throws IOException
        fileInputStream = fileResource.openStream(); // throws IOException        

        jarInputStream = sc.getResourceAsStream("WEB-INF/lib/TestJar.jar"); // throws IOException
    }

    protected void closeResources() throws IOException {
        IOException firstException = null;

        if ( fileResource != null ) {
            if ( fileInputStream != null ) {
                try {
                    fileInputStream.close(); // throws IOException
                } catch ( IOException e ) {
                    firstException = e;
                }
                fileInputStream = null;
            }
            fileResource = null;
        }
        
        if ( jarInputStream != null ) {
            try {
                jarInputStream.close(); // throws IOException
            } catch ( IOException e ) {
                if ( firstException == null ) {
                    firstException = e;
                } else {
                    e.printStackTrace();
                }
            }
            jarInputStream = null;
        }
        
        if ( firstException != null ) {
            throw firstException;
        }
    }
    
    /**
     * Servlet API: Main test method: Acquire or release web module
     * resources.
     *
     * Acquiring web module resources means obtaining and opening
     * a file resource, and opening an archive resource.  Opening
     * the resources causes activity in the zip cache.  Holding
     * the resources open keeps entries in the zip cache in an
     * open state.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        try ( PrintWriter responseWriter = response.getWriter(); ) {                
            String query = request.getQueryString();
            if ( query.contains("hold") ) {
                try {
                    openResources( request.getServletContext() );
                    responseWriter.println("hold: Success");
                } catch ( IOException e ) {
                    responseWriter.println("hold: Failed");
                    e.printStackTrace(responseWriter);
                }
            } else if ( query.contains("release") ) {
                try {
                    closeResources();
                    responseWriter.println("release: Success");
                } catch ( IOException e ) {
                    responseWriter.println("release: Failure");
                    e.printStackTrace(responseWriter);                
                }
            } else {
                responseWriter.println("Unknown [ " + query + " ]");
            }

            responseWriter.flush();
        }
    }
    
    public void destroy() {
        try {
            closeResources();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
