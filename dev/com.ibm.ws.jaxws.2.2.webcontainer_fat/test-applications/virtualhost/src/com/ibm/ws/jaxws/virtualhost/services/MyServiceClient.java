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
package com.ibm.ws.jaxws.virtualhost.services;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "MyService", targetNamespace = "http://services.virtualhost.jaxws.ws.ibm.com/")
public class MyServiceClient extends Service {
    private static final URL HELLOWEB_WSDL_LOCATION;
    private static final WebServiceException HELLOWEB_EXCEPTION;
    private static final QName HELLOWEB_QNAME = new QName("http://services.virtualhost.jaxws.ws.ibm.com/", "MyService");

    public MyServiceClient() {
        super(__getWsdlLocation(), HELLOWEB_QNAME);
    }

    public MyServiceClient(WebServiceFeature[] paramArrayOfWebServiceFeature) {
        super(__getWsdlLocation(), HELLOWEB_QNAME, paramArrayOfWebServiceFeature);
    }

    public MyServiceClient(URL paramURL) {
        super(paramURL, HELLOWEB_QNAME);
    }

    public MyServiceClient(URL paramURL, WebServiceFeature[] paramArrayOfWebServiceFeature) {
        super(paramURL, HELLOWEB_QNAME, paramArrayOfWebServiceFeature);
    }

    public MyServiceClient(URL paramURL, QName paramQName) {
        super(paramURL, paramQName);
    }

    public MyServiceClient(URL paramURL, QName paramQName, WebServiceFeature[] paramArrayOfWebServiceFeature) {
        super(paramURL, paramQName, paramArrayOfWebServiceFeature);
    }

    @WebEndpoint(name = "MyServiceEndpointPort")
    public MyService getMyServiceEndpointPort() {
        return (super.getPort(new QName("http://services.virtualhost.jaxws.ws.ibm.com/", "MyServiceEndpointPort"), MyService.class));
    }

    @WebEndpoint(name = "MyServiceEndpointPort")
    public MyService getMyServiceEndpointPort(WebServiceFeature[] paramArrayOfWebServiceFeature) {
        return (super.getPort(new QName("http://services.virtualhost.jaxws.ws.ibm.com/", "MyServiceEndpointPort"), MyService.class, paramArrayOfWebServiceFeature));
    }

    private static URL __getWsdlLocation() {
        if (HELLOWEB_EXCEPTION != null)
            throw HELLOWEB_EXCEPTION;

        return HELLOWEB_WSDL_LOCATION;
    }

    static {
        URL localURL = null;
        WebServiceException localWebServiceException = null;
        try {
            localURL = new URL("http://localhost:8010/cutDownTest/HelloWeb?wsdl");
        } catch (MalformedURLException localMalformedURLException) {
            localWebServiceException = new WebServiceException(localMalformedURLException);
        }
        HELLOWEB_WSDL_LOCATION = localURL;
        HELLOWEB_EXCEPTION = localWebServiceException;
    }
}