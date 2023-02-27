/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class CheckpointWithSecurityManager {
    @Rule
    public TestName testName = new TestName();

    @Server("checkpointWithSecurityManager")
    public static LibertyServer server;

    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testAtApplicationsMultRestore() throws Exception {
        server.setCheckpoint(new CheckpointInfo(CheckpointPhase.APPLICATIONS, false, true, true, null));
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int retureCode = output.getReturnCode();
        assertEquals("Wrong return code for failed checkpoint.", 72, retureCode);
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
