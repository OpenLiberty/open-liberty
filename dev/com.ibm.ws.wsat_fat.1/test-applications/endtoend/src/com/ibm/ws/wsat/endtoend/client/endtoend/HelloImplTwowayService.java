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
package com.ibm.ws.wsat.endtoend.client.endtoend;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloImplTwowayService", targetNamespace = "http://server.endtoend.wsat.ws.ibm.com/", wsdlLocation = "http://localhost:9888/endtoend/HelloImplTwowayService?wsdl")
public class HelloImplTwowayService
    extends Service
{

    private final static URL HELLOIMPLTWOWAYSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOIMPLTWOWAYSERVICE_EXCEPTION;
    private final static QName HELLOIMPLTWOWAYSERVICE_QNAME = new QName("http://server.endtoend.wsat.ws.ibm.com/", "HelloImplTwowayService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:9888/endtoend/HelloImplTwowayService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOIMPLTWOWAYSERVICE_WSDL_LOCATION = url;
        HELLOIMPLTWOWAYSERVICE_EXCEPTION = e;
    }

    public HelloImplTwowayService() {
        super(__getWsdlLocation(), HELLOIMPLTWOWAYSERVICE_QNAME);
    }

    public HelloImplTwowayService(URL wsdlLocation) {
        super(wsdlLocation, HELLOIMPLTWOWAYSERVICE_QNAME);
    }

    public HelloImplTwowayService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns HelloImplTwoway
     */
    @WebEndpoint(name = "HelloImplTwowayPort")
    public HelloImplTwoway getHelloImplTwowayPort() {
        return super.getPort(new QName("http://server.endtoend.wsat.ws.ibm.com/", "HelloImplTwowayPort"), HelloImplTwoway.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns HelloImplTwoway
     */
    @WebEndpoint(name = "HelloImplTwowayPort")
    public HelloImplTwoway getHelloImplTwowayPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.endtoend.wsat.ws.ibm.com/", "HelloImplTwowayPort"), HelloImplTwoway.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOIMPLTWOWAYSERVICE_EXCEPTION!= null) {
            throw HELLOIMPLTWOWAYSERVICE_EXCEPTION;
        }
        return HELLOIMPLTWOWAYSERVICE_WSDL_LOCATION;
    }

}
