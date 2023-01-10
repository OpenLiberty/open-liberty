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
package com.ibm.ws.userbundle5.myhandler;

import java.io.PrintStream;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class OutHandler1InBuldle5 implements Handler {
    private static PrintStream out = System.out;

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in OutHandler1 handleFault method");

    }

    @Override
    //add a new soap header and set Age property in Client's Out Flow
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("########################################");
        System.out.println("in OutHandler1 handlemessage method");
        String EType = msgctxt.getEngineType();
        String fType = msgctxt.getFlowType();
        System.out.println("get Engine.Type in OutHandler1:" + EType);
        System.out.println("get Flow.Type in OutHandler1:" + fType);
        SOAPMessageContext soapmsgctxt = msgctxt.adapt(SOAPMessageContext.class);

        //get WSDL_OPERATION and WSDL_SERVICE  
        System.out.println("get ServiceName in OutHandler1: " + soapmsgctxt.get(SOAPMessageContext.WSDL_SERVICE));
        System.out.println("get OperationName in OutHandler1: " + soapmsgctxt.get(SOAPMessageContext.WSDL_OPERATION));

        System.out.println("#####################iiiiiiiiiiiiiiiiiiii###################");
        System.out.println("get OperationName in InHandler1: " + msgctxt.getProperty(javax.xml.ws.handler.MessageContext.WSDL_OPERATION).toString());
        System.out.println("get ServiceName in InHandler1: " + msgctxt.getProperty(javax.xml.ws.handler.MessageContext.WSDL_SERVICE).toString());
        System.out.println("######################iiiiiiiiiiiiiiiiiiiiii##################");

        QName serviceQName = new QName("http://provider.jaxws.ws.ibm.com/", "AddNumbers");
        QName opQName = new QName("http://provider.jaxws.ws.ibm.com/", "addNumbers");
        System.out.println("get ServiceName in OutHandler1 " + soapmsgctxt.get(SOAPMessageContext.WSDL_SERVICE).equals(serviceQName));
        System.out.println("get OperationName in OutHandler1 " + soapmsgctxt.get(SOAPMessageContext.WSDL_OPERATION).equals(opQName));

        // add a new soap header in Client's Out Flow
        if (msgctxt.isClientSide() && msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            SOAPMessage oldMsg = soapmsgctxt.getMessage();
            SOAPEnvelope soapEnv = oldMsg.getSOAPPart().getEnvelope();
            SOAPHeader header = soapEnv.getHeader();
            try {
                out.println("soapmsgctxt.getMessage() =" + soapmsgctxt.getMessage());
                if(header == null) {
                    header = soapEnv.addHeader();
                }
                out.println("header ="+ header);
                QName qname = new QName("http://www.webservice.com", "licenseInfo", "ns");
                out.println("qname ="+ qname);
                header.addHeaderElement(qname).setValue("12345");
            } catch (Exception e1) {

                e1.printStackTrace();
                out.println("failuer when adding soapheader in OutHandler1InBuldle5");
            }

            //add one Property
            msgctxt.setProperty("age", 12);
        }

        //check result in server's outFlow to see whether the adding result is correct
        if (msgctxt.isServerSide() && msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            SOAPMessage message = soapmsgctxt.getMessage();
            if (checkResult(message))
            {
                out.println("got adding result 5007");
            }
        }

    }

    private boolean checkResult(SOAPMessage message)

    {
        try {
            SOAPBody soapBody = message.getSOAPBody();
            String result = soapBody.getElementsByTagName("return").item(0).getFirstChild().getTextContent();
            return result.equalsIgnoreCase("Result = 5007");
        } catch (SOAPException e) {

            return false;
        }

    }

}
