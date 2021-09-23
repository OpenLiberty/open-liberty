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
package com.ibm.jaxws.MTOM;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPBinding;

@WebService(serviceName = "MTOMService", endpointInterface = "com.ibm.jaxws.MTOM.MTOMInter", targetNamespace = "http://MTOMService/")
@BindingType(value = SOAPBinding.SOAP11HTTP_MTOM_BINDING)
public class MTOMService implements MTOMInter {

    @Resource
    WebServiceContext wsc;

    @Override
    public byte[] getAttachment() {

        // Use existing test to verify that we can properly cast a the MessageContext to the SOAPMessageContext
        SOAPMessageContext smc = (SOAPMessageContext) wsc.getMessageContext();
        SOAPMessage message = smc.getMessage();
        System.out.println(message.getContentDescription());

        byte[] barr = new byte[10000];
        Random r = new Random();
        r.nextBytes(barr);
        return barr;
    }

    @Override
    public String sendAttachment(byte[] att) {

        StringBuffer response = new StringBuffer();

        // Check to see if the Content-Type header has a value containing
        // 'multipart/related; type="application/xop+xml"'
        // If it doesn't, then the request is not in MTOM format and we should
        // send back a response that indicates failure
        MessageContext mc = wsc.getMessageContext();

        if (mc == null) {
            response.append("ERROR: no MessageContext found.");
        }

        Map<String, ArrayList> reqHeaders = (Map<String, ArrayList>) mc.get(MessageContext.HTTP_REQUEST_HEADERS);
        ArrayList<String> ct = reqHeaders.get("Content-Type");
        System.out.println("Content-Type header:");
        for (Object obj : ct) {
            String content = (String) obj;
            System.out.println(content);
            if (content != null && (content.indexOf("multipart/related") > -1) && (content.indexOf("application/xop+xml") > 0)) {
                response.append("Expected value is in Content-Type header.");
            } else {
                response.append("ERROR: Content-Type header does not indicate MTOM");
            }
        }

        if (att != null) {
            response.append("\nSuccessfully received attachment!");
        } else {
            response.append("\nERROR: No attachment sent");
        }

        System.out.println("Returning response " + response.toString());
        return response.toString();
    }
}
