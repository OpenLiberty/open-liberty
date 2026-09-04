/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package servlets.async;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Async dispatch with a path that has a query string containing an illegal encoded %2F (forward flash) character.
 *      1. Dispatch with a (path + encoded query string)
 *      2. Dispatch with an encoded path 
 */

@WebServlet(urlPatterns = "/AsyncDispatch/*", asyncSupported = true)
public class AsyncDispatchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = AsyncDispatchServlet.class.getName();
    private static final String dispatchURI = "/AsyncDispatched/dispatchedPathInfo";
    private static final String dispatchIllegalEncodedURL = "/AsyncDispatched/has_%2F_AForwardSlash";
    private static final String queryStringBadEncodedCharacter = "?param=test%2Fvalue";

    PrintWriter writer;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOG("AsyncDispatchServlet, doGet");
        
        writer = resp.getWriter();
        
        switch (req.getHeader("runTest")) {
            case "test_dispatchEncodedQueryString" : test_dispatchEncodedQueryString(req,resp); break;
            case "test_dispatchEncodedPath" : test_dispatchEncodedPath(req,resp); break;
        }

    }
    
    private void test_dispatchEncodedQueryString(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        String path = dispatchURI + queryStringBadEncodedCharacter ;
        

        writer.println("<br>*** AsyncDispatchServlet starting Async and dispatch (" + path +") ***");

        try{
            AsyncContext context = req.startAsync(req, resp);
            LOG("async dispatch to [" + path + "]");
            context.dispatch(path);
        }
        catch (Exception e){
            LOG("Exception during dispatch : " + e);
            writer.println("<br>*** AsyncDispatchServlet dispatch has exception [" + e +"] ***");
            throw e;    //make noise as this is not expected.
        }
        
        writer.println("<br>*** AsyncDispatchServlet returning after dipstach . PASS_1 ***************");
        
        LOG("<<< TESTING [" + method + "]");
    }
    
    private void test_dispatchEncodedPath(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = new Object() {}.getClass().getEnclosingMethod().getName();
        LOG(">>> TESTING [" + method + "]");
        
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        String path = dispatchIllegalEncodedURL;
        
        writer.println("<br>*** AsyncDispatchServlet starting Async and dispatch (" + path +") ***");
        try{
            AsyncContext context = req.startAsync(req, resp);
            LOG("async dispatch to [" + path + "]");
            context.dispatch(path);
        }
        catch (Exception e){
            LOG("Exception during dispatch : " + e);
            //Report and handle this expected exception in a 200 response
            writer.println("<br>*** AsyncDispatchServlet dispatch has exception [" + e +"] ***");
        }
        
        LOG("<<< TESTING [" + method + "]");
    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
