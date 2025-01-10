/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jbatch.test.FatUtils;
import com.ibm.ws.jbatch.test.BatchAppUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class CDITestCheckpoint extends BatchFATHelper {

    private static final Class testClass = CDITestCheckpoint.class;
    // save off the original server value so it can be restored after class
    private static final LibertyServer superServer = BatchFATHelper.server;

    @BeforeClass
    public static void setup() throws Exception {

        server = LibertyServerFactory.getLibertyServer("checkpointbatchFAT");

        BatchAppUtils.addDropinsBatchFATWar(server);
        
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, null);
        BatchFATHelper.startServer(server, testClass);
        FatUtils.waitForSmarterPlanet(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKY0041W");
        }
        // restore back to original server
        server = superServer;
    }

    @Test
    public void testSelfValidatingJobWithCDIJobListener() throws Exception {
        // Self-validating (if job completes, test passes).
        test("Basic", "jslName=CDIJobListener");
    }
}
