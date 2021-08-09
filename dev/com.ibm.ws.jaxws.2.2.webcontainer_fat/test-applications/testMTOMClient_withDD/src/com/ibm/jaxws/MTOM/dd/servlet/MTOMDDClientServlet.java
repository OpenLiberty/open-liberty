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
package com.ibm.jaxws.MTOM.dd.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.SOAPBinding;

import mtomservice.dd.MTOMInter;
import mtomservice.dd.MTOMService;

public class MTOMDDClientServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @WebServiceRef(name = "service/mtomservice", type = MTOMService.class)
    MTOMService service;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        String host = request.getLocalAddr();
        int port = request.getLocalPort();
        String newTarget = "http://" + host + ":" + port + "/testMTOM/MTOMService";

        MTOMInter proxy = service.getMTOMServicePort();
        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newTarget);
        SOAPBinding sb = (SOAPBinding) bp.getBinding();
        boolean mtomEnabled = sb.isMTOMEnabled();

        byte[] barr = new byte[10000];
        Random r = new Random();
        r.nextBytes(barr);
        String respString = proxy.sendAttachment(barr);

        writer.println(respString);
        writer.println("MTOM enabled? " + mtomEnabled);
        writer.flush();
        writer.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }
}
