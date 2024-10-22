/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.samples.jaxws.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

@WebServiceClient(name = "SayHelloService",
                  targetNamespace = "http://jaxws.samples.openliberty.io/")
public class ClientSayHelloWithHandlerService extends Service {

    private final static URL SAYHELLOSERVICE_WSDL_LOCATION;
    private static Logger logger = Logger.getLogger(ClientSayHelloWithHandlerService.class.getName());
    
    static {
        URL url = null;
        String urlString = null;
        try {
            String host = System.getProperty("hostName");
            if (host == null) {
                logger.info("Failed to obtain host from system property, hostName, falling back to localhost");
                host = "localhost";
            }
            urlString = new StringBuilder().append("http://" + host + ":").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/testWebServiceClient/SayHelloService?wsdl").toString();   
            logger.info("URL : " + urlString + " will be used for ClientSayHelloWithHandlerService.");
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            logger.log(Level.INFO, "Can not initialize the default wsdl from {0}", urlString);
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
        super(SAYHELLOSERVICE_WSDL_LOCATION, new QName("http://jaxws.samples.openliberty.io/", "SayHelloService"));
    }

    @WebEndpoint(name = "SayHelloPort")
    public SayHelloService getHelloServicePort() {
        return super.getPort(new QName("http://jaxws.samples.openliberty.io/", "SayHelloPort"),
                             SayHelloService.class);
    }
}
