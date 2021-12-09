/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.web.SimpleFS2PCCloudServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
public class DualServerDynamicFSTest1 extends DualServerDynamicCoreTest1 {
    @Server("com.ibm.ws.transaction_FSCLOUD001")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_FSCLOUD002")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    @BeforeClass
    public static void setUp() throws Exception {
        setup(firstServer, secondServer, "SimpleFS2PCCloudServlet", "FScloud001");
    }

    @Override
    protected void setUp(LibertyServer server) throws Exception {
    }
}
