/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.opentracing.internal.tck;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 */
@RunWith(FATRunner.class)
public class OpentracingRestClientTCKLauncher {
	
	final static String SERVER_NAME = "OpentracingRestClientTCKServer";

//    @ClassRule
//    public static RepeatTests r = RepeatTests.withoutModification()
//        .andWith(FATSuite.MP_REST_CLIENT("1.3", SERVER_NAME))
//        .andWith(FATSuite.MP_REST_CLIENT("1.4", SERVER_NAME));
    
    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    /*
     * CWWKG0014E - Ignore due to server.xml intermittently missing
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKG0014E", "CWPMI2005W");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchOpentracingRestClientTck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "io.openliberty.opentracing.2.0.internal_fat", this.getClass() + ":launchOpentracingRestClientTck", "rest-client-tck-suite.xml", Collections.emptyMap(), Collections.emptySet());
    }
}
