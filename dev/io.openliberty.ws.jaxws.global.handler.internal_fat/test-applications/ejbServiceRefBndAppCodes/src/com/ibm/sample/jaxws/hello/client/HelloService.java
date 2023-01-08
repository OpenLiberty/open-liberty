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
package com.ibm.sample.jaxws.hello.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloService", targetNamespace = "http://hello.jaxws.sample.ibm.com/", wsdlLocation = "META-INF/wsdl/HelloService.wsdl")
public class HelloService
                extends Service
{

    private final static URL HELLOSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOSERVICE_EXCEPTION;
    private final static QName HELLOSERVICE_QNAME = new QName("http://hello.jaxws.sample.ibm.com/", "HelloService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("META-INF/wsdl/HelloService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOSERVICE_WSDL_LOCATION = url;
        HELLOSERVICE_EXCEPTION = e;
    }

    public HelloService() {
        super(__getWsdlLocation(), HELLOSERVICE_QNAME);
    }

    public HelloService(WebServiceFeature... features) {
        super(__getWsdlLocation(), HELLOSERVICE_QNAME, features);
    }

    public HelloService(URL wsdlLocation) {
        super(wsdlLocation, HELLOSERVICE_QNAME);
    }

    public HelloService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, HELLOSERVICE_QNAME, features);
    }

    public HelloService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public HelloService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns Hello
     */
    @WebEndpoint(name = "HelloPort")
    public Hello getHelloPort() {
        return super.getPort(new QName("http://hello.jaxws.sample.ibm.com/", "HelloPort"), Hello.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns Hello
     */
    @WebEndpoint(name = "HelloPort")
    public Hello getHelloPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://hello.jaxws.sample.ibm.com/", "HelloPort"), Hello.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOSERVICE_EXCEPTION != null) {
            throw HELLOSERVICE_EXCEPTION;
        }
        return HELLOSERVICE_WSDL_LOCATION;
    }

}
