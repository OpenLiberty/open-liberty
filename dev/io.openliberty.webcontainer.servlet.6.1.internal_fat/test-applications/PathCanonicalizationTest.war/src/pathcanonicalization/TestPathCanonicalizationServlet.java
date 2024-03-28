/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package pathcanonicalization;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test the path canonicalization for ServletContext methods which accept path parameter.
 * The rules from Servlet 6.0, 3.5.2 are applied.
 *
 * These tests do not check for the actual functionality of the target methods.
 * Instead, they just make sure the paths are canonicalized (and rejected if not compliant with the rules)
 * before passing to the target methods.
 *
 */
@WebServlet("/TestPathCanonicalization")
public class TestPathCanonicalizationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestPathCanonicalizationServlet.class.getName();

    private final String GOOD_PATH = "/subFolder/index1.html";
    private final String DOT_ENCODED = "/subFolder/index1%2Ehtml";
    private final String FORWARD_ENCODED = "/subFolder%2Findex1.html";
    private final String DOT_DOT = "/subFolder/subFolder2/../index1.html";

    //to test with RequestDispatcher
    private final String RELATIVE_PATH = "../subFolder/index.html";
    private final String RELATIVE_PATH_ENCODED = "%2e%2E/subFolder/index.html";

    //to test getResourcePaths
    private final String SUB_PATH = "/subFolder";
    private final String SUB_DOT_DOT_PATH = "/subFolder/../subFolder/subFolder2";

    //Compare the real/working path contains this string
    final String index1 = "/PathCanonicalizationTest.war/subFolder/index1.html";


    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;
    ServletContext context;

    public TestPathCanonicalizationServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();
        context = getServletContext();

        LOG("ENTER doGet");
        String runTestMethod = request.getHeader("runTest");
        if (runTestMethod == null) {
            LOG("Run default testGetRealPath");
            runTestMethod = "testGetRealPath";
        }

        switch (request.getHeader("runTest")) {
            case "testGetRealPath" : testGetRealPath(); break;
            case "testGetRequestDispatcher" : testGetRequestDispatcher(); break;
            case "testGetResource" : testGetResource(); break;
            case "testGetResourceAsStream" : testGetResourceAsStream(); break;
            case "testGetResourcePaths" : testGetResourcePaths(); break;
            case "testRequestGetRequestDispatcher" : testRequestGetRequestDispatcher(); break;
        }

        LOG(responseSB.toString());
        sos.println(responseSB.toString());

        LOG("EXIT doGet");
    }

    private void testGetRealPath() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        LOG("Request URI [" + request.getRequestURI());

        String realPath = null;

        responseSB.append("\ntestPath [" + GOOD_PATH + "] ; return [" + (realPath = context.getRealPath(GOOD_PATH)) + "]\n");
        if (realPath.contains(index1))
            responseSB.append("\t test GOOD_PATH [" + GOOD_PATH + "] PASS");
        else
            responseSB.append("\t test GOOD_PATH [" + GOOD_PATH + "] FAIL");

        responseSB.append("\ntestPath [" + DOT_DOT + "] ; return [" + (realPath = context.getRealPath(DOT_DOT)) + "]\n");
        if (realPath.contains(index1))
            responseSB.append  ("\t test DOT_DOT [" + DOT_DOT +"] PASS");
        else
            responseSB.append  ("\t test DOT_DOT [" + DOT_DOT +"] FAIL");

        responseSB.append("\ntestPath [" + DOT_ENCODED + "] ; return [" + (realPath = context.getRealPath(DOT_ENCODED)) + "]\n");
        if (realPath == null)
            responseSB.append ("\t test DOT_ENCODED [" + DOT_ENCODED +"] PASS");
        else
            responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED +"]  FAIL");

        responseSB.append("\ntestPath [" + FORWARD_ENCODED + "] ; return [" + (realPath = context.getRealPath(FORWARD_ENCODED)) + "]\n");
        if (realPath == null)
            responseSB.append ("\t test FORWARD_ENCODED [" + FORWARD_ENCODED +"] PASS");
        else
            responseSB.append  ("\t test FORWARD_ENCODED [" + FORWARD_ENCODED +"] FAIL");

        LOG("<<<< TESTING [" + method + "]");
    }

    private void testGetRequestDispatcher() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        LOG("Request URI [" + request.getRequestURI());

        RequestDispatcher dispatcher;

        responseSB.append("\ntestPath [" + GOOD_PATH + "] ; return [" + (dispatcher = context.getRequestDispatcher(GOOD_PATH)) + "]\n");
        if (dispatcher != null)
            responseSB.append  ("\t test GOOD_PATH [" + GOOD_PATH +"] PASS");
        else
            responseSB.append  ("\t test GOOD_PATH [" + GOOD_PATH +"] FAIL");

        // dispatcher is null for these cases

        // "../subFolder/index.html" becomes "/../subFolder/index.html" before the normalization; thus fail to resolve.
        responseSB.append("\ntestPath [" + RELATIVE_PATH + "] ; return [" + (dispatcher = context.getRequestDispatcher(RELATIVE_PATH)) + "]\n");
        if (dispatcher == null)
            responseSB.append  ("\t test RELATIVE_PATH [" + RELATIVE_PATH +"] PASS");
        else
            responseSB.append  ("\t test RELATIVE_PATH [" + RELATIVE_PATH +"] FAIL");

        responseSB.append("\ntestPath [" + RELATIVE_PATH_ENCODED + "] ; return [" + (dispatcher = context.getRequestDispatcher(RELATIVE_PATH_ENCODED)) + "]\n");
        if (dispatcher == null)
            responseSB.append  ("\t test RELATIVE_PATH_ENCODED [" + RELATIVE_PATH_ENCODED +"] PASS");
        else
            responseSB.append  ("\t test RELATIVE_PATH_ENCODED [" + RELATIVE_PATH_ENCODED +"] FAIL");

        responseSB.append("\ntestPath [" + FORWARD_ENCODED + "] ; return [" + (dispatcher = context.getRequestDispatcher(FORWARD_ENCODED)) + "]\n");
        if (dispatcher == null)
            responseSB.append  ("\t test FORWARD_ENCODED [" + FORWARD_ENCODED +"] PASS");
        else
            responseSB.append  ("\t test FORWARD_ENCODED [" + FORWARD_ENCODED +"] FAIL");

        LOG("<<<< TESTING [" + method + "]");
    }


    private void testGetResource() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        LOG("Request URI [" + request.getRequestURI());

        URL urlResource;
        try {
            //resources are good
            responseSB.append("\ntestPath [" + GOOD_PATH + "] ; return [" + (urlResource = context.getResource(GOOD_PATH)) + "]\n");
            if (urlResource != null)
                responseSB.append("\t test GOOD_PATH [" + GOOD_PATH + "] PASS");
            else
                responseSB.append("\t test GOOD_PATH [" + GOOD_PATH + "] FAIL");

            responseSB.append("\ntestPath [" + DOT_DOT + "] ; return [" + (urlResource = context.getResource(DOT_DOT)) + "]\n");
            if (urlResource != null)
                responseSB.append("\t test DOT_DOT [" + DOT_DOT + "] PASS");
            else
                responseSB.append("\t test DOT_DOT [" + DOT_DOT + "] FAIL");

            //resource should be null
            responseSB.append("\ntestPath [" + DOT_ENCODED + "] ; return [" + (urlResource = context.getResource(DOT_ENCODED)) + "]\n");
            if (urlResource != null)
                responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED + "] FAIL");
            else
                responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED + "] PASS");

            //resource should be null
            responseSB.append("\ntestPath [" + FORWARD_ENCODED + "] ; return [" + (urlResource = context.getResource(FORWARD_ENCODED)) + "]\n");
            if (urlResource != null)
                responseSB.append("\t test FORWARD_ENCODED [" + FORWARD_ENCODED + "] FAIL");
            else
                responseSB.append("\t test FORWARD_ENCODED [" + FORWARD_ENCODED + "] PASS");
        }
        catch (Exception e) {
            responseSB.append("\t Exception during getResource . FAIL [" + e + "]");
            LOG (responseSB.toString());
        }

        LOG("<<<< TESTING [" + method + "]");
    }

    private void testGetResourceAsStream() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        LOG("Request URI [" + request.getRequestURI());

        InputStream input;
        //Good input
        responseSB.append("\ntestPath [" + GOOD_PATH + "] ; return [" + (input= context.getResourceAsStream(GOOD_PATH)) + "]\n");
        if (input != null)
            responseSB.append("\t test GOOD_PATH [" + GOOD_PATH + "] PASS");
        else
            responseSB.append("\t test GOOD_PATH [" + GOOD_PATH + "] FAIL");

        responseSB.append("\ntestPath [" + DOT_DOT + "] ; return [" + (input= context.getResourceAsStream(DOT_DOT)) + "]\n");
        if (input != null)
            responseSB.append("\t test DOT_DOT [" + DOT_DOT + "] PASS");
        else
            responseSB.append("\t test DOT_DOT [" + DOT_DOT + "] FAIL");

        //expect null
        responseSB.append("\ntestPath [" + DOT_ENCODED + "] ; return [" + (input= context.getResourceAsStream(DOT_ENCODED)) + "]\n");
        if (input == null)
            responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED + "] PASS");
        else
            responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED + "] FAIL");

        //expect null
        responseSB.append("\ntestPath [" + FORWARD_ENCODED + "] ; return [" + (input= context.getResourceAsStream(FORWARD_ENCODED)) + "]\n");
        if (input == null)
            responseSB.append("\t test FORWARD_ENCODED [" + FORWARD_ENCODED + "] PASS");
        else
            responseSB.append("\t test FORWARD_ENCODED [" + FORWARD_ENCODED + "] FAIL");

        LOG("<<<< TESTING [" + method + "]");
    }

    private void testGetResourcePaths() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        LOG("Request URI [" + request.getRequestURI());

        Set<String> sourcePaths;
        responseSB.append("\ntestPath [" + SUB_PATH + "] ; return [" + (sourcePaths = context.getResourcePaths(SUB_PATH)) + "]\n");
        if (sourcePaths != null)
            responseSB.append("\t test SUB_PATH [" + SUB_PATH + "] PASS");
        else
            responseSB.append("\t test SUB_PATH [" + SUB_PATH + "] FAIL");

        responseSB.append("\ntestPath [" + SUB_DOT_DOT_PATH + "] ; return [" + (sourcePaths = context.getResourcePaths(SUB_DOT_DOT_PATH)) + "]\n");
        if (sourcePaths != null)
            responseSB.append("\t test SUB_DOT_DOT_PATH [" + SUB_DOT_DOT_PATH + "] PASS");
        else
            responseSB.append("\t test SUB_DOT_DOT_PATH [" + SUB_DOT_DOT_PATH + "] FAIL");

        //expect null
        responseSB.append("\ntestPath [" + DOT_ENCODED + "] ; return [" + (sourcePaths= context.getResourcePaths(DOT_ENCODED)) + "]\n");
        if (sourcePaths == null)
            responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED + "] PASS");
        else
            responseSB.append("\t test DOT_ENCODED [" + DOT_ENCODED + "] FAIL");

        //expect null
        responseSB.append("\ntestPath [" + FORWARD_ENCODED + "] ; return [" + (sourcePaths= context.getResourcePaths(FORWARD_ENCODED)) + "]\n");
        if (sourcePaths == null)
            responseSB.append("\t test FORWARD_ENCODED [" + FORWARD_ENCODED + "] PASS");
        else
            responseSB.append("\t test FORWARD_ENCODED [" + FORWARD_ENCODED + "] FAIL");

        LOG("<<<< TESTING [" + method + "]");
    }

    private void testRequestGetRequestDispatcher() throws IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        LOG("Request URI [" + request.getRequestURI());

        RequestDispatcher dispatcher;

        responseSB.append("\ntestPath [" + GOOD_PATH + "] ; return [" + (dispatcher = request.getRequestDispatcher(GOOD_PATH)) + "]\n");
        if (dispatcher != null)
            responseSB.append  ("\t test GOOD_PATH [" + GOOD_PATH +"] PASS");
        else
            responseSB.append  ("\t test GOOD_PATH [" + GOOD_PATH +"] FAIL");

        // dispatcher is null for these cases
        responseSB.append("\ntestPath [" + RELATIVE_PATH + "] ; return [" + (dispatcher = request.getRequestDispatcher(RELATIVE_PATH)) + "]\n");
        if (dispatcher == null)
            responseSB.append  ("\t test RELATIVE_PATH [" + RELATIVE_PATH +"] PASS");
        else
            responseSB.append  ("\t test RELATIVE_PATH [" + RELATIVE_PATH +"] FAIL");

        responseSB.append("\ntestPath [" + RELATIVE_PATH_ENCODED + "] ; return [" + (dispatcher = request.getRequestDispatcher(RELATIVE_PATH_ENCODED)) + "]\n");
        if (dispatcher == null)
            responseSB.append  ("\t test RELATIVE_PATH_ENCODED [" + RELATIVE_PATH_ENCODED +"] PASS");
        else
            responseSB.append  ("\t test RELATIVE_PATH_ENCODED [" + RELATIVE_PATH_ENCODED +"] FAIL");

        responseSB.append("\ntestPath [" + FORWARD_ENCODED + "] ; return [" + (dispatcher = request.getRequestDispatcher(FORWARD_ENCODED)) + "]\n");
        if (dispatcher == null)
            responseSB.append  ("\t test FORWARD_ENCODED [" + FORWARD_ENCODED +"] PASS");
        else
            responseSB.append  ("\t test FORWARD_ENCODED [" + FORWARD_ENCODED +"] FAIL");

        LOG("<<<< TESTING [" + method + "]");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}

