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
package com.ibm.jaxws.properties.echo.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SimpleEchoService", targetNamespace = "http://echo.properties.jaxws.ibm.com/", wsdlLocation = "WEB-INF/wsdl/SimpleEchoService.wsdl")
public class SimpleEchoService extends Service {

    private final static URL SIMPLEECHOSERVICE_WSDL_LOCATION;
    private final static WebServiceException SIMPLEECHOSERVICE_EXCEPTION;
    private final static QName SIMPLEECHOSERVICE_QNAME = new QName("http://echo.properties.jaxws.ibm.com/", "SimpleEchoService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("WEB-INF/wsdl/SimpleEchoService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SIMPLEECHOSERVICE_WSDL_LOCATION = url;
        SIMPLEECHOSERVICE_EXCEPTION = e;
    }

    public SimpleEchoService() {
        super(__getWsdlLocation(), SIMPLEECHOSERVICE_QNAME);
    }

    public SimpleEchoService(WebServiceFeature... features) {
        super(__getWsdlLocation(), SIMPLEECHOSERVICE_QNAME, features);
    }

    public SimpleEchoService(URL wsdlLocation) {
        super(wsdlLocation, SIMPLEECHOSERVICE_QNAME);
    }

    public SimpleEchoService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SIMPLEECHOSERVICE_QNAME, features);
    }

    public SimpleEchoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SimpleEchoService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     *
     * @return
     *         returns SimpleEcho
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort() {
        return super.getPort(new QName("http://echo.properties.jaxws.ibm.com/", "SimpleEchoPort"), SimpleEcho.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns SimpleEcho
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://echo.properties.jaxws.ibm.com/", "SimpleEchoPort"), SimpleEcho.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SIMPLEECHOSERVICE_EXCEPTION != null) {
            throw SIMPLEECHOSERVICE_EXCEPTION;
        }
        return SIMPLEECHOSERVICE_WSDL_LOCATION;
    }

}
