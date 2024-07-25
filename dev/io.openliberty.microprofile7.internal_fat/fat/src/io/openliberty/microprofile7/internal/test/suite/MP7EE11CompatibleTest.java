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
public class MP7EE11CompatibleTest {

    private static final String SERVER_NAME = "MP70andEE11";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        MPCompatibilityTestUtils.setUp(server);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        MPCompatibilityTestUtils.cleanUp(server);
    }

    /**
     * microProfile-7.0 plus jakartaee-11.0
     * Should pass.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP70andEE11() throws Exception {
        MPCompatibilityTestUtils.runGetMethod(server, 200, "/helloworld/helloworld", MPCompatibilityTestUtils.MESSAGE);
    }

}
