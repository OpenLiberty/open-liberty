/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/BasicReadListenerAsyncFalseServlet")
public class TestAsyncFalseReadServlet extends HttpServlet {

    ServletInputStream input = null;
    ServletOutputStream output = null;
    /**  */
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TestAsyncFalseReadServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        try {
            input = req.getInputStream();
            output = res.getOutputStream();
            AsyncContext ac = null;
            // set up ReadListener to read data for processing

            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "AsyncFalseReadListenerImpl");
            input.setReadListener(readListener);
        } catch (Exception e) {
            LOG.info("In BasicReadListenerAsyncFalseServlet , caught IllegalStateException ," + e.toString() + " printing stack trace ...");
            output.print(e.toString());
            e.printStackTrace();
        }
    }
}