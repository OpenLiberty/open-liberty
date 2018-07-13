/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.simultaneousRequests.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class SimultaneousRequestsTestServlet extends HttpServlet {

    @Inject 
    Config config;

    private static AtomicBoolean firstRequest = new AtomicBoolean(true);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue"); //Ensure that a Config object is actually associated with both requests before either destroys it's Config. 
    
        Exception e = null;
        if (firstRequest.compareAndSet(true, false)) {
            try {
                delayFirstThread();
                TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue");
            } catch (Exception ex) {
                e = ex;
            }
        } else {
            TestUtils.assertContains(config, "onlyInFile", "onlyInFile.fileValue");
            try { 
                synchronized (firstRequest) {
                    firstRequest.notifyAll();
                }
            } catch (Exception ex) {
                e = ex;
            }
        }

        PrintWriter out = response.getWriter();

        if (e == null) {
            out.println("\nNo exceptions were thrown. Config object: " + config.toString());
        } else {
            out.println("\nAn exception was thrown: " + e.toString() + " Config object: " + config.toString());
        }
        
    }

    private void delayFirstThread() throws InterruptedException {
        synchronized (firstRequest) {
            firstRequest.wait();
            Thread.sleep(5000); //We wait for the second thread to finish, plus extra time for Liberty's internals to ensure that if Config will get deleted we will see it. 
        }
    }

}
