/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/TestAsyncReadServlet", asyncSupported = true)
public class TestAsyncReadServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TestAsyncReadServlet.class.getName());
    private String testToCall;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        PrintWriter pw = res.getWriter();
        pw.println("TestAsyncReadServlet.doGet()");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        // start async
        AsyncContext ac = req.startAsync();

        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            LOG.info("Request header : " + headerName + " = " + req.getHeader(headerName));
        }

        testToCall = req.getHeader("TestToCall");
        LOG.info("/*************************************************************************************/");
        LOG.info("/************ [TestRequestProperty]: " + testToCall + " Start ************/");

        // set up ReadListener to read data for processing
        ServletInputStream input = req.getInputStream();

        ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "TestAsyncReadServlet : " + testToCall);
        input.setReadListener(readListener);

    }
}
