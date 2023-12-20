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
package com.ibm.ws.rsuserbundle3.myhandler;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class RSMessageContextAPITestOutHandler1 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in RSMessageContextAPITestOutHandler1 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in RSMessageContextAPITestOutHandler1 handleMessage method");
        String engineType = msgctxt.getEngineType();
        String flowType = msgctxt.getFlowType();
        boolean isServerSide = msgctxt.isServerSide();
        boolean isClientSide = msgctxt.isClientSide();
        boolean containTestProperty4 = msgctxt.containsProperty("TestProperty4Value");
        System.out.println("get Engine.Type in RSMessageContextAPITestOutHandler1:" + engineType);
        System.out.println("get Flow.Type in RSMessageContextAPITestOutHandler1:" + flowType);
        System.out.println("get isServerSide in RSMessageContextAPITestOutHandler1:" + isServerSide);
        System.out.println("get isClientSide in RSMessageContextAPITestOutHandler1:" + isClientSide);
        System.out.println("get containTestProperty4 in RSMessageContextAPITestOutHandler1:" + containTestProperty4);
    }

}
