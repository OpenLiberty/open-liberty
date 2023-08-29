/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.servlet;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;

import javax.servlet.ServletContainerInitializer;

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
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test a failure during startup
 */
@RunWith(FATRunner.class)
public class ServletStartupTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12BasicServer";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE8, EERepeatActions.EE7);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "servletStartup.war")
                                   .addClasses(ServletInitializer.class, DummyServlet.class, DummyBean.class)
                                   .addAsServiceProvider(ServletContainerInitializer.class, ServletInitializer.class);

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY, DISABLE_VALIDATION);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testGracefulStartupFailure() throws Exception {
        // Check app fails to start
        assertNotNull(server.waitForStringInLog("CWWKZ0012I"));;

        // There should not be an NPE
        assertThat(server.findStringsInLogs("NullPointerException"), empty());
    }

}
