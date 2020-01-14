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
package com.ibm.ws.wsat.ejbcdi.client.cdi;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "CDIServiceService", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", wsdlLocation = "WEB-INF/wsdl/CDIServiceService.wsdl")
public class CDIServiceService
    extends Service
{

    private final static URL CDISERVICESERVICE_WSDL_LOCATION;
    private final static WebServiceException CDISERVICESERVICE_EXCEPTION;
    private final static QName CDISERVICESERVICE_QNAME = new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "CDIServiceService");

    static {
            CDISERVICESERVICE_WSDL_LOCATION = com.ibm.ws.wsat.ejbcdi.client.cdi.CDIServiceService.class.getResource("/WEB-INF/wsdl/CDIServiceService.wsdl");
        WebServiceException e = null;
        if (CDISERVICESERVICE_WSDL_LOCATION == null) {
            e = new WebServiceException("Cannot find 'WEB-INF/wsdl/CDIServiceService.wsdl' wsdl. Place the resource correctly in the classpath.");
        }
        CDISERVICESERVICE_EXCEPTION = e;
    }

    public CDIServiceService() {
        super(__getWsdlLocation(), CDISERVICESERVICE_QNAME);
    }

//    public CDIServiceService(WebServiceFeature... features) {
//        super(__getWsdlLocation(), CDISERVICESERVICE_QNAME, features);
//    }

    public CDIServiceService(URL wsdlLocation) {
        super(wsdlLocation, CDISERVICESERVICE_QNAME);
    }

//    public CDIServiceService(URL wsdlLocation, WebServiceFeature... features) {
//        super(wsdlLocation, CDISERVICESERVICE_QNAME, features);
//    }

    public CDIServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

//    public CDIServiceService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
//        super(wsdlLocation, serviceName, features);
//    }

    /**
     * 
     * @return
     *     returns CDIService
     */
    @WebEndpoint(name = "CDIServicePort")
    public CDIService getCDIServicePort() {
        return super.getPort(new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "CDIServicePort"), CDIService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns CDIService
     */
    @WebEndpoint(name = "CDIServicePort")
    public CDIService getCDIServicePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.ejbcdi.wsat.ws.ibm.com/", "CDIServicePort"), CDIService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (CDISERVICESERVICE_EXCEPTION!= null) {
            throw CDISERVICESERVICE_EXCEPTION;
        }
        return CDISERVICESERVICE_WSDL_LOCATION;
    }

}
