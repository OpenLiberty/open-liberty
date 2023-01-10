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
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class OutHandler3 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in OutHandler3 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        if (msgctxt.getFlowType() == HandlerConstants.FLOW_TYPE_OUT) {
            HttpServletRequest httpRequest = msgctxt.getHttpServletRequest();
            System.out.println("in OutHandler3 handlemessage method OutBound");
            if (httpRequest != null) {
                System.out.println(httpRequest.getCharacterEncoding());
                System.out.println(httpRequest.getLocalAddr());
            }

            SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);

            //get WSDL_OPERATION and WSDL_SERVICE
            System.out.println("OperationName: " + soapmsgctxt.get(SOAPMessageContext.WSDL_OPERATION));
            System.out.println("ServiceName: " + soapmsgctxt.get(SOAPMessageContext.WSDL_SERVICE));

            //add a new soap header
            SOAPMessage oldMsg = soapmsgctxt.getMessage();
            try {
                SOAPHeader header = oldMsg.getSOAPHeader();
                QName qname = new QName("http://www.webservice.com", "licenseInfo", "ns");
                header.addHeaderElement(qname).setValue("12345");
            } catch (SOAPException e1) {
                e1.printStackTrace();
            }

        } else {
            System.out.println("in OutHandler3 handlemessage() method InBound");

        }
    }

}
