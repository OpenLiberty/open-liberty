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
package com.ibm.samples.jaxws.spring.wsdlfirst.stub;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "CustomerServiceService",
                  wsdlLocation = "WEB-INF/CustomerService.wsdl",
                  targetNamespace = "http://customerservice.example.com/")
public class CustomerServiceService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://customerservice.example.com/", "CustomerServiceService");
    public final static QName CustomerServicePort = new QName("http://customerservice.example.com/", "CustomerServicePort");
    static {
        URL url = null;
        try {
            url = new URL("WEB-INF/CustomerService.wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(CustomerServiceService.class.getName()).log(java.util.logging.Level.INFO,
                                                                                           "Can not initialize the default wsdl from {0}", "CustomerService.wsdl");
        }
        WSDL_LOCATION = url;
    }

    public CustomerServiceService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public CustomerServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public CustomerServiceService() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     *
     * @return
     *         returns CustomerService
     */
    @WebEndpoint(name = "CustomerServicePort")
    public CustomerService getCustomerServicePort() {
        return super.getPort(CustomerServicePort, CustomerService.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns CustomerService
     */
    @WebEndpoint(name = "CustomerServicePort")
    public CustomerService getCustomerServicePort(WebServiceFeature... features) {
        return super.getPort(CustomerServicePort, CustomerService.class, features);
    }

}
