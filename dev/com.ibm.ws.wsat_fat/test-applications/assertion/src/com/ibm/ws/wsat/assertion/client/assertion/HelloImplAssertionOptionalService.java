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
package com.ibm.ws.wsat.assertion.client.assertion;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloImplAssertionOptionalService", targetNamespace = "http://server.assertion.wsat.ws.ibm.com/", wsdlLocation = "http://localhost:8010/assertion/HelloImplAssertionOptionalService?wsdl")
public class HelloImplAssertionOptionalService
    extends Service
{

    private final static URL HELLOIMPLASSERTIONOPTIONALSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION;
    private final static QName HELLOIMPLASSERTIONOPTIONALSERVICE_QNAME = new QName("http://server.assertion.wsat.ws.ibm.com/", "HelloImplAssertionOptionalService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/assertion/HelloImplAssertionOptionalService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOIMPLASSERTIONOPTIONALSERVICE_WSDL_LOCATION = url;
        HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION = e;
    }

    public HelloImplAssertionOptionalService() {
        super(__getWsdlLocation(), HELLOIMPLASSERTIONOPTIONALSERVICE_QNAME);
    }

    public HelloImplAssertionOptionalService(URL wsdlLocation) {
        super(wsdlLocation, HELLOIMPLASSERTIONOPTIONALSERVICE_QNAME);
    }

    public HelloImplAssertionOptionalService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplAssertionOptionalPort")
    public Hello getHelloImplAssertionOptionalPort() {
        return super.getPort(new QName("http://server.assertion.wsat.ws.ibm.com/", "HelloImplAssertionOptionalPort"), Hello.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplAssertionOptionalPort")
    public Hello getHelloImplAssertionOptionalPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.assertion.wsat.ws.ibm.com/", "HelloImplAssertionOptionalPort"), Hello.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION!= null) {
            throw HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION;
        }
        return HELLOIMPLASSERTIONOPTIONALSERVICE_WSDL_LOCATION;
    }

}
