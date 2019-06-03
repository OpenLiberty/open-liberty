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
package com.ibm.jaxws.MTOM.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import mtomservice.MTOMInter;

@WebServlet("/MTOMClientServlet")
public class MTOMClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public MTOMClientServlet() {
        super();
    }

    Service service = null;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        String host = request.getLocalAddr();
        String port = request.getParameter("port");
        String serviceName = request.getParameter("service");
        if (serviceName.equals("MTOMService")) {

        } else {
            writer.println("Not supported service: " + serviceName);
            writer.flush();
            writer.close();
            return;
        }

        QName qname = new QName("http://MTOMService/", "MTOMService");
        QName portName = new QName("http://MTOMService/", "MTOMServicePort");
        service = Service.create(qname);
        MTOMInter proxy = service.getPort(portName, MTOMInter.class);

        String newTarget = "http://" + host + ":" + port + "/testMTOM/MTOMService";
        BindingProvider bp = (BindingProvider) proxy;
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newTarget);
        byte[] bytes = proxy.getAttachment();
        writer.println("getAttachment() returned " + bytes);

        writer.flush();
        writer.close();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }
}
