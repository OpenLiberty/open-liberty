/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package web.assertion.client.assertion;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "HelloImplAssertionRequiredService",
                  targetNamespace = "http://server.assertion.web/",
                  wsdlLocation = "http://localhost:8010/assertion/HelloImplAssertionRequiredService?wsdl")
public class HelloImplAssertionRequiredService extends Service {

    private static final URL WSDL_LOCATION;
    private static final WebServiceException WSDL_EXCEPTION;
    private static final QName SERVICE_QNAME =
        new QName("http://server.assertion.web/", "HelloImplAssertionRequiredService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/assertion/HelloImplAssertionRequiredService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        WSDL_LOCATION = url;
        WSDL_EXCEPTION = e;
    }

    public HelloImplAssertionRequiredService() {
        super(__getWsdlLocation(), SERVICE_QNAME);
    }

    public HelloImplAssertionRequiredService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE_QNAME);
    }

    public HelloImplAssertionRequiredService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    @WebEndpoint(name = "HelloImplAssertionRequiredPort")
    public Hello getHelloImplAssertionRequiredPort() {
        return super.getPort(new QName("http://server.assertion.web/", "HelloImplAssertionRequiredPort"), Hello.class);
    }

    @WebEndpoint(name = "HelloImplAssertionRequiredPort")
    public Hello getHelloImplAssertionRequiredPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.assertion.web/", "HelloImplAssertionRequiredPort"), Hello.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WSDL_EXCEPTION != null) {
            throw WSDL_EXCEPTION;
        }
        return WSDL_LOCATION;
    }
}
