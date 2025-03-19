/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc22.tests;

import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.annotation.ExpectedFFDC;

/*
 * Verifies OnMessage.maxMessageSize throws DeploymentException per Spec #274
 * https://github.com/jakartaee/websocket/issues/274
 */
@RunWith(FATRunner.class)
public class Spec274MaxMessageTest {

    @Server("maxMessageTestServer")
    public static LibertyServer LS;

    private static final Logger LOG = Logger.getLogger(Spec274MaxMessageTest.class.getName());

    private static final String MAX_MESSAGE_WAR_NAME = "maxMessage";

    /*
     * Tests that a DeploymentException is thrown due to maxMessageSize being larger
     * than Integer.MAX_VALUE in EchoServerEP. The other ExpectedFFDCs are associated
     * with DeploymentException disrupting the starting of the server.
     */
    @Test
    @ExpectedFFDC(value = {"jakarta.websocket.DeploymentException", "com.ibm.ws.container.service.state.StateChangeException", "java.lang.RuntimeException"})
    public void testMaxMessage() throws Exception {
        try {
            ShrinkHelper.defaultDropinApp(LS, MAX_MESSAGE_WAR_NAME + ".war", "io.openliberty.wsoc.spec274");
            LS.startServer(Spec274MaxMessageTest.class.getSimpleName() + ".log");
        } catch (Exception e){
            LS.waitForStringInLog("jakarta.websocket.DeploymentException: CWWKH0056E");
        } finally {
            LS.stopServer("CWWKH0056E", "CWWKZ0002E");
        }
    }

}
