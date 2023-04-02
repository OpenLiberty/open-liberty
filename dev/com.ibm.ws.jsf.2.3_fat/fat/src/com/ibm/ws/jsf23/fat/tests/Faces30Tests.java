/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Faces 3.0 test cases.
 */
@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
@Mode(TestMode.FULL)
public class Faces30Tests {

    @Server("faces30Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "Faces30FacesConfigTest.war");

        server.startServer(Faces30Tests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test is run on a server that has an application deployed that contains a
     * faces-config.xml with a version element of 3.0.
     *
     * This test will ensure the application with the faces-config.xml we are testing
     * has been started.
     *
     * The test will ensure that the following exception is not found in the trace.log:
     *
     * CWWKC2262E: The server is unable to process the 3.0 version and the
     * https://jakarta.ee/xml/ns/jakartaee namespace in the /WEB-INF/faces-config.xml
     * deployment descriptor on line 15.
     *
     * @throws Exception
     */
    @Test
    public void testFacesConfigVersion30() throws Exception {
        assertTrue("The CWWKC2262E exception was found in the trace.log when it should not have been.",
                   server.findStringsInTrace("CWWKC2262E").isEmpty());
    }

}
