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

package com.ibm.ws.wssecurity.fat.x509sig;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

@WebServiceProvider(targetNamespace="http://x509sig.wssecfvt.test",
    portName="UrnX509SigAsync",
    serviceName="X509XmlSigService1Async",
    wsdlLocation = "WEB-INF/X509XmlSig.wsdl"
)

@ServiceMode(value = Service.Mode.MESSAGE)

public class X509SigWebSvcAsync implements Provider<SOAPMessage> {


    public SOAPMessage invoke(SOAPMessage request) {

        SOAPMessage response = null;

        try {

            System.out.println("Incoming Client Request as a SOAPMessage:");
            request.writeTo(System.out);

            String hdrText = request.getSOAPHeader().getTextContent();
            System.out.println("Incoming SOAP Header:" + hdrText);

            StringReader respMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><provider>This is WSSECFVT CXF X509SigWebSvcAsync.</provider></soapenv:Body></soapenv:Envelope>");

            // SOAPBody sb = request.getSOAPBody();
            // System.out.println("Incoming SOAPBody: " + sb.getValue() );

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

