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
package com.ibm.ws.userbundle3.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class TestHandler1InBundle3 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("handle fault in TestHandler1InBundle3");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("#####################iiiiiiiiiiiiiiiiiiii###################");
        System.out.println("get OperationName in TestHandler1InBundle3: " + msgctxt.getProperty(javax.xml.ws.handler.MessageContext.WSDL_OPERATION).toString());
        System.out.println("get OperationName in TestHandler1InBundle3: " + msgctxt.getProperty(javax.xml.ws.handler.MessageContext.WSDL_SERVICE).toString());
        System.out.println("######################iiiiiiiiiiiiiiiiiiiiii##################");

        if (msgctxt.getFlowType().equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            System.out.println("handle outbound message in TestHandler1InBundle3");
        } else {
            System.out.println("handle inbound message in TestHandler1InBundle3");

        }

    }

}
