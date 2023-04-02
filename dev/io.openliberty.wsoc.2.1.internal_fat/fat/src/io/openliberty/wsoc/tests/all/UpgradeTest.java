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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.ClientHelper;
import io.openliberty.wsoc.util.wsoc.TestWsocContainer;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import junit.framework.Assert;

/**
 * Contains test details (such as client endpoint, server uri endpoint, input, and expected output).
 * These test are invoked by Basic21Test class. 
 */
public class UpgradeTest {

    private WsocTest wsocTest = null;

    public UpgradeTest(WsocTest test) {
        this.wsocTest = test;
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - ByteBufferTest
     */
    public void testUpgradeViaServletToWS() throws Exception {

        String[] input1 = { "echoValue" };
        String[] output1 = { "echoValue" };
        //server endpoint uri is /annotatedByteArray/{boolean-var}
        String uri = "/basic21/upgradeEcho";
        wsocTest.runEchoTest(new ClientHelper.BasicClientEP(input1), uri, output1);

    }
}
