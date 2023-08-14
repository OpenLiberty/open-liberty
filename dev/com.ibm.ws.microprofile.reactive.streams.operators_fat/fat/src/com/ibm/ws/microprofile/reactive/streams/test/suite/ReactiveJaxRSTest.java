/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.test.suite;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class ReactiveJaxRSTest extends FATServletClient {

    public static final String SERVER_NAME = "ReactiveJaxRSTestServer";

    @ClassRule
    public static RepeatTests r = FATSuite.repeat(SERVER_NAME, TestMode.LITE, FATSuite.MPRS10, FATSuite.MPRS30_MP60);

    public static final String APP_NAME = "ReactiveWithJaxRS";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name APP_NAME.war once it's written to a file
        // Include the 'APP_NAME.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.reactive.streams.test.jaxrs");

        ShrinkHelper.exportDropinAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testReactiveChainWithJaxRS() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/ReactiveWithJaxRS/input?message=message1", FATServletClient.SUCCESS);
        HttpUtils.findStringInReadyUrl(server, "/ReactiveWithJaxRS/input?message=message2", FATServletClient.SUCCESS);
        HttpUtils.findStringInReadyUrl(server, "/ReactiveWithJaxRS/input?message=message3", FATServletClient.SUCCESS);
        HttpUtils.findStringInReadyUrl(server, "/ReactiveWithJaxRS/input?message=message4", FATServletClient.SUCCESS);
        HttpUtils.findStringInReadyUrl(server, "/ReactiveWithJaxRS/input?message=message5", FATServletClient.SUCCESS);

        HttpUtils.findStringInReadyUrl(server, "/ReactiveWithJaxRS/output?count=5", "message1", "message2", "message3", "message4", "message5");
    }

}
