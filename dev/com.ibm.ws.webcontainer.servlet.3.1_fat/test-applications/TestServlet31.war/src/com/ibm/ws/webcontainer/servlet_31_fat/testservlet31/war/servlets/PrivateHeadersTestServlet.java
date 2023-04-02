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
package com.ibm.ws.webcontainer.servlet_31_fat.testservlet31.war.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is a simple app to check the results of private headers in requests.
 */
@WebServlet("/PrivateHeadersTestServlet")
public class PrivateHeadersTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(PrivateHeadersTestServlet.class.getName());

    public PrivateHeadersTestServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();

        String TestToCall = request.getHeader("TestToCall");
        LOG.info("PrivateHeadersTestServlet test to call is " + TestToCall);

        if (TestToCall.equalsIgnoreCase("test_SendValid$WSSCPrivateHeader") || TestToCall.equalsIgnoreCase("test_SendInvalid$WSSCPrivateHeader")) {
            String scheme = request.getScheme();
            if (TestToCall.equalsIgnoreCase("test_SendValid$WSSCPrivateHeader")) {
                LOG.info("test_SendValid$WSSCPrivateHeader scheme is " + scheme);
            } else {
                LOG.info("test_SendInvalid$WSSCPrivateHeader scheme is " + scheme);
            }
            sos.println("scheme=" + scheme);
        } else if (TestToCall.equalsIgnoreCase("test_SendValid$WSPRPrivateHeader") || TestToCall.equalsIgnoreCase("test_SendInvalid$WSPRPrivateHeader")) {
            String protocol = request.getProtocol();
            if (TestToCall.equalsIgnoreCase("test_SendValid$WSPRPrivateHeader")) {
                LOG.info("test_SendValid$WSSCPrivateHeader protocol is " + protocol);
            } else {
                LOG.info("test_SendInvalid$WSSCPrivateHeader protocol is " + protocol);
            }
            sos.println("version=" + protocol);
        } else if (TestToCall.equalsIgnoreCase("test_SendValid$WSRAPrivateHeader") || TestToCall.equalsIgnoreCase("test_SendInvalid$WSRAPrivateHeader")) {
            String address = request.getRemoteAddr();
            if (TestToCall.equalsIgnoreCase("test_SendValid$WSRAPrivateHeader")) {
                LOG.info("test_SendValid$WSSCPrivateHeader address is " + address);
            } else {
                LOG.info("test_SendInvalid$WSRAPrivateHeader address is " + address);
            }
            sos.println("address=" + address);
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}