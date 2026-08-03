/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package http2.test.war.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that generates a large response for testing maxQueuedBytes limit.
 * The response size can be controlled via query parameters:
 * - "size" parameter: size in MB (default: 15)
 * - "sizeKB" parameter: size in KB (overrides "size" if specified)
 *
 * Examples:
 * - /H2LargeResponse?size=10 (10 MB)
 * - /H2LargeResponse?sizeKB=512 (512 KB)
 */
@WebServlet("/H2LargeResponse")
public class H2LargeResponse extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    private static final int DEFAULT_SIZE_MB = 15;
    private static final int CHUNK_SIZE = 1024; // 1 KB chunks

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Determine the size in KB
        int sizeKB;
        
        // Check if sizeKB parameter is specified (takes precedence)
        String sizeKBParam = request.getParameter("sizeKB");
        if (sizeKBParam != null) {
            try {
                sizeKB = Integer.parseInt(sizeKBParam);
            } catch (NumberFormatException e) {
                sizeKB = DEFAULT_SIZE_MB * 1024; // Default to 15 MB
            }
        } else {
            // Fall back to size parameter (in MB)
            String sizeParam = request.getParameter("size");
            int sizeMB = DEFAULT_SIZE_MB;
            if (sizeParam != null) {
                try {
                    sizeMB = Integer.parseInt(sizeParam);
                } catch (NumberFormatException e) {
                    sizeMB = DEFAULT_SIZE_MB;
                }
            }
            sizeKB = sizeMB * 1024;
        }
        
        // Calculate total chunks to send
        int totalChunks = (sizeKB * 1024) / CHUNK_SIZE;
        
        // Create a 1 KB data chunk
        byte[] dataChunk = new byte[CHUNK_SIZE];
        for (int i = 0; i < CHUNK_SIZE; i++) {
            dataChunk[i] = (byte) (i % 256);
        }
        
        // Set response headers
        response.setContentType("application/octet-stream");
        response.setHeader("X-Response-Size-KB", String.valueOf(sizeKB));
        response.setHeader("X-Response-Size-MB", String.valueOf(sizeKB / 1024.0));
        
        // Write the data in chunks
        for (int i = 0; i < totalChunks; i++) {
            response.getOutputStream().write(dataChunk);
            
            // Flush periodically to ensure data is sent
            if (i % 100 == 0) {
                response.getOutputStream().flush();
            }
        }
        
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
