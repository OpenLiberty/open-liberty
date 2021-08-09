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
package com.ibm.ws.wsatAppServiceOptional.client;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloImplService", targetNamespace = "http://server.wsatAppServiceOptional.ws.ibm.com/", wsdlLocation = "file:/C:/Jordan/workspace/WDT/cxfApp2/WebContent/WEB-INF/wsdl/hello.wsdl")
public class HelloImplService
    extends Service
{

    private final static URL HELLOIMPLSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOIMPLSERVICE_EXCEPTION;
    private final static QName HELLOIMPLSERVICE_QNAME = new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "HelloImplService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/C:/Jordan/workspace/WDT/cxfApp2/WebContent/WEB-INF/wsdl/hello.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOIMPLSERVICE_WSDL_LOCATION = url;
        HELLOIMPLSERVICE_EXCEPTION = e;
    }

    public HelloImplService() {
        super(__getWsdlLocation(), HELLOIMPLSERVICE_QNAME);
    }

    public HelloImplService(URL wsdlLocation) {
        super(wsdlLocation, HELLOIMPLSERVICE_QNAME);
    }

    public HelloImplService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplPort")
    public Hello getHelloImplPort() {
        return super.getPort(new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "HelloImplPort"), Hello.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplPort")
    public Hello getHelloImplPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.wsatAppServiceOptional.ws.ibm.com/", "HelloImplPort"), Hello.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOIMPLSERVICE_EXCEPTION!= null) {
            throw HELLOIMPLSERVICE_EXCEPTION;
        }
        return HELLOIMPLSERVICE_WSDL_LOCATION;
    }

}
