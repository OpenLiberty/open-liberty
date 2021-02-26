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
package com.ibm.ws.wsat.ejbcdi.client.normal;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "NormalServiceService", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", wsdlLocation = "WEB-INF/wsdl/NormalServiceService.wsdl")
public class NormalServiceService
    extends Service
{

    private final static URL NORMALSERVICESERVICE_WSDL_LOCATION;
    private final static WebServiceException NORMALSERVICESERVICE_EXCEPTION;
    private final static QName NORMALSERVICESERVICE_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "NormalServiceService");

    static {
            NORMALSERVICESERVICE_WSDL_LOCATION = com.ibm.ws.wsat.ejbcdi.client.normal.NormalServiceService.class.getResource("/WEB-INF/wsdl/NormalServiceService.wsdl");
        WebServiceException e = null;
        if (NORMALSERVICESERVICE_WSDL_LOCATION == null) {
            e = new WebServiceException("Cannot find 'WEB-INF/wsdl/NormalServiceService.wsdl' wsdl. Place the resource correctly in the classpath.");
        }
        NORMALSERVICESERVICE_EXCEPTION = e;
    }

    public NormalServiceService() {
        super(__getWsdlLocation(), NORMALSERVICESERVICE_QNAME);
    }

//    public NormalServiceService(WebServiceFeature... features) {
//        super(__getWsdlLocation(), NORMALSERVICESERVICE_QNAME, features);
//    }

    public NormalServiceService(URL wsdlLocation) {
        super(wsdlLocation, NORMALSERVICESERVICE_QNAME);
    }

//    public NormalServiceService(URL wsdlLocation, WebServiceFeature... features) {
//        super(wsdlLocation, NORMALSERVICESERVICE_QNAME, features);
//    }

    public NormalServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

//    public NormalServiceService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
//        super(wsdlLocation, serviceName, features);
//    }

    /**
     * 
     * @return
     *     returns NormalService
     */
    @WebEndpoint(name = "NormalServicePort")
    public NormalService getNormalServicePort() {
        return super.getPort(new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "NormalServicePort"), NormalService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns NormalService
     */
    @WebEndpoint(name = "NormalServicePort")
    public NormalService getNormalServicePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "NormalServicePort"), NormalService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (NORMALSERVICESERVICE_EXCEPTION!= null) {
            throw NORMALSERVICESERVICE_EXCEPTION;
        }
        return NORMALSERVICESERVICE_WSDL_LOCATION;
    }

}
