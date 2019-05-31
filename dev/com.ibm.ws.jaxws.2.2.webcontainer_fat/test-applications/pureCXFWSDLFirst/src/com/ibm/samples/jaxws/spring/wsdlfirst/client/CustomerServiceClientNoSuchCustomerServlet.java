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
package com.ibm.samples.jaxws.spring.wsdlfirst.client;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.samples.jaxws.spring.wsdlfirst.service.CustomerServiceImpl;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.CustomerService;
import com.ibm.samples.jaxws.spring.wsdlfirst.stub.NoSuchCustomerException;

@WebServlet("/CustomerServiceClientNoSuchCustomerServlet")
public class CustomerServiceClientNoSuchCustomerServlet extends HttpServlet {
    private static final long serialVersionUID = -1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        CustomerService customer = new CustomerServiceImpl();

        try {
            customer.getCustomersByName("None");
        } catch (NoSuchCustomerException e) {
            out.println(e.getMessage());
        }

    }
}
