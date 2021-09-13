/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config13.configOrdinalServerXMLWebApp.web.ConfigOrdinalServerXMLServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that server.xml config sources respect config_ordinal
 * <p>
 * The following config sources should be present with the following ordinals:
 * <ul>
 * <li>microprofile-config.properties (100)
 * <li>server.xml default variables(120)
 * <li>server.xml variables (150)
 * <li>environment variables (300)
 * <li>server.xml appProperties (350)
 * <li>system properties (400)
 * </ul>
 * The following keys are defined.
 * <ul>
 * <li>key_config_props
 * <li>key_serverxml_vars
 * <li>key_serverxml_default_vars
 * <li>key_env_vars
 * <li>key_serverxml_app_props
 * <li>key_system_props
 * </ul>
 *
 * Each config source defines a property for itself and a property for each config source of a higher ordinal, with the value always being the name of the config source.
 * <p>
 * E.g. the following environment variables are defined:
 *
 * <pre>
 * key_env_vars=env_vars
 * key_serverxml_app_props=env_vars
 * key_system_props=env_vars
 * </pre>
 *
 * <p>
 * In this way, it's sufficient to test that {@code key_foo == foo} for each key name
 */
@RunWith(FATRunner.class)
public class ConfigOrdinalServerXMLTest extends FATServletClient {

    public static final String APP_NAME = "configOrdinalServerXMLApp";
    public static final String SERVER_NAME = "ServerXMLConfigOrdinalServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = ConfigOrdinalServerXMLServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP50, MicroProfileActions.MP41, MicroProfileActions.MP20);

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultApp(server, APP_NAME, options, "com.ibm.ws.microprofile.config13.configOrdinalServerXMLWebApp.*");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
