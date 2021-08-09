/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.tck;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 */
@RunWith(FATRunner.class)
public class ReactiveMessagingTCKLauncher {

    @Server("ReactiveMessagingTCKServer")
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
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchReactiveMessagingTck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.microprofile.reactive.messaging_fat_tck", this.getClass() + ":launchReactiveMessagingTck");
    }

}
