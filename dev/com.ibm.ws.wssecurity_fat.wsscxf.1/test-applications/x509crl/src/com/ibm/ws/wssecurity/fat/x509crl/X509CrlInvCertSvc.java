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

package com.ibm.ws.wssecurity.fat.x509crl;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ibm.ws.wssecurity.fat.utils.common.SharedTools;

@WebServiceProvider(targetNamespace="http://x509crl.wssecfvt.test",
    portName="X509CrlInvCert",
    serviceName="X509CrlInvCertService",
    wsdlLocation = "WEB-INF/X509Crl.wsdl"
)

@ServiceMode(value = Service.Mode.MESSAGE)

public class X509CrlInvCertSvc implements Provider<SOAPMessage> {


    public X509CrlInvCertSvc() {
        super();
        try {
            SharedTools.fixProviderOrder("X509CrlInvCertSvc");
        } catch (Exception e) {
            System.out.println("Failed to either view or update the Java provider list - this MAY cause issues later");
        }
    }    
    
    public SOAPMessage invoke(SOAPMessage request) {

        SOAPMessage response = null;

        try {

            System.out.println("Incoming Client Request as a SOAPMessage:");
            request.writeTo(System.out);

            String hdrText = request.getSOAPHeader().getTextContent();
            System.out.println("Incoming SOAP Header:" + hdrText);

            StringReader respMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://x509crl.wssecfvt.test/types\"><provider>This is X509CrlInvCertService Web Service.</provider></soapenv:Body></soapenv:Envelope>");

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

