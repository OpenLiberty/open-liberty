/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.jaxws.ejbwscontext;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "EchoInfoService", targetNamespace = "http://ejbwscontext.jaxws.ws.ibm.com", wsdlLocation = "http://localhost:8010/EJBWSContext/EchoInfoService?wsdl")
public class EchoInfoService
                extends Service {

    private final static URL ECHOINFOSERVICE_WSDL_LOCATION;
    private final static WebServiceException ECHOINFOSERVICE_EXCEPTION;
    private final static QName ECHOINFOSERVICE_QNAME = new QName("http://ejbwscontext.jaxws.ws.ibm.com", "EchoInfoService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/EJBWSContext/EchoInfoService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ECHOINFOSERVICE_WSDL_LOCATION = url;
        ECHOINFOSERVICE_EXCEPTION = e;
    }

    public EchoInfoService() {
        super(__getWsdlLocation(), ECHOINFOSERVICE_QNAME);
    }

    public EchoInfoService(URL wsdlLocation) {
        super(wsdlLocation, ECHOINFOSERVICE_QNAME);
    }

    public EchoInfoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *         returns EchoInfoI
     */
    @WebEndpoint(name = "EchoInfoPort")
    public EchoInfoI getEchoInfoPort() {
        return super.getPort(new QName("http://ejbwscontext.jaxws.ws.ibm.com", "EchoInfoPort"), EchoInfoI.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns EchoInfoI
     */
    @WebEndpoint(name = "EchoInfoPort")
    public EchoInfoI getEchoInfoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbwscontext.jaxws.ws.ibm.com", "EchoInfoPort"), EchoInfoI.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ECHOINFOSERVICE_EXCEPTION != null) {
            throw ECHOINFOSERVICE_EXCEPTION;
        }
        return ECHOINFOSERVICE_WSDL_LOCATION;
    }

}
