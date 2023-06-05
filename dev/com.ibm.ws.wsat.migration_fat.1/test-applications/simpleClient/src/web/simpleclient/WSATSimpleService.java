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
package web.simpleclient;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "WSATSimpleService", targetNamespace = "http://simpleservice.web/", wsdlLocation = "http://localhost:9992/simpleService/WSATSimpleService?wsdl")
public class WSATSimpleService
    extends Service
{

    private final static URL WSATSIMPLESERVICE_WSDL_LOCATION;
    private final static WebServiceException WSATSIMPLESERVICE_EXCEPTION;
    private final static QName WSATSIMPLESERVICE_QNAME = new QName("http://simpleservice.web/", "WSATSimpleService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:9992/simpleService/WSATSimpleService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        WSATSIMPLESERVICE_WSDL_LOCATION = url;
        WSATSIMPLESERVICE_EXCEPTION = e;
    }

    public WSATSimpleService() {
        super(__getWsdlLocation(), WSATSIMPLESERVICE_QNAME);
    }

    public WSATSimpleService(URL wsdlLocation) {
        super(wsdlLocation, WSATSIMPLESERVICE_QNAME);
    }

    public WSATSimpleService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns WSATSimple
     */
    @WebEndpoint(name = "WSATSimplePort")
    public WSATSimple getWSATSimplePort() {
        return super.getPort(new QName("http://simpleservice.web/", "WSATSimplePort"), WSATSimple.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns WSATSimple
     */
    @WebEndpoint(name = "WSATSimplePort")
    public WSATSimple getWSATSimplePort(WebServiceFeature... features) {
        return super.getPort(new QName("http://simpleservice.web/", "WSATSimplePort"), WSATSimple.class, features);
    }

    private static URL __getWsdlLocation() {
        if (WSATSIMPLESERVICE_EXCEPTION!= null) {
            throw WSATSIMPLESERVICE_EXCEPTION;
        }
        return WSATSIMPLESERVICE_WSDL_LOCATION;
    }

}
