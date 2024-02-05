/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat;

import static org.junit.Assert.assertNotNull;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fats.cxf.jaxws22.mtom.client.MTOMAnnotationsTestServlet;

/**
 * Migrated from tWAS based com.ibm.ws.jaxws_fat jaxws22/mtom/** bucket
 *
 * These MTOM tests are divided in to two parts:
 *
 * 1.) The first set of tests in this bucket are testing the jaxws22 spec
 * creation of Services given a variety of Arguments and using MTOMFeature to create
 * the service. These test have relatively low value, but since they do test part of
 * the jaxws-2.2 spec and were relatively easy to port they were ported.
 *
 * 2.) The second set of tests in this bucket are testing actual end-to-end scenarios with
 * Messages being sent with MTOM enabled/disabled.
 *
 * The later test
 * Original Class Explanation from tWAS Buckets:
 * This class will test that the new factory methods in jaxws 2.2 work
 * and that conform statements are met.
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MtomAnnotationsTest extends FATServletClient {

    private static final String APP_NAME1 = "jaxws22mtom";

    @Server("com.ibm.ws.jaxws22.mtom_fat")
    @TestServlet(servlet = MTOMAnnotationsTestServlet.class, contextRoot = APP_NAME1)
    public static LibertyServer server;

    private final Class<?> thisClass = MtomAnnotationsTest.class;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app1 = ShrinkHelper.buildDefaultApp(APP_NAME1, "fats.cxf.jaxws22.mtom.server", "fats.cxf.jaxws22.mtom.client");

        ShrinkHelper.exportDropinAppToServer(server, app1);

        server.startServer();
        System.out.println("Starting Server");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME1));

        return;

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}