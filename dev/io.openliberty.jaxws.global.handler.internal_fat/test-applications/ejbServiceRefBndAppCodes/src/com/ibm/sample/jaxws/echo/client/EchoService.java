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
package com.ibm.sample.jaxws.echo.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "EchoService", targetNamespace = "http://echo.jaxws.sample.ibm.com/", wsdlLocation = "META-INF/wsdl/EchoService.wsdl")
public class EchoService
                extends Service
{

    private final static URL ECHOSERVICE_WSDL_LOCATION;
    private final static WebServiceException ECHOSERVICE_EXCEPTION;
    private final static QName ECHOSERVICE_QNAME = new QName("http://echo.jaxws.sample.ibm.com/", "EchoService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("META-INF/wsdl/EchoService.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ECHOSERVICE_WSDL_LOCATION = url;
        ECHOSERVICE_EXCEPTION = e;
    }

    public EchoService() {
        super(__getWsdlLocation(), ECHOSERVICE_QNAME);
    }

    public EchoService(WebServiceFeature... features) {
        super(__getWsdlLocation(), ECHOSERVICE_QNAME, features);
    }

    public EchoService(URL wsdlLocation) {
        super(wsdlLocation, ECHOSERVICE_QNAME);
    }

    public EchoService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, ECHOSERVICE_QNAME, features);
    }

    public EchoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EchoService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns Echo
     */
    @WebEndpoint(name = "EchoPort")
    public Echo getEchoPort() {
        return super.getPort(new QName("http://echo.jaxws.sample.ibm.com/", "EchoPort"), Echo.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns Echo
     */
    @WebEndpoint(name = "EchoPort")
    public Echo getEchoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://echo.jaxws.sample.ibm.com/", "EchoPort"), Echo.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ECHOSERVICE_EXCEPTION != null) {
            throw ECHOSERVICE_EXCEPTION;
        }
        return ECHOSERVICE_WSDL_LOCATION;
    }

}
