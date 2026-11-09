/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.untmmnonceprovider;

import java.io.StringReader;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

/**
 * Multi-module EAR provider. The server-side WS-SecurityPolicy comes from this module's
 * WEB-INF/policy-attachments-server.xml.
 */
@WebServiceProvider(targetNamespace = "http://wssec.basic.cxf.fats",
                    portName = "SoapPort3",
                    serviceName = "SOAPService3",
                    wsdlLocation = "WEB-INF/UsrTokenWebSvc.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
public class UsrTokenNonceWebSvc implements Provider<SOAPMessage> {

    @Override
    public SOAPMessage invoke(SOAPMessage request) {

        SOAPMessage response = null;

        try {
            StringReader respMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><provider>This is WSSECFVT CXF Web Service.</provider></soapenv:Body></soapenv:Envelope>");

            Source src = new StreamSource(respMsg);
            MessageFactory factory = MessageFactory.newInstance();
            response = factory.createMessage();
            response.getSOAPPart().setContent(src);
            response.saveChanges();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return response;
    }
}
