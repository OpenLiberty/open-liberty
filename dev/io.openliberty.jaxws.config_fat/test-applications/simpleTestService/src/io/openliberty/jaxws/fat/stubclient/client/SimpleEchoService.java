/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat.stubclient.client;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SimpleEchoService", targetNamespace = "http://stubclient.fat.jaxws.openliberty.io/", wsdlLocation = "WEB-INF/wsdl/SimpleEcho.wsdl")
public class SimpleEchoService
    extends Service
{

    private static final URL SIMPLEECHOSERVICE_WSDL_LOCATION;
    private static final WebServiceException SIMPLEECHOSERVICE_EXCEPTION;
    private static final QName SIMPLEECHOSERVICE_QNAME = new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            URL baseUrl = SimpleEchoService.class.getClassLoader().getResource(".");
            url = new URL(baseUrl, "../wsdl/SimpleEcho.wsdl");
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
     *     returns SimpleEchoImpl
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort() {
        return super.getPort(new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoPort"), SimpleEcho.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns SimpleEchoImpl
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoPort"), SimpleEcho.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SIMPLEECHOSERVICE_EXCEPTION!= null) {
            throw SIMPLEECHOSERVICE_EXCEPTION;
        }
        return SIMPLEECHOSERVICE_WSDL_LOCATION;
    }

}
