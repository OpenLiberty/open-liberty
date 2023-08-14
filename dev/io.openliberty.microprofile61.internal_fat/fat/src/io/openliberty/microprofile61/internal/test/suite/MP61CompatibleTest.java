/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package io.openliberty.microprofile61.internal.test.suite;

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
@Mode(TestMode.LITE)
public class MP61CompatibleTest {

    private static final String SERVER_NAME = "MP61CompatibleServer";

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
     * Just microProfile-6.1 ... Should always pass, a test
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void testMicroProfile61() throws Exception {
        MPCompatibilityTestUtils.runGetMethod(server, 200, "/helloworld/helloworld", MPCompatibilityTestUtils.MESSAGE);
    }
}
