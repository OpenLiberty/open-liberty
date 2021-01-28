/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
