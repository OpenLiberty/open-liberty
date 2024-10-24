/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.test;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Verify that the following error is not found in the trace log when using a faces-config.xml with
 * a version of 4.1:
 *
 * CWWKC2261E: The FacesConfigVersion41.war : WEB-INF/faces-config.xml deployment descriptor on line 15 specifies unsupported version 4.1.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FacesConfigVersion41Test {
    private static final String APP_NAME = "FacesConfigVersion41";

    @Server("faces41_FacesConfigVersion_41_Server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(FacesConfigVersion41Test.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     *
     * Verify that the following exception is not found in the trace.log:
     *
     * CWWKC2261E: The FacesConfigVersion41.war : WEB-INF/faces-config.xml deployment descriptor on line 15 specifies unsupported version 4.1.
     *
     * @throws Exception
     */
    @Test
    public void testFacesConfigVersion41() throws Exception {
        assertTrue("The CWWKC2261E exception was found in the trace.log when it should not have been.",
                   server.findStringsInTrace("CWWKC2261E").isEmpty());
    }
}
