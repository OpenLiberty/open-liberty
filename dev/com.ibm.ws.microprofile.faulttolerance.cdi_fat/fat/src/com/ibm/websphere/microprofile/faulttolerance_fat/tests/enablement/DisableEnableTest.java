/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests supplement the (fairly exhaustive) annotation enablement tests in the TCK
 *
 * They pick out specific liberty behaviour (like log messages) and also some specific edge cases we've tripped over in our implementation
 */
@RunWith(FATRunner.class)
public class DisableEnableTest extends FATServletClient {

    private static final String APP_NAME = "DisableEnable";
    private static final String SERVER_NAME = "CDIFaultTolerance";

    @Server(SERVER_NAME)
    @TestServlet(servlet = DisableEnableServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new FeatureReplacementAction("mpFaultTolerance-1.1").removeFeature("mpFaultTolerance-1.0").forServers(SERVER_NAME))
                    .andWith(new FeatureReplacementAction("mpFaultTolerance-1.0").removeFeature("mpFaultTolerance-1.1").forServers(SERVER_NAME));

    @BeforeClass
    public static void setUp() throws Exception {
        StringBuilder config = new StringBuilder();
        config.append("com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClient/Retry/enabled=false\n");
        config.append("com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClient/failWithOneRetry/Retry/enabled=true\n");
        config.append("com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement.DisableEnableClassAnnotatedClient/Retry/enabled=false\n");

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(DisableEnableServlet.class, DisableEnableClient.class, ConnectException.class)
                        .addAsResource(new StringAsset(config.toString()), "META-INF/microprofile-config.properties")
                        .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        server.stopServer();
        server.removeDropinsApplications(APP_NAME + ".war");
    }

}
