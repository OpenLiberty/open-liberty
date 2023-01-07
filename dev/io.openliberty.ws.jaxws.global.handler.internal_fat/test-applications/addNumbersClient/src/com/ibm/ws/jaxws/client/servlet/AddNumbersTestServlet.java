/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxws.client.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.client.AddNumbers;
import com.ibm.ws.jaxws.client.AddNumbers_Service;

/**
 *
 */
@WebServlet("/AddNumbersTestServlet")
public class AddNumbersTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @WebServiceRef
    private AddNumbers_Service serviceFromRef;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setCharacterEncoding("utf-8");

        AddNumbers addNumbers = serviceFromRef.getAddNumbersPort();

        reConfigPorts(req, (BindingProvider) addNumbers);

        PrintWriter out = null;
        int num1 = Integer.parseInt(req.getParameter("number1"));
        int num2 = Integer.parseInt(req.getParameter("number2"));

        try {
            out = resp.getWriter();
            out.println("Result = " + addNumbers.addNumbers(num1, num2));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void reConfigPorts(HttpServletRequest request, BindingProvider portFromRef) {
        String host = request.getLocalAddr();
        int port = request.getLocalPort();

        // Config portFromRef
        portFromRef.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://" + host + ":" + port + "/addNumbersProvider/AddNumbers");

    }

}
