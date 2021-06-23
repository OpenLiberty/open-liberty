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
package com.ibm.jaxws.properties.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceRef;

import com.ibm.jaxws.properties.hello.client.HelloService;

@WebServlet("/ReceiveTimeoutTestServlet")
public class ReceiveTimeoutTestServlet extends HttpServlet {
    @WebServiceRef(name = "service/HelloService")
    private HelloService helloService;

    public ReceiveTimeoutTestServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String errorMsg = null;
        try {
            helloService.getHelloPort().sayHello();
        } catch (Exception e) {
            Throwable t = e.getCause();
            if (null != t) {
                errorMsg = t.getMessage();
            }
        }

        PrintWriter writer = response.getWriter();
        if (null != errorMsg) {
            writer.println(errorMsg);
        }

        writer.close();
    }
}
