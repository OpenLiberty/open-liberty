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
package com.ibm.ws.wsat.simpleclient.client.simple;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "WSATSimpleService", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", wsdlLocation = "http://localhost:9992/simpleServer/WSATSimpleService?wsdl")
public class WSATSimpleService
    extends Service
{

    private final static URL WSATSIMPLESERVICE_WSDL_LOCATION;
    private final static WebServiceException WSATSIMPLESERVICE_EXCEPTION;
    private final static QName WSATSIMPLESERVICE_QNAME = new QName("http://server.simpleserver.wsat.ws.ibm.com/", "WSATSimpleService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:9992/simpleServer/WSATSimpleService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        WSATSIMPLESERVICE_WSDL_LOCATION = url;
        WSATSIMPLESERVICE_EXCEPTION = e;
    }

    public WSATSimpleService() {
        super(__getWsdlLocation(), WSATSIMPLESERVICE_QNAME);
    }

    public WSATSimpleService(URL wsdlLocation) {
        super(wsdlLocation, WSATSIMPLESERVICE_QNAME);
    }

    public WSATSimpleService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns WSATSimple
     */
    @WebEndpoint(name = "WSATSimplePort")
    public WSATSimple getWSATSimplePort() {
        return super.getPort(new QName("http://server.simpleserver.wsat.ws.ibm.com/", "WSATSimplePort"), WSATSimple.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WSATSimple
     */
    @WebEndpoint(name = "WSATSimplePort")
    public WSATSimple getWSATSimplePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.simpleserver.wsat.ws.ibm.com/", "WSATSimplePort"), WSATSimple.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WSATSIMPLESERVICE_EXCEPTION!= null) {
            throw WSATSIMPLESERVICE_EXCEPTION;
        }
        return WSATSIMPLESERVICE_WSDL_LOCATION;
    }

}
