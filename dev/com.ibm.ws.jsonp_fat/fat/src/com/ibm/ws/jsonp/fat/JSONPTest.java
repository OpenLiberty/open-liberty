/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsonp.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jsonp.app.standalone.web.JSONPStandaloneServlet;

/**
 * Runs tests in an application that only test the JSONP API.
 * Great for checking for regressions in the API or exploiting vulnerabilities.
 * Similar tests could be run outside of the application server (standalone)
 */
@RunWith(FATRunner.class)
public class JSONPTest extends FATServletClient {

    public static final String APP_JSONP = "jsonp";

    @Server("jsonp.fat")
    @TestServlet(servlet = JSONPStandaloneServlet.class, path = APP_JSONP + "/JSONPStandaloneServlet")
    public static LibertyServer server;

    // This test is really fast, okay to run all repeats in lite mode
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() // EE7 - jsonp-1.0
                    .andWith(FeatureReplacementAction.EE8_FEATURES()) // EE8 - jsonp-1.1
                    .andWith(FeatureReplacementAction.EE9_FEATURES()) // EE9 - jsonp-2.0
                    .andWith(FeatureReplacementAction.EE10_FEATURES()) // EE10 - jsonp-2.1
                    .andWith(FeatureReplacementAction.EE11_FEATURES()); // EE11 - jsonp-2.1 (no change)

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive jsonpWar = ShrinkWrap.create(WebArchive.class, APP_JSONP + ".war")
                        .addPackage("jsonp.app.standalone.web");
        ShrinkHelper.exportDropinAppToServer(server, jsonpWar);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}