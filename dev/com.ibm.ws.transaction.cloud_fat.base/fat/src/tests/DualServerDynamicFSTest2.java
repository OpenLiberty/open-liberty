/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
public class DualServerDynamicFSTest2 extends DualServerDynamicCoreTest2 {

    @Server("com.ibm.ws.transaction_FSCLOUD001")
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_FSCLOUD002")
    public static LibertyServer secondServer;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_FSCLOUD001",
                                                        "com.ibm.ws.transaction_FSCLOUD002",
    };

    @BeforeClass
    public static void setUp() throws Exception {
        setup(firstServer, secondServer, "SimpleFS2PCCloudServlet", "FScloud001");
    }

    @Override
    public void setUp(LibertyServer server) throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        tidyServersAfterTest(server1); // server2 is already stopped
    }
}
