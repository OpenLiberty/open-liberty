/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.test.wsr.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/MockSoapEndPoint")
public class MockSOAPEndpoint extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String REQUEST_MESSAGE = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                 + "  <soap:Body>"
                                 + "    <ns2:hello xmlns:ns2=\"http://server.wsr.test.jaxws.ws.ibm.com\">"
                                 + "      <arg0>World \"\\u6771\\u42ac\\u55b6\\u696d\\u90e8\"</arg0>"
                                 + "    </ns2:hello>"
                                 + "  </soap:Body>"
                                 + "</soap:Envelope>";
        resp.setStatus(200);
        resp.setContentType("text/xml");
//      resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(REQUEST_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
