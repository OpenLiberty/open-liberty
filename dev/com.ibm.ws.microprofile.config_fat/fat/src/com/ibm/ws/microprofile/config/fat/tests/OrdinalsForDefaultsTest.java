/*******************************************************************************
* Copyright (c) 2016, 2018 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.ordForDefaults.test.OrdinalsForDefaultsTestServlet;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig14EE8;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class OrdinalsForDefaultsTest extends FATServletClient {

    public static final String APP_NAME = "ordForDefaults";

    @ClassRule
    public static RepeatTests r = RepeatTests //selected combinations
                    .with(new RepeatConfig14EE8("OrdForDefaultsServer"));

    @Server("OrdForDefaultsServer")
    @TestServlet(servlet = OrdinalsForDefaultsTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ordForDefaults_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.ordForDefaults.test")
                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                               "microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, ordForDefaults_war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
