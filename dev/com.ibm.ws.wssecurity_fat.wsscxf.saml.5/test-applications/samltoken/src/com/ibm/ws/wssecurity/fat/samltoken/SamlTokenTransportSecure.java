/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.samltoken;
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
import com.ibm.ws.wssecurity.fat.samltoken.utils ;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import java.util.Iterator;

@WebServiceProvider(targetNamespace="http://wssec.basic.cxf.fats",
    portName="SamlTokenTransportSecurePort",
    serviceName="SamlTokenTransportSecure",
    wsdlLocation = "WEB-INF/SamlTokenWebSvc.wsdl"
)

@ServiceMode(value = Service.Mode.MESSAGE)

public class SamlTokenTransportSecure implements Provider<SOAPMessage> {


    public SOAPMessage invoke(SOAPMessage request) {

        SOAPMessage response = null;
        System.out.println("Just got into server side app");

        try {
            String hdrText = request.getSOAPHeader().getTextContent();
            System.out.println("Incoming SOAP Header:" + hdrText);
            
            StringReader respMsg = null ; 
            if (com.ibm.ws.wssecurity.fat.samltoken.utils.isSAMLAssertionInHeader(request)) {
                System.out.println("SAML Assertion found in SOAP Request Header") ;
                respMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><provider>This is WSSECFVT CXF SSL Web Service (using SAML).</provider></soapenv:Body></soapenv:Envelope>");
            } else {
                System.out.println("SAML Assertion was NOT found in SOAP Request Header") ;
                respMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://wssec.basic.cxf.fats/types\"><provider>SAML Assertion Missing in SSL Web Service.</provider></soapenv:Body></soapenv:Envelope>");                
            }

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

