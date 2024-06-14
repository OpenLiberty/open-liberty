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

import org.jboss.shrinkwrap.api.Archive;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import batch.fat.util.BatchFATHelper;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import com.ibm.ws.jbatch.test.FatUtils;
import componenttest.annotation.CheckpointTest;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
public class BasicJDBCPersistenceTestCheckpoint extends BatchFATHelper {

    @BeforeClass
    @CheckpointTest
    public static void setup() throws Exception {

        BatchFATHelper.setConfig("commonCheckpoint/server.xml", BasicJDBCPersistenceTestCheckpoint.class);

        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);
        
        
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, true, null);
        server.startServer();
        FatUtils.waitForSmarterPlanet(server);

        //wait for the security keys get generated.
        FatUtils.waitForLTPA(server);

        createDefaultRuntimeTables();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testBasicJDBCPersistence() throws Exception {
        test("Basic", "jslName=BasicPersistence");
    }

}
