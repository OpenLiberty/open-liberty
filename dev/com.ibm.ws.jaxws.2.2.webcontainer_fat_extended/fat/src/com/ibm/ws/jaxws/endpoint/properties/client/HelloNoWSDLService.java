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
package com.ibm.ws.jaxws.endpoint.properties.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloNoWSDLService", targetNamespace = "http://server.properties.endpoint.test.jaxws.ws.ibm.com/",
                  wsdlLocation = "http://localhost:9080/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl")
public class HelloNoWSDLService extends Service {

    private final static URL HELLONOWSDLSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLONOWSDLSERVICE_EXCEPTION;
    private final static QName HELLONOWSDLSERVICE_QNAME = new QName("http://server.properties.endpoint.test.jaxws.ws.ibm.com/", "HelloNoWSDLService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:9080/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLONOWSDLSERVICE_WSDL_LOCATION = url;
        HELLONOWSDLSERVICE_EXCEPTION = e;
    }

    public HelloNoWSDLService() {
        super(__getWsdlLocation(), HELLONOWSDLSERVICE_QNAME);
    }

    public HelloNoWSDLService(URL wsdlLocation) {
        super(wsdlLocation, HELLONOWSDLSERVICE_QNAME);
    }

    public HelloNoWSDLService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     *
     * @return
     *         returns HelloNoWSDLInterface
     */
    @WebEndpoint(name = "HelloNoWSDLPort")
    public HelloNoWSDLInterface getHelloNoWSDLPort() {
        return super.getPort(new QName("http://server.properties.endpoint.test.jaxws.ws.ibm.com/", "HelloNoWSDLPort"), HelloNoWSDLInterface.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns HelloNoWSDLInterface
     */
    @WebEndpoint(name = "HelloNoWSDLPort")
    public HelloNoWSDLInterface getHelloNoWSDLPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.properties.endpoint.test.jaxws.ws.ibm.com/", "HelloNoWSDLPort"), HelloNoWSDLInterface.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLONOWSDLSERVICE_EXCEPTION != null) {
            throw HELLONOWSDLSERVICE_EXCEPTION;
        }
        return HELLONOWSDLSERVICE_WSDL_LOCATION;
    }

}
