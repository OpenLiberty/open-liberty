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
package com.ibm.ws.test.wsfeatures.client;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.RespectBinding;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.MTOM;

import com.ibm.ws.test.client.stub.ImageService;
import com.ibm.ws.test.client.stub.ImageServiceImplService;

public class WSRefFeaturesPortServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @MTOM
    @RespectBinding(enabled = true)
    @Addressing
    @WebServiceRef(value = ImageServiceImplService.class)
    private ImageService imagePort;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int port = request.getLocalPort();
        String host = request.getLocalAddr();

        BindingProvider provider = (BindingProvider) imagePort;
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                         "http://" + host + ":" + port + "/webServiceRefFeatures/ImageServiceImplService");
        imagePort.uploadImage("PortInjection", new DataHandler(new FileDataSource("resources/" + "a.jpg")));
    }

}
