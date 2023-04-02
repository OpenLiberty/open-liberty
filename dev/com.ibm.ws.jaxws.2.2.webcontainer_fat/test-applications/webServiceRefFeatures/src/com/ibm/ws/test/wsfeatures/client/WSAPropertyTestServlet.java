/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.test.wsfeatures.client;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.test.client.stub.ImageService;
import com.ibm.ws.test.client.stub.ImageServiceImplServiceTwo;
import com.ibm.ws.test.client.stub.ImageServicePropertyImplService;
import com.ibm.ws.test.client.stub.ImageServiceTwo;

public class WSAPropertyTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @WebServiceRef(value = ImageServiceImplServiceTwo.class, name = "services/ImageServiceImplServiceTwo")
    ImageServiceImplServiceTwo imageService;

    @WebServiceRef(value = ImageServicePropertyImplService.class, name = "services/ImageServicePropertyImplService")
    ImageServicePropertyImplService imageServiceNegative;

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

        String methodName = request.getParameter("impl");
        // Decided to pass as parameter instead of getting from request since getting from request is not always reliable
        String serverURL = request.getParameter("serverurl");

        if (methodName != null) {
            if (methodName.equals("ImageServiceImplService")) {
                ImageService proxy = imageServiceNegative.getImageServiceImplPort();
                BindingProvider provider = (BindingProvider) proxy;
                provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                 serverURL + "/webServiceRefFeatures/ImageServiceImplService");

                proxy.uploadImage("ServiceInjection", new DataHandler(new FileDataSource("resources/" + "a.jpg")));
            } else if (methodName.equals("ImageServiceImplServiceTwo")) {// method=testCxfPropertyUnsupportedPolicy or null
                ImageServiceTwo proxy = imageService.getImageServiceImplPortTwo();
                BindingProvider provider = (BindingProvider) proxy;
                provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                 serverURL + "/webServiceRefFeatures/ImageServiceImplServiceTwo");

                proxy.uploadImage("ServiceInjection", new DataHandler(new FileDataSource("resources/" + "a.jpg")));
            }
        }
    }
}
