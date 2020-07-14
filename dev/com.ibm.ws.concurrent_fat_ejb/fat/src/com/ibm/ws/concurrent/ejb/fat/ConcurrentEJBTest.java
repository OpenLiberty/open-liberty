/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.ejb.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.ConcurrentFATServlet;

/**
 * Tests for EE Concurrency Utilities, including tests that make updates to the server
 * configuration while the server is running.
 * A setUpPerTest method runs before each test to restore to the original configuration,
 * so that tests do not interfere with eachother.
 */
@RunWith(FATRunner.class)
public class ConcurrentEJBTest extends FATServletClient {

    public static final String APP_NAME = "concurrent";

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .withoutModification()
                    .andWith(new JakartaEE9Action());

    @Server("com.ibm.ws.concurrent.fat.ejb")
    @TestServlet(servlet = ConcurrentFATServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        if (config.getFeatureManager().getFeatures().contains("servlet-5.0") && config.getFeatureManager().getFeatures().remove("ejblite-3.2")) {
            config.getFeatureManager().getFeatures().add("ejbLite-4.0");
            config.getFeatureManager().getFeatures().add("jndi-1.0");
        }
        server.updateServerConfiguration(config);
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage("web")
                        .addPackage("ejb")
                        .addAsWebInfResource(new File("test-applications/" + APP_NAME + "/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportToServer(server, "dropins", app);
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR0019E");
    }
}