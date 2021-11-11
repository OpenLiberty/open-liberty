/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import app1.TestServletA;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;

@RunWith(FATRunner.class)
public class TestWithFATServlet2 extends FATServletClient {

    public static final String APP_NAME = "app1";

    @Server("FATServer")
    @TestServlet(servlet = TestServletA.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "app1.web");
    }

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(Phase.APPLICATIONS);
        server.startServer();
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }

}
