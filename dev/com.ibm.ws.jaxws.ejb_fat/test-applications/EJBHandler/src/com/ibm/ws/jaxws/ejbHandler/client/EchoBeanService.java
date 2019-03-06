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
package com.ibm.ws.jaxws.ejbHandler.client;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "EchoBeanService", targetNamespace = "http://ejbHandler.jaxws.ws.ibm.com/",
                  wsdlLocation = "META-INF/resources/wsdl/EchoBeanService.wsdl")
public class EchoBeanService extends Service {
    private final static URL ECHOBEANSERVICE_WSDL_LOCATION;
    private final static WebServiceException ECHOBEANSERVICE_EXCEPTION;
    private final static QName ECHOBEANSERVICE_QNAME = new QName("http://ejbHandler.jaxws.ws.ibm.com/", "EchoBeanService");

    static {
        WebServiceException webServiceException = null;
        URL url = null;
        try {
            url = EchoBeanService.class.getResource("EchoBeanService.wsdl");
        } catch (Exception e) {
            webServiceException = new WebServiceException(e);
        }
        ECHOBEANSERVICE_EXCEPTION = webServiceException;
        ECHOBEANSERVICE_WSDL_LOCATION = url;
    }

    public EchoBeanService() {
        super(__getWsdlLocation(), ECHOBEANSERVICE_QNAME);
    }

    public EchoBeanService(URL wsdlLocation) {
        super(wsdlLocation, ECHOBEANSERVICE_QNAME);
    }

    public EchoBeanService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     *
     * @return
     *         returns EchoBean
     */
    @WebEndpoint(name = "EchoBeanPort")
    public EchoBean getEchoBeanPort() {
        return super.getPort(new QName("http://ejbHandler.jaxws.ws.ibm.com/", "EchoBeanPort"), EchoBean.class);
    }

    /**
     *
     * @param features
     *                     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy. Supported features not in the <code>features</code> parameter will have their
     *                     default
     *                     values.
     * @return
     *         returns EchoBean
     */
    @WebEndpoint(name = "EchoBeanPort")
    public EchoBean getEchoBeanPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://ejbHandler.jaxws.ws.ibm.com/", "EchoBeanPort"), EchoBean.class, features);
    }

    private static URL __getWsdlLocation() {
        if (ECHOBEANSERVICE_EXCEPTION != null) {
            throw ECHOBEANSERVICE_EXCEPTION;
        }
        return ECHOBEANSERVICE_WSDL_LOCATION;
    }

}
