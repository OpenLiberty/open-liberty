/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile7.internal.test.suite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MP7EE9CompatibleTest {

    private static final String SERVER_NAME = "MP70andEE9";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        MPCompatibilityTestUtils.setUp(server, false); //let the test start the server
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        MPCompatibilityTestUtils.cleanUp(server); //not passing any messages to ignore because actually the test should have already stopped the server
    }

    /**
     * microProfile-7.0 plus jakartaee-9.0
     * Should fail because microProfile-7.0 is not compatible with jakartaee-9.0
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testMP70andEE9() throws Exception {
        try {
            server.startServerAndValidate(true, //preClean
                                          false, //cleanStart
                                          false, //validateApps - false because the app won't start without the features
                                          false, //expectStartFailure - false because although the features won't start, the server as a whole still does
                                          false); //validateTimedExit
        } finally {
            MPCompatibilityTestUtils.validateMessagesAndStop(server,
                                                             "CWWKF0033E: The singleton features .* and .* cannot be loaded at the same time",
                                                             "CWWKF0047E: The .* and .* features cannot be loaded at the same time", //note that an EE9/EE10 clash gives us 47E rather than 44E which we had for a EE8/EE9 clash
                                                             "CWWKF0046W: The configuration includes an incompatible combination of features");
        }
    }
}
