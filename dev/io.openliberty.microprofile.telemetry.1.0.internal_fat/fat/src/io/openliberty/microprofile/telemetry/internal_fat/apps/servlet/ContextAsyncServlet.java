/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(urlPatterns = "/contextAsync", asyncSupported = true)
public class ContextAsyncServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Tracer tracer;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final AsyncContext asyncContext = request.startAsync(request, response);
        Context currentContext = Context.current();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Span span = tracer.spanBuilder("contextAsync-task").startSpan();
                try (Scope scope = span.makeCurrent()) {
                    ServletResponse response = asyncContext.getResponse();
                    response.setContentType("text/plain");
                    PrintWriter out = response.getWriter();
                    Thread.sleep(2000); // 2 second sleep
                    out.println("ContextAsyncServlet");
                    out.flush();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    span.end();
                }
                asyncContext.complete();
            }
        };

        asyncContext.start(currentContext.wrap(runnable));

    }
}
