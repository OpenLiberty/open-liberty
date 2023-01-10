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
package com.ibm.samples.jaxws.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

/**
 *
 */
@WebServiceClient(name = "SayHelloService",
                  targetNamespace = "http://jaxws.samples.ibm.com.handler/",
                  wsdlLocation = "META-INF/resources/wsdl/sayHelloService.wsdl")
public class ClientSayHelloService extends Service {

    private final static URL SAYHELLOSERVICE_WSDL_LOCATION;

    static {
        URL url = null;
        try {
            URL baseUrl = ClientSayHelloService.class.getClassLoader().getResource(".");
            url = new URL(baseUrl, "sayHelloService.wsdl");
        } catch (MalformedURLException e) {
            Logger.getLogger(ClientSayHelloService.class.getName()).log(Level.INFO, "Can not initialize the default wsdl from {0}", "sayHelloService.wsdl");
        }
        SAYHELLOSERVICE_WSDL_LOCATION = url;
    }

    /**
     * @param wsdlDocumentLocation
     * @param serviceName
     */
    public ClientSayHelloService(URL wsdlDocumentLocation, QName serviceName) {
        super(wsdlDocumentLocation, serviceName);
    }

    public ClientSayHelloService() {
        super(SAYHELLOSERVICE_WSDL_LOCATION, new QName("http://jaxws.samples.ibm.com.handler/", "SayHelloService"));
    }

    @WebEndpoint(name = "SayHelloPort")
    public SayHelloService getHelloServicePort() {
        return super.getPort(new QName("http://jaxws.samples.ibm.com.handler/", "SayHelloPort"),
                             SayHelloService.class);
    }
}
