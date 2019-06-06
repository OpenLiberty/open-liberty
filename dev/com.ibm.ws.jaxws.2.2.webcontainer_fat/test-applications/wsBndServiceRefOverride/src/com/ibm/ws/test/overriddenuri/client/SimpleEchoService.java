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
package com.ibm.ws.test.overriddenuri.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SimpleEchoService", targetNamespace = "http://server.overriddenuri.test.ws.ibm.com/", wsdlLocation = "WEB-INF/wsdl/SimpleEchoService.wsdl")
public class SimpleEchoService extends Service {

    private final static URL SIMPLEECHOSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(com.ibm.ws.test.overriddenuri.client.SimpleEchoService.class.getName());

    static {
        URL url = null;
        try {
            url = com.ibm.ws.test.overriddenuri.client.SimpleEchoService.class.getResource("/WEB-INF/wsdl/SimpleEchoService.wsdl");
            if (url == null)
                throw new MalformedURLException("/WEB-INF/wsdl/SimpleEchoService.wsdl does not exist in the module.");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'WEB-INF/wsdl/SimpleEchoService.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        SIMPLEECHOSERVICE_WSDL_LOCATION = url;
    }

    public SimpleEchoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SimpleEchoService() {
        super(SIMPLEECHOSERVICE_WSDL_LOCATION, new QName("http://server.overriddenuri.test.ws.ibm.com/", "SimpleEchoService"));
    }

    /**
     *
     * @return
     *         returns SimpleEcho
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort() {
        return super.getPort(new QName("http://server.overriddenuri.test.ws.ibm.com/", "SimpleEchoPort"), SimpleEcho.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns SimpleEcho
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.overriddenuri.test.ws.ibm.com/", "SimpleEchoPort"), SimpleEcho.class, features);
    }

}
