/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import io.openliberty.wsoc.endpoints.client.basic.ClientHelper;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 *  Miscellaneous tests 
 */
public class MiscTest {

    private WsocTest wsocTest = null;

    public MiscTest(WsocTest test) {
        this.wsocTest = test;
    }

    public void testGetRequestURIReturnsFullURI() throws Exception {
        String uri = "/basic21/GetRequestURI";
        String expectedURI = wsocTest.getServerUrl(uri);

        String[] input = { "anyValue" }; // timeout value 
        String[] output1 = { expectedURI }; // timeout value 

        wsocTest.runEchoTest(new ClientHelper.BasicClientEP(input), uri, output1);
    }

    public void testVerifyDefaultConfigurator() throws Exception {

        // Simple test to verify that the getContainerDefaultConfigurator method can be accessed. In reality
        // if the method couldn't be accessed we would expect a compilation error rather than a runtime exception
        String uri = "/basic21/testEchoConfigurator";

        String[] input1 = { "echoValue" };
        String[] output1 = { "echoValue" };

        wsocTest.runEchoTest(new ClientHelper.BasicClientEP(input1), uri, output1);

    }

}
