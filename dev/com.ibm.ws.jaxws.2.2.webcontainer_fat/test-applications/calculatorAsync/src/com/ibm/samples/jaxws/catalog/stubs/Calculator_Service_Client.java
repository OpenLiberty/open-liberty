/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.samples.jaxws.catalog.stubs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated from the WSDL file calculator.wsdl
 *
 * Service class for Calculator Web Service
 */
@WebServiceClient(name = "Calculator",
                  targetNamespace = "http://catalog.jaxws.samples.ibm.com",
                  wsdlLocation = "file:calculator.wsdl")
public class Calculator_Service_Client extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://catalog.jaxws.samples.ibm.com", "Calculator");
    public final static QName CalculatorPort = new QName("http://catalog.jaxws.samples.ibm.com", "CalculatorPort");

    private static final Logger log = Logger.getLogger(Calculator_Service_Client.class.getName());

    static {
        URL url = null;
        try {
            url = new URL("file:calculator.wsdl");

        } catch (MalformedURLException e) {
            log.log(Level.WARNING, "Can not initialize the default wsdl from {0}", "file:calculator.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public Calculator_Service_Client(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public Calculator_Service_Client(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public Calculator_Service_Client() {
        super(WSDL_LOCATION, SERVICE);
    }

    public Calculator_Service_Client(WebServiceFeature... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public Calculator_Service_Client(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public Calculator_Service_Client(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * Get the Calculator port
     *
     * @return returns CalculatorPortType
     */
    @WebEndpoint(name = "CalculatorPort")
    public CalculatorPortType getCalculatorPort() {
        return super.getPort(CalculatorPort, CalculatorPortType.class);
    }

    /**
     * Get the Calculator port with features
     *
     * @param features A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.
     * @return returns CalculatorPortType
     */
    @WebEndpoint(name = "CalculatorPort")
    public CalculatorPortType getCalculatorPort(WebServiceFeature... features) {
        return super.getPort(CalculatorPort, CalculatorPortType.class, features);
    }

}
