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
package test.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.listeners.TestAsyncNBReadListener;

/**
 * Servlet implementation class NBReadPostDataFromInputStreamServlet
 */
@WebServlet(
            urlPatterns = { "/ReadParameterFilter/NBReadPostDataFromInputStreamServlet",
                            "/ReadPostDataFromInputStreamFilter/NBReadPostDataFromInputStreamServlet",
                            "/Nofilter/NBReadPostDataFromInputStreamServlet" },
            asyncSupported = true)
public class NBReadPostDataFromInputStreamServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(NBReadPostDataFromInputStreamServlet.class.getName());
    private final String classname = "NBReadPostDataFromInputStreamServlet";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public NBReadPostDataFromInputStreamServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        //response.getWriter().append("Served at: ").append(request.getContextPath());
        this.doWork(request, response);

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doWork(request, response);
    }

    private void doWork(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String testTorun = request.getHeader("TestToCall");

        AsyncContext ac = this.setupAsyncListener(request, response);

        ServletInputStream input = request.getInputStream();
        LOG.info(classname + " input ->" + input);
        ReadListener readListener = null;
        if (request.getRequestURI().contains("ReadParameterFilter")) {
            readListener = new TestAsyncNBReadListener(input, request, response, ac, "ReadParameterFilter_ReadPostDataFromInputStreamServlet");
        } else if ("NBInputStream_Reader".equalsIgnoreCase(testTorun)) {
            readListener = new TestAsyncNBReadListener(input, request, response, ac, "NBInputStream_Reader");
        } else {
            readListener = new TestAsyncNBReadListener(input, request, response, ac, "NBReadPostDataFromInputStreamServlet");
        }
        //setListener
        input.setReadListener(readListener);
    }

    private AsyncContext setupAsyncListener(ServletRequest req, ServletResponse res) {
        // set up ReadListener to read data for processing

        // start async
        AsyncContext ac = req.startAsync();
        // set up async listener
        ac.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info(classname + " my asyncListener TestAsyncNBReadListener Complete");
                LOG.info("--------------------------------------------------------------");
            }

            @Override
            public void onError(AsyncEvent event) {
                LOG.info(classname + event.getThrowable().toString());
                LOG.info("--------------------------------------------------------------");
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                LOG.info(classname + " my asyncListener TestAsyncNBReadListener.onStartAsync");
                LOG.info("--------------------------------------------------------------");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                LOG.info(classname + " my asyncListener TestAsyncNBReadListener.onTimeout");
                LOG.info("--------------------------------------------------------------");
            }
        }, req, res);

        return ac;
    }

}
