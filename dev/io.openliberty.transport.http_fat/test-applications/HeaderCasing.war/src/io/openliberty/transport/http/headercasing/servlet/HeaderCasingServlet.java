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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger logger = Logger.getLogger(HeaderCasingServlet.class.getName());

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
        } else if ("testEdgeCases".equals(action)) {
            testEdgeCases(request, response);
        } else {
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("Invalid action. Use: echo, setCustom, setStandard, setMixed, setPopularHeaders, or testEdgeCases");
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

    /**
     * Test edge cases for setHeader and addHeader methods including:
     * - null header names
     * - null header values
     * - empty string header names
     * - empty string header values
     * - special characters
     * - very long values
     * All exceptions are caught and logged.
     */
    private void testEdgeCases(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        out.println("Testing edge cases for setHeader and addHeader:");
        out.println("==============================================\n");

        // Test 1: null header name with setHeader
        out.println("Test 1: setHeader with null header name");
        try {
            response.setHeader(null, "value");
            out.println("  Result: No exception thrown (unexpected)");
        } catch (NullPointerException e) {
            out.println("  Result: NullPointerException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with null header name threw NullPointerException", e);
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with null header name threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with null header name threw unexpected exception", e);
        }

        // Test 2: null header value with setHeader
        out.println("\nTest 2: setHeader with null header value");
        try {
            response.setHeader("X-Null-Value-Test", null);
            out.println("  Result: No exception thrown");
        } catch (NullPointerException e) {
            out.println("  Result: NullPointerException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with null header value threw NullPointerException", e);
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with null header value threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with null header value threw unexpected exception", e);
        }

        // Test 3: null header name with addHeader
        out.println("\nTest 3: addHeader with null header name");
        try {
            response.addHeader(null, "value");
            out.println("  Result: No exception thrown (unexpected)");
        } catch (NullPointerException e) {
            out.println("  Result: NullPointerException caught - " + e.getMessage());
            logger.log(Level.INFO, "addHeader with null header name threw NullPointerException", e);
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "addHeader with null header name threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "addHeader with null header name threw unexpected exception", e);
        }

        // Test 4: null header value with addHeader
        out.println("\nTest 4: addHeader with null header value");
        try {
            response.addHeader("X-Null-Value-Add-Test", null);
            out.println("  Result: No exception thrown");
        } catch (NullPointerException e) {
            out.println("  Result: NullPointerException caught - " + e.getMessage());
            logger.log(Level.INFO, "addHeader with null header value threw NullPointerException", e);
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "addHeader with null header value threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "addHeader with null header value threw unexpected exception", e);
        }

        // Test 5: empty string header name with setHeader
        out.println("\nTest 5: setHeader with empty string header name");
        try {
            response.setHeader("", "value");
            out.println("  Result: No exception thrown");
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with empty header name threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with empty header name threw unexpected exception", e);
        }

        // Test 6: empty string header value with setHeader
        out.println("\nTest 6: setHeader with empty string header value");
        try {
            response.setHeader("X-Empty-Value-Test", "");
            out.println("  Result: No exception thrown");
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with empty header value threw unexpected exception", e);
        }

        // Test 7: empty string header name with addHeader
        out.println("\nTest 7: addHeader with empty string header name");
        try {
            response.addHeader("", "value");
            out.println("  Result: No exception thrown");
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "addHeader with empty header name threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "addHeader with empty header name threw unexpected exception", e);
        }

        // Test 8: empty string header value with addHeader
        out.println("\nTest 8: addHeader with empty string header value");
        try {
            response.addHeader("X-Empty-Value-Add-Test", "");
            out.println("  Result: No exception thrown");
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "addHeader with empty header value threw unexpected exception", e);
        }

        // Test 9: whitespace-only header name
        out.println("\nTest 9: setHeader with whitespace-only header name");
        try {
            response.setHeader("   ", "value");
            out.println("  Result: No exception thrown");
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with whitespace header name threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with whitespace header name threw unexpected exception", e);
        }

        // Test 10: whitespace-only header value
        out.println("\nTest 10: setHeader with whitespace-only header value");
        try {
            response.setHeader("X-Whitespace-Value-Test", "   ");
            out.println("  Result: No exception thrown");
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with whitespace header value threw unexpected exception", e);
        }

        // Test 11: header name with special characters
        out.println("\nTest 11: setHeader with special characters in header name");
        try {
            response.setHeader("X-Special@Char#Test", "value");
            out.println("  Result: No exception thrown");
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with special chars in header name threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with special chars in header name threw unexpected exception", e);
        }

        // Test 12: header value with newline characters
        out.println("\nTest 12: setHeader with newline in header value");
        try {
            response.setHeader("X-Newline-Test", "value\nwith\nnewlines");
            out.println("  Result: No exception thrown");
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with newline in header value threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with newline in header value threw unexpected exception", e);
        }

        // Test 13: very long header value
        out.println("\nTest 13: setHeader with very long header value (10000 chars)");
        try {
            StringBuilder longValue = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longValue.append("A");
            }
            response.setHeader("X-Long-Value-Test", longValue.toString());
            out.println("  Result: No exception thrown");
        } catch (Exception e) {
            out.println("  Result: Exception caught - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.INFO, "setHeader with very long header value threw exception", e);
        }

        // Test 14: both null name and value
        out.println("\nTest 14: setHeader with both null name and value");
        try {
            response.setHeader(null, null);
            out.println("  Result: No exception thrown (unexpected)");
        } catch (NullPointerException e) {
            out.println("  Result: NullPointerException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with both null threw NullPointerException", e);
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "setHeader with both null threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "setHeader with both null threw unexpected exception", e);
        }

        // Test 15: both empty strings
        out.println("\nTest 15: addHeader with both empty string name and value");
        try {
            response.addHeader("", "");
            out.println("  Result: No exception thrown");
        } catch (IllegalArgumentException e) {
            out.println("  Result: IllegalArgumentException caught - " + e.getMessage());
            logger.log(Level.INFO, "addHeader with both empty strings threw IllegalArgumentException", e);
        } catch (Exception e) {
            out.println("  Result: Unexpected exception - " + e.getClass().getName() + ": " + e.getMessage());
            logger.log(Level.WARNING, "addHeader with both empty strings threw unexpected exception", e);
        }

        out.println("\n==============================================");
        out.println("Edge case testing completed. Check logs for exception details.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}

