/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.logging.fat.tbasic.servlet;

import java.io.IOException;
import java.util.logging.Logger;
import com.ibm.websphere.logging.WsLevel;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/TBasicServlet")
@SuppressWarnings("serial")
public class TBasicServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(TBasicServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.logp(WsLevel.AUDIT, TBasicServlet.class.getName(),"helloMethod", "HELLO0001W: Hello World!");
        response.getWriter().println("Hello world!");
    }
}

