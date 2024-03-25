/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package getparameter.servlets;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This test will test the runtime exception IllegalStateException during parsing of request parameter
 * from query string, post body.
 * The parsing of param in multipart request has been covered in the FAT 4 FileUploadFileCountMaxTest
 */
@WebServlet(urlPatterns = {"/TestGetParameter/*"}, name = "GetParametersServlet")
public class GetParametersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = GetParametersServlet.class.getName();

    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;

    public GetParametersServlet() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req,resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG("ENTER doGet");

        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        String runTestMethod = request.getHeader("runTest");
        if (runTestMethod == null) {
            LOG("Run default testGetParameter");
            runTestMethod = "testGetParameter";
        }

        if (runTestMethod.equalsIgnoreCase("testGetParameter"))
            testGetParameter();
        if (runTestMethod.equalsIgnoreCase("testGetParameterNames"))
            testGetParameterNames();
        if (runTestMethod.equalsIgnoreCase("testGetParameterValues"))
            testGetParameterValues();
        if (runTestMethod.equalsIgnoreCase("testGetParameterMap"))
            testGetParameterMap();

        LOG(responseSB.toString());
        sos.println(responseSB.toString());

        LOG("EXIT doGet");
    }


    /*
     * Test getParameter.
     * The key is not matter since the POST data has bad encoding and causes IllegalStateException
     */
    private void testGetParameter() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOG(">>>>>>>> TESTING [" + method + "]  <<<<<<<<<<");

        try {
            LOG("All query string [" + request.getQueryString() + "]");
            LOG("calling request.getParameter..");
            responseSB.append("getParameter(\"KEY\") = "+ request.getParameter("KEY"));
        }
        catch (IllegalStateException e) {       //only handle ISE, let other exceptions fail the test
            responseSB.append("Exception found [" + e + "]");
        }

        LOG("<<<< TESTING [" + method + "]");
    }

    /*
     * Test getParameterNames
     */
    private void testGetParameterNames() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOG(">>>>>>>> TESTING [" + method + "]  <<<<<<<<<<");

        try {
            LOG("=======<LIST ALL Params >===============");
            Enumeration<String> e = request.getParameterNames();
            while (e.hasMoreElements()) {
                String name = e.nextElement();
                String values[] = request.getParameterValues(name);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        LOG(name + ":" + values[i]);
                        responseSB.append(name + ":" + values[i]);
                        responseSB.append("\n");
                    }
                }
            }
            LOG("=======</LIST ALL Params >===============");
        }
        catch (IllegalStateException e) {
            responseSB.append("Exception found [" + e + "]");
        }

        LOG("<<<< TESTING [" + method + "]");
    }

    /*
     * Test getParameterValues
     *
     * The name parameter is not matter since the POST data has bad encoding and causes IllegalStateException if ANY key or value is bad
     */
    private void testGetParameterValues() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOG(">>>>>>>> TESTING [" + method + "]  <<<<<<<<<<");

        try {
            request.getParameterValues("ANY_NAME");
        }
        catch (IllegalStateException e) {
            responseSB.append("Exception found [" + e + "]");
        }

        LOG("<<<< TESTING [" + method + "]");
    }

    private void testGetParameterMap() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOG(">>>>>>>> TESTING [" + method + "]  <<<<<<<<<<");

        try {
            request.getParameterMap();
        }
        catch (IllegalStateException e) {
            responseSB.append("Exception found [" + e + "]");
        }

        LOG("<<<< TESTING [" + method + "]");
    }
    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
