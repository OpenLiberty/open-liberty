/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.samples.jaxws.catalog.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import com.ibm.samples.jaxws.catalog.stubs.AddResponse;
import com.ibm.samples.jaxws.catalog.stubs.CalculatorPortType;
import com.ibm.samples.jaxws.catalog.stubs.Calculator_Service_Client;

/**
 * We create multiple asynchronous requests to check the limits
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AsyncClientConnectionServlet")
public class AsyncClientConnectionServlet extends HttpServlet {

    private final Logger log = Logger.getLogger(AsyncClientConnectionServlet.class.getName());

    public AsyncClientConnectionServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        URL wsdlURL = new URL(new StringBuilder().append("http://").append(request.getServerName()).append(":").append(request.getServerPort()).append("/calculatorAsync/calculatorAsync?wsdl").toString());
        log.info("wsdlURL: " + wsdlURL);
        String isAsync = request.getParameter("isAsync");
        log.info("AsyncClientConnectionServlet isAsync: " + isAsync);

        try (PrintWriter writer = response.getWriter()) {
            Calculator_Service_Client service = new Calculator_Service_Client(wsdlURL);
            CalculatorPortType port = service.getCalculatorPort();

            if ("true".equalsIgnoreCase(isAsync)) {
                // Asynchronous calls
                // Creating a thread safe list to add all of results collected from other threads
                final List<String> results = Collections.synchronizedList(new ArrayList<>());
                final CountDownLatch latch = new CountDownLatch(4);

                //Create 4 async client requests
                for (int i = 1; i < 5; i++) {
                    port.addAsync(i, i, new AsyncHandler<AddResponse>() {
                        @Override
                        public void handleResponse(Response<AddResponse> response) {
                            try {
                                String ret = "result = " + response.get().getReturn();
                                results.add(ret + "\n");
                                log.info(ret);
                            } catch (Exception e) {
                                results.add("ERROR: " + e.getMessage());
                                log.info("ERROR: " + e.getMessage());
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
                }
                // Wait for all callbacks
                boolean completed = latch.await(30, TimeUnit.SECONDS);
                log.info("Async processes are completed: " + completed);
                for (String result : results) {
                    writer.write(result);
                }
            } else {
                //Synchronous call
                for (int i = 1; i < 5; i++) {
                    int result = port.add(i, i);
                    writer.write("result = " + result);
                }
            }
            writer.flush();
            writer.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
