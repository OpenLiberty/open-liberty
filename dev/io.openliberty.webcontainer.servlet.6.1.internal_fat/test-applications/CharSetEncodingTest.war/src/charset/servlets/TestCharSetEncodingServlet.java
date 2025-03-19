/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
        test_Response_SetCharEncoding_NullCharset(response,sBuilder);
        test_Request_SetCharEncoding_NullCharset(request,sBuilder);

        sos.println("Test Results from " + CLASS_NAME + ": \n" + sBuilder.toString());
    }

    private void testRequestSetCharEncodingviaCharset(HttpServletRequest request, StringBuilder sBuilder) {
        LOG(">>> Testing testRequestSetCharEncodingviaCharset");

        request.setCharacterEncoding(Charset.defaultCharset());
        encoding = request.getCharacterEncoding();
        LOG("Request Charset.defaultCharset() test. request.getCharacterEncoding() ["+ encoding +"]");

        if (encoding.equals(Charset.defaultCharset().name()))
            sBuilder.append("Test Request Charset.defaultCharset [PASS]\n");
        else
            sBuilder.append("Test Request Charset.defaultCharset [FAIL]. Found [" + encoding + "] \n");

        request.setCharacterEncoding(StandardCharsets.US_ASCII);
        encoding = request.getCharacterEncoding();
        LOG("Charset US_ASCII test. request.getCharacterEncoding() ["+ encoding +"]");

        if (encoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("Test Request US_ASCII [PASS]\n");
        else
            sBuilder.append("Test Request US_ASCII [FAIL]. Found [" + encoding + "]\n");

        sBuilder.append("===============================\n");
    }

    private void testResponseSetCharEncodingviaCharset(HttpServletResponse response, StringBuilder sBuilder) {
        LOG(">>> Testing testResponseSetCharEncodingviaCharset");

        response.setCharacterEncoding(Charset.defaultCharset());
        encoding = response.getCharacterEncoding();
        LOG("Response Charset.defaultCharset() test; response.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(Charset.defaultCharset().name()))
            sBuilder.append("Test Response Charset.defaultCharset [PASS]\n");
        else
            sBuilder.append("Test Response Charset.defaultCharset [FAIL]. Found [" + encoding + "] \n");

        response.setCharacterEncoding(StandardCharsets.US_ASCII);
        encoding = response.getCharacterEncoding();
        LOG("Response Charset US_ASCII test; response.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("Test Response US_ASCII [PASS]\n");
        else
            sBuilder.append("Test Response US_ASCII [FAIL]. Found [" + encoding + "] \n");

        sBuilder.append("===============================\n");
    }

    /*
     * Retrieve the encoding from SCI which set as follow:
     *
     * context.setRequestCharacterEncoding(Charset.defaultCharset());
     * context.setResponseCharacterEncoding(StandardCharsets.US_ASCII);
     */
    private void testServletContextSetCharset(ServletContext context, StringBuilder sBuilder) {
        LOG(">>> Testing testServletContextSetCharset");
        String reqEncoding = context.getRequestCharacterEncoding();
        String resEncoding = context.getResponseCharacterEncoding();
        LOG("Context Request encoding [" + reqEncoding + "]");
        LOG("Context Response encoding [" + resEncoding + "]");

        if (reqEncoding.equals(Charset.defaultCharset().name()))
            sBuilder.append("Test Context SCI Request Charset.defaultCharset [PASS]\n");
        else
            sBuilder.append("Test Context SCI Request Charset.defaultCharset [FAIL]. Found [" + reqEncoding + "] \n");
        if (resEncoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("Test Context SCI Response US_ASCII [PASS]\n");
        else
            sBuilder.append("Test Context SCI Response US_ASCII [FAIL]. Found [" + resEncoding + "] \n");

        sBuilder.append("===============================\n");
    }

    /*
     * Test response null charset which should reset all previous setting, except ServletContext encoding
     * ServletContext response US_ASCII will be used.
     */
    private void test_Response_SetCharEncoding_NullCharset(HttpServletResponse response, StringBuilder sBuilder) {
        LOG(">>> Testing test_Response_SetCharEncoding_NullCharset ENTER");

        //first set a response charset UTF_8
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        encoding = response.getCharacterEncoding();
        LOG("Response null charset test; first response.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(StandardCharsets.UTF_8.name()))
            sBuilder.append("Test Response Null charset. First Response set to UTF_8 [PASS]\n");
        else
            sBuilder.append("Test Response Null charset. First Response set to UTF_8 [FAIL]. Found [" + encoding + "] \n");

        /*
            second set a response null charset which reset all previous setting.
            If ServletContext (from SCI) response encoding is set, use it  <<< this is the case for this test
            Else Default response will be used
         */

        Charset null_cs = null;
        response.setCharacterEncoding(null_cs);
        encoding = response.getCharacterEncoding();     // The ServletContext (in SCI) response encoding US_ASCII will be used.
        LOG("Response null charset test; response.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(StandardCharsets.US_ASCII.name()))
            sBuilder.append("Test Response Null charset. After null CS setting, Response is reset to US_ASCII [PASS]\n");
        else
            sBuilder.append("Test Response Null charset. After null CS setting, Response is NOT reset to US_ASCII [FAIL]. Found [" + encoding + "] \n");

        LOG("<<< Testing test_Response_SetCharEncoding_NullCharset EXIT");

        sBuilder.append("===============================\n");
    }


    /*
     * Test request set null charset encoding which is ignored.
     */
    private void test_Request_SetCharEncoding_NullCharset(HttpServletRequest request, StringBuilder sBuilder) {
        LOG(">>> Testing test_Request_SetCharEncoding_NullCharset ENTER");

        //first set a request charset UTF_8
        request.setCharacterEncoding(StandardCharsets.UTF_8);
        encoding = request.getCharacterEncoding();
        LOG("Request null charset test; first request.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(StandardCharsets.UTF_8.name()))
            sBuilder.append("Test Request Null charset. First Request set to UTF_8 [PASS]\n");
        else
            sBuilder.append("Test Request Null charset. First Request set to UTF_8 [FAIL]. Found [" + encoding + "] \n");

        /*
         * null charset is ignored.
         */
        Charset null_cs = null;
        request.setCharacterEncoding(null_cs);
        encoding = request.getCharacterEncoding();
        LOG("Request null charset test; request.getCharacterEncoding() ["+  encoding+"]");

        if (encoding.equals(StandardCharsets.UTF_8.name()))
            sBuilder.append("Test Request Null charset. After null CS setting, Request set to UTF_8 [PASS]\n");
        else
            sBuilder.append("Test Request Null charset. After null CS setting, Request set to UTF_8 [FAIL]. Found [" + encoding + "] \n");

        LOG("<<< Testing test_Request_SetCharEncoding_NullCharset EXIT");

    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
