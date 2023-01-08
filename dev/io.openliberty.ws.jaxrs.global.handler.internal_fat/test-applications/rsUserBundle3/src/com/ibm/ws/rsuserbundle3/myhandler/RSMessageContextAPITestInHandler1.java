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

public class RSMessageContextAPITestInHandler1 implements Handler {

    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in RSMessageContextAPITestInHandler1 handleFault method");

    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {

        System.out.println("in RSMessageContextAPITestInHandler1 handleMessage method");
        String engineType = msgctxt.getEngineType();
        String flowType = msgctxt.getFlowType();
        boolean isServerSide = msgctxt.isServerSide();
        boolean isClientSide = msgctxt.isClientSide();
        msgctxt.setProperty("TestProperty1", "TestProperty1Value");
        msgctxt.setProperty("TestProperty2", "TestProperty2Value");
        msgctxt.setProperty("TestProperty3", "TestProperty3Value");
        msgctxt.setProperty("TestProperty4", "TestProperty4Value");
        msgctxt.removeProperty("TestProperty2");
        boolean containTestProperty2 = msgctxt.containsProperty("TestProperty2Value");
        System.out.println("get Engine.Type in RSMessageContextAPITestInHandler1:" + engineType);
        System.out.println("get Flow.Type in RSMessageContextAPITestInHandler1:" + flowType);
        System.out.println("get isServerSide in RSMessageContextAPITestInHandler1:" + isServerSide);
        System.out.println("get isClientSide in RSMessageContextAPITestInHandler1:" + isClientSide);
        System.out.println("get containTestProperty2 in RSMessageContextAPITestInHandler1:" + containTestProperty2);
    }

}
