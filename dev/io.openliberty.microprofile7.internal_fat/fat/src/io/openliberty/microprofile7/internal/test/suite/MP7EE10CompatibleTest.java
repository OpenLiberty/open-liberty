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

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MP7EE10CompatibleTest {

    private static final String SERVER_NAME = "MP70andEE10";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        MPCompatibilityTestUtils.setUp(server);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        //I think CWWWC0002W is a mpGraphQL-1.0 bug and should not appear
        //See issue 15496
        MPCompatibilityTestUtils.cleanUp(server, "CWWWC0002W");
    }

    /**
     * microProfile-7.0 plus jakartaee-10.0
     * Should pass.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP70andEE10() throws Exception {
        MPCompatibilityTestUtils.runGetMethod(server, 200, "/helloworld/helloworld", MPCompatibilityTestUtils.MESSAGE);
    }

}
