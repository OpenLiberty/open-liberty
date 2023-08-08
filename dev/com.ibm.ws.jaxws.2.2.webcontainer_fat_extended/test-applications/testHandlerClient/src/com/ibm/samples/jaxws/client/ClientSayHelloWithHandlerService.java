/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

@WebServiceClient(name = "SayHelloServiceWithHandler",
                  targetNamespace = "http://jaxws.samples.ibm.com.handler/",
                  wsdlLocation = "META-INF/resources/wsdl/sayHelloServiceWithHandler.wsdl")
public class ClientSayHelloWithHandlerService extends Service {

    private final static URL SAYHELLOSERVICE_WSDL_LOCATION;

    static {
        URL url = null;
        try {
            URL baseUrl = ClientSayHelloWithHandlerService.class.getClassLoader().getResource(".");
            url = new URL(baseUrl, "sayHelloServiceWithHandler.wsdl");
        } catch (MalformedURLException e) {
            Logger.getLogger(ClientSayHelloWithHandlerService.class.getName()).log(Level.INFO, "Can not initialize the default wsdl from {0}", "sayHelloServiceWithHandler.wsdl");
        }
        SAYHELLOSERVICE_WSDL_LOCATION = url;
    }

    /**
     * @param wsdlDocumentLocation
     * @param serviceName
     */
    public ClientSayHelloWithHandlerService(URL wsdlDocumentLocation, QName serviceName) {
        super(wsdlDocumentLocation, serviceName);
    }

    public ClientSayHelloWithHandlerService() {
        super(SAYHELLOSERVICE_WSDL_LOCATION, new QName("http://jaxws.samples.ibm.com.handler/", "SayHelloServiceWithHandler"));
    }

    @WebEndpoint(name = "SayHelloPort")
    public SayHelloService getHelloServicePort() {
        return super.getPort(new QName("http://jaxws.samples.ibm.com.handler/", "SayHelloPort"),
                             SayHelloService.class);
    }
}
