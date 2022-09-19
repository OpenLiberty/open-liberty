/*******************************************************************************
 * Copyright (c) 2018,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.v1_3.fat.tck;

import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

@RunWith(FATRunner.class)
public class MPContextPropagationTCKLauncher {

    private static final String SERVER_NAME = "tckServerForMPContextPropagation13";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP60, MicroProfileActions.MP50);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0014W"); // Updates after the app is deleted. Can occur due to Arquillian use.
    }

    @AllowedFFDC({ "java.lang.IllegalStateException", // transaction cannot be propagated to 2 threads at the same time
                   "java.lang.NegativeArraySizeException", // intentionally raised by test case to simulate failure during completion stage action
                   "org.jboss.weld.contexts.ContextNotActiveException" // expected when testing TransactionScoped bean cannot be accessed outside of transaction
    })
    @Test
    public void launchMPContextPropagation_1_3_Tck() throws Exception {
        // TODO use this to only test with local build (when tckRunner/tck.pom.xml specifies a #.#-SNAPSHOT version)
        //if (FATRunner.FAT_TEST_LOCALRUN)
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.concurrency.mp.1.3_fat_tck", this.getClass() + ":launchMPContextPropagationTck");
        Map<String, String> resultInfo = MvnUtils.getResultInfo(server);
        resultInfo.put("results_type", "MicroProfile");
        resultInfo.put("feature_name", "Context Propogation");
        resultInfo.put("feature_version", "1.3");
        MvnUtils.preparePublicationFile(resultInfo);
    }
}
