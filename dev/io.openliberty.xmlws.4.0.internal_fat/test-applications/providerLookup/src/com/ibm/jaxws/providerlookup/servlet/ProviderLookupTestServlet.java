/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jaxws.providerlookup.servlet;

import java.io.IOException;

import com.ibm.jaxws.providerlookup.echo.client.SimpleEchoService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.WebServiceRef;

/**
 * Servlet implementation class TestServlet
 * These tests requires only Web Service Ref to see if assigned spi provider works or not
 */
@WebServlet("/ProviderLookupTestServlet")
public class ProviderLookupTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @WebServiceRef(name = "service/SimpleEchoService")
    private SimpleEchoService echoService;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ProviderLookupTestServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }
}
