/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package charset.servlets;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test request and response setCharacterEncoding(Charset)
 */
@WebServlet("/TestCharSetEncoding")
public class TestCharSetEncodingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestCharSetEncodingServlet.class.getName();
    String encoding;

    public TestCharSetEncodingServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        StringBuilder sBuilder = new StringBuilder();
        ServletContext context = getServletContext();

        testRequestSetCharEncodingviaCharset(request, sBuilder);
        testResponseSetCharEncodingviaCharset(response, sBuilder);
        testServletContextSetCharset(context, sBuilder);

        sos.println("Test Results from " + CLASS_NAME + ": \n" + sBuilder.toString());
    }

    private void testRequestSetCharEncodingviaCharset(HttpServletRequest request, StringBuilder sBuilder) {
    	LOG(">>> Testing testRequestSetCharEncodingviaCharset");

    	request.setCharacterEncoding(Charset.defaultCharset());
    	encoding = request.getCharacterEncoding();
    	LOG("Request Charset.defaultCharset() test. request.getCharacterEncoding() ["+ encoding +"]");
    	if (encoding.equals(Charset.defaultCharset().name()))
    	    sBuilder.append("test Request Charset.defaultCharset [PASS]\n");
    	else
    	    sBuilder.append("test Request Charset.defaultCharset [FAIL]\n");

    	request.setCharacterEncoding(StandardCharsets.US_ASCII);
    	encoding = request.getCharacterEncoding();
        LOG("Charset US_ASCII test. request.getCharacterEncoding() ["+ encoding +"]");
        if (encoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("test Request US_ASCII [PASS]\n");
        else
            sBuilder.append("test Request US_ASCII [FAIL]\n");
    }
    
    private void testResponseSetCharEncodingviaCharset(HttpServletResponse response, StringBuilder sBuilder) {
        LOG(">>> Testing testResponseSetCharEncodingviaCharset");

        response.setCharacterEncoding(Charset.defaultCharset());
        encoding = response.getCharacterEncoding();
        LOG("Response Charset.defaultCharset() test; response.getCharacterEncoding() ["+  encoding+"]");
        if (encoding.equals(Charset.defaultCharset().name()))
            sBuilder.append("test Response Charset.defaultCharset [PASS]\n");
        else
            sBuilder.append("test Response Charset.defaultCharset [FAIL]\n");

        response.setCharacterEncoding(StandardCharsets.US_ASCII);
        encoding = response.getCharacterEncoding();
        LOG("Response Charset US_ASCII test; response.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("test Response US_ASCII [PASS]\n");
        else
            sBuilder.append("test Response US_ASCII [FAIL]\n");    
    }
    
    /*
     * Retrieve the set encoding which set in the SCI
     */
    private void testServletContextSetCharset(ServletContext context, StringBuilder sBuilder) {
        LOG(">>> Testing testServletContextSetCharset");
        String reqEncoding = context.getRequestCharacterEncoding();
        String resEncoding = context.getResponseCharacterEncoding();
        LOG("Context Request encoding [" + reqEncoding + "]");
        LOG("Context Response encoding [" + resEncoding + "]");
        
        if (reqEncoding.equals(Charset.defaultCharset().name()))
            sBuilder.append("test Context Request Charset.defaultCharset [PASS]\n");
        else
            sBuilder.append("test Context Request Charset.defaultCharset [FAIL]\n");
        
        if (resEncoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("test Context Response US_ASCII [PASS]\n");
        else
            sBuilder.append("test Context Response  US_ASCII [FAIL]\n");    
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
