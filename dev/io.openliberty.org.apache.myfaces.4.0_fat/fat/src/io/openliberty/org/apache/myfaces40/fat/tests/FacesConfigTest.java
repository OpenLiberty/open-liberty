/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.org.apache.myfaces40.fat.tests;

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
 * Faces 4.0 faces-config.xml version="4.0" test.
 *
 *
 * This test is run on a server that has an application deployed that contains a
 * faces-config.xml with a version element of 4.0.
 *
 * This test will ensure the application with the faces-config.xml we are testing
 * has been started.
 *
 * The test will ensure that the following exception is not found in the trace.log:
 *
 * CWWKC2261E: The FacesConfigTest.war : WEB-INF/faces-config.xml deployment descriptor on line 15 specifies unsupported version 4.0.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FacesConfigTest {
    private static final String APP_NAME = "FacesConfigTest";

    @Server("faces40_facesConfigServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");

        server.startServer(FacesConfigTest.class.getSimpleName() + ".log");
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
     * The test will ensure that the following exception is not found in the trace.log:
     *
     * CWWKC2261E: The FacesConfigTest.war : WEB-INF/faces-config.xml deployment descriptor on line 15 specifies unsupported version 4.0.
     *
     * @throws Exception
     */
    @Test
    public void testFacesConfigVersion40() throws Exception {
        assertTrue("The CWWKC2261E exception was found in the trace.log when it should not have been.",
                   server.findStringsInTrace("CWWKC2261E").isEmpty());
    }

}
