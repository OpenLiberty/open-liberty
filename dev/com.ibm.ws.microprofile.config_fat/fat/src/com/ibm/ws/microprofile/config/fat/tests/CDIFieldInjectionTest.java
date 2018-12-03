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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.cdi.web.FieldTestServlet;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig11EE8;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfig13EE8;
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
public class CDIFieldInjectionTest extends FATServletClient {

    public static final String APP_NAME = "cdiConfig";

    @Server("CDIConfigServer")
    @TestServlet(servlet = FieldTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = SharedShrinkWrapApps.cdiConfigServerApps();

        ShrinkHelper.exportDropinAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatConfig11EE8("CDIConfigServer"))
                    .andWith(new RepeatConfig13EE8("CDIConfigServer"))
                    .andWith(new RepeatConfig14EE8("CDIConfigServer"));

}
