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
package io.openliberty.wsoc.tests.all;

import io.openliberty.wsoc.endpoints.client.basic.ClientHelper;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 * Contains test details (such as client endpoint, server uri endpoint, input, and expected output).
 * These test are invoked by Basic21Test class. 
 */
public class TimeOutTest {

    private WsocTest wsocTest = null;

    public TimeOutTest(WsocTest test) {
        this.wsocTest = test;
    }

    public void testZeroTimeOut() throws Exception {
        String[] input1 = { "Text1" };
        String[] output1 = { "0" }; // timeout value 

        String uri = "/basic21/zeroTimeout";
        wsocTest.runEchoTest(new ClientHelper.BasicClientEP(input1), uri, output1);
    }
    
    public void testNegativeTimeOut() throws Exception {
        String[] input1 = { "Text1" };
        String[] output1 = { "-12" };  // timeout value 

        String uri = "/basic21/negativeTimeout";
        wsocTest.runEchoTest(new ClientHelper.BasicClientEP(input1), uri, output1);
    }
}
