/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.fileupload.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/ChunkSizeTestServlet")
public class ChunkSizeTestServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("true".equals(request.getParameter("readBody"))) {
            // Body-reading path: used to test messageSizeLimit hardening
            try{
                InputStream is = request.getInputStream();
                byte[] buf = new byte[4096];
                while (is.read(buf) != -1) { }
                response.setStatus(200);
                response.getWriter().println("OK");
            } catch (Exception e){
                response.sendError(400, e.getMessage());
            }
            
        } else if ("true".equals(request.getParameter("reportParsed"))) {
            // Body was successfully parsed by transport (chunk-size guard did NOT fire).
            // If we reach here, the chunk-size was accepted. The actual body bytes
            // may be fewer than declared (connection closed early), causing an IOException.
            try {
                InputStream is = request.getInputStream();
                byte[] buf = new byte[4096];
                while (is.read(buf) != -1) { }
                response.setStatus(200);
                response.getWriter().println("CHUNK_SIZE_ACCEPTED: body fully received");
            } catch (IOException e) {
                // EOFException or similar — chunk-size was accepted but body was incomplete.
                // This is DIFFERENT from IllegalHttpBodyException (which means size was rejected).
                String exType = e.getClass().getSimpleName();
                if (e.getMessage() != null && e.getMessage().contains("Chunk size overflow")) {
                    // Fix guard fired — chunk-size was REJECTED
                    response.sendError(400, "CHUNK_SIZE_REJECTED: " + e.getMessage());
                } else {
                    // Chunk-size was accepted, body just wasn't fully delivered
                    response.setStatus(200);
                    response.getWriter().println("CHUNK_SIZE_ACCEPTED: incomplete body (" + exType + ")");
                }
            }
        } else {
            // Non-body-reading path: used to test smuggling 
            response.setStatus(200);
            response.getWriter().println("OK");
        }
        
    }
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
