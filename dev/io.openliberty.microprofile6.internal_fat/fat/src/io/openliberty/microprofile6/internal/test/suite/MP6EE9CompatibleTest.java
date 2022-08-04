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
public class MP6EE9CompatibleTest extends MPCompatibilityTestUtils {

    private static final String SERVER_NAME = "MP6andEE9";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        setUp(server);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        cleanUp(server);
    }

    /**
     * microProfile-6.0 plus jakartaee-9.1
     * This should work for now but compatibility should be removed before GA
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMP60andEE9() throws Exception {
        runGetMethod(server, 200, "/helloworld/helloworld", MESSAGE);
    }
}
