/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.asyncshutdown;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(value = "/async-task-wait", asyncSupported = true)
public class AsyncRequestWaitingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AsyncShutdownBean bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AsyncContext ctx = req.startAsync();
        PrintWriter writer = ctx.getResponse().getWriter();
        // Start async task which will complete the request when it finishes
        bean.runFiniteAsyncTask(5)
                        .exceptionally(Throwable::toString)
                        .thenAccept(s -> {
                            System.out.println("Got result: " + s);
                            writer.println("Got result: " + s);
                            ctx.complete();
                        });
    }

}
