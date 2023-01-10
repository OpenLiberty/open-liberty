/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.untoken;

import java.io.StringReader;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@WebServiceProvider(targetNamespace = "http://wssec.basicssl.cxf.fats",
                    serviceName = "FVTVersionBAService", portName = "UrnBasicPlcyBA",
                    wsdlLocation = "WEB-INF/BasicPlcyBA.wsdl")

@ServiceMode(value = Service.Mode.MESSAGE)

/**
 * Server side implementation of Web Services Security tests.
 * Contains invoke method called by clients which returns
 * WssecfvtConst.TEST_STRING_OUT.
 * 
 * @author Todd Roling
 */
public class FVTVersion_ba01 implements javax.xml.ws.Provider<SOAPMessage> {

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.ws.Provider#invoke(java.lang.Object)
     */
    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        SOAPMessage response = null;
        try {
//            System.out.println("Incoming Client Request as a SOAPMessage");
            // SOAPBody sb = request.getSOAPBody();
            // System.out.println("Incoming SOAPBody: " + sb);
//            MessageFactory factory = MessageFactory.newInstance();
//            InputStream is = getClass().getResourceAsStream("NoWssecResp.xml");
//            response = factory.createMessage(null, is);
//            is.close();

            System.out.println("Incoming Client Request as a SOAPMessage:");
            request.writeTo(System.out);

            //issue 23060
            //Retrieve mustUnderstand variable if exists from the request message soap header
            Node n = request.getSOAPHeader().getFirstChild();
            NamedNodeMap nmap = n.getAttributes();
            Node mu = null;
            if (nmap != null) {
                mu = nmap.item(0);
            }
            String muheader = "soapenv:mustUnderstand=\"1\"";
            boolean muheader_exists = true;
            muheader_exists = muheader.equalsIgnoreCase(mu.toString());
            StringReader respMsg;
            if (muheader_exists) {
                //default behavior, mustunderstand="1"
                respMsg = new StringReader("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><provider><message>WSSECFVT FVTVersion_ba01</message></provider></SOAP-ENV:Body></SOAP-ENV:Envelope>");
            } else {
                // if we set ws-security.must-understand="false" explicitly in server.xml , then this header should not exist
                respMsg = new StringReader("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><provider><message>WSSECFVT FVTVersion_ba01_NO_mustUnderstand_in_header_expected</message></provider></SOAP-ENV:Body></SOAP-ENV:Envelope>");
            }

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
