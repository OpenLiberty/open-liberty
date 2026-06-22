/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.headercasing.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to test HTTP header casing behavior.
 * This servlet echoes back request headers and allows setting response headers
 * with specific casing to verify that header names maintain consistent casing.
 */
@WebServlet("/HeaderCasingServlet")
public class HeaderCasingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if ("echo".equals(action)) {
            echoRequestHeaders(request, response);
        } else if ("setCustom".equals(action)) {
            setCustomResponseHeaders(request, response);
        } else if ("setStandard".equals(action)) {
            setStandardResponseHeaders(request, response);
        } else if ("setMixed".equals(action)) {
            setMixedCasingHeaders(request, response);
        } else if ("setPopularHeaders".equals(action)) {
            setPopularHeaders(request, response);
        } else {
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("Invalid action. Use: echo, setCustom, setStandard, setMixed, or setPopularHeaders");
        }
    }

    /**
     * Echo back all request headers with their exact casing
     */
    private void echoRequestHeaders(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        
        out.println("Request Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            out.println(headerName + ": " + headerValue);
        }
    }

    /**
     * Set custom response headers with specific casing
     */
    private void setCustomResponseHeaders(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Set custom headers with specific casing
        response.setHeader("X-CUSTOM-Header", "FirstValue");
        response.addHeader("X-custom-HEADER", "SecondValue");
        
        // Try setting the same header with different casing
        response.addHeader("x-custom-header", "ThirdValue");
        response.addHeader("X-CUSTOM-HEADER", "FourthValue");
        
        // Another custom header
        response.setHeader("X-Test-Casing", "TestValue");
        
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.println("Custom headers set. Check response headers.");
    }

    /**
     * Set standard HTTP headers with various casings
     */
    private void setStandardResponseHeaders(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Set standard headers with proper casing
        response.setHeader("Content-Type", "text/plain");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Frame-Options", "DENY");
        
        // Try setting with different casing (should normalize to standard)
        response.addHeader("cache-control", "no-store");
        response.addHeader("CACHE-CONTROL", "must-revalidate");
        
        PrintWriter out = response.getWriter();
        out.println("Standard headers set with various casings. Check response headers.");
    }

    /**
     * Set a mix of standard and custom headers with various casings
     */
    private void setMixedCasingHeaders(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        // Standard headers
        response.setHeader("Content-Type", "text/plain");
        response.setHeader("content-type", "text/html"); // Should override with normalized casing
        
        // Custom headers
        response.setHeader("X-First-Custom", "Value1");
        response.addHeader("x-first-custom", "Value2"); // Should use first occurrence casing
        
        response.setHeader("X-Second-Custom", "ValueA");
        response.addHeader("X-SECOND-CUSTOM", "ValueB"); // Should use first occurrence casing
        
        PrintWriter out = response.getWriter();
        out.println("Mixed headers set. Check response headers for casing consistency.");
    }

    /**
     * Set popular HTTP headers with various casings to test normalization
     */
    private void setPopularHeaders(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Set popular headers with odd casing first
        response.setHeader("content-TYPE", "text/plain");
        response.setHeader("CACHE-Control", "no-cache, no-store");
        response.setHeader("ACCept", "text/html");
        response.setHeader("AccepT-EncodING", "gzip, deflate");
        response.setHeader("User-AGENT", "TestAgent/1.0");
        response.setHeader("AuThoriZatioN", "Bearer test-token");
        
        // Try setting some with different casing
        response.addHeader("content-type", "text/html"); // Should use Content-Type
        response.addHeader("cache-control", "must-revalidate"); // Should use Cache-Control
        response.addHeader("accept-encoding", "br"); // Should use Accept-Encoding
        
        PrintWriter out = response.getWriter();
        out.println("Popular headers set with various casings. Check response headers for proper casing.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}