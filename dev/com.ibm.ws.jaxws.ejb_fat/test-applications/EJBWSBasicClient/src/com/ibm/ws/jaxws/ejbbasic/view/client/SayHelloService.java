/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.jaxws.ejbbasic.view.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SayHelloService", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/", wsdlLocation = "file:/D:/temp/wsgen/SayHelloService.wsdl")
public class SayHelloService
                extends Service
{

    private final static URL SAYHELLOSERVICE_WSDL_LOCATION;
    private final static WebServiceException SAYHELLOSERVICE_EXCEPTION;
    private final static QName SAYHELLOSERVICE_QNAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "SayHelloService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/D:/temp/wsgen/SayHelloService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SAYHELLOSERVICE_WSDL_LOCATION = url;
        SAYHELLOSERVICE_EXCEPTION = e;
    }

    public SayHelloService() {
        super(__getWsdlLocation(), SAYHELLOSERVICE_QNAME);
    }

    public SayHelloService(URL wsdlLocation) {
        super(wsdlLocation, SAYHELLOSERVICE_QNAME);
    }

    public SayHelloService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *         returns SayHelloInterface
     */
    @WebEndpoint(name = "SayHelloPort")
    public SayHelloInterface getSayHelloPort() {
        return super.getPort(new QName("http://ejbbasic.jaxws.ws.ibm.com/", "SayHelloPort"), SayHelloInterface.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns SayHelloInterface
     */
    @WebEndpoint(name = "SayHelloPort")
    public SayHelloInterface getSayHelloPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbbasic.jaxws.ws.ibm.com/", "SayHelloPort"), SayHelloInterface.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SAYHELLOSERVICE_EXCEPTION != null) {
            throw SAYHELLOSERVICE_EXCEPTION;
        }
        return SAYHELLOSERVICE_WSDL_LOCATION;
    }

}
