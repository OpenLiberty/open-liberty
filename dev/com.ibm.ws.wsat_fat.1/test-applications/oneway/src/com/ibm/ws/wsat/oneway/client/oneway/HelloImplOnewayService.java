/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.oneway.client.oneway;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloImplOnewayService", targetNamespace = "http://server.oneway.wsat.ws.ibm.com/", wsdlLocation = "http://localhost:8010/oneway/HelloImplOnewayService?wsdl")
public class HelloImplOnewayService
    extends Service
{

    private final static URL HELLOIMPLONEWAYSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOIMPLONEWAYSERVICE_EXCEPTION;
    private final static QName HELLOIMPLONEWAYSERVICE_QNAME = new QName("http://server.oneway.wsat.ws.ibm.com/", "HelloImplOnewayService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/oneway/HelloImplOnewayService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOIMPLONEWAYSERVICE_WSDL_LOCATION = url;
        HELLOIMPLONEWAYSERVICE_EXCEPTION = e;
    }

    public HelloImplOnewayService() {
        super(__getWsdlLocation(), HELLOIMPLONEWAYSERVICE_QNAME);
    }

    public HelloImplOnewayService(URL wsdlLocation) {
        super(wsdlLocation, HELLOIMPLONEWAYSERVICE_QNAME);
    }

    public HelloImplOnewayService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns HelloImplOneway
     */
    @WebEndpoint(name = "HelloImplOnewayPort")
    public HelloImplOneway getHelloImplOnewayPort() {
        return super.getPort(new QName("http://server.oneway.wsat.ws.ibm.com/", "HelloImplOnewayPort"), HelloImplOneway.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns HelloImplOneway
     */
    @WebEndpoint(name = "HelloImplOnewayPort")
    public HelloImplOneway getHelloImplOnewayPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.oneway.wsat.ws.ibm.com/", "HelloImplOnewayPort"), HelloImplOneway.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOIMPLONEWAYSERVICE_EXCEPTION!= null) {
            throw HELLOIMPLONEWAYSERVICE_EXCEPTION;
        }
        return HELLOIMPLONEWAYSERVICE_WSDL_LOCATION;
    }

}
