/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat.stubclient.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SimpleEchoService", targetNamespace = "http://stubclient.fat.jaxws.openliberty.io/")
public class SimpleEchoService extends Service {

    private final static URL SIMPLEECHOSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(io.openliberty.jaxws.fat.stubclient.client.SimpleEchoService.class.getName());


    static {
		String host = System.getProperty("hostName");
   		if (host == null) {
                logger.info("Failed to obtain host from system property, hostName, falling back to localhost");
                host = "localhost";
    	}
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = io.openliberty.jaxws.fat.stubclient.client.SimpleEchoService.class.getResource(".");

            url = new URL(baseUrl, new StringBuilder().append("http://" + host
                                                              + ":").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/simpleTestService/SimpleEchoService?wsdl").toString());
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: "
                           + new StringBuilder().append("http://" + host
                                                              + ":").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/simpleService/SimpleEchoService?wsdl").toString()
                           + ", retrying as a local file");
            logger.warning(e.getMessage());
        }
        SIMPLEECHOSERVICE_WSDL_LOCATION = url;
    }

    public SimpleEchoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SimpleEchoService() {
        super(SIMPLEECHOSERVICE_WSDL_LOCATION, new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoService"));
    }

    /**
     *
     * @return
     *         returns SimpleEcho
     */
    @WebEndpoint(name = "SimpleEchoPort")
    public SimpleEcho getSimpleEchoPort() {
        return super.getPort(new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoPort"), SimpleEcho.class);
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
        return super.getPort(new QName("http://stubclient.fat.jaxws.openliberty.io/", "SimpleEchoPort"), SimpleEcho.class, features);
    }

}
