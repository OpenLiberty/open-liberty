/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.test;

import com.ibm.ws.classloading.exporting.test.TestInterface2;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * Class SpiVisibleRESTHandlerImpl is used to verify the server can load
 * BELL service classes that implement SPI exported by features.
 */
public class SpiVisibleRESTHandlerImpl implements TestInterface2, RESTHandler {

    public SpiVisibleRESTHandlerImpl() {
        System.out.println("SpiVisibilityRESTHandlerImpl.<ctor>: hello");
    }

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) {
        System.out.println("SpiVisibilityRESTHandlerImpl.handleRequest: hello");
    }

    @Override
    public String isThere2(String name) {
        return name + " is there, SPI impl class SpiVisibilityRESTHandlerImpl";
    }
}
