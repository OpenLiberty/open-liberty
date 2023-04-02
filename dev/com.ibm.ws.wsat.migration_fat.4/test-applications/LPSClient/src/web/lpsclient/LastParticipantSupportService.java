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
package web.lpsclient;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "LastParticipantSupportService", targetNamespace = "http://lpsservice.web/", wsdlLocation = "http://localhost:9992/LPSService/LastParticipantSupportService?wsdl")
public class LastParticipantSupportService
    extends Service
{

    private final static URL LASTPARTICIPANTSUPPORTSERVICE_WSDL_LOCATION;
    private final static WebServiceException LASTPARTICIPANTSUPPORTSERVICE_EXCEPTION;
    private final static QName LASTPARTICIPANTSUPPORTSERVICE_QNAME = new QName("http://lpsservice.web/", "LastParticipantSupportService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:9992/LPSService/LastParticipantSupportService?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        LASTPARTICIPANTSUPPORTSERVICE_WSDL_LOCATION = url;
        LASTPARTICIPANTSUPPORTSERVICE_EXCEPTION = e;
    }

    public LastParticipantSupportService() {
        super(__getWsdlLocation(), LASTPARTICIPANTSUPPORTSERVICE_QNAME);
    }

    public LastParticipantSupportService(URL wsdlLocation) {
        super(wsdlLocation, LASTPARTICIPANTSUPPORTSERVICE_QNAME);
    }

    public LastParticipantSupportService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    /**
     * 
     * @return
     *     returns LastParticipantSupport
     */
    @WebEndpoint(name = "LastParticipantSupportPort")
    public LastParticipantSupport getLastParticipantSupportPort() {
        return super.getPort(new QName("http://lpsservice.web/", "LastParticipantSupportPort"), LastParticipantSupport.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns LastParticipantSupport
     */
    @WebEndpoint(name = "LastParticipantSupportPort")
    public LastParticipantSupport getLastParticipantSupportPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://lpsservice.web/", "LastParticipantSupportPort"), LastParticipantSupport.class, features);
    }

    private static URL __getWsdlLocation() {
        if (LASTPARTICIPANTSUPPORTSERVICE_EXCEPTION!= null) {
            throw LASTPARTICIPANTSUPPORTSERVICE_EXCEPTION;
        }
        return LASTPARTICIPANTSUPPORTSERVICE_WSDL_LOCATION;
    }

}
