/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.properties.test.servlet;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.annotation.WebServlet;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import org.junit.Test;

import com.ibm.ws.properties.test.client.stub.ImageService;
import com.ibm.ws.properties.test.client.stub.ImageServiceImplService;
import com.ibm.ws.properties.test.client.stub.ImageServiceImplServiceTwo;
import com.ibm.ws.properties.test.client.stub.ImageServiceTwo;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/LibertyCXFNegativePropertiesTestServlet")
public class LibertyCXFNegativePropertiesTestServlet extends FATServlet {

    @WebServiceRef(value = ImageServiceImplService.class, name = "services/ImageServicePropertyImplService")
    ImageServiceImplService imageService;

    @WebServiceRef(value = ImageServiceImplServiceTwo.class, name = "services/ImageServiceImplServiceTwo")
    ImageServiceImplServiceTwo imageServiceTwo;

    private static final String serverURL = "https://localhost" + ":" + Integer.getInteger("bvt.prop.HTTP_default.secure");

    private static final long serialVersionUID = 1L;

    private static DataHandler imageDataHandler = new DataHandler(new FileDataSource("resources/" + "a.jpg"));

    /*
     * Testing cxf.multipart.attachment property is not set or set to false
     * it sets up the attachment data out in SwAOutInterceptor
     *
     * Test is expected to pass, with a different output check in the trace at tear down.
     */
    @Test
    public void testCxfAttachmentOutputProperty() throws Exception {

        ImageService proxy = imageService.getImageServiceImplPort();

        BindingProvider provider = (BindingProvider) proxy;

        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                         serverURL + "/libertyCXFProperty/ImageServiceImplService");

        proxy.uploadImage("ServiceInjection", imageDataHandler);

    }

    /*
     * Testing cxf.ignore.unsupported.policy for used alternative policies
     * When this property is not set, it allows addition of used policies
     * into ws-policy.validated.alternatives in PolicyVerificationInInterceptor
     */
    @Test
    public void testCxfUsedAlternativePolicyProperty() throws Exception {
        ImageService proxy = imageService.getImageServiceImplPort();

        BindingProvider provider = (BindingProvider) proxy;

        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                         serverURL + "/libertyCXFProperty/ImageServiceImplService");

        proxy.uploadImage("ServiceInjection", imageDataHandler);

    }

    /*
     * Testing cxf.ignore.unsupported.policy for not supported alternative policies.
     * Not setting this property make alternative policies to not be supported in PolicyEngineImpl
     */
    @Test(expected = AssertionError.class)
    public void testCxfUnsupportedPolicyProperty() throws Exception {

        ImageServiceTwo proxy2 = imageServiceTwo.getImageServiceImplPortTwo();

        BindingProvider provider = (BindingProvider) proxy2;

        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                         serverURL + "/libertyCXFProperty/ImageServiceImplServiceTwo");

        proxy2.uploadImage("ServiceInjection", imageDataHandler);

    }

}
