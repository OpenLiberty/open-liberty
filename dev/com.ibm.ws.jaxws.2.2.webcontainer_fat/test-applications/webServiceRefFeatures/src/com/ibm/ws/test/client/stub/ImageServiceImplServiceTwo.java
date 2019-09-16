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
package com.ibm.ws.test.client.stub;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "ImageServiceImplServiceTwo",
                  targetNamespace = "http://jaxws.service/",
                  wsdlLocation = "image.wsdl")
public class ImageServiceImplServiceTwo extends Service {

    private final static URL IMAGESERVICEIMPLSERVICETWO_WSDL_LOCATION;
    private final static WebServiceException IMAGESERVICEIMPLSERVICETWO_EXCEPTION;
    private final static QName IMAGESERVICEIMPLSERVICETWO_QNAME = new QName("http://jaxws.service/", "ImageServiceImplServiceTwo");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            URL baseUrl;
            baseUrl = ImageServiceImplService.class.getResource(".");
            url = new URL(baseUrl, "image.wsdl");
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(ImageServiceImplService.class.getName()).log(java.util.logging.Level.INFO,
                                                                                            "Can not initialize the default wsdl from {0}", "wsdl.xml");
        }
        IMAGESERVICEIMPLSERVICETWO_WSDL_LOCATION = url;
        IMAGESERVICEIMPLSERVICETWO_EXCEPTION = e;
    }

    public ImageServiceImplServiceTwo() {
        super(__getWsdlLocation(), IMAGESERVICEIMPLSERVICETWO_QNAME);
    }

    public ImageServiceImplServiceTwo(WebServiceFeature... features) {
        super(__getWsdlLocation(), IMAGESERVICEIMPLSERVICETWO_QNAME, features);
    }

    public ImageServiceImplServiceTwo(URL wsdlLocation) {
        super(wsdlLocation, IMAGESERVICEIMPLSERVICETWO_QNAME);
    }

    public ImageServiceImplServiceTwo(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, IMAGESERVICEIMPLSERVICETWO_QNAME, features);
    }

    public ImageServiceImplServiceTwo(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ImageServiceImplServiceTwo(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     *
     * @return
     *         returns ImageServiceTwo
     */
    @WebEndpoint(name = "ImageServiceImplPortTwo")
    public ImageServiceTwo getImageServiceImplPortTwo() {
        return super.getPort(new QName("http://jaxws.service/", "ImageServiceImplPortTwo"), ImageServiceTwo.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns ImageServiceTwo
     */
    @WebEndpoint(name = "ImageServiceImplPortTwo")
    public ImageServiceTwo getImageServiceImplPortTwo(WebServiceFeature... features) {
        return super.getPort(new QName("http://jaxws.service/", "ImageServiceImplPortTwo"), ImageServiceTwo.class, features);
    }

    private static URL __getWsdlLocation() {
        if (IMAGESERVICEIMPLSERVICETWO_EXCEPTION != null) {
            throw IMAGESERVICEIMPLSERVICETWO_EXCEPTION;
        }
        return IMAGESERVICEIMPLSERVICETWO_WSDL_LOCATION;
    }

}
