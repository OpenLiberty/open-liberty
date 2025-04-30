/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.writeafterredirect.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import componenttest.rules.repeater.JakartaEEAction;
import java.util.logging.Logger;

/**
 * Simple servlet that sends a slow, continuous response for testing write
 * failures
 */
@WebServlet("/slow-response")
public class SlowResponseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SlowResponseServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        LOG.info("SlowResponseServlet: Starting response");

        response.setContentType("text/plain");

        ServletOutputStream out = response.getOutputStream();

        // Send initial content
        out.println("Beginning slow response...");
        out.flush();

        // Create some data to send
        byte[] buffer = new byte[4096];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) ('A' + (i % 26));
        }

        try {
            // Send data in a loop with small pauses
            for (int i = 0; i < 100; i++) {
                out.println("Chunk #" + i);
                out.write(buffer);
                out.flush();

                // Small delay between writes
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    LOG.info("Caught InterruptedException");
                    // Ignore
                }
            }
        } catch (IOException e) {
            // Expected when client disconnects - log it, then let it propagate
            LOG.info("Caught IOException:" + e);
            throw e;
        }
    }

}
