/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.userbundle.myhandler;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class InHandler1 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in InHandler1 handleFault method");

    }

    @Override
    //add a new soap header and set Age property
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        //get HttpServletRequest
        HttpServletRequest httpRequest = msgctxt.getHttpServletRequest();
        System.out.println("in InHandler1 handlemessage method");
        if (httpRequest != null) {
            System.out.println("get Encoding in InHandler1:" + httpRequest.getCharacterEncoding());
            System.out.println("get Address in InHandler1:" + httpRequest.getLocalAddr());
        }
        String EType = msgctxt.getEngineType();
        String fType = msgctxt.getFlowType();
        System.out.println("get Engine.Type in InHandler1:" + EType);
        System.out.println("get Flow.Type in InHandler1:" + fType);
        SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);

        //get WSDL_OPERATION and WSDL_SERVICE
        System.out.println("get OperationName in InHandler1: " + soapmsgctxt.get(SOAPMessageContext.WSDL_OPERATION));
        System.out.println("get ServiceName in InHandler1: " + soapmsgctxt.get(SOAPMessageContext.WSDL_SERVICE));

        // add a new soap header
        SOAPMessage oldMsg = soapmsgctxt.getMessage();
        try {
            SOAPHeader header = oldMsg.getSOAPHeader();
            QName qname = new QName("http://www.webservice.com", "licenseInfo", "ns");
            header.addHeaderElement(qname).setValue("12345");
        } catch (Exception e1) {

            e1.printStackTrace();
        }

        //add one Property
        msgctxt.setProperty("age", 12);

    }

}
