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
package com.ibm.ws.wsat.threadedclient.client.threaded;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "MultiThreadedService", targetNamespace = "http://server.threadedserver.wsat.ws.ibm.com/", wsdlLocation = "http://localhost:9992/threadedServer/MultiThreadedService?wsdl")
public class MultiThreadedService
    extends Service
{

    private final static URL MULTITHREADEDSERVICE_WSDL_LOCATION;
    private final static WebServiceException MULTITHREADEDSERVICE_EXCEPTION;
    private final static QName MULTITHREADEDSERVICE_QNAME = new QName("http://server.threadedserver.wsat.ws.ibm.com/", "MultiThreadedService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:9992/threadedServer/MultiThreadedService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        MULTITHREADEDSERVICE_WSDL_LOCATION = url;
        MULTITHREADEDSERVICE_EXCEPTION = e;
    }

    public MultiThreadedService() {
        super(__getWsdlLocation(), MULTITHREADEDSERVICE_QNAME);
    }

    public MultiThreadedService(URL wsdlLocation) {
        super(wsdlLocation, MULTITHREADEDSERVICE_QNAME);
    }

    public MultiThreadedService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns MultiThreaded
     */
    @WebEndpoint(name = "MultiThreadedPort")
    public MultiThreaded getMultiThreadedPort() {
        return super.getPort(new QName("http://server.threadedserver.wsat.ws.ibm.com/", "MultiThreadedPort"), MultiThreaded.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns MultiThreaded
     */
    @WebEndpoint(name = "MultiThreadedPort")
    public MultiThreaded getMultiThreadedPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.threadedserver.wsat.ws.ibm.com/", "MultiThreadedPort"), MultiThreaded.class, features);
    }

    private static URL __getWsdlLocation() {
        if (MULTITHREADEDSERVICE_EXCEPTION!= null) {
            throw MULTITHREADEDSERVICE_EXCEPTION;
        }
        return MULTITHREADEDSERVICE_WSDL_LOCATION;
    }

}
