/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package sendredirect.servlets;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test HTTP response sendRedirect methods: 
 *
 * sendRedirect(String location)                        => sendRedirect(location, 302, true)
 * sendRedirect(String location, int status_code)       => sendRedirect(location, status_code, true)
 * sendRedirect(String location, boolean clearBuffer)   => sendRedirect(location, 302, clearBuffer)
 * 
 * sendRedirect(String location, int status_code, boolean clearBuffer)
 */
@WebServlet("/TestResponseSendRedirect")
public class TestResponseSendRedirectServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestResponseSendRedirectServlet.class.getName();
    private static final String LOCATION =  "https://github.com/OpenLiberty";
    private static final String RESP_CUSTOM_TEXT_BODY = "Send Redirect Custom Message Text";

    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        request = req;
        response = resp;
        responseSB = new StringBuilder();

        LOG("ENTER doGet");

        String runTestMethod = request.getHeader("runTest");
        if (runTestMethod == null) {
            LOG("No test specify. Run default testSendRedirect");
            runTestMethod = "testSendRedirect";
        }

        if (runTestMethod.equalsIgnoreCase("testSendRedirect"))
            testSendRedirect();
        else if (runTestMethod.equalsIgnoreCase("testSendRedirect_301"))
            testSendRedirect_301();
        else if (runTestMethod.equalsIgnoreCase("testSendRedirect_303"))
            testSendRedirect_303(); 
        if (runTestMethod.equalsIgnoreCase("testSendRedirect_writer"))
            testSendRedirect_writer();
        else if (runTestMethod.equalsIgnoreCase("testSendRedirect_clearBuffer_false"))
            testSendRedirect_clearBuffer_false(); 
        else if (runTestMethod.equalsIgnoreCase("testSendRedirect_301_clearBuffer_false"))
            testSendRedirect_301_clearBuffer_false(); 
        else if (runTestMethod.equalsIgnoreCase("testSendRedirect_303_clearBuffer_true"))
            testSendRedirect_303_clearBuffer_true(); 

        LOG("EXIT doGet");
    }

    /*
     * sendRedirect(String Location) with the default 302 status code
     * No body is provided
     */
    private void testSendRedirect() throws IOException{
        LOG(">>> Testing testSendRedirect_Location");

        try {
            response.sendRedirect(LOCATION);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect");

    }
    
    /*
     * sendRedirect(String Location) with the default 302 status code
     * 
     * Provide a body using PrinterWriter.  
     * Server's sendRedirect will replace the body and also switch b/w outputStream and Writer to accommodate the correct output type.
     */
    private void testSendRedirect_writer() throws IOException{
        LOG(">>> Testing testSendRedirect_Writer");
        try {
            //Writer to write first
            response.getWriter().println("Testing [" + CLASS_NAME + "] , method [testSendRedirect]");
            response.sendRedirect(LOCATION);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect_Writer");

    }

    /*
     * sendRedirect(String Location, int status_code)
     * 
     * with status code 301 and OutputStream body
     * Server will replace the body.
     */
    private void testSendRedirect_301() throws IOException{
        LOG(">>> Testing testSendRedirect_301");
        try {
            //OutputStream to write first
            response.getOutputStream().println("Testing [" + CLASS_NAME + "] , method [testSendRedirect_301]");
            response.sendRedirect(LOCATION, HttpServletResponse.SC_MOVED_PERMANENTLY);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect_301");

    }

    /*
     * sendRedirect(String Location, int status_code)
     * with status code 303
     */
    private void testSendRedirect_303() throws IOException{
        LOG(">>> Testing testSendRedirect_303");

        try {
            response.sendRedirect(LOCATION, HttpServletResponse.SC_SEE_OTHER);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect_303");
    }

    /*
     * sendRedirect(String Location, boolean clearBuffer)
     * with default status code 302, using a outputStream to write a body, and without reset the current buffer.
     */
    private void testSendRedirect_clearBuffer_false() throws IOException{
        LOG(">>> Testing testSendRedirect_clearBuffer_false");

        try {
            response.getOutputStream().println(RESP_CUSTOM_TEXT_BODY);
            response.sendRedirect(LOCATION, false);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect_clearBuffer_false");
    }
    
    /*
     * sendRedirect(String Location, int sc, boolean clearBuffer)
     * with status code 301, using writer to write a body, and without reset the current buffer.
     */
    
    private void testSendRedirect_301_clearBuffer_false() throws IOException{
        LOG(">>> Testing testSendRedirect_301_clearBuffer_false");

        try {
            response.getWriter().println(RESP_CUSTOM_TEXT_BODY);
            response.sendRedirect(LOCATION, HttpServletResponse.SC_MOVED_PERMANENTLY, false);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect_301_clearBuffer_false");
    }
    
    /*
     * sendRedirect(String Location, int sc, boolean clearBuffer)
     * with status code 303, using writer to write a body, and explicitly clearBuffer = true
     * App's body will be reset and replaced with default hypertext URL
     * Server will switch dynamically b/w outputStream and Writer to write out the default hypertext
     */
    
    private void testSendRedirect_303_clearBuffer_true() throws IOException{
        LOG(">>> Testing testSendRedirect_303_clearBuffer_true");

        try {
            response.getWriter().println(RESP_CUSTOM_TEXT_BODY);
            response.sendRedirect(LOCATION, HttpServletResponse.SC_SEE_OTHER, true);
        }
        catch (Exception e){
            LOG("Exception during sendRedirect [" + e + "]");
            throw e;
        }
        LOG("<<< Testing testSendRedirect_303_clearBuffer_true");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
