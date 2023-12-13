/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxrs.fat.globalhandler.simple.userbundle;

import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;

public class MySimpleHandler implements Handler {
    @Override
    public void handleFault(GlobalHandlerMessageContext arg0) {
        System.out.println("in MyHandler handleFault method");
    }

    @Override
    public void handleMessage(GlobalHandlerMessageContext msgctxt) throws Exception {
        System.out.println("Hello from MyHandler!");
    }
}
