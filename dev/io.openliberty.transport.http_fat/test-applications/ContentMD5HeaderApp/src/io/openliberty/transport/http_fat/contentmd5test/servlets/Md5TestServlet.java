/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.transport.http_fat.contentmd5test.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

/**
 * A simple servlet that attempts to generate an MD5 hash and set the
 * Content-MD5 header.
 */
@WebServlet("/md5test")
public class Md5TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger Log = Logger.getLogger(Md5TestServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String responseBody = "This is a test response.";
        byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        Log.info("Md5TestServlet: Preparing the digest.");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(bodyBytes);
            String encodedHash = Base64.getEncoder().encodeToString(hash);

            response.setHeader("Content-MD5", encodedHash);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            Log.info("Md5TestServlet: Response headers set.");

        } catch (NoSuchAlgorithmException e) {

            throw new ServletException("Failed to get MD5 instance.", e);
        } catch (Exception e){

            throw new ServletException("Exception occured.", e);
        }

        PrintWriter out = response.getWriter();
        out.print(responseBody);
        out.flush();
        Log.info("Md5TestServlet: Response sent.");
    }
}