/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile6.internal.test.suite;

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
public class MP6EE10CompatibleTest {

    private static final String SERVER_NAME = "MP6andEE10";

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
     * microProfile-6.0 plus jakartaee-10.0
     * At the moment, not all of the MP 6.0 features work with EE10 ... but they should before GA
     * The server.xml has been modified to only include those features which currently work
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP60andEE10() throws Exception {
        MPCompatibilityTestUtils.runGetMethod(server, 200, "/helloworld/helloworld", MPCompatibilityTestUtils.MESSAGE);
    }

}
