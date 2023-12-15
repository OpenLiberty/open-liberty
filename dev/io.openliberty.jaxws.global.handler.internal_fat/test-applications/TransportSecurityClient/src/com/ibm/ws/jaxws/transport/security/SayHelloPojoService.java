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
package com.ibm.ws.jaxws.transport.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "SayHelloPojoService",
                  targetNamespace = "http://ibm.com/ws/jaxws/transport/security/",
                  wsdlLocation = "META-INF/resources/wsdl/EmployPojoService.wsdl")
public class SayHelloPojoService
                extends Service
{

    private final static URL SAYHELLOPOJOSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(com.ibm.ws.jaxws.transport.security.SayHelloPojoService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = com.ibm.ws.jaxws.transport.security.SayHelloPojoService.class.getResource(".");
            url = new URL(baseUrl, "http://9.125.30.70:8010/TransportSecurityProvider/unauthorized/employPojoService?wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'http://9.125.30.70:8010/TransportSecurityProvider/unauthorized/employPojoService?wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        SAYHELLOPOJOSERVICE_WSDL_LOCATION = url;
    }

    public SayHelloPojoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SayHelloPojoService() {
        super(SAYHELLOPOJOSERVICE_WSDL_LOCATION, new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloPojoService"));
    }

    /**
     * 
     * @return
     *         returns SayHello
     */
    @WebEndpoint(name = "SayHelloPojoPort")
    public SayHello getSayHelloPojoPort() {
        return super.getPort(new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloPojoPort"), SayHello.class);
    }

    /**
     * 
     * @param features
     *            A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their default
     *            values.
     * @return
     *         returns SayHello
     */
    @WebEndpoint(name = "SayHelloPojoPort")
    public SayHello getSayHelloPojoPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ibm.com/ws/jaxws/transport/security/", "SayHelloPojoPort"), SayHello.class, features);
    }

}
