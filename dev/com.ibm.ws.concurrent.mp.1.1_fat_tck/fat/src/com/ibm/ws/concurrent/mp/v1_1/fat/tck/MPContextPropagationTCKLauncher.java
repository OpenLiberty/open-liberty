/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.v1_1.fat.tck;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

@RunWith(FATRunner.class)
public class MPContextPropagationTCKLauncher {

    @Server("tckServerForMPContextPropagation11")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @AllowedFFDC({ "java.lang.IllegalStateException", // transaction cannot be propagated to 2 threads at the same time
                   "java.lang.NegativeArraySizeException", // intentionally raised by test case to simulate failure during completion stage action
                   "org.jboss.weld.contexts.ContextNotActiveException" // expected when testing TransactionScoped bean cannot be accessed outside of transaction
    })
    @Test
    public void launchMPContextPropagation_1_1_Tck() throws Exception {
        // TODO remove the guard once a copy of the v1.1 TCK is available other than a local snapshot build
        if (FATRunner.FAT_TEST_LOCALRUN)
            MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.concurrency.mp.1.1_fat_tck", this.getClass() + ":launchMPContextPropagationTck");
    }
}
