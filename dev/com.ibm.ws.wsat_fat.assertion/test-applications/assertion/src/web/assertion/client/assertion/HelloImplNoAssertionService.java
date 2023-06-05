/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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

@WebServiceClient(name = "HelloImplNoAssertionService", targetNamespace = "http://server.assertion.web/", wsdlLocation = "http://localhost:8010/assertion/HelloImplNoAssertionService?wsdl")
public class HelloImplNoAssertionService
    extends Service
{

    private final static URL HELLOIMPLNOASSERTIONSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOIMPLNOASSERTIONSERVICE_EXCEPTION;
    private final static QName HELLOIMPLNOASSERTIONSERVICE_QNAME = new QName("http://server.assertion.web/", "HelloImplNoAssertionService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/assertion/HelloImplNoAssertionService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOIMPLNOASSERTIONSERVICE_WSDL_LOCATION = url;
        HELLOIMPLNOASSERTIONSERVICE_EXCEPTION = e;
    }

    public HelloImplNoAssertionService() {
        super(__getWsdlLocation(), HELLOIMPLNOASSERTIONSERVICE_QNAME);
    }

    public HelloImplNoAssertionService(URL wsdlLocation) {
        super(wsdlLocation, HELLOIMPLNOASSERTIONSERVICE_QNAME);
    }

    public HelloImplNoAssertionService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplNoAssertionPort")
    public Hello getHelloImplNoAssertionPort() {
        return super.getPort(new QName("http://server.assertion.web/", "HelloImplNoAssertionPort"), Hello.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplNoAssertionPort")
    public Hello getHelloImplNoAssertionPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.assertion.web/", "HelloImplNoAssertionPort"), Hello.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOIMPLNOASSERTIONSERVICE_EXCEPTION!= null) {
            throw HELLOIMPLNOASSERTIONSERVICE_EXCEPTION;
        }
        return HELLOIMPLNOASSERTIONSERVICE_WSDL_LOCATION;
    }

}
