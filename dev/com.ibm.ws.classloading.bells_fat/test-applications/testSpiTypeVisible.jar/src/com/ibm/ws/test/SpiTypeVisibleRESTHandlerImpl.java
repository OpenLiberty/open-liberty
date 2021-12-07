/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Class SpiTypeVisibleRESTHandlerImpl is used to verify the server can load
 * BELL service classes that implement SPI exported by features.
 */
public class SpiTypeVisibleRESTHandlerImpl implements TestInterface2, RESTHandler {

    public SpiTypeVisibleRESTHandlerImpl() {
        System.out.println("SpiTypeVisibilityRESTHandlerImpl.<ctor>: hello");
    }

    @Override
    public void handleRequest​(RESTRequest request, RESTResponse response) {
        System.out.println("SpiTypeVisibilityRESTHandlerImpl.handleRequest: hello");
    }

    @Override
    public String isThere2(String name) {
        return name + " is there, SPI impl class SpiTypeVisibilityRESTHandlerImpl";
    }
}