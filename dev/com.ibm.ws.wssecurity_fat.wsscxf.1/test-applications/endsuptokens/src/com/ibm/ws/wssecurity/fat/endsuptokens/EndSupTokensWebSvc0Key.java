/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.endsuptokens;

import java.io.StringReader;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
// import javax.xml.soap.SOAPBody;
// import javax.xml.soap.SOAPPart;
// import javax.xml.soap.SOAPEnvelope;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(targetNamespace = "http://endsuptokens.wssecfvt.test",
                    portName = "EndSupTokensX509EndorsingKeyPort",
                    serviceName = "EndSupTokensService0Key",
                    wsdlLocation = "WEB-INF/EndSupTokens.wsdl")

@ServiceMode(value = Service.Mode.MESSAGE)

public class EndSupTokensWebSvc0Key implements Provider<SOAPMessage> {

    @Override
    public SOAPMessage invoke(SOAPMessage request) {

        SOAPMessage response = null;

        try {

            System.out.println("Incoming Client Request as a SOAPMessage:");
            request.writeTo(System.out);

            String hdrText = request.getSOAPHeader().getTextContent();
            System.out.println("Incoming SOAP Header:" + hdrText);

            StringReader respMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://endsuptokens.wssecfvt.test/types\"><provider>This is EndSupTokensWebSvc0Key Web Service.</provider></soapenv:Body></soapenv:Envelope>");

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
