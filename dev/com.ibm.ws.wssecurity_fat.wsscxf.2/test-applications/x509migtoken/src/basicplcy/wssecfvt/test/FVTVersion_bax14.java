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

package basicplcy.wssecfvt.test;

import java.io.StringReader;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(targetNamespace = "http://x509mig.libertyfat.test/contract",
                    serviceName = "FatBAX14Service", portName = "UrnX509Token14",
                    wsdlLocation = "WEB-INF/x509migtoken.wsdl")

@ServiceMode(value = Service.Mode.MESSAGE)

/**
 * Server side implementation of Web Services Security tests.
 * Contains invoke method called by clients which returns
 * WssecfvtConst.TEST_STRING_OUT.
 * 
 * @author Todd Roling
 */
public class FVTVersion_bax14 implements javax.xml.ws.Provider<SOAPMessage> {

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Provider#invoke(java.lang.Object)
     */
    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        SOAPMessage response = null;
        try {
            System.out.println("Incoming Client Request as a SOAPMessage(mig bax14)");
            //SOAPBody sb = request.getSOAPBody();
            //System.out.println("Incoming SOAPBody: " + sb);
            StringReader respMsg = new StringReader("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                                    "<SOAP-ENV:Body><provider><message>LIBERTYFAT X509 bax14</message></provider></SOAP-ENV:Body>" +
                                                    "</SOAP-ENV:Envelope>");
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
