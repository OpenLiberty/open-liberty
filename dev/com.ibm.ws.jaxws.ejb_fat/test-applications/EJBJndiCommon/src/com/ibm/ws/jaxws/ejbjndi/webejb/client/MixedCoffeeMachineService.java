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

package com.ibm.ws.jaxws.ejbjndi.webejb.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "MixedCoffeeMachineService", targetNamespace = "http://webejb.ejbjndi.jaxws.ws.ibm.com/", wsdlLocation = "META-INF/MixedCoffeeMachineService.wsdl")
public class MixedCoffeeMachineService
                extends Service
{

    private final static URL MIXEDCOFFEEMACHINESERVICE_WSDL_LOCATION;
    private final static WebServiceException MIXEDCOFFEEMACHINESERVICE_EXCEPTION;
    private final static QName MIXEDCOFFEEMACHINESERVICE_QNAME = new QName("http://webejb.ejbjndi.jaxws.ws.ibm.com/", "MixedCoffeeMachineService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://ivan-pc:8010/EJBJndiWebEJB/MixedCoffeeMachineService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        MIXEDCOFFEEMACHINESERVICE_WSDL_LOCATION = url;
        MIXEDCOFFEEMACHINESERVICE_EXCEPTION = e;
    }

    public MixedCoffeeMachineService() {
        super(__getWsdlLocation(), MIXEDCOFFEEMACHINESERVICE_QNAME);
    }

    public MixedCoffeeMachineService(WebServiceFeature... features) {
        super(__getWsdlLocation(), MIXEDCOFFEEMACHINESERVICE_QNAME, features);
    }

    public MixedCoffeeMachineService(URL wsdlLocation) {
        super(wsdlLocation, MIXEDCOFFEEMACHINESERVICE_QNAME);
    }

    public MixedCoffeeMachineService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, MIXEDCOFFEEMACHINESERVICE_QNAME, features);
    }

    public MixedCoffeeMachineService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public MixedCoffeeMachineService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *         returns CoffeeMachine
     */
    @WebEndpoint(name = "MixedCoffeeMachinePort")
    public CoffeeMachine getMixedCoffeeMachinePort() {
        return super.getPort(new QName("http://webejb.ejbjndi.jaxws.ws.ibm.com/", "MixedCoffeeMachinePort"), CoffeeMachine.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns CoffeeMachine
     */
    @WebEndpoint(name = "MixedCoffeeMachinePort")
    public CoffeeMachine getMixedCoffeeMachinePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://webejb.ejbjndi.jaxws.ws.ibm.com/", "MixedCoffeeMachinePort"), CoffeeMachine.class, features);
    }

    private static URL __getWsdlLocation() {
        if (MIXEDCOFFEEMACHINESERVICE_EXCEPTION != null) {
            throw MIXEDCOFFEEMACHINESERVICE_EXCEPTION;
        }
        return MIXEDCOFFEEMACHINESERVICE_WSDL_LOCATION;
    }

}
