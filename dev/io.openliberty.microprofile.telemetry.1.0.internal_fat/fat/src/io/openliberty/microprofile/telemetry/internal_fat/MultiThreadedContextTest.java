/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.context.MultiThreadedContextServlet;

/**
 * Check that the Context API can be used to transfer context between threads when submitting tasks to a ManagedExecutor,
 * and child spans can be created within the async task.
 *
 * We don't have to transfer this context automatically, but the user should be able to transfer it manually using the Context API
 */
@RunWith(FATRunner.class)
public class MultiThreadedContextTest extends FATServletClient {

    public static final String CONTEXT_TEST_APP_NAME = "contextTest";
    public static final String SERVER_NAME = "Telemetry10Context";

    @TestServlets({
                    @TestServlet(contextRoot = CONTEXT_TEST_APP_NAME, servlet = MultiThreadedContextServlet.class)

    })

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {

        // API test app
        WebArchive apiTestWar = ShrinkWrap.create(WebArchive.class, CONTEXT_TEST_APP_NAME + ".war")
                        .addClass(MultiThreadedContextServlet.class);

        ShrinkHelper.exportAppToServer(server, apiTestWar, SERVER_ONLY);
        server.startServer();
    }
    
    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);
    
    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }
}
