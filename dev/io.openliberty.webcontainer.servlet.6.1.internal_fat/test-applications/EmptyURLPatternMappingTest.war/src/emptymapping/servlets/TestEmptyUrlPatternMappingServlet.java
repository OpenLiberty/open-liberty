/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package emptymapping.servlets;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test special empty urlPatterns mapping servlet which is mapped to the context root
 * 
 * Expecting:
 * context path is the /context root
 * servlet path is empty
 * path info is a "/"
 */
@WebServlet(name="TestEmptyUrlPatternMappingServlet", displayName="TestEmptyPatternMapping", urlPatterns={""})
public class TestEmptyUrlPatternMappingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestEmptyUrlPatternMappingServlet.class.getName();

    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;
    String contextPath, servletPath, pathInfo;

    public TestEmptyUrlPatternMappingServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        LOG("ENTER doGet");
        sos.println("Hello from the " + CLASS_NAME);

        testEmptyURLPatternMapping();
        
        LOG("EXIT doGet");
    }

    /**
     * Request to a context root without ending slash. Server will append the ending slash and redirect.
     * 
     * The results are the same for request to context-root with or without ending slash.
     */
    private void testEmptyURLPatternMapping() throws IOException {
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING ["+method+"]");
        

        if ((contextPath = request.getContextPath()).equals("/EmptyURLPatternMappingTest")) {
            sos.println("Test context path [PASS]. Found [" + contextPath + "]"); 
        }
        else {
            sos.println("Test context path [FAIL]. Found ["+contextPath+"] ; Expecting [/EmptyURLPatternMappingTest]");         
        }
        
        if ((servletPath = request.getServletPath()).isEmpty()) {
            sos.println("Test servlet path [PASS]. Found [" + servletPath + "]"); 
        }
        else {
            sos.println("Test servlet path [FAIL]. Found [" + servletPath + "] ; Expecting []");            
        }

        if ((pathInfo = request.getPathInfo()).equals("/")) {
            sos.println("Test path info [PASS]. Found [" + pathInfo + "]"); 
        }
        else {
            sos.println("Test path info [FAIL]. Found [" + pathInfo + "] ; Expecting [/]");                   
        }

        LOG("<<<< TESTING ["+method+"]");
    }
    
    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
