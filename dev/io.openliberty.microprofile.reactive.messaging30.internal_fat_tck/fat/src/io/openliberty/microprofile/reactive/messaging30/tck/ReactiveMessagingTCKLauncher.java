/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.reactive.messaging30.tck;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 */
@RunWith(FATRunner.class)
public class ReactiveMessagingTCKLauncher {

    public static final String SERVER_NAME = "ReactiveMessaging30TCKServer";

    @ClassRule
    public static RepeatTests r = FATSuite.repeatDefault(SERVER_NAME);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ000[24]E"); // Ignore app start errors - there are lots of tests for invalid apps
    }

    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "org.jboss.weld.exceptions.DeploymentException", "com.ibm.ws.container.service.state.StateChangeException" }) // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchReactiveMessaging30Tck() throws Exception {
        String bucketName = "io.openliberty.microprofile.reactive.messaging30.internal_fat_tck";
        String testName = this.getClass() + ":launchReactiveMessaging30Tck";
        Type type = Type.MICROPROFILE;
        String specName = "Reactive Messaging";
        TCKRunner.runTCK(server, bucketName, testName, type, specName);
    }

}
