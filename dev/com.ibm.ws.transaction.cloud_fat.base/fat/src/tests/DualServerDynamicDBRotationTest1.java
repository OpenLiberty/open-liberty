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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import servlets.Simple2PCCloudServlet;

@Mode
@RunWith(FATRunner.class)
@AllowedFFDC(value = { "javax.resource.spi.ResourceAllocationException" })
@SkipIfSysProp(SkipIfSysProp.OS_IBMI) //Skip on IBM i due to Db2 native driver in JDK
public class DualServerDynamicDBRotationTest1 extends DualServerDynamicCoreTest1 {

    @Server("com.ibm.ws.transaction_ANYDBCLOUD001")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_ANYDBCLOUD002")
    @TestServlet(servlet = Simple2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    public static String[] serverNames = new String[] {
                                                        "com.ibm.ws.transaction_ANYDBCLOUD001",
                                                        "com.ibm.ws.transaction_ANYDBCLOUD002",
    };

    @BeforeClass
    public static void setUp() throws Exception {
        TxTestContainerSuite.beforeSuite();
        setup(firstServer, secondServer, "Simple2PCCloudServlet", "cloud001");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        TxTestContainerSuite.afterSuite();
    }

    @After
    public void tearDown() throws Exception {
        tidyServersAfterTest(server1, server2);
    }
}
