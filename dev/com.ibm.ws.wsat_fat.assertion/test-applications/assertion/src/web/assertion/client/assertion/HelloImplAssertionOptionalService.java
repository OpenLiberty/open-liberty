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

@WebServiceClient(name = "HelloImplAssertionOptionalService", targetNamespace = "http://server.assertion.web/", wsdlLocation = "http://localhost:8010/assertion/HelloImplAssertionOptionalService?wsdl")
public class HelloImplAssertionOptionalService
    extends Service
{

    private final static URL HELLOIMPLASSERTIONOPTIONALSERVICE_WSDL_LOCATION;
    private final static WebServiceException HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION;
    private final static QName HELLOIMPLASSERTIONOPTIONALSERVICE_QNAME = new QName("http://server.assertion.web/", "HelloImplAssertionOptionalService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/assertion/HelloImplAssertionOptionalService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        HELLOIMPLASSERTIONOPTIONALSERVICE_WSDL_LOCATION = url;
        HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION = e;
    }

    public HelloImplAssertionOptionalService() {
        super(__getWsdlLocation(), HELLOIMPLASSERTIONOPTIONALSERVICE_QNAME);
    }

    public HelloImplAssertionOptionalService(URL wsdlLocation) {
        super(wsdlLocation, HELLOIMPLASSERTIONOPTIONALSERVICE_QNAME);
    }

    public HelloImplAssertionOptionalService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplAssertionOptionalPort")
    public Hello getHelloImplAssertionOptionalPort() {
        return super.getPort(new QName("http://server.assertion.web/", "HelloImplAssertionOptionalPort"), Hello.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns Hello
     */
    @WebEndpoint(name = "HelloImplAssertionOptionalPort")
    public Hello getHelloImplAssertionOptionalPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.assertion.web/", "HelloImplAssertionOptionalPort"), Hello.class, features);
    }

    private static URL __getWsdlLocation() {
        if (HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION!= null) {
            throw HELLOIMPLASSERTIONOPTIONALSERVICE_EXCEPTION;
        }
        return HELLOIMPLASSERTIONOPTIONALSERVICE_WSDL_LOCATION;
    }

}
