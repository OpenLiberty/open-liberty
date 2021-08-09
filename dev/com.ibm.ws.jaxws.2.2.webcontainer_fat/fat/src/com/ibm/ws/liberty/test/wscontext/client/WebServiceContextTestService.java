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
package com.ibm.ws.liberty.test.wscontext.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "WebServiceContextTestService", targetNamespace = "http://wscontext.test.liberty.ws.ibm.com",
                  wsdlLocation = "http://localhost:7080/WebTest/WebServiceContextTestService?wsdl")
public class WebServiceContextTestService extends Service {

    private final static URL WEBSERVICECONTEXTTESTSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(com.ibm.ws.liberty.test.wscontext.client.WebServiceContextTestService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = com.ibm.ws.liberty.test.wscontext.client.WebServiceContextTestService.class.getResource(".");
            url = new URL(baseUrl, "http://localhost:7080/WebTest/WebServiceContextTestService?wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://localhost:7080/WebTest/WebServiceContextTestService?wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        WEBSERVICECONTEXTTESTSERVICE_WSDL_LOCATION = url;
    }

    public WebServiceContextTestService(URL wsdlLocation) {
        super(wsdlLocation, new QName("http://wscontext.test.liberty.ws.ibm.com", "WebServiceContextTestService"));
    }

    public WebServiceContextTestService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WebServiceContextTestService() {
        super(WEBSERVICECONTEXTTESTSERVICE_WSDL_LOCATION, new QName("http://wscontext.test.liberty.ws.ibm.com", "WebServiceContextTestService"));
    }

    /**
     *
     * @return
     *         returns WebServiceContextTestServicePortType
     */
    @WebEndpoint(name = "WebServiceContextTestServicePort")
    public WebServiceContextTestServicePortType getWebServiceContextTestServicePort() {
        return super.getPort(new QName("http://wscontext.test.liberty.ws.ibm.com", "WebServiceContextTestServicePort"), WebServiceContextTestServicePortType.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns WebServiceContextTestServicePortType
     */
    @WebEndpoint(name = "WebServiceContextTestServicePort")
    public WebServiceContextTestServicePortType getWebServiceContextTestServicePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://wscontext.test.liberty.ws.ibm.com", "WebServiceContextTestServicePort"), WebServiceContextTestServicePortType.class, features);
    }

}
