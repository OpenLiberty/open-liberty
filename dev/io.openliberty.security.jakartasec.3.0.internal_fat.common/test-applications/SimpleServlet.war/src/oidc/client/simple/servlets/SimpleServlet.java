/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.simple.servlets;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oidc.client.base.utils.RequestLogger;
import oidc.client.base.utils.ServletLogger;
import oidc.client.base.utils.WSSubjectLogger;

@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public SimpleServlet() {
        super();
    }

    protected final AtomicInteger counter = new AtomicInteger();

    public int getCounter() {
        return counter.incrementAndGet();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ServletOutputStream outputStream = response.getOutputStream();

        recordAppInfo(request, outputStream);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        counter.set(0);

    }

    protected void recordAppInfo(HttpServletRequest request, ServletOutputStream outputStream) throws IOException {

        ServletLogger.printBlankLine(outputStream);

        ServletLogger.printLine(outputStream, ServletMessageConstants.APP_REQUEST_COUNT + counter.get());

        recordWhichApp(outputStream);

        RequestLogger requestLogger = new RequestLogger(request, ServletMessageConstants.SERVLET + ServletMessageConstants.REQUEST);
        requestLogger.printRequest(outputStream);

        WSSubjectLogger subjectLogger = new WSSubjectLogger(request, ServletMessageConstants.SERVLET + ServletMessageConstants.WSSUBJECT);
        subjectLogger.printProgrammaticApiValues(outputStream);

    }

    protected void recordWhichApp(ServletOutputStream outputStream) throws IOException {

        ServletLogger.printLine(outputStream, "got here servlet");
        ServletLogger.printLine(outputStream, ServletMessageConstants.HELLO_MSG + ServletLogger.getShortName(this.getClass().getSuperclass().getName()));
        ServletLogger.printLine(outputStream, ServletMessageConstants.HELLO_MSG + ServletLogger.getShortName(this.getClass().getName()));

    }
}
