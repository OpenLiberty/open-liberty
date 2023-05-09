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
package web.ejbcdi.client.ejb;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "EJBServiceService", targetNamespace = "http://server.ejbcdi.web/", wsdlLocation = "WEB-INF/wsdl/EJBServiceService.wsdl")
public class EJBServiceService
    extends Service
{

    private final static URL EJBSERVICESERVICE_WSDL_LOCATION;
    private final static WebServiceException EJBSERVICESERVICE_EXCEPTION;
    private final static QName EJBSERVICESERVICE_QNAME = new QName("http://server.ejbcdi.web/", "EJBServiceService");

    static {
            EJBSERVICESERVICE_WSDL_LOCATION = web.ejbcdi.client.ejb.EJBServiceService.class.getResource("/WEB-INF/wsdl/EJBServiceService.wsdl");
        WebServiceException e = null;
        if (EJBSERVICESERVICE_WSDL_LOCATION == null) {
            e = new WebServiceException("Cannot find 'WEB-INF/wsdl/EJBServiceService.wsdl' wsdl. Place the resource correctly in the classpath.");
        }
        EJBSERVICESERVICE_EXCEPTION = e;
    }

    public EJBServiceService() {
        super(__getWsdlLocation(), EJBSERVICESERVICE_QNAME);
    }

//    public EJBServiceService(WebServiceFeature... features) {
//        super(__getWsdlLocation(), EJBSERVICESERVICE_QNAME, features);
//    }

    public EJBServiceService(URL wsdlLocation) {
        super(wsdlLocation, EJBSERVICESERVICE_QNAME);
    }

//    public EJBServiceService(URL wsdlLocation, WebServiceFeature... features) {
//        super(wsdlLocation, EJBSERVICESERVICE_QNAME, features);
//    }

    public EJBServiceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

//    public EJBServiceService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
//        super(wsdlLocation, serviceName, features);
//    }

    /**
     * 
     * @return
     *     returns EJBService
     */
    @WebEndpoint(name = "EJBServicePort")
    public EJBService getEJBServicePort() {
        return super.getPort(new QName("http://server.ejbcdi.web/", "EJBServicePort"), EJBService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns EJBService
     */
    @WebEndpoint(name = "EJBServicePort")
    public EJBService getEJBServicePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.ejbcdi.web/", "EJBServicePort"), EJBService.class, features);
    }

    private static URL __getWsdlLocation() {
        if (EJBSERVICESERVICE_EXCEPTION!= null) {
            throw EJBSERVICESERVICE_EXCEPTION;
        }
        return EJBSERVICESERVICE_WSDL_LOCATION;
    }

}
