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
package com.ibm.ws.jaxws.client;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;

@WebServiceClient(name = "AddNumbers", targetNamespace = "http://provider.jaxws.ws.ibm.com/", wsdlLocation = "AddNumbers.wsdl")
public class AddNumbers_Service
                extends Service {

    private final static URL ADDNUMBERS_WSDL_LOCATION;
    private final static WebServiceException ADDNUMBERS_EXCEPTION;
    private final static QName ADDNUMBERS_QNAME = new QName("http://provider.jaxws.ws.ibm.com/", "WEB-INF/AddNumbers");

    static {
        URL url = null;
        WebServiceException e = null;
        URL baseUrl = AddNumbers_Service.class.getResource(".");
        try {
            url = new URL(baseUrl, "WEB-INF/AddNumbers.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        ADDNUMBERS_WSDL_LOCATION = url;
        ADDNUMBERS_EXCEPTION = e;
    }

    public AddNumbers_Service() {
        super(__getWsdlLocation(), ADDNUMBERS_QNAME);
    }

    public AddNumbers_Service(URL wsdlLocation) {
        super(wsdlLocation, ADDNUMBERS_QNAME);
    }

    public AddNumbers_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *         returns AddNumbers
     */
    @WebEndpoint(name = "AddNumbersPort")
    public AddNumbers getAddNumbersPort() {
        return super.getPort(new QName("http://provider.jaxws.ws.ibm.com/", "AddNumbersPort"), AddNumbers.class);
    }

    private static URL __getWsdlLocation() {
        if (ADDNUMBERS_EXCEPTION != null) {
            throw ADDNUMBERS_EXCEPTION;
        }
        return ADDNUMBERS_WSDL_LOCATION;
    }

}
