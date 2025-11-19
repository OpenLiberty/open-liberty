/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package decompression.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Reads the response (which should be compressesd) and displays it to the user.
 * An IllegalHttpBodyException error should occur if a malformed zip is sent
 */

@WebServlet(urlPatterns = "/DecompressionServlet")
public class DecompressionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        try {
            writer.print("Welcome to the DecompressionServlet!");
            StringBuilder payload = new StringBuilder();
            BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null) {
                    payload.append(line);
                }
            writer.print("Payload: " + payload.toString());
         } catch (Exception e) {
            writer.print("Exception Occurred: " + e.toString());
        } finally {
            writer.flush();
            writer.close();
        }
    }
}
