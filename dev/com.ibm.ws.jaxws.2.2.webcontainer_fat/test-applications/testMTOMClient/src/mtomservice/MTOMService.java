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
package mtomservice;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "MTOMService", targetNamespace = "http://MTOMService/", wsdlLocation = "http://localhost:8010/MTOMTest/MTOMService?wsdl")
public class MTOMService extends Service {

    private final static URL MTOMSERVICE_WSDL_LOCATION;
    private final static WebServiceException MTOMSERVICE_EXCEPTION;
    private final static QName MTOMSERVICE_QNAME = new QName("http://MTOMService/", "MTOMService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8010/MTOMTest/MTOMService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        MTOMSERVICE_WSDL_LOCATION = url;
        MTOMSERVICE_EXCEPTION = e;
    }

    public MTOMService() {
        super(__getWsdlLocation(), MTOMSERVICE_QNAME);
    }

//    public MTOMService(WebServiceFeature... features) {
//        super(__getWsdlLocation(), MTOMSERVICE_QNAME, features);
//    }

    public MTOMService(URL wsdlLocation) {
        super(wsdlLocation, MTOMSERVICE_QNAME);
    }

//    public MTOMService(URL wsdlLocation, WebServiceFeature... features) {
//        super(wsdlLocation, MTOMSERVICE_QNAME, features);
//    }

    public MTOMService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

//    public MTOMService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
//        super(wsdlLocation, serviceName, features);
//    }

    /**
     *
     * @return
     *         returns MTOMInter
     */
    @WebEndpoint(name = "MTOMServicePort")
    public MTOMInter getMTOMServicePort() {
        return super.getPort(new QName("http://MTOMService/", "MTOMServicePort"), MTOMInter.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default values.
     * @return
     *         returns MTOMInter
     */
    @WebEndpoint(name = "MTOMServicePort")
    public MTOMInter getMTOMServicePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://MTOMService/", "MTOMServicePort"), MTOMInter.class, features);
    }

    private static URL __getWsdlLocation() {
        if (MTOMSERVICE_EXCEPTION != null) {
            throw MTOMSERVICE_EXCEPTION;
        }
        return MTOMSERVICE_WSDL_LOCATION;
    }

}
