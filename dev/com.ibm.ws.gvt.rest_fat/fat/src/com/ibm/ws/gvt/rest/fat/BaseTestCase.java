/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.gvt.rest.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * Abstract test class that can extended from concrete test classes to user common testing functionality.
 */
public abstract class BaseTestCase {

    protected static final String USER1_NAME = "user1";
    protected static final String USER1_PASSWORD = "user1Password";
    protected static final String ENDPOINT_NOTIFICATION = "/IBMJMXConnectorREST/notifications";
    protected static final String ENDPOINT_MBEAN = "/IBMJMXConnectorREST/mbeans";
    protected static final String DELIVERY_INTERVAL = "{\"deliveryInterval\": \"\\u0036\\u0030\\u0030\\u0030\\u0030\"}";

    protected static final String CLASS_NAME = "{\"className\": \"\\u0063\\u006f\\u006d\\u002e\\u0069\\u0062\\u006d\\u002e\\u0077\\u0065\\u0062\\u0073\\u0070\\u0068\\u0065\\u0072\\u0065\\u002e\\u0063\\u006f\\u006e\\u0066\\u0069\\u0067\\u002e\\u006d\\u0062\\u0065\\u0061\\u006e\\u0073\\u002e\\u0046\\u0065\\u0061\\u0074\\u0075\\u0072\\u0065\\u004c\\u0069\\u0073\\u0074\\u004d\\u0042\\u0065\\u0061\\u006e\"}";

    @Rule
    public final TestName testName = new TestName();

    @Before
    public void beforeTest() throws Exception {

        Log.info(getClass(), "beforeTest", ">>>>>>>>>>>>>>>>>>> ENTERING TEST " + getClass().getName() + "." + testName.getMethodName());
    }

    @After
    public void afterTest() throws Exception {

        Log.info(getClass(), "afterTest", "<<<<<<<<<<<<<<<<<<< EXITING TEST " + getClass().getName() + "." + testName.getMethodName());
    }

    /**
     * Stop the server with the expected exceptions.
     *
     * @param server             The server to stop.
     * @param expectedExceptions The expected exceptions.
     * @throws Exception if there was an error stopping the server.
     */
    protected static void stopServer(LibertyServer server, String... expectedExceptions) throws Exception {
        if (server != null && server.isStarted()) {
            try {
                server.stopServer(expectedExceptions);
            } catch (Exception e) {
                Log.error(BaseTestCase.class, "stopServer", e, "Encountered error stopping server " + server.getServerName());
                throw e;
            }
        }
    }

    /**
     * Wait for the HTTPS endpoint for the server to start.
     *
     * @param server The server to check the logs for the message.
     */
    protected static void waitForDefaultHttpsEndpoint(LibertyServer server) {
        assertNotNull("The SSL TCP Channel for default HTTPS endpoint did not start in time.",
                      server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl.*" + server.getHttpDefaultSecurePort()));
    }
}
