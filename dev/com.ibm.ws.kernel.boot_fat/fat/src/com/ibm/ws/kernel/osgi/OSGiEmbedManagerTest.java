/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.osgi;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class OSGiEmbedManagerTest {

    private static final String SERVER_NAME = "com.ibm.ws.kernel.osgi.fat";

    private static final LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

    @BeforeClass
    public static void before() throws Exception {
        ShrinkHelper.defaultApp(server, "osgiEmbedManager", "com.ibm.ws.kernel.osgi.web.embed.manager");
        server.startServer();
    }

    @AfterClass
    public static void after() throws Exception {
        server.stopServer();
    }

    @Test
    /**
     * Verify an embedded OSGi framework instance starts within a web application
     */
    public void testWebEmbedFrameworkStarts() throws Exception {
        Assert.assertNotNull("The embedded framework started!", server.waitForStringInLogUsingMark("The embedded framework started!"));
    }

}
